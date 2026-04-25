<template>
  <div class="checkout-page">
    <!-- 步骤条 -->
    <div class="steps" role="navigation" aria-label="结算步骤">
      <div v-for="(step, i) in stepList" :key="i"
        :class="['step', { active: currentStep >= i, done: currentStep > i }]"
        :aria-current="currentStep === i ? 'step' : undefined"
        role="listitem">
        <div class="step-dot" :aria-label="currentStep > i ? step + '已完成' : '第' + (i+1) + '步 ' + step">
          <el-icon v-if="currentStep > i"><Check /></el-icon>
          <span v-else>{{ i + 1 }}</span>
        </div>
        <span>{{ step }}</span>
      </div>
      <div class="step-line">
        <div class="step-line-fill" :style="{ width: (currentStep / (stepList.length - 1) * 100) + '%' }"></div>
      </div>
    </div>

    <div class="checkout-content">
      <!-- Step 1: 确认商品 -->
      <div v-if="currentStep === 0" class="step-content">
        <h2 class="step-title">确认商品信息</h2>

        <div class="order-items">
          <div v-for="item in cartItems" :key="item.id" class="order-item">
            <img :src="item.imageUrl || '/placeholder.svg'" class="item-thumb" />
            <div class="item-info">
              <span class="item-name">{{ item.productName }}</span>
              <span class="item-qty">× {{ item.quantity }}</span>
            </div>
            <div class="item-price">¥{{ (item.price * item.quantity).toFixed(2) }}</div>
          </div>
        </div>

        <div class="step-footer">
          <div class="order-totals">
            <div class="total-line">
              <span>商品总价</span>
              <span>¥{{ totalPrice }}</span>
            </div>
            <div class="total-line discount" v-if="discount > 0">
              <span>优惠</span>
              <span>-¥{{ discount }}</span>
            </div>
            <div class="total-line shipping">
              <span>运费</span>
              <span>{{ shippingFee === 0 ? '免运费' : '¥' + shippingFee }}</span>
            </div>
            <div class="total-line final">
              <span>合计</span>
              <div class="total-amount">
                <span class="symbol">¥</span>{{ finalPrice }}
              </div>
            </div>
          </div>
          <button class="btn-next" @click="currentStep = 1">
            下一步：填写信息 <el-icon><ArrowRight /></el-icon>
          </button>
        </div>
      </div>

      <!-- Step 2: 填写收货信息 -->
      <div v-if="currentStep === 1" class="step-content">
        <h2 class="step-title">收货信息</h2>

        <!-- Address List -->
        <div class="address-section" v-if="addresses.length">
          <div class="section-subtitle">
            <span>选择收货地址</span>
            <button class="btn-text" @click="showAddressForm = !showAddressForm">
              <el-icon><Plus /></el-icon>
              添加新地址
            </button>
          </div>
          <div class="address-grid">
            <div
              v-for="addr in addresses"
              :key="addr.id"
              class="address-card"
              :class="{ selected: selectedAddress?.id === addr.id }"
              @click="selectedAddress = addr"
            >
              <div class="address-default" v-if="addr.isDefault">默认</div>
              <div class="address-name">{{ addr.name }}</div>
              <div class="address-phone">{{ addr.phone }}</div>
              <div class="address-detail">{{ addr.province }} {{ addr.city }} {{ addr.district }} {{ addr.detail }}</div>
              <div class="address-actions">
                <button class="btn-text small" @click.stop="editAddress(addr)">编辑</button>
                <button class="btn-text small danger" @click.stop="deleteAddress(addr.id)" v-if="!addr.isDefault">删除</button>
              </div>
              <div class="address-check">
                <el-icon><Check /></el-icon>
              </div>
            </div>
          </div>
        </div>

        <!-- Address Form -->
        <div class="address-form-section" v-if="showAddressForm || !addresses.length">
          <div class="section-subtitle">
            <span>{{ editingAddress ? '编辑地址' : '新增收货地址' }}</span>
          </div>
          <form class="shipping-form" @submit.prevent="saveAddress">
            <div class="form-row">
              <div class="form-group">
                <label>收货人 <span class="required">*</span></label>
                <input v-model="addressForm.name" placeholder="请输入收货人姓名" required />
              </div>
              <div class="form-group">
                <label>联系电话 <span class="required">*</span></label>
                <input v-model="addressForm.phone" placeholder="请输入手机号" required pattern="^1[3-9]\d{9}$" />
              </div>
            </div>
            <div class="form-row">
              <div class="form-group">
                <label>省份 <span class="required">*</span></label>
                <input v-model="addressForm.province" placeholder="如：广东省" required />
              </div>
              <div class="form-group">
                <label>城市 <span class="required">*</span></label>
                <input v-model="addressForm.city" placeholder="如：深圳市" required />
              </div>
              <div class="form-group">
                <label>区县</label>
                <input v-model="addressForm.district" placeholder="如：南山区" />
              </div>
            </div>
            <div class="form-group full">
              <label>详细地址 <span class="required">*</span></label>
              <input v-model="addressForm.detail" placeholder="请输入详细地址，如街道、门牌号等" required />
            </div>
            <div class="form-actions">
              <button type="button" class="btn-back" @click="cancelAddress" v-if="editingAddress">取消</button>
              <button type="submit" class="btn-save">保存地址</button>
            </div>
          </form>
        </div>

        <!-- Coupon -->
        <div class="coupon-section">
          <div class="section-subtitle">
            <span>优惠券</span>
          </div>
          <div class="coupon-list">
            <div
              v-for="coupon in availableCoupons"
              :key="coupon.id"
              class="coupon-item"
              :class="{ selected: selectedCoupon?.id === coupon.id, disabled: !coupon.canUse }"
              @click="coupon.canUse && selectCoupon(coupon)"
            >
              <div class="coupon-left">
                <span class="coupon-value">¥{{ coupon.value }}</span>
                <span class="coupon-desc">{{ coupon.desc }}</span>
              </div>
              <div class="coupon-right">
                <span class="coupon-name">{{ coupon.name }}</span>
                <span class="coupon-time">{{ coupon.time }}</span>
                <span class="coupon-use" v-if="!coupon.canUse">不可用</span>
              </div>
              <div class="coupon-check">
                <el-icon><Check /></el-icon>
              </div>
            </div>
            <div class="coupon-item no-coupon" @click="selectedCoupon = null">
              <span>不使用优惠券</span>
              <div class="coupon-check" :class="{ selected: !selectedCoupon }">
                <el-icon><Check /></el-icon>
              </div>
            </div>
          </div>
        </div>

        <!-- Payment Method -->
        <div class="payment-section">
          <div class="section-subtitle">
            <span>支付方式</span>
          </div>
          <div class="payment-list">
            <div
              v-for="method in paymentMethods"
              :key="method.id"
              class="payment-item"
              :class="{ selected: selectedPayment === method.id }"
              @click="selectedPayment = method.id"
            >
              <span class="payment-icon">{{ method.icon }}</span>
              <span class="payment-name">{{ method.name }}</span>
              <div class="payment-check">
                <el-icon><Check /></el-icon>
              </div>
            </div>
          </div>
        </div>

        <!-- Invoice -->
        <div class="invoice-section">
          <div class="section-subtitle">
            <span>发票信息</span>
            <label class="toggle-label">
              <input type="checkbox" v-model="needInvoice" />
              <span class="toggle-switch"></span>
              <span>需要发票</span>
            </label>
          </div>
          <div class="invoice-form" v-if="needInvoice">
            <input v-model="invoiceTitle" placeholder="请输入发票抬头" />
          </div>
        </div>

        <div class="step-footer">
          <button class="btn-back" @click="currentStep = 0">
            <el-icon><ArrowLeft /></el-icon> 上一步
          </button>
          <button class="btn-submit" @click="submitOrder" :disabled="submitting">
            <span v-if="submitting">提交中...</span>
            <span v-else>确认提交订单</span>
          </button>
        </div>
      </div>

      <!-- Step 3: 成功 -->
      <div v-if="currentStep === 2" class="step-content success-content">
        <div class="success-icon">
          <el-icon><CircleCheckFilled /></el-icon>
        </div>
        <h2>订单提交成功</h2>
        <p>您的订单已提交，请等待发货</p>
        <div class="order-number">
          订单号：<span>{{ orderNo }}</span>
        </div>
        <div class="success-actions">
          <button class="btn-primary" @click="$router.push('/order')">查看订单</button>
          <button class="btn-ghost" @click="$router.push('/')">继续购物</button>
        </div>
      </div>
    </div>

    <!-- Order Summary Sidebar (Step 2) -->
    <div class="order-summary" v-if="currentStep === 1">
      <h3>订单摘要</h3>
      <div class="summary-items">
        <div class="summary-item" v-for="item in cartItems" :key="item.id">
          <img :src="item.imageUrl || '/placeholder.svg'" class="summary-thumb" />
          <div class="summary-info">
            <span class="summary-name">{{ item.productName }}</span>
            <span class="summary-qty">× {{ item.quantity }}</span>
          </div>
          <span class="summary-price">¥{{ (item.price * item.quantity).toFixed(2) }}</span>
        </div>
      </div>
      <div class="summary-totals">
        <div class="total-line">
          <span>商品总价</span>
          <span>¥{{ totalPrice }}</span>
        </div>
        <div class="total-line discount" v-if="discount > 0">
          <span>优惠</span>
          <span>-¥{{ discount }}</span>
        </div>
        <div class="total-line shipping">
          <span>运费</span>
          <span>{{ shippingFee === 0 ? '免运费' : '¥' + shippingFee }}</span>
        </div>
        <div class="total-line final">
          <span>合计</span>
          <div class="total-amount">
            <span class="symbol">¥</span>{{ finalPrice }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getCartList, createOrder, getCoupons } from '../api'
