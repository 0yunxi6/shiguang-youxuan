<template>
  <div class="order-page">
    <h1 class="page-title">我的订单</h1>

    <div class="status-tabs">
      <button
        v-for="tab in statusTabs"
        :key="tab.value === null ? 'all' : tab.value"
        :class="{ active: currentStatus === tab.value }"
        @click="changeStatus(tab.value)"
      >
        {{ tab.label }}
      </button>
    </div>

    <div v-if="loading" class="order-list">
      <div v-for="i in 3" :key="i" class="order-card">
        <div class="order-header">
          <div class="sk skeleton-pulse" style="width:200px;height:16px;border-radius:4px"></div>
          <div class="sk skeleton-pulse" style="width:60px;height:24px;border-radius:12px"></div>
        </div>
        <div style="padding:16px 20px;display:flex;gap:12px">
          <div class="sk skeleton-pulse" style="width:64px;height:64px;border-radius:8px"></div>
          <div style="flex:1;display:flex;flex-direction:column;gap:8px">
            <div class="sk skeleton-pulse" style="width:50%;height:14px;border-radius:4px"></div>
            <div class="sk skeleton-pulse" style="width:30%;height:14px;border-radius:4px"></div>
          </div>
        </div>
      </div>
    </div>

    <div v-else-if="orders.length === 0" class="empty-state">
      <div class="empty-icon">📋</div>
      <p class="empty-title">暂无订单</p>
      <p class="empty-desc">去挑选一些好物吧</p>
      <button class="btn-shop" @click="$router.push('/')">去逛逛</button>
    </div>

    <div v-else class="order-list">
      <div v-for="order in orders" :key="order.id" class="order-card">
        <div class="order-header">
          <span class="order-no">订单号：{{ order.orderNo }}</span>
          <span :class="['status-tag', `status-${order.status}`]">
            {{ statusTexts[order.status] || '未知' }}
          </span>
        </div>

        <div class="order-items">
          <div v-for="item in order.items" :key="item.id" class="order-item">
            <img :src="item.productImage || '/placeholder.svg'" class="item-thumb" />
            <div class="item-detail">
              <span class="item-name">{{ item.productName }}</span>
              <span class="item-meta">¥{{ item.price }} × {{ item.quantity }}</span>
            </div>
          </div>
        </div>

        <div class="order-footer">
          <div class="order-info">
            <span>{{ order.receiverName }} · {{ order.receiverPhone }}</span>
            <span v-if="order.couponName">优惠：{{ order.couponName }}</span>
            <span v-if="order.paymentMethod">支付方式：{{ paymentText(order.paymentMethod) }}</span>
            <span class="order-time">{{ formatTime(order.createTime) }}</span>
          </div>
          <div class="order-total-wrap">
            <div class="price-breakdown">
              <span v-if="Number(order.originalAmount || 0) > 0">商品 ¥{{ formatAmount(order.originalAmount) }}</span>
              <span v-if="Number(order.discountAmount || 0) > 0" class="discount">优惠 -¥{{ formatAmount(order.discountAmount) }}</span>
              <span>运费 {{ Number(order.shippingFee || 0) === 0 ? '免运费' : '¥' + formatAmount(order.shippingFee) }}</span>
            </div>
            <div class="order-total">
              <span class="total-label">合计</span>
              <span class="total-amount">¥{{ formatAmount(order.totalAmount) }}</span>
            </div>
            <div class="order-actions">
              <button v-if="order.status === 0" class="btn-action pay" :disabled="actionLoadingId === order.id" @click="handlePay(order)">立即支付</button>
              <button v-if="order.canCancel" class="btn-action ghost" :disabled="actionLoadingId === order.id" @click="handleCancel(order)">取消订单</button>
              <button v-if="order.canConfirm" class="btn-action primary" :disabled="actionLoadingId === order.id" @click="handleConfirm(order)">确认收货</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { cancelOrder, confirmOrder, getOrderList, payOrder } from '../api'
import { ElMessage } from 'element-plus'

const orders = ref([])
const loading = ref(true)
const actionLoadingId = ref(null)
const currentStatus = ref(null)

const statusTexts = ['待支付', '已支付', '已发货', '已完成', '已取消']
const statusTabs = [
  { label: '全部', value: null },
  { label: '待支付', value: 0 },
  { label: '待发货', value: 1 },
  { label: '待收货', value: 2 },
  { label: '已完成', value: 3 },
  { label: '已取消', value: 4 }
]

const loadOrders = async () => {
  loading.value = true
  try {
    const params = currentStatus.value === null ? undefined : { status: currentStatus.value }
    const res = await getOrderList(params)
    orders.value = res.data
  } catch (error) { console.error(error) }
  finally { loading.value = false }
}

const changeStatus = (status) => {
  currentStatus.value = status
  loadOrders()
}

const formatTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}

const formatAmount = (amount) => Number(amount || 0).toFixed(2)

const paymentText = (paymentMethod) => {
  const map = {
    alipay: '支付宝',
    wechat: '微信支付',
    bank: '银行卡'
  }
  return map[paymentMethod] || paymentMethod || '未设置'
}

const handleCancel = async (order) => {
  actionLoadingId.value = order.id
  try {
    await cancelOrder(order.id)
    ElMessage.success('订单已取消')
    await loadOrders()
  } finally {
    actionLoadingId.value = null
  }
}

