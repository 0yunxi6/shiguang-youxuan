<template>
  <div class="cart-page">
    <div class="page-header">
      <h1>购物车</h1>
      <span class="item-count" v-if="cartList.length">{{ cartList.length }} 件商品</span>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="cart-items" style="border-radius:14px;overflow:hidden">
      <div v-for="i in 3" :key="i" class="cart-item">
        <div class="sk skeleton-pulse" style="width:80px;height:80px;border-radius:10px"></div>
        <div style="flex:1;display:flex;flex-direction:column;gap:8px">
          <div class="sk skeleton-pulse" style="width:60%;height:16px;border-radius:4px"></div>
          <div class="sk skeleton-pulse" style="width:40%;height:14px;border-radius:4px"></div>
        </div>
        <div class="sk skeleton-pulse" style="width:80px;height:16px;border-radius:4px"></div>
      </div>
    </div>

    <!-- Empty Cart -->
    <div v-else-if="cartList.length === 0" class="empty-cart">
      <div class="empty-illustration">
        <svg viewBox="0 0 120 120" fill="none">
          <circle cx="60" cy="60" r="50" fill="#f5f7fa"/>
          <path d="M40 50h40l8 40H32l8-40z" fill="#e5e7eb" stroke="#d1d5db" stroke-width="2"/>
          <circle cx="45" cy="95" r="5" fill="#d1d5db"/>
          <circle cx="75" cy="95" r="5" fill="#d1d5db"/>
          <circle cx="60" cy="65" r="12" stroke="#d1d5db" stroke-width="2" fill="none"/>
        </svg>
      </div>
      <p class="empty-title">购物车还是空的</p>
      <p class="empty-desc">去发现一些好物吧</p>
      <button class="btn-shop" @click="$router.push('/')">去逛逛</button>
      <div class="empty-suggestions" v-if="suggestedLinks.length">
        <p class="suggest-label">或者看看这些</p>
        <div class="suggest-links">
          <button v-for="s in suggestedLinks" :key="s.path" class="suggest-link" @click="$router.push(s.path)">{{ s.label }}</button>
        </div>
      </div>
    </div>

    <template v-else>
      <div class="cart-container">
        <div class="cart-items">
          <div class="cart-header">
            <label class="select-all">
              <input type="checkbox" v-model="selectAll" />
              <span class="checkmark"></span>
              <span class="label-text">全选</span>
            </label>
            <div class="cart-titles">
              <span class="col-product">商品</span>
              <span class="col-price">单价</span>
              <span class="col-qty">数量</span>
              <span class="col-subtotal">小计</span>
              <span class="col-action">操作</span>
            </div>
          </div>

          <transition-group name="cart-item" tag="div" class="cart-list">
            <div v-for="item in cartList" :key="item.id" class="cart-item" :class="{ selected: selectedItems.includes(item.id) }">
              <label class="item-select">
                <input type="checkbox" v-model="selectedItems" :value="item.id" />
                <span class="checkmark"></span>
              </label>

              <div class="item-image" @click="$router.push(`/product/${item.productId}`)">
                <img :src="item.imageUrl || '/placeholder.svg'" :alt="item.productName" />
                <span v-if="item.stock === 0" class="stock-badge sold-out">已售罄</span>
                <span v-else-if="item.stock <= 5" class="stock-badge low">仅剩{{ item.stock }}件</span>
              </div>

              <div class="item-info">
                <h3 @click="$router.push(`/product/${item.productId}`)">
                  {{ item.productName }}
                </h3>
                <p class="item-desc">{{ item.description || '暂无描述' }}</p>
              </div>

              <div class="item-price">
                <span class="price-symbol">¥</span>{{ Number(item.price || 0).toFixed(2) }}
              </div>

              <div class="item-qty">
                <div class="qty-control">
                  <button @click="changeQty(item, -1)" :disabled="item.quantity <= 1 || item.stock === 0">-</button>
                  <span>{{ item.quantity }}</span>
                  <button @click="changeQty(item, 1)" :disabled="item.quantity >= item.stock || item.stock === 0">+</button>
                </div>
              </div>

              <div class="item-subtotal">
                <span class="price-symbol">¥</span>{{ ((item.price || 0) * item.quantity).toFixed(2) }}
              </div>

              <button class="item-remove" @click="removeItem(item)" title="删除">
                <el-icon><Delete /></el-icon>
              </button>
            </div>
          </transition-group>
        </div>

        <div class="cart-summary">
          <div class="summary-left">
            <label class="select-all">
              <input type="checkbox" v-model="selectAll" />
              <span class="checkmark"></span>
              <span class="label-text">全选</span>
            </label>
            <button class="clear-btn" @click="confirmClear" v-if="selectedItems.length > 0">
              <el-icon><Delete /></el-icon>
              删除选中
            </button>
            <button class="clear-btn" @click="confirmClear">
              <el-icon><Delete /></el-icon>
              清空购物车
            </button>
          </div>

          <div class="summary-right">
            <div class="summary-details">
              <div class="detail-line">
                <span>商品件数</span>
                <span>{{ totalCount }} 件</span>
              </div>
              <div class="detail-line">
                <span>选中件数</span>
                <span class="highlight">{{ selectedItems.length }} 件</span>
              </div>
              <div class="detail-line">
                <span>商品总价</span>
                <span>¥{{ totalPrice }}</span>
              </div>
              <div class="detail-line discount" v-if="discount > 0">
                <span>优惠</span>
                <span class="discount-value">-¥{{ discount }}</span>
              </div>
              <div class="detail-line shipping" v-if="shippingFee > 0">
                <span>运费</span>
                <span>+¥{{ shippingFee }}</span>
              </div>
              <div class="detail-line shipping free" v-else>
                <span>运费</span>
                <span class="free-tag">免运费</span>
              </div>
            </div>

            <div class="summary-total">
              <span>合计</span>
              <div class="total-price">
                <span class="price-symbol">¥</span>{{ finalPrice }}
              </div>
              <span class="total-hint" v-if="finalPrice < 99">再买¥{{ (99 - finalPrice).toFixed(2) }}即可免运费</span>
            </div>

            <button
              class="btn-checkout"
              @click="goToCheckout"
              :disabled="selectedItems.length === 0"
            >
              去结算
              <span v-if="selectedItems.length > 0">（{{ selectedItems.length }}件）</span>
            </button>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getCartList, updateCartItem, deleteCartItem, clearCart as clearCartApi } from '../api'
