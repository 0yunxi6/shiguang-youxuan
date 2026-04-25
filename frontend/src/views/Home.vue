<template>
  <div class="home-page">
    <section class="hero-section">
      <div class="hero-carousel" @mouseenter="pauseAutoPlay" @mouseleave="startAutoPlay">
        <div class="carousel-track" :style="{ transform: `translateX(-${currentSlide * 100}%)` }">
          <div v-for="(banner, i) in banners" :key="i" class="carousel-slide" :style="{ background: banner.bg }">
            <div class="slide-inner">
              <div class="slide-tag">{{ banner.tag }}</div>
              <h2 class="slide-title">{{ banner.title }}</h2>
              <p class="slide-desc">{{ banner.desc }}</p>
              <button class="slide-cta" @click="$router.push(banner.link)">{{ banner.cta }}
                <svg width="14" height="14" viewBox="0 0 16 16" fill="none"><path d="M6 3l5 5-5 5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
              </button>
            </div>
          </div>
        </div>
        <div class="carousel-nav">
          <button v-for="(_, i) in banners" :key="i" :class="{ active: i === currentSlide }" @click="goToSlide(i)"></button>
        </div>
      </div>
    </section>

    <section class="category-nav" v-if="categories.length">
      <div class="cat-row">
        <span v-for="cat in categories" :key="cat.id" class="cat-link" @click="$router.push(`/category/${cat.id}`)">{{ cat.name }}</span>
      </div>
    </section>

    <section class="section flash-section" v-if="flashProducts.length">
      <div class="section-bar">
        <div class="section-title-group">
          <h2>限时特惠</h2>
          <div class="countdown-box">
            <span class="cd-unit">{{ countdown.hours }}</span><span class="cd-sep">:</span>
            <span class="cd-unit">{{ countdown.minutes }}</span><span class="cd-sep">:</span>
            <span class="cd-unit">{{ countdown.seconds }}</span>
          </div>
        </div>
        <router-link to="/search?tag=flash" class="see-more">查看更多</router-link>
      </div>
      <div class="flash-row">
        <div v-for="product in flashProducts" :key="product.id" class="flash-item" @click="$router.push(`/product/${product.id}`)">
          <div class="flash-img"><img :src="product.imageUrl || '/placeholder.svg'" :alt="product.name" /><span class="flash-badge">特惠</span></div>
          <div class="flash-meta">
            <span class="flash-name">{{ product.name }}</span>
            <div class="flash-prices"><span class="flash-now">¥{{ product.price }}</span><span class="flash-was">¥{{ (product.price * 1.3).toFixed(0) }}</span></div>
          </div>
        </div>
      </div>
    </section>

    <section class="section product-feed" ref="productsRef">
      <div class="section-bar"><h2>为你推荐</h2></div>
      <div class="sort-tabs">
        <button v-for="tab in sortTabs" :key="tab.value" :class="{ active: currentSort === tab.value }" @click="changeSort(tab.value)">{{ tab.label }}</button>
        <span v-if="selectedCategory" class="active-filter">{{ selectedCategoryName }}<button @click="clearCategory">&times;</button></span>
      </div>
      <div class="product-grid" v-if="loading">
        <div v-for="i in 8" :key="i" class="skeleton-card"><div class="sk-img"></div><div class="sk-body"><div class="sk-line" style="width:75%"></div><div class="sk-line" style="width:55%"></div><div class="sk-line short"></div></div></div>
      </div>
      <div class="product-grid" v-else-if="products.length">
        <ProductCard v-for="product in products" :key="product.id" :product="product" @click="$router.push(`/product/${product.id}`)" @add-cart="handleAddCart" @toggle-favorite="toggleFavorite" />
      </div>
      <div v-else class="empty-state"><p class="empty-title">暂无商品</p><p class="empty-hint">看看其他分类吧</p></div>
      <div class="load-more" v-if="products.length >= pageSize && !loading">
        <button @click="loadMore" :disabled="loadingMore">{{ loadingMore ? '加载中...' : '加载更多' }}</button>
      </div>
    </section>

    <section class="trust-bar">
      <div class="trust-item" v-for="f in trustItems" :key="f.title">
        <svg v-html="f.svg" width="20" height="20" viewBox="0 0 20 20" fill="none"></svg>
        <div><span class="trust-title">{{ f.title }}</span><span class="trust-desc">{{ f.desc }}</span></div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { getProductList } from '../api'
