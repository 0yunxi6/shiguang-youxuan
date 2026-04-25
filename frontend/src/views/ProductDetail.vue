<template>
  <div class="detail-page">
    <div class="breadcrumb">
      <router-link to="/">首页</router-link>
      <span class="sep">/</span>
      <router-link :to="`/category/${product?.categoryId}`">{{ product?.categoryName || '商品分类' }}</router-link>
      <span class="sep">/</span>
      <span>{{ product?.name || '商品详情' }}</span>
    </div>

    <!-- 骨架屏 -->
    <div class="detail-main" v-if="loading">
      <div class="detail-gallery">
        <div class="main-image skeleton-pulse"></div>
        <div class="thumbnail-list">
          <div v-for="i in 4" :key="i" class="thumbnail skeleton-pulse"></div>
        </div>
      </div>
      <div class="detail-info">
        <div class="sk-line skeleton-pulse" style="width:60%;height:28px"></div>
        <div class="sk-line skeleton-pulse" style="width:90%;height:16px"></div>
        <div class="sk-block skeleton-pulse" style="height:80px"></div>
        <div class="sk-line skeleton-pulse" style="width:40%;height:16px"></div>
        <div class="sk-line skeleton-pulse" style="width:50%;height:16px"></div>
        <div style="display:flex;gap:12px;margin-top:16px">
          <div class="sk-btn skeleton-pulse"></div>
          <div class="sk-btn skeleton-pulse"></div>
        </div>
      </div>
    </div>

    <div class="detail-main" v-else-if="product">
      <div class="detail-gallery">
        <div class="main-image" @click="openPreview(0)">
          <img :src="currentImage" :alt="product.name" />
          <div class="zoom-hint">
            <el-icon><ZoomIn /></el-icon>
            点击查看大图
          </div>
        </div>
        <div class="thumbnail-list" v-if="images.length > 1">
          <div
            v-for="(img, idx) in images"
            :key="idx"
            class="thumbnail"
            :class="{ active: currentImageIndex === idx }"
            @click="currentImageIndex = idx"
          >
            <img :src="img" :alt="`图片${idx + 1}`" />
          </div>
        </div>
      </div>

      <div class="detail-info">
        <div class="product-header">
          <h1>{{ product.name }}</h1>
          <div class="product-actions">
            <button class="action-btn" :class="{ active: isFavorited }" @click="toggleFavorite">
              <el-icon :size="20"><Star /></el-icon>
              <span>{{ isFavorited ? '已收藏' : '收藏' }}</span>
            </button>
            <button class="action-btn" @click="shareProduct">
              <el-icon :size="20"><Share /></el-icon>
              <span>分享</span>
            </button>
          </div>
        </div>

        <p class="desc-text">{{ product.description || '暂无描述' }}</p>

        <div class="price-block">
          <div class="price-main">
            <span class="price-label">价格</span>
            <div class="price-value">
              <span class="symbol">¥</span>
              <span class="amount">{{ priceInt }}</span>
              <span class="decimal">.{{ priceDec }}</span>
            </div>
          </div>
          <div class="price-meta">
            <span class="meta-item">
              <el-icon><TrendCharts /></el-icon>
              历史最低价
            </span>
            <span class="meta-item">
              <el-icon><CircleCheck /></el-icon>
              全网比价
            </span>
          </div>
        </div>

        <div class="promo-block" v-if="promo">
          <div class="promo-item">
            <span class="promo-tag">限时优惠</span>
            <span class="promo-text">满99减10，领取优惠券</span>
          </div>
        </div>

        <div class="meta-row">
          <div class="meta-item">
            <span class="meta-label">库存</span>
            <span class="meta-value" :class="{ 'low-stock': product.stock <= 10 }">
              <template v-if="product.stock > 0">
                {{ product.stock }} 件
                <span class="stock-hint" v-if="product.stock <= 10">（库存紧张）</span>
              </template>
              <template v-else>暂时缺货</template>
            </span>
          </div>
          <div class="meta-item">
            <span class="meta-label">销量</span>
            <span class="meta-value">{{ product.sales || 0 }} 件</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">收藏</span>
            <span class="meta-value">{{ product.favoriteCount || 0 }} 人</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">运费</span>
            <span class="meta-value free" v-if="product.price >= 99">免运费</span>
            <span class="meta-value" v-else>满99元包邮</span>
          </div>
        </div>

        <div class="quantity-row">
          <span class="meta-label">数量</span>
          <div class="qty-control">
            <button @click="quantity > 1 && quantity--" :disabled="quantity <= 1">-</button>
            <input v-model.number="quantity" type="number" :min="1" :max="product.stock" @change="validateQty" />
            <button @click="quantity < product.stock && quantity++" :disabled="quantity >= product.stock">+</button>
          </div>
          <span class="qty-hint" v-if="product.stock > 0 && product.stock <= 10">
            库存仅剩 {{ product.stock }} 件
          </span>
        </div>

        <div class="action-row">
          <button class="btn-cart" @click="handleAddCart" :disabled="product.stock === 0 || addingCart">
            <el-icon :size="18"><ShoppingCart /></el-icon>
            {{ addingCart ? '添加中...' : '加入购物车' }}
          </button>
          <button class="btn-buy" @click="handleBuyNow" :disabled="product.stock === 0 || addingCart">
            {{ addingCart ? '处理中...' : '立即购买' }}
          </button>
        </div>

        <div class="guarantee-row">
          <span><el-icon><CircleCheck /></el-icon> 正品保障</span>
          <span><el-icon><RefreshRight /></el-icon> 7天无理由退换</span>
          <span><el-icon><Lightning /></el-icon> 极速发货</span>
        </div>
      </div>
    </div>

    <section class="review-section" v-if="product">
      <div class="review-head">
        <div>
          <h3>用户评价</h3>
          <p>真实反馈会保留细节，也会帮助后来的人更快做决定</p>
        </div>
        <div class="review-score">
          <strong>{{ Number(reviewSummary.avgRating || 0).toFixed(1) }}</strong>
          <span>综合评分</span>
          <div class="score-stars">{{ renderStars(Math.round(reviewSummary.avgRating || 0)) }}</div>
        </div>
      </div>

      <div class="review-toolbar">
        <button :class="{ active: ratingFilter === null }" @click="changeRatingFilter(null)">
          全部 {{ reviewSummary.reviewCount || 0 }}
        </button>
        <button v-for="n in [5,4,3,2,1]" :key="n" :class="{ active: ratingFilter === n }" @click="changeRatingFilter(n)">
          {{ n }}星 {{ reviewSummary[`rating${n}`] || 0 }}
        </button>
      </div>

      <div class="review-form">
        <div class="form-title">写下你的使用感受</div>
        <div class="star-picker">
          <button
            v-for="n in 5"
            :key="n"
            :class="{ active: reviewForm.rating >= n }"
            @click="reviewForm.rating = n"
            type="button"
          >★</button>
          <span>{{ reviewForm.rating }} 星</span>
        </div>
        <textarea v-model.trim="reviewForm.content" maxlength="500" placeholder="说说包装、质感、使用体验，5个字以上即可"></textarea>
        <div class="form-bottom">
          <span>{{ reviewForm.content.length }}/500</span>
          <button class="submit-review" :disabled="reviewSubmitting" @click="submitReview">
            {{ reviewSubmitting ? '提交中...' : '发布评价' }}
          </button>
        </div>
      </div>

      <div class="review-list" :class="{ loading: reviewLoading }">
        <div v-if="reviewLoading" class="review-empty">评价加载中...</div>
        <div v-if="!reviewLoading && reviews.length === 0" class="review-empty">
          暂无评价，等你来分享第一条体验。
        </div>
        <div v-for="item in reviews" :key="item.id" class="review-card">
          <div class="review-user">
            <UserAvatar :src="item.avatar" :name="item.nickname" :size="36" />
            <div>
              <strong>{{ item.nickname || '用户' }}</strong>
              <span>{{ formatReviewTime(item.createTime) }}</span>
            </div>
          </div>
          <div class="review-rating">{{ renderStars(item.rating) }}</div>
          <p class="review-content">{{ item.content }}</p>
        </div>
      </div>

      <div class="review-more" v-if="reviewTotal > reviews.length">
        <button @click="loadMoreReviews">查看更多评价</button>
      </div>
    </section>

    <!-- Related Products -->
    <section class="related-section" v-if="relatedProducts.length">
      <div class="related-header">
        <h3>相关推荐</h3>
        <router-link :to="`/category/${product?.categoryId}`" class="more-link">
          查看更多 →
        </router-link>
      </div>
      <div class="related-grid">
        <ProductCard
          v-for="p in relatedProducts"
          :key="p.id"
          :product="p"
          @click="$router.push(`/product/${p.id}`)"
          @add-cart="handleAddCartRelated"
        />
      </div>
    </section>

    <!-- Image Preview Modal -->
    <Teleport to="body">
      <div class="preview-modal" v-if="previewVisible" @click="previewVisible = false">
        <button class="preview-close" @click="previewVisible = false">
          <el-icon :size="24"><Close /></el-icon>
        </button>
        <button class="preview-nav prev" @click="prevImage" v-if="images.length > 1">
          <el-icon :size="24"><ArrowLeft /></el-icon>
        </button>
        <img :src="images[previewIndex]" class="preview-image" @click.stop />
        <button class="preview-nav next" @click="nextImage" v-if="images.length > 1">
          <el-icon :size="24"><ArrowRight /></el-icon>
        </button>
        <div class="preview-info" v-if="images.length > 1">
          {{ previewIndex + 1 }} / {{ images.length }}
        </div>
      </div>
    </Teleport>

    <!-- Floating Buy Bar -->
    <transition name="slide-up">
      <div class="floating-bar" v-if="showFloatingBar && product && product.stock > 0">
        <div class="floating-info">
          <img :src="currentImage" class="floating-thumb" />
          <div class="floating-text">
            <span class="floating-name">{{ product.name }}</span>
            <span class="floating-price">¥{{ product.price }}</span>
          </div>
        </div>
        <div class="floating-actions">
          <button class="floating-cart" @click="handleAddCart">
            <el-icon :size="18"><ShoppingCart /></el-icon>
            加入购物车
          </button>
          <button class="floating-buy" @click="handleBuyNow">立即购买</button>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, reactive, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  getProductDetail,
  addToCart,
  getProductList,
  getProductReviews,
  getProductReviewSummary,
  createProductReview,
  addFavorite,
  removeFavorite
} from '../api'
import { useCartStore } from '../store/cart'
import { useUserStore } from '../store/user'
import { ElMessage } from 'element-plus'
import {
  ZoomIn, Star, Share, TrendCharts, CircleCheck,
  ShoppingCart, RefreshRight, Lightning, Close,
  ArrowLeft, ArrowRight
} from '@element-plus/icons-vue'
import ProductCard from '../components/ProductCard.vue'
import UserAvatar from '../components/UserAvatar.vue'