import { useCartStore } from '../store/cart'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'

const router = useRouter()
const cartStore = useCartStore()
const cartList = ref([])
const loading = ref(true)
const selectedItems = ref([])
const suggestedLinks = [
  { label: '热门推荐', path: '/' },
  { label: '搜索商品', path: '/search?keyword=推荐' },
  { label: '全部分类', path: '/category/1' }
]

const selectAll = computed({
  get: () => cartList.value.length > 0 && selectedItems.value.length === cartList.value.length,
  set: (val) => {
    selectedItems.value = val ? cartList.value.map(i => i.id) : []
  }
})

const selectedCartItems = computed(() =>
  cartList.value.filter(item => selectedItems.value.includes(item.id))
)

const totalPrice = computed(() =>
  selectedCartItems.value.reduce((t, i) => t + (i.price || 0) * i.quantity, 0).toFixed(2)
)

const totalCount = computed(() =>
  selectedCartItems.value.reduce((t, i) => t + i.quantity, 0)
)

const shippingFee = computed(() => {
  const total = parseFloat(totalPrice.value)
  return total >= 99 || total === 0 ? 0 : 10
})

const discount = computed(() => {
  return 0
})

const finalPrice = computed(() => {
  return (parseFloat(totalPrice.value) + shippingFee.value - discount.value).toFixed(2)
})