import { useCartStore } from '../store/cart'
import { useCategoryStore } from '../store/category'
import { useAddToCart } from '../composables/useAddToCart'
import ProductCard from '../components/ProductCard.vue'
import { ElMessage } from 'element-plus'

const router = useRouter()
const cartStore = useCartStore()
const categoryStore = useCategoryStore()
const { handleAddCart } = useAddToCart()
const products = ref([])
const categories = computed(() => categoryStore.categories)
const productsRef = ref(null)
const loading = ref(true)
const loadingMore = ref(false)
const currentSort = ref('default')
const selectedCategory = ref(null)
const currentPage = ref(1)
const pageSize = 12
const total = ref(0)
const currentSlide = ref(0)
let autoPlayTimer = null

const banners = [
  { tag: '应季精选', title: '春日焕新，好物上新', desc: '精选当季好物，品质生活从这里开始', bg: 'linear-gradient(135deg, #3a3a3a 0%, #1a1a1a 100%)', cta: '去逛逛', link: '/search?keyword=新品' },
  { tag: '限时活动', title: '品质不打折，价格更实在', desc: '严选好物低至5折，限时抢购中', bg: 'linear-gradient(135deg, #c45c3e 0%, #8b3a25 100%)', cta: '立即抢购', link: '/search?tag=flash' },
  { tag: '会员专享', title: '每周上新，惊喜不断', desc: '注册即享新人专属优惠', bg: 'linear-gradient(135deg, #5a6e5a 0%, #3d4d3d 100%)', cta: '了解详情', link: '/search?keyword=推荐' }
]
const flashProducts = ref([])
const countdown = reactive({ hours: '00', minutes: '00', seconds: '00' })
let countdownTimer = null
const sortTabs = [{ label: '综合', value: 'default' }, { label: '销量', value: 'sales' }, { label: '价格', value: 'price_asc' }, { label: '最新', value: 'newest' }]
const trustItems = [
  { title: '满99包邮', desc: '全场免运费', svg: '<path d="M2 14h16M4 14V6a2 2 0 012-2h8a2 2 0 012 2v8M7 14v3M13 14v3M5 17h4M11 17h4" stroke="#999" stroke-width="1.2" stroke-linecap="round"/>' },
  { title: '7天无理由', desc: '放心退换', svg: '<path d="M4 10a6 6 0 1011.3 3M15.3 13l1-3-3 1" stroke="#999" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"/>' },
  { title: '正品保障', desc: '品质承诺', svg: '<path d="M10 2l6 3v5c0 3.5-2.5 6.5-6 8-3.5-1.5-6-4.5-6-8V5l6-3z" stroke="#999" stroke-width="1.2"/><path d="M7 10l2 2 4-4" stroke="#999" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"/>' },
  { title: '在线客服', desc: '随时响应', svg: '<path d="M3 10a7 7 0 1114 0M3 10v3a2 2 0 002 2h1M17 10v3a2 2 0 01-2 2h-1M7 15h6" stroke="#999" stroke-width="1.2" stroke-linecap="round"/>' }
]

