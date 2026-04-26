<template>
  <div class="user-avatar" :style="wrapStyle">
    <img :src="avatarSrc" :alt="name || '默认头像'" @error="imgError = true" />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  src: { type: String, default: '' },
  name: { type: String, default: '' },
  size: { type: Number, default: 32 }
})

const imgError = ref(false)
const avatarSrc = computed(() => (props.src && !imgError.value) ? props.src : '/default-avatar.png')
const wrapStyle = computed(() => ({
  width: props.size + 'px',
  height: props.size + 'px',
  fontSize: Math.max(props.size * 0.4, 12) + 'px'
}))
</script>

<style scoped>
.user-avatar {
  border-radius: 50%;
  background: #1a1a1a;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
}
.user-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
</style>
