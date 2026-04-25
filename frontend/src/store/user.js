import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { storage } from '../utils/storage'

export const useUserStore = defineStore('user', () => {
  const token = ref(storage.get('token', ''))
  const userInfo = ref(storage.getJSON('userInfo', null))

  const isLogin = computed(() => !!token.value)

  const setToken = (newToken) => {
    token.value = newToken
    storage.set('token', newToken)
  }

  const setUserInfo = (info) => {
    userInfo.value = info
    storage.setJSON('userInfo', info)
  }

  const logout = () => {
    token.value = ''
    userInfo.value = null
    storage.remove('token')
    storage.remove('userInfo')
  }

  return { token, userInfo, isLogin, setToken, setUserInfo, logout }
})
