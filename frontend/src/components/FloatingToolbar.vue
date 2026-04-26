<template>
  <aside class="floating-toolbar" aria-label="快捷工具栏">
    <button class="tool-item cart" @click="go('/cart')">
      <span v-if="cartCount" class="tool-badge">{{ cartCount > 99 ? '99+' : cartCount }}</span>
      <el-icon><ShoppingCart /></el-icon>
      <span>购物车</span>
    </button>
    <button class="tool-item" @click="go('/profile')">
      <el-icon><User /></el-icon>
      <span>我的</span>
    </button>
    <button class="tool-item" @click="go('/service')">
      <el-icon><Service /></el-icon>
      <span>客服</span>
    </button>
    <button class="tool-item" @click="go('/profile?tab=messages')">
      <el-icon><Bell /></el-icon>
      <span>消息</span>
    </button>
    <button class="tool-item" @click="feedback">
      <el-icon><EditPen /></el-icon>
      <span>反馈</span>
    </button>
  </aside>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ShoppingCart, User, Service, Bell, EditPen } from '@element-plus/icons-vue'
import { useCartStore } from '../store/cart'

const router = useRouter()
const cartStore = useCartStore()
const cartCount = computed(() => cartStore.cartCount)

const go = (path) => router.push(path)
const feedback = () => ElMessage.success('反馈入口已收到，后续可接入工单系统')
</script>

<style scoped>
.floating-toolbar {
  position: fixed;
  right: 18px;
  top: 50%;
  transform: translateY(-50%);
  width: 56px;
  padding: 8px 0;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid #eef0f5;
  border-radius: 18px;
  box-shadow: 0 18px 38px rgba(15, 23, 42, 0.12);
  display: flex;
  flex-direction: column;
  align-items: center;
  z-index: 900;
  overflow: hidden;
}

.tool-item {
  position: relative;
  width: 100%;
  border: none;
  background: transparent;
  color: #374151;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
  padding: 9px 4px;
  cursor: pointer;
  font-size: 12px;
  line-height: 1.2;
  transition: all 0.18s ease;
}

.tool-item svg {
  width: 20px;
  height: 20px;
}

.tool-item:hover {
  background: #f3f7ff;
  color: #1677ff;
}

.tool-badge {
  position: absolute;
  top: 5px;
  right: 11px;
  min-width: 17px;
  height: 17px;
  padding: 0 4px;
  border-radius: 999px;
  background: #ff2d55;
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  line-height: 17px;
}

@media (max-width: 900px) {
  .floating-toolbar {
    display: none;
  }
}
</style>