const loadCart = async () => {
  loading.value = true
  try {
    const res = await getCartList()
    cartList.value = res.data.map(item => ({
      ...item,
      stock: item.stock ?? 0
    }))
    cartStore.setCartItems(cartList.value)
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}


const changeQty = async (item, delta) => {
  const newQty = item.quantity + delta
  if (newQty < 1) return
  item.quantity = newQty
  try {
    await updateCartItem({ id: item.id, quantity: newQty })
  } catch (e) {
    item.quantity -= delta
    ElMessage.error('更新失败')
  }
}

const removeItem = async (item) => {
  try {
    await deleteCartItem(item.id)
    cartList.value = cartList.value.filter(i => i.id !== item.id)
    selectedItems.value = selectedItems.value.filter(id => id !== item.id)
    cartStore.setCartItems(cartList.value)
    ElMessage.success('已删除')
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

const confirmClear = async () => {
  try {
    await ElMessageBox.confirm(
      selectedItems.value.length > 0
        ? `确定要删除选中的 ${selectedItems.value.length} 件商品吗？`
        : '确定要清空购物车吗？',
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    if (selectedItems.value.length > 0) {
      // 删除选中的
      await Promise.all(selectedItems.value.map(id => deleteCartItem(id)))
      cartList.value = cartList.value.filter(i => !selectedItems.value.includes(i.id))
      selectedItems.value = []
      ElMessage.success('已删除选中商品')
      cartStore.setCartItems(cartList.value)
    } else {
      // 清空全部
      await clearCartApi()
      cartList.value = []
      cartStore.clearCart()
      ElMessage.success('已清空')
    }
  } catch (e) {
    // 取消
  }
}

const goToCheckout = () => {
  if (selectedItems.value.length === 0) {
    ElMessage.warning('请选择要结算的商品')
    return
  }
  router.push({ path: '/checkout', query: { items: selectedItems.value.join(',') } })
}

watch(cartList, (newList) => {
  // 清理已删除的选中项
  selectedItems.value = selectedItems.value.filter(id =>
    newList.some(item => item.id === id)
  )
}, { deep: true })

onMounted(() => {
  loadCart()
})
</script>

<style scoped>
.cart-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px 60px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 32px;
}

.page-header h1 {
  font-size: 28px;
  font-weight: 700;
  color: #111;
  margin: 0;
}

.item-count {
  padding: 4px 12px;
  background: #f0f0f0;
  border-radius: 10px;
  font-size: 13px;
  color: #666;
}

.cart-container {
  display: flex;
  gap: 24px;
}

/* ===== Cart Items ===== */
.cart-items {
  flex: 1;
  background: #fff;
  border-radius: 10px;
  border: 1px solid #ebeef5;
  overflow: hidden;
}

.cart-header {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  background: #fafbfc;
  border-bottom: 1px solid #ebeef5;
}

.select-all {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  user-select: none;
}

.select-all input {
  display: none;
}

.checkmark {
  width: 20px;
  height: 20px;
  border: 2px solid #dcdfe6;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  background: #fff;
}

.select-all input:checked + .checkmark {
  background: #c45c3e;
  border-color: #c45c3e;
}

.select-all input:checked + .checkmark::after {
  content: '✓';
  color: #fff;
  font-size: 12px;
  font-weight: 700;
}

.label-text {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.cart-titles {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-left: 40px;
}

.cart-titles span {
  font-size: 12px;
  color: #909399;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.col-product { flex: 1; }
.col-price { width: 100px; text-align: center; }
.col-qty { width: 120px; text-align: center; }
.col-subtotal { width: 100px; text-align: center; }
.col-action { width: 60px; text-align: center; }

.cart-list {
  display: flex;
  flex-direction: column;
}

.cart-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  border-bottom: 1px solid #f0f0f0;
  transition: background 0.2s;
}

.cart-item:last-child {
  border-bottom: none;
}

.cart-item:hover {
  background: #fafbff;
}

.cart-item.selected {
  background: #f9f7ff;
}

.item-select {
  flex-shrink: 0;
}

.item-select input {
  display: none;
}

.item-image {
  width: 90px;
  height: 90px;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
  cursor: pointer;
  background: #fafaf8;
  position: relative;
}

.item-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.item-image:hover img {
  transform: scale(1.05);
}

.stock-badge {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  color: #fff;
  font-weight: 500;
}

.stock-badge.low {
  background: rgba(230, 162, 60, 0.9);
}

.item-info {
  flex: 1;
  min-width: 0;
}

.item-info h3 {
  font-size: 15px;
  font-weight: 600;
  color: #111;
  margin: 0 0 4px;
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color 0.2s;
}

.item-info h3:hover {
  color: #c45c3e;
}

.item-desc {
  font-size: 13px;
  color: #999;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-price {
  width: 100px;
  text-align: center;
  font-size: 15px;
  font-weight: 600;
  color: #333;
}

.price-symbol {
  font-size: 12px;
}

.item-qty {
  width: 120px;
  display: flex;
  justify-content: center;
}

.qty-control {
  display: flex;
  align-items: center;
  border: 1.5px solid #e5e5e5;
  border-radius: 10px;
  overflow: hidden;
}

.qty-control button {
  width: 36px;
  height: 36px;
  border: none;
  background: #fafaf8;
  font-size: 16px;
  cursor: pointer;
  color: #333;
  transition: background 0.2s;
}

.qty-control button:hover:not(:disabled) {
  background: #f0f0f0;
}

.qty-control button:disabled {
  color: #ccc;
  cursor: not-allowed;
}

.qty-control span {
  width: 44px;
  text-align: center;
  font-size: 14px;
  font-weight: 600;
  border-left: 1.5px solid #e5e5e5;
  border-right: 1.5px solid #e5e5e5;
  line-height: 36px;
}

.item-subtotal {
  width: 100px;
  text-align: center;
  font-size: 16px;
  font-weight: 700;
  color: #f56c6c;
}

.item-remove {
  width: 40px;
  height: 40px;
  border: none;
  background: transparent;
  color: #ccc;
  cursor: pointer;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  flex-shrink: 0;
}

.item-remove:hover {
  background: #fef0f0;
  color: #f56c6c;
}

/* ===== Summary ===== */
.cart-summary {
  width: 340px;
  flex-shrink: 0;
  background: #fff;
  border-radius: 10px;
  border: 1px solid #ebeef5;
  padding: 24px;
  height: fit-content;
  position: sticky;
  top: 88px;
}

.summary-left {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 16px;
}

.clear-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border: 1px solid #e5e5e5;
  border-radius: 6px;
  background: #fff;
  font-size: 12px;
  color: #999;
  cursor: pointer;
  transition: all 0.2s;
}

.clear-btn:hover {
  border-color: #f56c6c;
  color: #f56c6c;
}

.summary-details {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.detail-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  color: #666;
}

.detail-line .highlight {
  color: #c45c3e;
  font-weight: 600;
}

.detail-line.discount {
  color: #67c23a;
}

.discount-value {
  font-weight: 600;
}

.detail-line.shipping {
  font-size: 13px;
  color: #999;
}

.detail-line .free-tag {
  color: #67c23a;
  font-weight: 500;
}

.summary-total {
  margin-bottom: 20px;
}

.summary-total span:first-child {
  font-size: 14px;
  color: #666;
  margin-right: 8px;
}

.total-price {
  display: flex;
  align-items: baseline;
  font-size: 32px;
  font-weight: 700;
  color: #f56c6c;
  margin-top: 4px;
}

.total-price .price-symbol {
  font-size: 18px;
}

.total-hint {
  display: block;
  font-size: 12px;
  color: #e6a23c;
  margin-top: 4px;
}

.btn-checkout {
  width: 100%;
  padding: 14px;
  border-radius: 8px;
  border: none;
  background: #1a1a1a;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 4px 16px rgba(26, 26, 26, 0.15);
}

.btn-checkout:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(26, 26, 26, 0.2);
}

.btn-checkout:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* ===== Empty Cart ===== */
.empty-cart {
  text-align: center;
  padding: 80px 0;
}

.empty-illustration {
  width: 120px;
  height: 120px;
  margin: 0 auto 24px;
}

.empty-illustration svg {
  width: 100%;
  height: 100%;
}

.empty-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin: 0 0 6px;
}

.empty-desc {
  font-size: 14px;
  color: #999;
  margin: 0 0 24px;
}

.empty-suggestions {
  margin-top: 32px;
}

.suggest-label {
  font-size: 13px;
  color: #bbb;
  margin: 0 0 12px;
}

.suggest-links {
  display: flex;
  gap: 10px;
  justify-content: center;
  flex-wrap: wrap;
}

.suggest-link {
  padding: 8px 18px;
  border-radius: 20px;
  border: 1px solid #e5e5e5;
  background: #fff;
  font-size: 13px;
  color: #555;
  cursor: pointer;
  transition: all 0.2s;
}

.suggest-link:hover {
  border-color: #c45c3e;
  color: #c45c3e;
}

.btn-shop {
  padding: 12px 32px;
  border-radius: 10px;
  border: none;
  background: #1a1a1a;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-shop:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(26, 26, 26, 0.15);
}

/* ===== List Animation ===== */
.cart-item-enter-active, .cart-item-leave-active {
  transition: all 0.3s ease;
}

.cart-item-enter-from {
  opacity: 0;
  transform: translateX(-20px);
}

.cart-item-leave-to {
  opacity: 0;
  transform: translateX(20px);
}

/* ===== Skeleton ===== */
.sk {
  background: #f0f0f0;
}

@keyframes skeleton-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.skeleton-pulse {
  animation: skeleton-pulse 1.5s ease-in-out infinite;
}

@media (max-width: 1024px) {
  .cart-container {
    flex-direction: column;
  }

  .cart-summary {
    width: 100%;
    position: static;
  }
}

@media (max-width: 768px) {
  .cart-header {
    display: none;
  }

  .cart-item {
    flex-wrap: wrap;
    gap: 12px;
  }

  .item-select {
    position: absolute;
    top: 20px;
    left: 20px;
  }

  .item-image {
    margin-left: 36px;
  }

  .item-info {
    min-width: calc(100% - 150px);
    margin-left: 36px;
  }

  .cart-titles {
    display: none;
  }
}
</style>
