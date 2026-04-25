import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'
import { storage } from './storage'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000
})

const publicAuthPaths = [
  '/auth/login',
  '/auth/register',
  '/auth/forgot-password/verify',
  '/auth/forgot-password/reset'
]

const isPublicAuthRequest = (config) => {
  const url = config?.url || ''
  return publicAuthPaths.some(path => url.includes(path))
}

let redirectingToLogin = false
let lastError = { message: '', time: 0 }

const toastError = (message) => {
  const now = Date.now()
  if (lastError.message === message && now - lastError.time < 1200) return
  lastError = { message, time: now }
  ElMessage.error(message)
}

const shouldShowError = (config) => !config?.silentError

const clearSession = () => {
  storage.remove('token')
  storage.remove('userInfo')
  storage.remove('cartItems')
}

const shouldRetry = (error) => {
  const config = error.config || {}
  const method = (config.method || 'get').toLowerCase()
  return method === 'get' && !config.__retried && (!error.response || error.code === 'ECONNABORTED')
}

request.interceptors.request.use(
  config => {
    const token = storage.get('token', '')
    if (token && !isPublicAuthRequest(config)) {
      config.headers = config.headers || {}
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

request.interceptors.response.use(
  response => {
    const res = response.data
    if (res?.code !== undefined && res.code !== 200) {
      const message = res.message || '请求失败，请稍后重试'
      if (shouldShowError(response.config)) toastError(message)
      return Promise.reject(new Error(message))
    }
    return res
  },
  async error => {
    const config = error.config || {}

    if (shouldRetry(error)) {
      config.__retried = true
      return request(config)
    }

    const status = error.response?.status
    const serverMessage = error.response?.data?.message

    if (status === 401) {
      if (isPublicAuthRequest(config)) {
        if (shouldShowError(config)) toastError(serverMessage || '用户名或密码错误')
      } else {
        clearSession()
        if (!redirectingToLogin) {
          redirectingToLogin = true
          if (shouldShowError(config)) toastError('登录状态已过期，请重新登录')
          router.replace({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
            .finally(() => { redirectingToLogin = false })
        }
      }
    } else if (status === 403) {
      if (shouldShowError(config)) toastError(serverMessage || '没有权限访问该资源')
    } else if (error.code === 'ECONNABORTED') {
      if (shouldShowError(config)) toastError('请求超时，请检查网络后重试')
    } else if (!error.response) {
      if (shouldShowError(config)) toastError('暂时无法连接服务器，请稍后再试')
    } else {
      if (shouldShowError(config)) toastError(serverMessage || '请求失败，请稍后重试')
    }
    return Promise.reject(error)
  }
)

export default request