import { useCartStore } from '../store/cart'
import { storage } from '../utils/storage'
import { ElMessage } from 'element-plus'
import {
  Check, ArrowRight, ArrowLeft, Plus, CircleCheckFilled
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const cartStore = useCartStore()

const currentStep = ref(0)
const cartItems = ref([])
const submitting = ref(false)
const orderNo = ref('')

const stepList = ['确认订单', '填写信息', '完成']

// Address
const defaultAddresses = [
  {
    id: 1,
    name: '张三',
    phone: '13800138000',
    province: '广东省',
    city: '深圳市',
    district: '南山区',
    detail: '科技园中路123号A栋1001室',
    isDefault: true
  }
]
const addresses = ref(storage.getJSON('checkoutAddresses', defaultAddresses))
const selectedAddress = ref(null)
const showAddressForm = ref(false)
const editingAddress = ref(null)
const addressForm = reactive({
  id: null,
  name: '',
  phone: '',
  province: '',
  city: '',
  district: '',
  detail: ''
})

// Coupon
const availableCoupons = ref([])
const selectedCoupon = ref(null)

// Payment
const paymentMethods = ref([
  { id: 'alipay', name: '支付宝', icon: '' },
  { id: 'wechat', name: '微信支付', icon: '' },
  { id: 'bank', name: '银行卡', icon: '' }
])
const selectedPayment = ref('alipay')

// Invoice
const needInvoice = ref(false)
const invoiceTitle = ref('')

const totalPrice = computed(() => {
  return cartItems.value.reduce((sum, item) => sum + item.price * item.quantity, 0).toFixed(2)
})

const discount = computed(() => {
  return selectedCoupon.value ? selectedCoupon.value.value : 0
})

const shippingFee = computed(() => {
  return 0
})

const finalPrice = computed(() => {
  return Math.max(parseFloat(totalPrice.value) - discount.value + shippingFee.value, 0).toFixed(2)
})

const selectedCartItemIds = computed(() => {
  const raw = route.query.items || ''
  return String(raw)
    .split(',')
    .map(id => Number(id))
    .filter(id => Number.isInteger(id) && id > 0)
})

const loadCart = async () => {
  try {
    const res = await getCartList()
    const allItems = res.data || []
    cartItems.value = selectedCartItemIds.value.length
      ? allItems.filter(item => selectedCartItemIds.value.includes(item.id))
      : allItems
    if (cartItems.value.length === 0) {
      ElMessage.warning('购物车为空')
      router.push('/cart')
    }
    // Select default address
    selectedAddress.value = addresses.value.find(a => a.isDefault) || addresses.value[0]
    await loadCoupons()
  } catch (error) {
    console.error(error)
  }
}

const loadCoupons = async () => {
  try {
    const res = await getCoupons({ orderAmount: Number(totalPrice.value), availableOnly: false })
    availableCoupons.value = (res.data || []).map(coupon => ({
      id: coupon.id,
      name: coupon.name,
      value: Number(coupon.discountAmount || 0),
      desc: coupon.description || `满${Number(coupon.minAmount || 0)}可用`,
      time: coupon.endTime ? `${new Date(coupon.endTime).toLocaleDateString('zh-CN')}到期` : '长期有效',
      canUse: coupon.canUse !== false
    }))
    if (selectedCoupon.value) {
      selectedCoupon.value = availableCoupons.value.find(coupon => coupon.id === selectedCoupon.value.id && coupon.canUse) || null
    }
  } catch (error) {
    availableCoupons.value = []
  }
}

const saveAddress = () => {
  if (editingAddress.value) {
    // Update existing
    const idx = addresses.value.findIndex(a => a.id === editingAddress.value.id)
    if (idx !== -1) {
      addresses.value[idx] = { ...addressForm, id: editingAddress.value.id, isDefault: editingAddress.value.isDefault }
    }
    storage.setJSON('checkoutAddresses', addresses.value)
    ElMessage.success('地址已更新')
  } else {
    // Add new
    const newId = addresses.value.length + 1
    addresses.value.push({ ...addressForm, id: newId, isDefault: false })
    storage.setJSON('checkoutAddresses', addresses.value)
    ElMessage.success('地址已保存')
  }
  showAddressForm.value = false
  editingAddress.value = null
  Object.assign(addressForm, { id: null, name: '', phone: '', province: '', city: '', district: '', detail: '' })
}

const editAddress = (addr) => {
  editingAddress.value = addr
  Object.assign(addressForm, addr)
  showAddressForm.value = true
}

const deleteAddress = (id) => {
  addresses.value = addresses.value.filter(a => a.id !== id)
  storage.setJSON('checkoutAddresses', addresses.value)
  ElMessage.success('地址已删除')
}

const cancelAddress = () => {
  showAddressForm.value = false
  editingAddress.value = null
  Object.assign(addressForm, { id: null, name: '', phone: '', province: '', city: '', district: '', detail: '' })
}

const selectCoupon = (coupon) => {
  if (selectedCoupon.value?.id === coupon.id) {
    selectedCoupon.value = null
  } else {
    selectedCoupon.value = coupon
  }
}

const submitOrder = async () => {
  if (!selectedAddress.value) {
    ElMessage.warning('请选择收货地址')
    return
  }

  const orderData = {
    receiverName: selectedAddress.value.name,
    receiverPhone: selectedAddress.value.phone,
    receiverAddress: `${selectedAddress.value.province} ${selectedAddress.value.city} ${selectedAddress.value.district} ${selectedAddress.value.detail}`,
    remark: '',
    couponId: selectedCoupon.value?.id || null,
    paymentMethod: selectedPayment.value,
    cartItemIds: cartItems.value.map(item => item.id)
  }

  if (needInvoice.value && invoiceTitle.value) {
    orderData.invoiceTitle = invoiceTitle.value
  }

  submitting.value = true
  try {
    const res = await createOrder(orderData)
    orderNo.value = res.data?.orderNo || `ORD${Date.now()}`
    const orderedIds = new Set(cartItems.value.map(item => item.id))
    cartStore.setCartItems(cartStore.cartItems.filter(item => !orderedIds.has(item.id)))
    currentStep.value = 2
    ElMessage.success('订单提交成功')
  } catch (error) {
    ElMessage.error('提交失败，请重试')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadCart()
})
</script>

<style scoped>
.checkout-page {
  max-width: 1100px;
  margin: 0 auto;
  padding: 0 24px 60px;
  display: flex;
  flex-direction: column;
  gap: 32px;
}

/* ===== Steps ===== */
.steps {
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: relative;
  padding: 0 60px;
  margin-bottom: 20px;
}

.step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  position: relative;
  z-index: 1;
}

