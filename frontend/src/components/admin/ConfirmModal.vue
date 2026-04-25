<template>
  <Teleport to="body">
    <transition name="modal-fade">
      <div v-if="visible" class="confirm-modal-overlay" @click.self="handleCancel">
        <div class="confirm-modal" :class="sizeClass">
          <div class="confirm-header">
            <div class="confirm-icon" :class="`icon-${type}`">
              <component :is="iconComponent" />
            </div>
            <h3 class="confirm-title">{{ title }}</h3>
          </div>
          <div class="confirm-body">
            <p>{{ message }}</p>
            <p v-if="detail" class="confirm-detail">{{ detail }}</p>
          </div>
          <div class="confirm-footer">
            <button class="btn-cancel" @click="handleCancel" :disabled="loading">
              {{ cancelText }}
            </button>
            <button class="btn-confirm" :class="`btn-${type}`" @click="handleConfirm" :disabled="loading">
              <span v-if="loading" class="loading-spinner"></span>
              <span v-else>{{ confirmText }}</span>
            </button>
          </div>
        </div>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
import { computed } from 'vue'
import { WarningFilled, CircleCheckFilled, CircleCloseFilled, InfoFilled } from '@element-plus/icons-vue'

const props = defineProps({
  visible: Boolean,
  title: { type: String, default: '确认操作' },
  message: { type: String, default: '确定要执行此操作吗？' },
  detail: String,
  type: { type: String, default: 'warning', validator: v => ['warning', 'danger', 'success', 'info'].includes(v) },
  confirmText: { type: String, default: '确定' },
  cancelText: { type: String, default: '取消' },
  loading: { type: Boolean, default: false },
  size: { type: String, default: 'small', validator: v => ['small', 'medium', 'large'].includes(v) }
})

const emit = defineEmits(['update:visible', 'confirm', 'cancel'])

const sizeClass = computed(() => `size-${props.size}`)

const iconComponent = computed(() => {
  const icons = {
    warning: WarningFilled,
    danger: CircleCloseFilled,
    success: CircleCheckFilled,
    info: InfoFilled
  }
  return icons[props.type]
})

const handleConfirm = () => emit('confirm')
const handleCancel = () => emit('update:visible', false)
</script>

<style scoped>
.confirm-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}

.confirm-modal {
  background: #fff;
  border-radius: 16px;
  padding: 28px;
  width: 100%;
  max-width: 400px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.15);
}

.confirm-modal.size-small { max-width: 360px; padding: 24px; }
.confirm-modal.size-large { max-width: 500px; }

.confirm-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.confirm-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.confirm-icon svg {
  width: 24px;
  height: 24px;
}

.confirm-icon.icon-warning { background: #fef3cd; color: #e6a23c; }
.confirm-icon.icon-danger { background: #fde8e8; color: #f56c6c; }
.confirm-icon.icon-success { background: #e8f8ee; color: #67c23a; }
.confirm-icon.icon-info { background: #e8f0fe; color: #409eff; }

.confirm-title {
  font-size: 18px;
  font-weight: 600;
  color: #111;
  margin: 0;
  text-align: center;
}

.confirm-body {
  text-align: center;
  margin-bottom: 24px;
}

.confirm-body p {
  font-size: 14px;
  color: #666;
  margin: 0;
  line-height: 1.6;
}

.confirm-detail {
  font-size: 13px !important;
  color: #999 !important;
  margin-top: 8px !important;
}

.confirm-footer {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.btn-cancel, .btn-confirm {
  padding: 10px 28px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
  min-width: 100px;
}

.btn-cancel {
  background: #f5f5f5;
  color: #666;
}

.btn-cancel:hover:not(:disabled) {
  background: #eee;
}

.btn-confirm {
  color: #fff;
}

.btn-confirm.btn-warning { background: #e6a23c; }
.btn-confirm.btn-warning:hover:not(:disabled) { background: #d59420; }
.btn-confirm.btn-danger { background: #f56c6c; }
.btn-confirm.btn-danger:hover:not(:disabled) { background: #e54545; }
.btn-confirm.btn-success { background: #67c23a; }
.btn-confirm.btn-success:hover:not(:disabled) { background: #5daf34; }
.btn-confirm.btn-info { background: #409eff; }
.btn-confirm.btn-info:hover:not(:disabled) { background: #337ecc; }

.btn-confirm:disabled, .btn-cancel:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.loading-spinner {
  display: inline-block;
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.modal-fade-enter-active { animation: modal-in 0.25s ease-out; }
.modal-fade-leave-active { animation: modal-out 0.2s ease-in; }

@keyframes modal-in {
  from { opacity: 0; transform: scale(0.95); }
  to { opacity: 1; transform: scale(1); }
}

@keyframes modal-out {
  from { opacity: 1; transform: scale(1); }
  to { opacity: 0; transform: scale(0.95); }
}
</style>
