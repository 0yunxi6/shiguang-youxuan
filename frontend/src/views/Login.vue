<template>
  <div class="login-page" :class="[`cloud-${cloudState}`]" @mousemove="handleMouseMove" @mouseleave="resetEyeOffset">
    <div class="login-card">
      <div class="cloud-panel">
        <div class="cloud-scene">
          <div class="cloud-orbit orbit-1"></div>
          <div class="cloud-orbit orbit-2"></div>
          <div class="cloud-shadow"></div>

          <div class="cloud-mascot">
            <div class="cloud-body">
              <span class="cloud-bump bump-1"></span>
              <span class="cloud-bump bump-2"></span>
              <span class="cloud-bump bump-3"></span>
              <span class="cloud-bump bump-4"></span>

              <div class="cloud-face normal-face">
                <span class="eye-socket left"><span class="eye-pupil" :style="pupilStyle"></span></span>
                <span class="eye-socket right"><span class="eye-pupil" :style="pupilStyle"></span></span>
                <span class="eyebrow left"></span>
                <span class="eyebrow right"></span>
                <span class="blush left"></span>
                <span class="blush right"></span>
                <span class="mouth"></span>
                <span class="sparkle sparkle-1"></span>
                <span class="sparkle sparkle-2"></span>
              </div>

              <div class="cloud-face sad-face">
                <span class="eye-socket left"><span class="eye-pupil"></span></span>
                <span class="eye-socket right"><span class="eye-pupil"></span></span>
                <span class="blush left"></span>
                <span class="blush right"></span>
                <span class="mouth"></span>
              </div>

              <div class="cloud-arm arm-left"></div>
              <div class="cloud-arm arm-right"></div>
              <div class="peek-line"></div>
            </div>
          </div>

          <div class="cloud-text">
            <h2>云朵精灵陪你登录</h2>
          </div>
        </div>
      </div>

      <div class="login-panel">
        <div class="login-header">
          <div class="brand-badge">拾</div>
          <div>
            <h1>{{ pageTitle }}</h1>
            <p>{{ pageSubtitle }}</p>
          </div>
        </div>

        <form class="login-form" @submit.prevent="handleSubmit">
          <div class="form-group">
            <label>账号</label>
            <div class="input-wrap" :class="{ active: activeField === 'username' }">
              <el-icon class="input-icon"><User /></el-icon>
              <input
                v-model="form.username"
                type="text"
                placeholder="请输入用户名"
                required
                minlength="3"
                maxlength="20"
                @focus="activeField = 'username'"
                @blur="handleFieldBlur"
              />
            </div>
          </div>

          <template v-if="mode === 'login' || mode === 'register'">
            <div class="form-group">
              <div class="field-row">
                <label>密码</label>
                <button v-if="mode === 'login'" type="button" class="text-link" @click="switchMode('forgot')">忘记密码？</button>
              </div>
              <div class="input-wrap" :class="{ active: activeField === 'password' || activeField === 'confirmPassword' || activeField === 'resetPassword' || activeField === 'resetConfirmPassword' }">
                <el-icon class="input-icon"><Lock /></el-icon>
                <input
                  v-model="form.password"
                  :type="showPwd ? 'text' : 'password'"
                  placeholder="请输入密码"
                  required
                  minlength="6"
                  @focus="activeField = 'password'"
                  @blur="handleFieldBlur"
                />
                <button type="button" class="pwd-toggle" @click="showPwd = !showPwd">
                  <el-icon><View v-if="!showPwd" /><Hide v-else /></el-icon>
                </button>
              </div>
              <div v-if="mode === 'register'" class="pwd-strength">
                <div class="strength-bars">
                  <span :class="{ active: pwdStrength >= 1, weak: pwdStrength === 1, medium: pwdStrength === 2, strong: pwdStrength >= 3 }"></span>
                  <span :class="{ active: pwdStrength >= 2, medium: pwdStrength === 2, strong: pwdStrength >= 3 }"></span>
                  <span :class="{ active: pwdStrength >= 3, strong: pwdStrength >= 3 }"></span>
                </div>
                <span class="strength-text">{{ pwdStrengthText }}</span>
              </div>
            </div>
          </template>

          <template v-if="mode === 'register'">
            <div class="register-panel">
              <div class="register-panel-title">完善注册信息</div>

              <div class="form-group">
                <label>确认密码</label>
                <div class="input-wrap" :class="{ active: activeField === 'confirmPassword' }">
                  <el-icon class="input-icon"><Lock /></el-icon>
                  <input
                    v-model="form.confirmPassword"
                    :type="showPwd ? 'text' : 'password'"
                    placeholder="请再次输入密码"
                    required
                    @focus="activeField = 'confirmPassword'"
                    @blur="handleFieldBlur"
                  />
                </div>
              </div>

              <div class="form-row">
                <div class="form-group compact">
                  <label>邮箱</label>
                  <div class="input-wrap">
                    <el-icon class="input-icon"><Message /></el-icon>
                    <input v-model="form.email" type="email" placeholder="your@email.com" required />
                  </div>
                </div>
                <div class="form-group compact">
                  <label>手机号</label>
                  <div class="input-wrap">
                    <el-icon class="input-icon"><Phone /></el-icon>
                    <input v-model="form.phone" type="tel" placeholder="13800138000" required pattern="^1[3-9]\d{9}$" />
                  </div>
                </div>
              </div>
            </div>
          </template>

          <template v-if="mode === 'forgot'">
            <div v-if="forgotStep === 1" class="register-panel">
              <div class="register-panel-title">身份验证</div>
              <div class="form-group">
                <label>邮箱</label>
                <div class="input-wrap" :class="{ active: activeField === 'email' }">
                  <el-icon class="input-icon"><Message /></el-icon>
                  <input
                    v-model="form.email"
                    type="email"
                    placeholder="请输入注册邮箱"
                    required
                    @focus="activeField = 'email'"
                    @blur="handleFieldBlur"
                  />
                </div>
              </div>
            </div>

            <div v-else class="register-panel">
              <div class="register-panel-title">重置密码</div>
              <div class="form-group">
                <label>新密码</label>
                <div class="input-wrap" :class="{ active: activeField === 'resetPassword' }">
                  <el-icon class="input-icon"><Lock /></el-icon>
                  <input
                    v-model="form.resetPassword"
                    :type="showPwd ? 'text' : 'password'"
                    placeholder="请输入新密码"
                    required
                    minlength="6"
                    @focus="activeField = 'resetPassword'"
                    @blur="handleFieldBlur"
                  />
                  <button type="button" class="pwd-toggle" @click="showPwd = !showPwd">
                    <el-icon><View v-if="!showPwd" /><Hide v-else /></el-icon>
                  </button>
                </div>
              </div>

              <div class="form-group">
                <label>确认新密码</label>
                <div class="input-wrap" :class="{ active: activeField === 'resetConfirmPassword' }">
                  <el-icon class="input-icon"><Lock /></el-icon>
                  <input
                    v-model="form.resetConfirmPassword"
                    :type="showPwd ? 'text' : 'password'"
                    placeholder="请再次输入新密码"
                    required
                    @focus="activeField = 'resetConfirmPassword'"
                    @blur="handleFieldBlur"
                  />
                </div>
              </div>
            </div>
          </template>

          <p v-if="errorMsg" class="status-msg error-msg">{{ errorMsg }}</p>
          <p v-if="successMsg" class="status-msg success-msg">{{ successMsg }}</p>

          <button type="submit" class="submit-btn" :disabled="loading">
            <span v-if="loading" class="spinner"></span>
            {{ submitLabel }}
          </button>
        </form>

        <div class="auth-switch">
          <template v-if="mode === 'login'">
            还没有账号？
            <a href="#" @click.prevent="switchMode('register')">去注册</a>
          </template>
          <template v-else-if="mode === 'register'">
            已有账号？
            <a href="#" @click.prevent="switchMode('login')">去登录</a>
          </template>
          <template v-else>
            想起密码了？
            <a href="#" @click.prevent="switchMode('login')">返回登录</a>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { login, register, forgotPasswordVerify, forgotPasswordReset } from '../api'