.step-dot {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #f0f0f0;
  color: #999;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  font-weight: 600;
  transition: all 0.3s;
}

.step.active .step-dot {
  background: #c45c3e;
  color: #fff;
  box-shadow: 0 4px 12px rgba(196, 92, 62, 0.3);
}

.step.done .step-dot {
  background: #67c23a;
  color: #fff;
}

.step span {
  font-size: 14px;
  color: #999;
  font-weight: 500;
  white-space: nowrap;
}

.step.active span {
  color: #c45c3e;
}

.step.done span {
  color: #67c23a;
}

.step-line {
  position: absolute;
  top: 20px;
  left: 100px;
  right: 100px;
  height: 3px;
  background: #f0f0f0;
  border-radius: 2px;
}

.step-line-fill {
  height: 100%;
  background: linear-gradient(90deg, #c45c3e, #67c23a);
  border-radius: 2px;
  transition: width 0.4s ease;
}

/* ===== Content ===== */
.checkout-content {
  flex: 1;
  background: #fff;
  border-radius: 10px;
  border: 1px solid #ebeef5;
  padding: 32px;
}

.step-content {
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.step-title {
  font-size: 22px;
  font-weight: 700;
  color: #111;
  margin: 0 0 24px;
}

.section-subtitle {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-subtitle span {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

/* ===== Order Items ===== */
.order-items {
  display: flex;
  flex-direction: column;
  gap: 1px;
  background: #f5f5f5;
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 24px;
}

.order-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  background: #fff;
}

.item-thumb {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  object-fit: cover;
  background: #fafaf8;
}

.item-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.item-name {
  font-size: 14px;
  font-weight: 600;
  color: #111;
}

.item-qty {
  font-size: 13px;
  color: #999;
}

.item-price {
  font-size: 15px;
  font-weight: 700;
  color: #f56c6c;
}

/* ===== Order Totals ===== */
.order-totals {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 24px;
  padding: 20px;
  background: #fafaf8;
  border-radius: 8px;
}

.total-line {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  color: #666;
}

.total-line.discount {
  color: #67c23a;
}

.total-line.final {
  padding-top: 12px;
  border-top: 1px dashed #e5e5e5;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.total-amount {
  display: flex;
  align-items: baseline;
  font-size: 28px;
  font-weight: 700;
  color: #f56c6c;
}

.total-amount .symbol {
  font-size: 16px;
}

/* ===== Step Footer ===== */
.step-footer {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 16px;
  padding-top: 24px;
  border-top: 1px solid #f0f0f0;
}

.btn-next, .btn-submit, .btn-primary {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 32px;
  border-radius: 10px;
  border: none;
  background: #1a1a1a;
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 4px 16px rgba(26, 26, 26, 0.15);
}

.btn-next:hover, .btn-submit:hover:not(:disabled), .btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(26, 26, 26, 0.2);
}

.btn-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-back {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 14px 24px;
  border-radius: 10px;
  border: 1.5px solid #e5e5e5;
  background: #fff;
  color: #666;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-back:hover {
  border-color: #c45c3e;
  color: #c45c3e;
}

/* ===== Address ===== */
.address-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.address-card {
  position: relative;
  padding: 20px;
  background: #fafaf8;
  border: 2px solid transparent;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.address-card:hover {
  background: #fff;
  border-color: #e5e5e5;
}

.address-card.selected {
  background: #f9f7ff;
  border-color: #c45c3e;
}

.address-default {
  position: absolute;
  top: 12px;
  right: 12px;
  padding: 2px 8px;
  background: #c45c3e;
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  border-radius: 4px;
}

.address-name {
  font-size: 16px;
  font-weight: 600;
  color: #111;
  margin-bottom: 8px;
}

.address-phone {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}

.address-detail {
  font-size: 13px;
  color: #999;
  line-height: 1.5;
}

.address-actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}

.address-check {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #c45c3e;
  color: #fff;
  display: none;
  align-items: center;
  justify-content: center;
}

.address-card.selected .address-check {
  display: flex;
}

/* ===== Address Form ===== */
.address-form-section {
  padding: 24px;
  background: #fafaf8;
  border-radius: 8px;
  margin-bottom: 24px;
}

.shipping-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-row {
  display: flex;
  gap: 16px;
}

.form-group {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-group.full {
  flex: 1;
}

.form-group label {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.required {
  color: #f56c6c;
}

.form-group input {
  padding: 12px 16px;
  border: 1.5px solid #e5e5e5;
  border-radius: 10px;
  font-size: 14px;
  color: #333;
  outline: none;
  transition: border-color 0.2s;
  font-family: inherit;
}

.form-group input:focus {
  border-color: #c45c3e;
}

.form-group input::placeholder {
  color: #bbb;
}

.form-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.btn-save {
  padding: 10px 24px;
  border-radius: 8px;
  border: none;
  background: #1a1a1a;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-save:hover {
  transform: translateY(-1px);
}

/* ===== Coupon ===== */
.coupon-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 24px;
}

.coupon-item {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  background: #fff;
  border: 2px solid #e5e5e5;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
}

.coupon-item:hover:not(.disabled) {
  border-color: #c45c3e;
}

.coupon-item.selected {
  border-color: #c45c3e;
  background: #f9f7ff;
}

.coupon-item.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.coupon-item.no-coupon {
  justify-content: space-between;
  color: #666;
}

.coupon-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 100px;
}

.coupon-value {
  font-size: 24px;
  font-weight: 700;
  color: #f56c6c;
}

.coupon-desc {
  font-size: 12px;
  color: #999;
}

.coupon-right {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-left: 20px;
}

.coupon-name {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.coupon-time {
  font-size: 12px;
  color: #999;
}

.coupon-use {
  font-size: 11px;
  color: #999;
  background: #f5f5f5;
  padding: 2px 8px;
  border-radius: 4px;
  width: fit-content;
}

.coupon-check {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: 2px solid #e5e5e5;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.coupon-check.selected {
  background: #c45c3e;
  border-color: #c45c3e;
  color: #fff;
}

/* ===== Payment ===== */
.payment-list {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.payment-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 24px;
  background: #fafaf8;
  border: 2px solid transparent;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
}

.payment-item:hover {
  background: #fff;
  border-color: #e5e5e5;
}

.payment-item.selected {
  background: #f9f7ff;
  border-color: #c45c3e;
}

.payment-icon {
  font-size: 24px;
}

.payment-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.payment-check {
  position: absolute;
  bottom: -1px;
  right: -1px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #c45c3e;
  color: #fff;
  display: none;
  align-items: center;
  justify-content: center;
  font-size: 12px;
}

.payment-item.selected .payment-check {
  display: flex;
}

/* ===== Invoice ===== */
.invoice-section {
  margin-bottom: 24px;
}

.toggle-label {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  font-size: 14px;
  color: #666;
}

.toggle-label input {
  display: none;
}

.toggle-switch {
  width: 40px;
  height: 22px;
  background: #dcdfe6;
  border-radius: 11px;
  position: relative;
  transition: background 0.2s;
}

.toggle-switch::after {
  content: '';
  position: absolute;
  top: 2px;
  left: 2px;
  width: 18px;
  height: 18px;
  background: #fff;
  border-radius: 50%;
  transition: transform 0.2s;
}

.toggle-label input:checked + .toggle-switch {
  background: #c45c3e;
}

.toggle-label input:checked + .toggle-switch::after {
  transform: translateX(18px);
}

.invoice-form {
  margin-top: 12px;
}

.invoice-form input {
  width: 100%;
  max-width: 400px;
  padding: 12px 16px;
  border: 1.5px solid #e5e5e5;
  border-radius: 10px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
}

.invoice-form input:focus {
  border-color: #c45c3e;
}

/* ===== Success ===== */
.success-content {
  text-align: center;
  padding: 60px 0;
}

.success-icon {
  font-size: 80px;
  color: #67c23a;
  margin-bottom: 20px;
}

.success-content h2 {
  font-size: 26px;
  font-weight: 700;
  color: #111;
  margin: 0 0 8px;
}

.success-content p {
  font-size: 15px;
  color: #666;
  margin: 0 0 24px;
}

.order-number {
  display: inline-block;
  padding: 12px 24px;
  background: #fafaf8;
  border-radius: 10px;
  font-size: 14px;
  color: #666;
  margin-bottom: 32px;
}

.order-number span {
  font-weight: 600;
  color: #c45c3e;
  font-family: monospace;
}

.success-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.btn-ghost {
  padding: 14px 28px;
  border-radius: 10px;
  border: 1.5px solid #e5e5e5;
  background: #fff;
  color: #555;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-ghost:hover {
  border-color: #c45c3e;
  color: #c45c3e;
}

/* ===== Summary Sidebar ===== */
.order-summary {
  width: 300px;
  flex-shrink: 0;
  background: #fff;
  border-radius: 10px;
  border: 1px solid #ebeef5;
  padding: 24px;
  height: fit-content;
  position: sticky;
  top: 88px;
}

.order-summary h3 {
  font-size: 18px;
  font-weight: 700;
  color: #111;
  margin: 0 0 20px;
}

.summary-items {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 16px;
}

.summary-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.summary-thumb {
  width: 50px;
  height: 50px;
  border-radius: 8px;
  object-fit: cover;
  background: #fafaf8;
}

.summary-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.summary-name {
  font-size: 13px;
  font-weight: 500;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.summary-qty {
  font-size: 12px;
  color: #999;
}

.summary-price {
  font-size: 13px;
  font-weight: 600;
  color: #666;
}

.summary-totals {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* ===== Text Button ===== */
.btn-text {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border: none;
  background: transparent;
  color: #c45c3e;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: color 0.2s;
}

.btn-text:hover {
  color: #a84830;
}

.btn-text.small {
  font-size: 12px;
  padding: 4px 8px;
}

.btn-text.danger {
  color: #f56c6c;
}

.btn-text.danger:hover {
  color: #e54545;
}

@media (max-width: 768px) {
  .checkout-page {
    flex-direction: column;
  }

  .order-summary {
    width: 100%;
    position: static;
  }

  .form-row {
    flex-direction: column;
  }

  .payment-list {
    flex-wrap: wrap;
  }
}
</style>