const route = useRoute()
const router = useRouter()
const cartStore = useCartStore()
const userStore = useUserStore()

const product = ref(null)
const quantity = ref(1)
const loading = ref(true)
const addingCart = ref(false)
const isFavorited = ref(false)
const currentImageIndex = ref(0)
const previewVisible = ref(false)
const previewIndex = ref(0)
const showFloatingBar = ref(false)
const relatedProducts = ref([])
const reviews = ref([])
const reviewSummary = ref({ avgRating: 0, reviewCount: 0 })
const reviewTotal = ref(0)
const reviewPage = ref(1)
const reviewPageSize = 5
const reviewLoading = ref(false)
const reviewSubmitting = ref(false)
const ratingFilter = ref(null)
const reviewForm = reactive({
  rating: 5,
  content: ''
})

const images = computed(() => {
  if (Array.isArray(product.value?.imageList) && product.value.imageList.length) {
    return product.value.imageList
  }
  if (product.value?.imageUrl) {
    return [product.value.imageUrl]
  }
  return []
})

const currentImage = computed(() => {
  return images.value[currentImageIndex.value] || product.value?.imageUrl || '/placeholder.svg'
})

const promo = ref({
  text: '满99减10，领取优惠券',
  tag: '限时优惠'
})

const priceStr = computed(() => Number(product.value?.price || 0).toFixed(2))
const priceInt = computed(() => priceStr.value.split('.')[0])
const priceDec = computed(() => priceStr.value.split('.')[1])