import { useUserStore } from '../store/user'
import { User, Lock, View, Hide, Message, Phone } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const mode = ref('login')
const forgotStep = ref(1)
const loading = ref(false)
const errorMsg = ref('')
const successMsg = ref('')
const showPwd = ref(false)
const activeField = ref('')
const loginResult = ref('idle')
const resetToken = ref('')
const eyeOffset = reactive({ x: 0, y: 0 })
const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  email: '',
  phone: '',
  resetPassword: '',
  resetConfirmPassword: ''
})

const pageTitle = computed(() => {
  if (mode.value === 'register') return '创建账号'
  if (mode.value === 'forgot') return forgotStep.value === 1 ? '找回密码' : '设置新密码'
  return '欢迎回来'
})

const pageSubtitle = computed(() => {
  if (mode.value === 'register') return '注册后即可开始你的优选生活'
  if (mode.value === 'forgot') return forgotStep.value === 1 ? '输入用户名和注册邮箱验证身份' : '设置新密码后即可重新登录'
  return '登录账号，继续拾取美好'
})

const submitLabel = computed(() => {
  if (mode.value === 'register') return '注册'
  if (mode.value === 'forgot') return forgotStep.value === 1 ? '验证身份' : '重置密码'
  return '登录'
})

const cloudState = computed(() => {
  if (loading.value) return 'loading'
  if (loginResult.value === 'success') return 'success'
  if (loginResult.value === 'error') return 'error'
  if (activeField.value === 'username' || activeField.value === 'email') return 'username'
  if (['password', 'confirmPassword', 'resetPassword', 'resetConfirmPassword'].includes(activeField.value)) return 'password'
  return 'idle'
})

