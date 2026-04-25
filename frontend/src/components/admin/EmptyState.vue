<template>
  <div class="empty-state">
    <div class="empty-illustration">
      <svg viewBox="0 0 120 120" fill="none" xmlns="http://www.w3.org/2000/svg">
        <circle cx="60" cy="60" r="50" fill="#f5f5f3"/>
        <rect x="30" y="40" width="60" height="8" rx="4" fill="#e5e7eb"/>
        <rect x="30" y="56" width="40" height="6" rx="3" fill="#eaeaea"/>
        <rect x="30" y="70" width="50" height="6" rx="3" fill="#eaeaea"/>
        <circle cx="60" cy="85" r="4" fill="#d1d5db"/>
      </svg>
    </div>
    <h3 class="empty-title">{{ title }}</h3>
    <p class="empty-desc">{{ description }}</p>
    <slot />
  </div>
</template>

<script setup>
defineProps({
  title: { type: String, default: '暂无数据' },
  description: { type: String, default: '当前没有数据，请稍后再试' }
})
</script>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
}

.empty-illustration {
  width: 120px;
  height: 120px;
  margin-bottom: 20px;
  opacity: 0.9;
}

.empty-illustration svg {
  width: 100%;
  height: 100%;
}

.empty-title {
  font-size: 16px;
  font-weight: 600;
  color: #374151;
  margin: 0 0 8px;
}

.empty-desc {
  font-size: 14px;
  color: #9ca3af;
  margin: 0 0 20px;
  max-width: 280px;
}
</style>