const selectedCategoryName = computed(() => categories.value.find(c => c.id === selectedCategory.value)?.name || '')
const nextSlide = () => { currentSlide.value = (currentSlide.value + 1) % banners.length }
const goToSlide = (i) => { currentSlide.value = i }
const startAutoPlay = () => { autoPlayTimer = setInterval(nextSlide, 5000) }
const pauseAutoPlay = () => { clearInterval(autoPlayTimer) }
const updateCountdown = () => {
  const now = new Date(), end = new Date(); end.setHours(23, 59, 59, 999)
  const d = end - now
  countdown.hours = String(Math.floor(d / 3600000)).padStart(2, '0')
  countdown.minutes = String(Math.floor((d % 3600000) / 60000)).padStart(2, '0')
  countdown.seconds = String(Math.floor((d % 60000) / 1000)).padStart(2, '0')
}
const loadProducts = async (append = false) => {
  if (append) loadingMore.value = true; else { loading.value = true; currentPage.value = 1 }
  try {
    const params = { page: currentPage.value, size: pageSize, sort: currentSort.value }
    if (selectedCategory.value) params.categoryId = selectedCategory.value
    const res = await getProductList(params)
    const list = res.data?.records || res.data || []
    products.value = append ? [...products.value, ...list] : list
    total.value = res.data?.total || products.value.length
  } catch (e) { console.error(e) } finally { loading.value = false; loadingMore.value = false }
}
const loadMore = async () => { if (products.value.length >= total.value) return; currentPage.value++; await loadProducts(true) }
const changeSort = (s) => { currentSort.value = s; loadProducts() }
const clearCategory = () => { selectedCategory.value = null; loadProducts() }
const toggleFavorite = (p) => { ElMessage.success(p._favorited ? '已取消收藏' : '已添加收藏') }
onMounted(async () => {
  try {
    const [prodRes] = await Promise.all([getProductList({ page: 1, size: 12 }), categoryStore.fetchCategories()])
    products.value = prodRes.data?.records || prodRes.data || []
    total.value = prodRes.data?.total || products.value.length
    flashProducts.value = products.value.slice(0, 4)
  } catch (e) { console.error(e) } finally { loading.value = false }
  startAutoPlay(); updateCountdown(); countdownTimer = setInterval(updateCountdown, 1000)
})
onUnmounted(() => { pauseAutoPlay(); clearInterval(countdownTimer) })
</script>

<style scoped>
.home-page { max-width: 1200px; margin: 0 auto; padding: 0 20px 40px; }