const pwdStrength = computed(() => {
  const pwd = form.password
  if (!pwd) return 0
  let score = 0
  if (pwd.length >= 6) score++
  if (/[A-Z]/.test(pwd) && /[a-z]/.test(pwd)) score++
  if (/\d/.test(pwd) && /[^a-zA-Z0-9]/.test(pwd)) score++
  return score
})
const pwdStrengthText = computed(() => ['', '弱', '中', '强'][pwdStrength.value] || '')

const pupilStyle = computed(() => ({
  transform: `translate(${eyeOffset.x}px, ${eyeOffset.y}px)`
}))

let mouseTicking = false
const handleMouseMove = (event) => {
  if (mouseTicking) return
  mouseTicking = true
  requestAnimationFrame(() => {
    const { innerWidth, innerHeight } = window
    const dx = (event.clientX - innerWidth / 2) / innerWidth
    const dy = (event.clientY - innerHeight / 2) / innerHeight
    eyeOffset.x = Math.max(-1, Math.min(1, dx)) * 8
    eyeOffset.y = Math.max(-1, Math.min(1, dy)) * 6
    mouseTicking = false
  })
}

const resetEyeOffset = () => {
  eyeOffset.x = 0
  eyeOffset.y = 0
}

const handleFieldBlur = () => {
  activeField.value = ''
}

const resetFeedbackState = () => {
  errorMsg.value = ''
  successMsg.value = ''
  loginResult.value = 'idle'
}

const resetForgotState = () => {
  forgotStep.value = 1
  resetToken.value = ''
  form.resetPassword = ''
  form.resetConfirmPassword = ''
}