const renderStars = (rating = 0) => {
  const value = Math.max(0, Math.min(5, Number(rating) || 0))
  return '★'.repeat(value) + '☆'.repeat(5 - value)
}

const loadProduct = async () => {
  loading.value = true
  try {
    const res = await getProductDetail(route.params.id)
    product.value = res.data
    isFavorited.value = !!res.data?.favorited
    quantity.value = 1
    currentImageIndex.value = 0
    relatedProducts.value = []
    reviews.value = []
    reviewTotal.value = 0
    reviewPage.value = 1
    ratingFilter.value = null
    reviewForm.rating = 5
    reviewForm.content = ''

    // Load related products
    if (product.value?.categoryId) {
      try {
        const relatedRes = await getProductList({
          page: 1,
          size: 4,
          categoryId: product.value.categoryId
        })
        relatedProducts.value = (relatedRes.data?.records || relatedRes.data || [])
          .filter(p => p.id !== product.value.id)
          .slice(0, 4)
      } catch (e) {
        console.error(e)
      }
    }
    await Promise.all([loadReviewSummary(), loadReviews(true)])
  } catch (error) {
    ElMessage.error('商品不存在')
    router.push('/')
  } finally {
    loading.value = false
  }
}

const loadReviewSummary = async () => {
  if (!product.value?.id) return
  try {
    const res = await getProductReviewSummary(product.value.id, { silentError: true })
    reviewSummary.value = res.data || { avgRating: 0, reviewCount: 0 }
  } catch (e) {
    reviewSummary.value = { avgRating: 0, reviewCount: 0 }
  }
}