/* Hero */
.hero-section { padding: 20px 0 0; }
.hero-carousel { position: relative; border-radius: 10px; overflow: hidden; }
.carousel-track { display: flex; transition: transform 0.45s ease; }
.carousel-slide { min-width: 100%; height: 320px; }
.slide-inner { max-width: 1200px; margin: 0 auto; padding: 56px 48px; height: 100%; display: flex; flex-direction: column; justify-content: center; }
.slide-tag { display: inline-block; padding: 4px 12px; background: rgba(255,255,255,0.15); border-radius: 3px; color: rgba(255,255,255,0.85); font-size: 12px; letter-spacing: 1px; margin-bottom: 16px; width: fit-content; }
.slide-title { font-size: 28px; font-weight: 700; color: #fff; margin: 0 0 10px; line-height: 1.3; }
.slide-desc { font-size: 14px; color: rgba(255,255,255,0.6); margin: 0 0 24px; }
.slide-cta { display: inline-flex; align-items: center; gap: 6px; padding: 10px 24px; background: #fff; color: #1a1a1a; border: none; border-radius: 6px; font-size: 13px; font-weight: 600; cursor: pointer; transition: opacity 0.2s; width: fit-content; }
.slide-cta:hover { opacity: 0.9; }
.carousel-nav { position: absolute; bottom: 16px; left: 48px; display: flex; gap: 6px; }
.carousel-nav button { width: 20px; height: 3px; border: none; background: rgba(255,255,255,0.3); border-radius: 2px; cursor: pointer; transition: all 0.3s; padding: 0; }
.carousel-nav button.active { width: 32px; background: #fff; }

/* Categories */
.category-nav { padding: 20px 0 8px; border-bottom: 1px solid #eee; }
.cat-row { display: flex; gap: 24px; overflow-x: auto; scrollbar-width: none; }
.cat-row::-webkit-scrollbar { display: none; }
.cat-link { flex-shrink: 0; font-size: 14px; color: #666; cursor: pointer; padding: 4px 0; transition: color 0.15s; white-space: nowrap; position: relative; }
.cat-link:hover { color: #1a1a1a; }
.cat-link::after { content: ''; position: absolute; bottom: 0; left: 0; right: 0; height: 2px; background: #1a1a1a; transform: scaleX(0); transition: transform 0.2s; }
.cat-link:hover::after { transform: scaleX(1); }

/* Sections */
.section { padding: 28px 0; }
.section-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.section-bar h2 { font-size: 18px; font-weight: 700; color: #1a1a1a; margin: 0; }
.section-title-group { display: flex; align-items: center; gap: 14px; }
.see-more { font-size: 13px; color: #999; transition: color 0.2s; }
.see-more:hover { color: #666; }

/* Countdown */
.countdown-box { display: flex; align-items: center; gap: 2px; }
.cd-unit { display: inline-flex; align-items: center; justify-content: center; width: 24px; height: 24px; background: #1a1a1a; color: #fff; border-radius: 4px; font-size: 12px; font-weight: 700; font-family: 'SF Mono', 'Menlo', monospace; }
.cd-sep { color: #1a1a1a; font-weight: 700; font-size: 12px; margin: 0 1px; }

/* Flash Sale */
.flash-section { background: #fff; margin: 0 -20px; padding: 28px 20px; border-radius: 8px; }
.flash-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.flash-item { cursor: pointer; border-radius: 8px; overflow: hidden; border: 1px solid #f0eeea; transition: border-color 0.2s; }
.flash-item:hover { border-color: #d0cec8; }
.flash-img { position: relative; aspect-ratio: 1/1; background: #f7f7f5; }
.flash-img img { width: 100%; height: 100%; object-fit: cover; }
.flash-badge { position: absolute; top: 8px; left: 8px; padding: 2px 8px; background: var(--color-primary, #c45c3e); color: #fff; font-size: 11px; font-weight: 500; border-radius: 3px; }
.flash-meta { padding: 10px 12px; }
.flash-name { font-size: 13px; color: #333; display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; margin-bottom: 6px; }
.flash-prices { display: flex; align-items: baseline; gap: 6px; }
.flash-now { font-size: 16px; font-weight: 700; color: var(--color-price, #c45c3e); }
.flash-was { font-size: 12px; color: #bbb; text-decoration: line-through; }

/* Sort Tabs */
.sort-tabs { display: flex; align-items: center; gap: 4px; margin-bottom: 16px; border-bottom: 1px solid #eee; padding-bottom: 12px; }
.sort-tabs button { padding: 6px 14px; border: none; background: transparent; font-size: 13px; color: #999; cursor: pointer; border-radius: 4px; transition: all 0.15s; }
.sort-tabs button:hover { color: #333; }
.sort-tabs button.active { color: #1a1a1a; font-weight: 600; background: #f2efe9; }
.active-filter { display: inline-flex; align-items: center; gap: 4px; margin-left: auto; padding: 4px 10px; background: #f2efe9; border-radius: 4px; font-size: 12px; color: #666; }
.active-filter button { border: none; background: none; cursor: pointer; color: #999; font-size: 14px; padding: 0 2px; }

/* Product Grid */
.product-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.empty-state { text-align: center; padding: 60px 0; }
.empty-title { font-size: 16px; color: #333; margin: 0 0 4px; }
.empty-hint { font-size: 13px; color: #999; margin: 0; }
.load-more { text-align: center; margin-top: 28px; }
.load-more button { padding: 10px 36px; border: 1px solid #d0cec8; background: #fff; color: #333; font-size: 13px; border-radius: 6px; cursor: pointer; transition: all 0.15s; }
.load-more button:hover { border-color: #999; }
.load-more button:disabled { opacity: 0.5; cursor: not-allowed; }

/* Trust Bar */
.trust-bar { display: flex; justify-content: space-between; padding: 24px 0; border-top: 1px solid #eee; margin-top: 12px; }
.trust-item { display: flex; align-items: center; gap: 10px; }
.trust-item svg { flex-shrink: 0; }
.trust-title { font-size: 13px; font-weight: 600; color: #333; display: block; }
.trust-desc { font-size: 11px; color: #999; }

/* Skeleton */
.skeleton-card { background: #fff; border-radius: 8px; overflow: hidden; border: 1px solid #f0eeea; }
.sk-img { aspect-ratio: 1/1; background: #f0eeea; animation: sk-pulse 1.5s ease-in-out infinite; }
.sk-body { padding: 14px; display: flex; flex-direction: column; gap: 8px; }
.sk-line { height: 12px; border-radius: 3px; background: #f0eeea; animation: sk-pulse 1.5s ease-in-out infinite; }
.sk-line.short { width: 35%; height: 18px; }
@keyframes sk-pulse { 0%,100% { opacity: 1; } 50% { opacity: 0.4; } }
@media (max-width: 1024px) { .product-grid, .flash-row { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 768px) {
  .slide-inner { padding: 36px 24px; }
  .slide-title { font-size: 22px; }
  .product-grid, .flash-row { grid-template-columns: repeat(2, 1fr); }
  .trust-bar { flex-wrap: wrap; gap: 16px; }
  .trust-item { flex: 0 0 calc(50% - 8px); }
  .carousel-nav { left: 24px; }
}
</style>