const switchMode = (nextMode) => {
  mode.value = nextMode
  activeField.value = ''
  showPwd.value = false
  resetFeedbackState()
  if (nextMode !== 'forgot') resetForgotState()
}

const handleSubmit = async () => {
  resetFeedbackState()

  if (mode.value === 'register' && form.password !== form.confirmPassword) {
    errorMsg.value = '两次输入的密码不一致'
    loginResult.value = 'error'
    return
  }

  if (mode.value === 'forgot' && forgotStep.value === 2 && form.resetPassword !== form.resetConfirmPassword) {
    errorMsg.value = '两次输入的新密码不一致'
    loginResult.value = 'error'
    return
  }

  loading.value = true
  try {
    if (mode.value === 'register') {
      await register({
        username: form.username,
        password: form.password,
        email: form.email,
        phone: form.phone
      })
      successMsg.value = '注册成功，请登录'
      loginResult.value = 'success'
      mode.value = 'login'
      form.password = ''
      form.confirmPassword = ''
    } else if (mode.value === 'forgot') {
      if (forgotStep.value === 1) {
        const res = await forgotPasswordVerify({ username: form.username, email: form.email })
        resetToken.value = res.data.resetToken
        forgotStep.value = 2
        successMsg.value = '身份验证成功，请设置新密码'
        loginResult.value = 'success'
      } else {
        await forgotPasswordReset({ resetToken: resetToken.value, newPassword: form.resetPassword })
        successMsg.value = '密码重置成功，请重新登录'
        loginResult.value = 'success'
        form.password = ''
        resetForgotState()
        mode.value = 'login'
      }
    } else {
      const res = await login({ username: form.username, password: form.password })
      userStore.setToken(res.data.token)
      if (res.data.user) userStore.setUserInfo(res.data.user)
      loginResult.value = 'success'
      setTimeout(() => {
        router.push(route.query.redirect || '/')
      }, 650)
    }
  } catch (e) {
    errorMsg.value = e.response?.data?.message || (mode.value === 'register' ? '注册失败' : mode.value === 'forgot' ? '操作失败，请稍后重试' : '用户名或密码错误')
    loginResult.value = 'error'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px;
  background:
    radial-gradient(circle at top left, rgba(255, 235, 238, 0.92), transparent 34%),
    radial-gradient(circle at bottom right, rgba(79, 195, 247, 0.16), transparent 32%),
    linear-gradient(135deg, rgba(224, 247, 250, 0.92) 0%, rgba(248, 253, 255, 0.78) 52%, rgba(255, 235, 238, 0.88) 100%);
}

.login-card {
  width: 100%;
  max-width: 1100px;
  display: grid;
  grid-template-columns: minmax(360px, 1.08fr) minmax(0, 0.92fr);
  background: rgba(255, 255, 255, 0.46);
  border: 1px solid rgba(255, 255, 255, 0.62);
  border-radius: 24px;
  box-shadow: 0 24px 60px rgba(79, 195, 247, 0.14);
  overflow: hidden;
  backdrop-filter: blur(18px);
}

.cloud-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 34px;
  background:
    radial-gradient(circle at 24% 18%, rgba(255, 255, 255, 0.62), transparent 24%),
    linear-gradient(180deg, rgba(224, 247, 250, 0.46) 0%, rgba(255, 235, 238, 0.34) 100%);
}

.cloud-scene {
  width: 100%;
  min-height: 520px;
  border-radius: 30px;
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.42);
  overflow: hidden;
}

.cloud-orbit {
  position: absolute;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.26);
  filter: blur(2px);
}

.orbit-1 {
  width: 160px;
  height: 160px;
  top: 46px;
  left: 36px;
}

.orbit-2 {
  width: 120px;
  height: 120px;
  right: 42px;
  bottom: 62px;
}

.cloud-shadow {
  position: absolute;
  bottom: 124px;
  width: 204px;
  height: 26px;
  border-radius: 50%;
  background: rgba(66, 66, 66, 0.08);
  filter: blur(5px);
  transition: transform 0.45s ease, opacity 0.45s ease;
}