const loadReviews = async (reset = false) => {
  if (!product.value?.id) return
  if (reset) {
    reviewPage.value = 1
    reviews.value = []
  }
  reviewLoading.value = true
  try {
    const params = {
      page: reviewPage.value,
      size: reviewPageSize
    }
    if (ratingFilter.value) params.rating = ratingFilter.value
    const res = await getProductReviews(product.value.id, params, { silentError: true })
    const pageData = res.data || {}
    const records = pageData.records || []
    reviews.value = reset ? records : reviews.value.concat(records)
    reviewTotal.value = pageData.total || reviews.value.length
  } catch (e) {
    if (reset) reviews.value = []
    reviewTotal.value = reviews.value.length
  } finally {
    reviewLoading.value = false
  }
}

const changeRatingFilter = (rating) => {
  ratingFilter.value = rating
  loadReviews(true)
}

const loadMoreReviews = () => {
  reviewPage.value += 1
  loadReviews(false)
}

const submitReview = async () => {
  if (!userStore.isLogin) {
    ElMessage.warning('请先登录后再评价')
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  if (reviewForm.content.trim().length < 5) {
    ElMessage.warning('评价内容至少5个字')
    return
  }
  reviewSubmitting.value = true
  try {
    await createProductReview({
      productId: product.value.id,
      rating: reviewForm.rating,
      content: reviewForm.content
    })
    ElMessage.success('评价发布成功')
    reviewForm.rating = 5
    reviewForm.content = ''
    await Promise.all([loadReviewSummary(), loadReviews(true)])
  } finally {
    reviewSubmitting.value = false
  }
}

const formatReviewTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleDateString('zh-CN')
}

