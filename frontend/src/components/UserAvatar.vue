<template>
  <div class="user-avatar" :style="wrapStyle">
    <img v-if="src && !imgError" :src="src" :alt="name" @error="imgError = true" />
    <span v-else class="avatar-letter">{{ letter }}</span>
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
const letter = computed(() => props.name?.charAt(0)?.toUpperCase() || 'U')
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
.avatar-letter {
  font-weight: 700;
  line-height: 1;
  user-select: none;
}
</style>