.cloud-mascot {
  position: relative;
  width: 270px;
  height: 220px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.45s ease;
}

.cloud-body {
  position: relative;
  width: 208px;
  height: 122px;
  background: rgba(255, 255, 255, 0.96);
  border-radius: 64px;
  box-shadow: 0 18px 34px rgba(79, 195, 247, 0.16);
  transition: transform 0.45s ease, border-radius 0.45s ease, box-shadow 0.45s ease;
}

.cloud-bump {
  position: absolute;
  background: rgba(255, 255, 255, 0.98);
  border-radius: 50%;
}

.bump-1 { width: 62px; height: 62px; left: 12px; top: -18px; }
.bump-2 { width: 84px; height: 84px; left: 52px; top: -36px; }
.bump-3 { width: 66px; height: 66px; right: 38px; top: -24px; }
.bump-4 { width: 50px; height: 50px; right: 10px; top: -10px; }

.cloud-face {
  position: absolute;
  inset: 0;
  transition: opacity 0.3s ease;
}

.sad-face {
  opacity: 0;
}

.eye-socket {
  position: absolute;
  top: 44px;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  transition: transform 0.35s ease, height 0.35s ease, width 0.35s ease;
}

.eye-socket.left { left: 72px; }
.eye-socket.right { right: 72px; }

.eye-pupil {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #424242;
  transition: transform 0.16s ease-out, width 0.35s ease, height 0.35s ease;
}

.eyebrow {
  position: absolute;
  top: 34px;
  width: 18px;
  height: 6px;
  border-top: 2px solid rgba(66, 66, 66, 0.28);
  border-radius: 50%;
  transition: transform 0.35s ease, opacity 0.35s ease;
}

.eyebrow.left { left: 70px; }
.eyebrow.right { right: 70px; }

.blush {
  position: absolute;
  top: 64px;
  width: 22px;
  height: 12px;
  background: rgba(255, 190, 201, 0.76);
  border-radius: 50%;
  transition: opacity 0.35s ease, transform 0.35s ease;
}

.blush.left { left: 42px; }
.blush.right { right: 42px; }

.mouth {
  position: absolute;
  left: 50%;
  top: 74px;
  width: 24px;
  height: 12px;
  border: 3px solid #424242;
  border-top: 0;
  border-radius: 0 0 18px 18px;
  transform: translateX(-50%);
  transition: all 0.35s ease;
}

.sad-face .mouth {
  top: 78px;
  border-top: 3px solid #424242;
  border-bottom: 0;
  border-radius: 18px 18px 0 0;
}

.sparkle {
  position: absolute;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: rgba(79, 195, 247, 0.45);
}

.sparkle-1 { top: 30px; right: 48px; }
.sparkle-2 { top: 54px; left: 48px; width: 4px; height: 4px; }

.cloud-arm {
  position: absolute;
  top: 62px;
  width: 20px;
  height: 58px;
  background: rgba(255, 255, 255, 0.96);
  border-radius: 16px;
  box-shadow: 0 8px 16px rgba(79, 195, 247, 0.1);
  transition: transform 0.4s ease, top 0.4s ease, left 0.4s ease, right 0.4s ease;
}

.arm-left {
  left: 18px;
  transform: rotate(18deg);
}

.arm-right {
  right: 18px;
  transform: rotate(-18deg);
}

.peek-line {
  position: absolute;
  right: -16px;
  top: 56px;
  width: 20px;
  height: 3px;
  border-radius: 999px;
  background: rgba(79, 195, 247, 0.22);
  opacity: 0;
  transition: opacity 0.35s ease, transform 0.35s ease;
}

.cloud-text {
  margin-top: 28px;
  text-align: center;
  color: #607d8b;
  max-width: 340px;
}