const handlePay = async (order) => {
  actionLoadingId.value = order.id
  try {
    await payOrder(order.id)
    ElMessage.success('支付成功')
    await loadOrders()
  } finally {
    actionLoadingId.value = null
  }
}

const handleConfirm = async (order) => {
  actionLoadingId.value = order.id
  try {
    await confirmOrder(order.id)
    ElMessage.success('确认收货成功')
    await loadOrders()
  } finally {
    actionLoadingId.value = null
  }
}

onMounted(() => { loadOrders() })
</script>

<style scoped>
.order-page { max-width: 800px; margin: 0 auto; padding: 0 24px; }
.page-title { font-size: 28px; font-weight: 700; color: #111; margin: 0 0 32px; }

.status-tabs {
  display: flex;
  gap: 8px;
  margin: -12px 0 24px;
  overflow-x: auto;
  padding-bottom: 4px;
}

.status-tabs button {
  padding: 8px 16px;
  border-radius: 999px;
  border: 1px solid #e5e5e5;
  background: #fff;
  color: #666;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s;
}

.status-tabs button:hover,
.status-tabs button.active {
  border-color: #111;
  background: #111;
  color: #fff;
}

.order-list { display: flex; flex-direction: column; gap: 16px; }

.order-card {
  border-radius: 8px; overflow: hidden;
  border: 1px solid #f0f0f0; transition: box-shadow 0.2s;
}
.order-card:hover { box-shadow: 0 4px 16px rgba(0,0,0,0.04); }

.order-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 16px 20px; background: #fafaf8;
}
.order-no { font-size: 13px; color: #666; font-weight: 500; }
.status-tag {
  font-size: 12px; font-weight: 600; padding: 3px 10px; border-radius: 8px;
}
.status-0 { background: #fff3e0; color: #e6a23c; }
.status-1 { background: #e8f5e9; color: #67c23a; }
.status-2 { background: #e3f2fd; color: #409eff; }
.status-3 { background: #f5f5f5; color: #999; }
.status-4 { background: #fce4ec; color: #e74c3c; }

.order-items { padding: 16px 20px; display: flex; flex-direction: column; gap: 12px; }
.order-item { display: flex; gap: 12px; align-items: center; }
.item-thumb { width: 56px; height: 56px; border-radius: 8px; object-fit: cover; background: #fafaf8; }
.item-detail { flex: 1; display: flex; flex-direction: column; gap: 2px; }
.item-name { font-size: 14px; font-weight: 600; color: #111; }
.item-meta { font-size: 13px; color: #999; }

.order-footer {
  display: flex; justify-content: space-between; align-items: center;
  padding: 14px 20px; border-top: 1px solid #f0f0f0;
}
.order-info { display: flex; flex-direction: column; gap: 2px; }
.order-info span { font-size: 13px; color: #666; }
.order-time { font-size: 12px; color: #bbb; }
.order-total-wrap { display: flex; align-items: center; gap: 12px; }
.price-breakdown {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
  font-size: 12px;
  color: #999;
}
.price-breakdown .discount { color: #67c23a; }
.order-total { display: flex; align-items: baseline; gap: 6px; }
.total-label { font-size: 13px; color: #999; }
.total-amount { font-size: 22px; font-weight: 700; color: #e74c3c; }
.order-actions { display: flex; gap: 8px; }
.btn-action {
  padding: 8px 14px; border-radius: 10px; border: 1px solid #ddd; background: #fff; color: #333;
  font-size: 13px; font-weight: 600; cursor: pointer; transition: all 0.2s;
}
.btn-action.primary { background: #111; color: #fff; border-color: #111; }
.btn-action.pay { background: #c45c3e; color: #fff; border-color: #c45c3e; }
.btn-action.ghost:hover { border-color: #111; color: #111; }
.btn-action.primary:hover { opacity: 0.9; }
.btn-action.pay:hover { opacity: 0.9; }
.btn-action:disabled { opacity: 0.6; cursor: not-allowed; }

.empty-state { text-align: center; padding: 80px 0; }
.empty-icon { font-size: 48px; margin-bottom: 16px; }
.empty-title { font-size: 18px; font-weight: 600; color: #333; margin: 0 0 6px; }
.empty-desc { font-size: 14px; color: #999; margin: 0 0 24px; }
.btn-shop {
  padding: 10px 28px; border-radius: 10px; border: 1.5px solid #ddd;
  background: #fff; color: #333; font-size: 14px; font-weight: 600;
  cursor: pointer; transition: all 0.2s;
}
.btn-shop:hover { border-color: #111; color: #111; }

@media (max-width: 768px) {
  .order-page { padding: 0 16px; }
  .order-footer { flex-direction: column; align-items: flex-start; gap: 12px; }
  .order-total-wrap { width: 100%; flex-direction: column; align-items: flex-start; }
  .order-actions { width: 100%; }
  .btn-action { flex: 1; }
}

/* 骨架屏 */
.sk { background: #f0f0f0; }
@keyframes skeleton-pulse { 0%,100%{opacity:1} 50%{opacity:0.4} }
.skeleton-pulse { animation: skeleton-pulse 1.5s ease-in-out infinite; }
</style>
