<template>
  <transition name="fade">
    <button v-if="visible" class="back-to-top" @click="scrollToTop" aria-label="回到顶部">
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
        <polyline points="18 15 12 9 6 15"></polyline>
      </svg>
    </button>
  </transition>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const visible = ref(false)
let ticking = false

const handleScroll = () => {
  if (!ticking) {
    requestAnimationFrame(() => {
      visible.value = window.scrollY > 300
      ticking = false
    })
    ticking = true
  }
}

const scrollToTop = () => {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(() => window.addEventListener('scroll', handleScroll, { passive: true }))
onUnmounted(() => window.removeEventListener('scroll', handleScroll))
</script>

<style scoped>
.back-to-top {
  position: fixed; bottom: 40px; right: 24px; width: 44px; height: 44px;
  background: #111; color: #fff; border-radius: 12px; cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 4px 16px rgba(0,0,0,0.12); z-index: 999;
  transition: all 0.2s; border: none;
}
.back-to-top:hover { background: #333; transform: translateY(-2px); box-shadow: 0 6px 20px rgba(0,0,0,0.16); }
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s, transform 0.3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; transform: translateY(8px); }
</style>