.cloud-text h2 {
  margin: 0;
  font-size: 32px;
  color: #424242;
  font-weight: 800;
}

.login-panel {
  padding: 46px 38px;
  background: rgba(255, 255, 255, 0.3);
}

.login-header {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 28px;
}

.brand-badge {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #1a1a1a;
  color: #fff;
  font-size: 20px;
  font-weight: 800;
  box-shadow: 0 10px 22px rgba(26, 26, 26, 0.18);
}

.login-header h1 {
  margin: 0 0 4px;
  font-size: 30px;
  color: #424242;
}

.login-header p {
  margin: 0;
  font-size: 14px;
  color: #78909c;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-group label {
  font-size: 14px;
  font-weight: 600;
  color: #424242;
}

.field-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.register-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.42);
  border: 1px solid rgba(255, 255, 255, 0.55);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.5);
}

.register-panel-title {
  font-size: 13px;
  font-weight: 700;
  color: #29b6f6;
  letter-spacing: 0.04em;
}

.form-group.compact label {
  font-size: 13px;
}

.input-wrap {
  display: flex;
  align-items: center;
  min-height: 54px;
  border: 1px solid #eeeeee;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.8);
  transition: border-color 0.25s ease, box-shadow 0.25s ease, transform 0.25s ease, background 0.25s ease;
}

.input-wrap.active,
.input-wrap:focus-within {
  border-color: #4fc3f7;
  box-shadow: 0 0 0 4px rgba(79, 195, 247, 0.12);
  transform: translateY(-1px);
  background: rgba(255, 255, 255, 0.96);
}

.input-icon {
  padding-left: 16px;
  color: #90a4ae;
  font-size: 18px;
  flex-shrink: 0;
}

.input-wrap input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  padding: 14px 14px 14px 10px;
  font-size: 14px;
  color: #424242;
}

.input-wrap input::placeholder {
  color: #b0bec5;
}

.pwd-toggle,
.text-link {
  border: none;
  background: transparent;
  color: #29b6f6;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
}

.pwd-toggle {
  color: #90a4ae;
  padding: 0 16px 0 8px;
  transition: color 0.25s ease, transform 0.25s ease;
}

.pwd-toggle:hover {
  color: #4fc3f7;
  transform: scale(1.06);
}

