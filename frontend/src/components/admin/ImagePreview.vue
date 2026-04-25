<template>
  <div class="image-preview-overlay" @click.self="$emit('close')">
    <button class="close-btn" @click="$emit('close')">
      <Close />
    </button>
    <button class="nav-btn prev" @click="prev" v-if="images.length > 1">
      <ArrowLeft />
    </button>
    <div class="preview-container">
      <img :src="currentImage" :alt="`图片 ${currentIndex + 1}`" @load="onLoad" @error="onError" />
      <div class="preview-info" v-if="images.length > 1">
        {{ currentIndex + 1 }} / {{ images.length }}
      </div>
    </div>
    <button class="nav-btn next" @click="next" v-if="images.length > 1">
      <ArrowRight />
    </button>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { Close, ArrowLeft, ArrowRight } from '@element-plus/icons-vue'

const props = defineProps({
  images: { type: Array, default: () => [] },
  initialIndex: { type: Number, default: 0 }
})

const emit = defineEmits(['close'])

const currentIndex = ref(props.initialIndex)
const currentImage = computed(() => props.images[currentIndex.value] || '')

watch(() => props.initialIndex, v => { currentIndex.value = v })

const prev = () => {
  currentIndex.value = currentIndex.value > 0 ? currentIndex.value - 1 : props.images.length - 1
}

const next = () => {
  currentIndex.value = currentIndex.value < props.images.length - 1 ? currentIndex.value + 1 : 0
}

const onLoad = () => {}
const onError = () => {}

const handleKeydown = (e) => {
  if (e.key === 'Escape') emit('close')
  else if (e.key === 'ArrowLeft') prev()
  else if (e.key === 'ArrowRight') next()
}

onMounted(() => window.addEventListener('keydown', handleKeydown))
onUnmounted(() => window.removeEventListener('keydown', handleKeydown))
</script>

<style scoped>
.image-preview-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.9);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 3000;
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.close-btn {
  position: absolute;
  top: 20px;
  right: 20px;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: rgba(255,255,255,0.1);
  border: none;
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
  z-index: 10;
}

.close-btn svg {
  width: 20px;
  height: 20px;
}

.close-btn:hover { background: rgba(255,255,255,0.2); }

.nav-btn {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: rgba(255,255,255,0.1);
  border: none;
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}

.nav-btn svg {
  width: 24px;
  height: 24px;
}

.nav-btn:hover { background: rgba(255,255,255,0.2); }
.nav-btn.prev { left: 20px; }
.nav-btn.next { right: 20px; }

.preview-container {
  max-width: 90vw;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.preview-container img {
  max-width: 100%;
  max-height: calc(90vh - 40px);
  object-fit: contain;
  border-radius: 8px;
  box-shadow: 0 10px 40px rgba(0,0,0,0.5);
}

.preview-info {
  color: rgba(255,255,255,0.7);
  font-size: 14px;
  margin-top: 16px;
}
</style>