const openPreview = (index) => {
  previewIndex.value = index
  previewVisible.value = true
}

const prevImage = () => {
  previewIndex.value = previewIndex.value > 0 ? previewIndex.value - 1 : images.value.length - 1
}

const nextImage = () => {
  previewIndex.value = previewIndex.value < images.value.length - 1 ? previewIndex.value + 1 : 0
}

const toggleFavorite = () => {
  if (!userStore.isLogin) {
    ElMessage.warning('请先登录后再收藏')
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  const nextState = !isFavorited.value
  const request = nextState ? addFavorite(product.value.id) : removeFavorite(product.value.id)
  Promise.resolve(request)
    .then(() => {
      isFavorited.value = nextState
      if (product.value) {
        const currentCount = Number(product.value.favoriteCount || 0)
        product.value.favoriteCount = nextState ? currentCount + 1 : Math.max(currentCount - 1, 0)
        product.value.favorited = nextState
      }
      ElMessage.success(nextState ? '已添加到收藏' : '已取消收藏')
    })
    .catch(() => {})
}

const shareProduct = () => {
  const targetUrl = window.location.href
  if (navigator.clipboard?.writeText) {
    navigator.clipboard.writeText(targetUrl)
      .then(() => ElMessage.success('分享链接已复制到剪贴板'))
      .catch(() => ElMessage.success('请手动复制当前页面链接'))
    return
  }
  ElMessage.success('请手动复制当前页面链接')
}

const validateQty = () => {
  if (quantity.value < 1) quantity.value = 1
  if (quantity.value > product.value?.stock) quantity.value = product.value?.stock || 1
}

const handleAddCart = async () => {
  addingCart.value = true
  try {
    await addToCart({ productId: product.value.id, quantity: quantity.value })
    cartStore.addCartItem({
      productId: product.value.id,
      quantity: quantity.value,
      price: product.value.price
    })
    ElMessage.success(`已加入购物车（${quantity.value}件）`)
  } catch (error) {
    ElMessage.error('请先登录')
    router.push('/login')
  } finally {
    addingCart.value = false
  }
}

const handleAddCartRelated = async (p) => {
  try {
    await addToCart({ productId: p.id, quantity: 1 })
    cartStore.addCartItem({ productId: p.id, quantity: 1, price: p.price })
    ElMessage.success('已加入购物车')
  } catch (error) {
    ElMessage.error('请先登录')
    router.push('/login')
  }
}

const handleBuyNow = async () => {
  await handleAddCart()
  if (!addingCart.value) {
    router.push('/cart')
  }
}

// Floating bar scroll handler (throttled with rAF)
let scrollTicking = false
const handleScroll = () => {
  if (!scrollTicking) {
    requestAnimationFrame(() => {
      showFloatingBar.value = window.scrollY > 600
      scrollTicking = false
    })
    scrollTicking = true
  }
}

onMounted(() => {
  loadProduct()
  window.addEventListener('scroll', handleScroll, { passive: true })
})

watch(() => route.params.id, () => {
  loadProduct()
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>

<style scoped>
.detail-page {
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 24px 60px;
}

.breadcrumb {
  padding: 20px 0;
  font-size: 13px;
  color: #999;
  display: flex;
  align-items: center;
  gap: 8px;
}

.breadcrumb a {
  color: #666;
  transition: color 0.2s;
  text-decoration: none;
}

.breadcrumb a:hover {
  color: #c45c3e;
}

.sep {
  color: #ddd;
}

.detail-main {
  display: flex;
  gap: 48px;
  padding-bottom: 40px;
}

/* ===== Gallery ===== */
.detail-gallery {
  flex: 0 0 500px;
}

.main-image {
  position: relative;
  width: 100%;
  aspect-ratio: 1/1;
  border-radius: 10px;
  overflow: hidden;
  background: #fafaf8;
  cursor: zoom-in;
}

.main-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.main-image:hover img {
  transform: scale(1.02);
}

.zoom-hint {
  position: absolute;
  bottom: 16px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  background: rgba(0,0,0,0.6);
  color: #fff;
  font-size: 12px;
  border-radius: 10px;
  opacity: 0;
  transition: opacity 0.2s;
}

.main-image:hover .zoom-hint {
  opacity: 1;
}

.thumbnail-list {
  display: flex;
  gap: 12px;
  margin-top: 16px;
  overflow-x: auto;
}

.thumbnail {
  width: 80px;
  height: 80px;
  flex-shrink: 0;
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  border: 2px solid transparent;
  transition: all 0.2s;
}

.thumbnail img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.thumbnail:hover {
  border-color: #c45c3e;
}

.thumbnail.active {
  border-color: #c45c3e;
  box-shadow: 0 0 0 2px rgba(196, 92, 62, 0.2);
}

/* ===== Info ===== */
.detail-info {
  flex: 1;
  padding-top: 8px;
}

.product-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.detail-info h1 {
  font-size: 26px;
  font-weight: 700;
  color: #111;
  margin: 0;
  line-height: 1.3;
  flex: 1;
  padding-right: 20px;
}

.product-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border: 1.5px solid #e5e5e5;
  border-radius: 8px;
  background: #fff;
  color: #666;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.action-btn:hover {
  border-color: #c45c3e;
  color: #c45c3e;
}

.action-btn.active {
  background: #fef3e6;
  border-color: #e6a23c;
  color: #e6a23c;
}

.desc-text {
  font-size: 15px;
  color: #888;
  line-height: 1.6;
  margin: 0 0 24px;
}

/* ===== Price Block ===== */
.price-block {
  background: #f7f7f5;
  border-radius: 8px;
  padding: 20px 24px;
  margin-bottom: 20px;
}

.price-main {
  display: flex;
  align-items: center;
  gap: 16px;
}

.price-label {
  font-size: 14px;
  color: #999;
}

.price-value {
  display: flex;
  align-items: baseline;
  color: #f56c6c;
}

.price-value .symbol {
  font-size: 18px;
  font-weight: 600;
}

.price-value .amount {
  font-size: 40px;
  font-weight: 700;
  line-height: 1;
}

.price-value .decimal {
  font-size: 18px;
  font-weight: 600;
}

.price-meta {
  display: flex;
  gap: 16px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed #e5e5e5;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #67c23a;
}

/* ===== Promo ===== */
.promo-block {
  background: #fff5f5;
  border-radius: 10px;
  padding: 14px 16px;
  margin-bottom: 20px;
}

.promo-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.promo-tag {
  padding: 3px 8px;
  background: #f56c6c;
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  border-radius: 4px;
}

.promo-text {
  font-size: 13px;
  color: #f56c6c;
}

/* ===== Meta Row ===== */
.meta-row {
  display: flex;
  gap: 32px;
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.meta-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.meta-label {
  font-size: 13px;
  color: #999;
}

.meta-value {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.meta-value.low-stock {
  color: #e6a23c;
}

.stock-hint {
  color: #e6a23c;
  font-size: 12px;
}

.meta-value.free {
  color: #67c23a;
}

/* ===== Quantity ===== */
.quantity-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 28px;
}

.qty-control {
  display: flex;
  align-items: center;
  border: 1.5px solid #e5e5e5;
  border-radius: 10px;
  overflow: hidden;
}

.qty-control button {
  width: 40px;
  height: 40px;
  border: none;
  background: #fafaf8;
  font-size: 18px;
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

.qty-control input {
  width: 60px;
  height: 40px;
  border: none;
  border-left: 1.5px solid #e5e5e5;
  border-right: 1.5px solid #e5e5e5;
  text-align: center;
  font-size: 15px;
  font-weight: 600;
  color: #111;
  outline: none;
  -moz-appearance: textfield;
}

.qty-control input::-webkit-outer-spin-button,
.qty-control input::-webkit-inner-spin-button {
  -webkit-appearance: none;
}

.qty-hint {
  font-size: 13px;
  color: #e6a23c;
}

/* ===== Action Buttons ===== */
.action-row {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.btn-cart,
.btn-buy {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 14px 24px;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
}

.btn-cart {
  background: #fff;
  color: #111;
  border: 1.5px solid #e5e5e5;
}

.btn-cart:hover:not(:disabled) {
  border-color: #c45c3e;
  color: #c45c3e;
}

.btn-buy {
  background: #1a1a1a;
  color: #fff;
  box-shadow: 0 4px 16px rgba(26, 26, 26, 0.15);
}

.btn-buy:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(26, 26, 26, 0.2);
}

.btn-cart:disabled,
.btn-buy:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* ===== Guarantee ===== */
.guarantee-row {
  display: flex;
  gap: 20px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
}

.guarantee-row span {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #909399;
}

/* ===== Reviews ===== */
.review-section {
  background: #fff;
  border: 1px solid #f0eeea;
  border-radius: 14px;
  padding: 24px;
  margin: 20px 0 48px;
}

.review-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f4f1ec;
}

.review-head h3 {
  font-size: 20px;
  font-weight: 700;
  color: #111;
  margin: 0 0 8px;
}

.review-head p {
  color: #888;
  font-size: 14px;
  margin: 0;
}

.review-score {
  text-align: right;
  min-width: 120px;
}

.review-score strong {
  display: block;
  font-size: 36px;
  line-height: 1;
  color: #c45c3e;
}

.review-score span {
  display: block;
  color: #999;
  font-size: 12px;
  margin-top: 4px;
}

.score-stars,
.review-rating {
  color: #e6a23c;
  letter-spacing: 1px;
  font-size: 14px;
}

.review-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 18px 0;
}

.review-toolbar button {
  border: 1px solid #eee6dc;
  background: #faf8f5;
  color: #666;
  padding: 7px 13px;
  border-radius: 999px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.review-toolbar button.active,
.review-toolbar button:hover {
  background: #1a1a1a;
  border-color: #1a1a1a;
  color: #fff;
}

.review-form {
  background: #faf9f7;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 18px;
}

.form-title {
  font-size: 14px;
  font-weight: 700;
  color: #333;
  margin-bottom: 10px;
}

.star-picker {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 10px;
}

.star-picker button {
  border: none;
  background: transparent;
  color: #ddd;
  font-size: 22px;
  cursor: pointer;
  transition: color 0.15s;
}

.star-picker button.active {
  color: #e6a23c;
}

.star-picker span {
  color: #888;
  font-size: 13px;
  margin-left: 8px;
}

.review-form textarea {
  width: 100%;
  min-height: 88px;
  resize: vertical;
  border: 1px solid #ebe5dc;
  border-radius: 10px;
  background: #fff;
  padding: 12px;
  font-size: 14px;
  line-height: 1.6;
  color: #333;
  outline: none;
  box-sizing: border-box;
}

.review-form textarea:focus {
  border-color: #c45c3e;
  box-shadow: 0 0 0 3px rgba(196, 92, 62, 0.08);
}

.form-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
  color: #aaa;
  font-size: 12px;
}

.submit-review {
  border: none;
  background: #1a1a1a;
  color: #fff;
  border-radius: 8px;
  padding: 9px 18px;
  cursor: pointer;
  font-weight: 600;
}

.submit-review:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.review-list {
  min-height: 80px;
}

.review-empty {
  padding: 34px 0;
  text-align: center;
  color: #aaa;
  font-size: 14px;
}

.review-card {
  padding: 18px 0;
  border-bottom: 1px solid #f4f1ec;
}

.review-card:last-child {
  border-bottom: none;
}

.review-user {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.review-user strong {
  display: block;
  color: #333;
  font-size: 14px;
}

.review-user span {
  display: block;
  color: #aaa;
  font-size: 12px;
  margin-top: 2px;
}

.review-content {
  color: #555;
  font-size: 14px;
  line-height: 1.7;
  margin: 8px 0 0;
}

.review-more {
  text-align: center;
  padding-top: 14px;
}

.review-more button {
  border: 1px solid #e5e5e5;
  background: #fff;
  color: #555;
  border-radius: 8px;
  padding: 9px 20px;
  cursor: pointer;
}

/* ===== Related Products ===== */
.related-section {
  margin-top: 48px;
  padding-top: 40px;
  border-top: 1px solid #f0f0f0;
}

.related-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.related-header h3 {
  font-size: 20px;
  font-weight: 700;
  color: #111;
  margin: 0;
}

.more-link {
  color: #c45c3e;
  font-size: 14px;
  font-weight: 500;
  text-decoration: none;
}

.more-link:hover {
  color: #a84830;
}

.related-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

/* ===== Preview Modal ===== */
.preview-modal {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.92);
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

.preview-close {
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

.preview-close:hover {
  background: rgba(255,255,255,0.2);
}

.preview-nav {
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

.preview-nav:hover {
  background: rgba(255,255,255,0.2);
}

.preview-nav.prev {
  left: 20px;
}

.preview-nav.next {
  right: 20px;
}

.preview-image {
  max-width: 90vw;
  max-height: 90vh;
  object-fit: contain;
  border-radius: 8px;
}

.preview-info {
  position: absolute;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  color: rgba(255,255,255,0.7);
  font-size: 14px;
}

/* ===== Floating Bar ===== */
.floating-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fff;
  border-top: 1px solid #e5e5e5;
  box-shadow: 0 -4px 20px rgba(0,0,0,0.08);
  padding: 12px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  z-index: 100;
}

.floating-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.floating-thumb {
  width: 56px;
  height: 56px;
  border-radius: 8px;
  object-fit: cover;
  background: #f5f5f5;
}

.floating-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.floating-name {
  font-size: 14px;
  color: #333;
  font-weight: 500;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.floating-price {
  font-size: 18px;
  color: #f56c6c;
  font-weight: 700;
}

.floating-actions {
  display: flex;
  gap: 12px;
}

.floating-cart,
.floating-buy {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 12px 24px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
}

.floating-cart {
  background: #fff;
  color: #111;
  border: 1.5px solid #e5e5e5;
}

.floating-cart:hover {
  border-color: #c45c3e;
  color: #c45c3e;
}

.floating-buy {
  background: #1a1a1a;
  color: #fff;
}

.floating-buy:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(26, 26, 26, 0.15);
}

.slide-up-enter-active,
.slide-up-leave-active {
  transition: transform 0.3s ease;
}

.slide-up-enter-from,
.slide-up-leave-to {
  transform: translateY(100%);
}

/* ===== Skeleton ===== */
.sk-line { border-radius: 6px; background: #f0f0f0; margin-bottom: 12px; }
.sk-block { border-radius: 12px; background: #f0f0f0; margin-bottom: 12px; }
.sk-btn { width: 140px; height: 48px; border-radius: 10px; background: #f0f0f0; }
@keyframes skeleton-pulse { 0%,100%{opacity:1} 50%{opacity:0.4} }
.skeleton-pulse { animation: skeleton-pulse 1.5s ease-in-out infinite; }

@media (max-width: 768px) {
  .detail-main { flex-direction: column; gap: 24px; }
  .detail-gallery { flex: none; max-width: 100%; }
  .review-head { flex-direction: column; }
  .review-score { text-align: left; }
  .related-grid { grid-template-columns: repeat(2, 1fr); }
  .floating-bar { padding: 12px 16px; }
  .floating-info { display: none; }
}
</style>
