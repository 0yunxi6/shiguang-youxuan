<template>
  <div class="not-found">
    <div class="content">
      <div class="error-code">404</div>
      <h2>页面走丢了</h2>
      <p>你访问的页面不存在或已被移除</p>
      <p class="countdown-text">{{ seconds }} 秒后自动返回首页</p>
      <button class="btn-home" @click="$router.push('/')">返回首页</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const seconds = ref(10)
let timer = null

onMounted(() => {
  timer = setInterval(() => {
    seconds.value--
    if (seconds.value <= 0) {
      clearInterval(timer)
      router.push('/')
    }
  }, 1000)
})
onUnmounted(() => clearInterval(timer))
</script>

<style scoped>
.not-found {
  display: flex; align-items: center; justify-content: center;
  min-height: 60vh; padding: 40px 24px;
}
.content { text-align: center; animation: fadeIn 0.4s ease; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(12px); } to { opacity: 1; transform: translateY(0); } }

.error-code {
  font-size: 120px; font-weight: 800; color: #f0f0f0;
  line-height: 1; letter-spacing: -4px; margin-bottom: 8px;
  background: linear-gradient(135deg, #e0e0e0 0%, #f5f5f5 100%);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent;
  background-clip: text;
}
.content h2 { font-size: 22px; font-weight: 700; color: #111; margin: 0 0 8px; }
.content p { font-size: 15px; color: #999; margin: 0 0 32px; }
.btn-home {
  padding: 10px 28px; border-radius: 10px; border: none;
  background: #111; color: #fff; font-size: 14px; font-weight: 600;
  cursor: pointer; transition: all 0.2s;
}
.btn-home:hover { background: #333; transform: translateY(-1px); }
.countdown-text { font-size: 13px; color: #bbb; margin: 0 0 20px; }
</style>