.pwd-strength {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  padding: 0 4px;
}
.strength-bars {
  display: flex;
  gap: 4px;
}
.strength-bars span {
  width: 40px;
  height: 4px;
  border-radius: 2px;
  background: #e0e0e0;
  transition: background 0.3s;
}
.strength-bars span.active.weak { background: #f56c6c; }
.strength-bars span.active.medium { background: #e6a23c; }
.strength-bars span.active.strong { background: #67c23a; }
.strength-text {
  font-size: 11px;
  font-weight: 600;
  color: #999;
}

.text-link:hover {
  text-decoration: underline;
}

.status-msg {
  margin: 0;
  font-size: 13px;
  line-height: 1.5;
}

.error-msg {
  color: #ef5350;
}

.success-msg {
  color: #26a69a;
}

.submit-btn {
  height: 54px;
  border: none;
  border-radius: 16px;
  background: linear-gradient(135deg, #4fc3f7 0%, #29b6f6 100%);
  color: #fff;
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  box-shadow: 0 12px 22px rgba(79, 195, 247, 0.28);
  transition: transform 0.25s ease, box-shadow 0.25s ease, filter 0.25s ease;
}

.submit-btn:hover:not(:disabled) {
  transform: scale(1.03);
  box-shadow: 0 16px 28px rgba(79, 195, 247, 0.34);
  filter: brightness(1.02);
}

.submit-btn:disabled {
  opacity: 0.72;
  cursor: not-allowed;
}

.spinner {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.36);
  border-top-color: #fff;
  animation: spin 0.7s linear infinite;
}

.auth-switch {
  margin-top: 22px;
  text-align: center;
  color: #78909c;
  font-size: 14px;
}

.auth-switch a {
  margin-left: 4px;
  color: #29b6f6;
  text-decoration: none;
  font-weight: 700;
}

.auth-switch a:hover {
  text-decoration: underline;
}

.cloud-idle .cloud-mascot {
  transform: translateY(0);
}

.cloud-username .cloud-mascot {
  transform: translateX(14px) translateY(-3px);
}

.cloud-username .cloud-body {
  transform: rotate(10deg);
}

.cloud-username .eye-socket.left,
.cloud-username .eye-socket.right {
  transform: translateX(8px);
}

.cloud-username .peek-line {
  opacity: 1;
  transform: translateX(8px);
}

.cloud-password .cloud-mascot {
  transform: translateY(4px);
}

.cloud-password .arm-left {
  top: 36px;
  left: 62px;
  transform: rotate(-30deg);
}

.cloud-password .arm-right {
  top: 36px;
  right: 62px;
  transform: rotate(30deg);
}

.cloud-password .eye-socket {
  width: 15px;
  height: 8px;
  border-radius: 999px;
  transform: translateY(6px);
}

.cloud-password .eye-pupil {
  width: 14px;
  height: 3px;
  border-radius: 999px;
  transform: translate(0, 0) !important;
}

.cloud-password .eyebrow {
  opacity: 0;
}

.cloud-password .mouth {
  width: 16px;
  height: 8px;
}

.cloud-password .blush {
  transform: scale(1.12);
}

.cloud-loading .cloud-mascot {
  transform: translateY(-4px);
}

.cloud-loading .cloud-body {
  transform: scale(1.03);
}

.cloud-success .cloud-mascot {
  animation: cloud-happy-bounce 0.85s ease;
}

.cloud-success .cloud-body {
  transform: translateY(-8px) scale(1.04);
  box-shadow: 0 22px 36px rgba(79, 195, 247, 0.24);
}

.cloud-success .mouth {
  width: 30px;
  height: 15px;
}

.cloud-success .blush {
  transform: scale(1.16);
}

.cloud-success .sparkle {
  background: rgba(79, 195, 247, 0.78);
  transform: scale(1.35);
}

.cloud-error .normal-face {
  opacity: 0;
}

.cloud-error .sad-face {
  opacity: 1;
}

.cloud-error .cloud-mascot {
  transform: translateY(8px);
}

.cloud-error .cloud-body {
  transform: rotate(-5deg);
  border-radius: 60px 60px 68px 68px;
}

.cloud-error .cloud-shadow {
  transform: scaleX(0.92);
  opacity: 0.8;
}

.cloud-error .arm-left {
  transform: rotate(42deg) translateY(6px);
}

.cloud-error .arm-right {
  transform: rotate(-42deg) translateY(6px);
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@keyframes cloud-happy-bounce {
  0% { transform: translateY(0) scale(1); }
  30% { transform: translateY(-14px) scale(1.05); }
  55% { transform: translateY(2px) scale(0.99); }
  78% { transform: translateY(-6px) scale(1.02); }
  100% { transform: translateY(0) scale(1); }
}

@media (max-width: 980px) {
  .login-card {
    grid-template-columns: 1fr;
  }

  .cloud-panel {
    padding: 24px 24px 6px;
  }

  .cloud-scene {
    min-height: 360px;
  }

  .login-panel {
    padding-top: 28px;
  }
}

@media (max-width: 640px) {
  .login-page {
    padding: 16px;
  }

  .cloud-panel,
  .login-panel {
    padding-left: 18px;
    padding-right: 18px;
  }

  .cloud-text h2 {
    font-size: 24px;
  }

  .form-row {
    grid-template-columns: 1fr;
  }

  .cloud-mascot {
    width: 220px;
    height: 190px;
  }

  .cloud-body {
    transform: scale(0.92);
  }
}
</style>
