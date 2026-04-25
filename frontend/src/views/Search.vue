<template>
  <div class="search-page">
    <div class="search-header">
      <h2>搜索结果</h2>
      <p class="result-info" v-if="keyword">
        "<span class="keyword">{{ keyword }}</span>" 共找到 <strong>{{ total }}</strong> 件商品
      </p>
    </div>

    <div class="search-filters">
      <div class="filter-tabs">
        <button
          v-for="tab in sortTabs"
          :key="tab.value"
          :class="{ active: currentSort === tab.value }"
          @click="changeSort(tab.value)"
        >
          {{ tab.label }}
          <span class="sort-icon" v-if="currentSort === tab.value">
            {{ tab.asc ? '↑' : '↓' }}
          </span>
        </button>
      </div>

      <div class="filter-right">
        <div class="price-range">
          <input
            v-model.number="minPrice"
            type="number"
            placeholder="最低价"
            class="price-input"
            @change="applyPriceFilter"
          />
          <span class="price-sep">-</span>
          <input
            v-model.number="maxPrice"
            type="number"
            placeholder="最高价"
            class="price-input"
            @change="applyPriceFilter"
          />
          <button class="price-btn" @click="applyPriceFilter">确定</button>
        </div>

        <span class="result-count">{{ total }} 件商品</span>
      </div>
    </div>

    <!-- Popular Tags -->
    <div class="popular-tags" v-if="popularTags.length && !keyword">
      <span class="tags-label">热门搜索：</span>
      <div class="tags-list">
        <button
          v-for="tag in popularTags"
          :key="tag"
          class="tag-btn"
          @click="searchByTag(tag)"
        >
          {{ tag }}
        </button>
      </div>
    </div>

    <!-- Loading -->
    <div class="product-grid" v-if="loading">
      <div v-for="i in 8" :key="i" class="skeleton-card">
        <div class="skeleton-img skeleton-pulse"></div>
        <div class="skeleton-body">
          <div class="skeleton-line skeleton-pulse" style="width:80%"></div>
          <div class="skeleton-line skeleton-pulse" style="width:60%"></div>
          <div class="skeleton-line skeleton-pulse short"></div>
        </div>
      </div>
    </div>

    <div class="product-grid" v-else-if="products.length > 0">
      <ProductCard
        v-for="product in products"
        :key="product.id"
        :product="product"
        @click="goToDetail(product.id)"
        @add-cart="handleAddCart"
      />
    </div>

    <div v-else-if="!loading" class="empty-state">
      <div class="empty-illustration">
        <svg viewBox="0 0 120 120" fill="none">
          <circle cx="60" cy="60" r="50" fill="#f5f7fa"/>
          <circle cx="50" cy="52" r="20" stroke="#e5e7eb" stroke-width="4" fill="none"/>
          <line x1="65" y1="67" x2="80" y2="82" stroke="#e5e7eb" stroke-width="4" stroke-linecap="round"/>
        </svg>
      </div>
      <p class="empty-title">未找到相关商品</p>
      <p class="empty-desc">换个关键词试试吧</p>
      <div class="search-suggestions" v-if="suggestions.length">
        <span class="suggestions-label">猜你想找：</span>
        <button
          v-for="s in suggestions"
          :key="s"
          class="suggestion-btn"
          @click="searchByTag(s)"
        >
          {{ s }}
        </button>
      </div>
    </div>

    <div class="pagination-wrap" v-if="total > pageSize && !loading">
      <button class="page-btn" :disabled="page <= 1" @click="changePage(page - 1)">
        上一页
      </button>
      <div class="page-numbers">
        <button
          v-for="p in visiblePages"
          :key="p"
          :class="{ active: p === page, ellipsis: p === '...' }"
          @click="p !== '...' && changePage(p)"
        >
          {{ p }}
        </button>
      </div>
      <button class="page-btn" :disabled="page >= totalPages" @click="changePage(page + 1)">
        下一页
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getProductList } from '../api'
import { useAddToCart } from '../composables/useAddToCart'
import { useVisiblePages } from '../composables/usePagination'
import ProductCard from '../components/ProductCard.vue'

const route = useRoute()
const router = useRouter()
const { handleAddCart } = useAddToCart()

const products = ref([])
const keyword = ref('')
const page = ref(1)
const pageSize = 12
const total = ref(0)
const loading = ref(true)
const currentSort = ref('default')
const sortAsc = ref(false)
const minPrice = ref(null)
const maxPrice = ref(null)

const popularTags = ref(['手机', '电脑', '运动鞋', '连衣裙', '零食', '护肤品'])
const suggestions = ref(['蓝牙耳机', '运动手表', '保温杯'])

const sortTabs = computed(() => [
  { label: '综合推荐', value: 'default', asc: false },
  { label: '销量', value: 'sales', asc: false },
  { label: '价格', value: 'price', asc: sortAsc.value },
  { label: '最新', value: 'newest', asc: false }
])

const { totalPages, visiblePages } = useVisiblePages(page, total, pageSize)

const loadProducts = async () => {
  loading.value = true
  try {
    const params = {
      page: page.value,
      size: pageSize
    }

    if (keyword.value) {
      params.keyword = keyword.value
    }

    if (currentSort.value === 'sales') {
      params.sort = 'sales'
    } else if (currentSort.value === 'price') {
      params.sort = sortAsc.value ? 'price_asc' : 'price_desc'
    } else if (currentSort.value === 'newest') {
      params.sort = 'newest'
    }

    if (minPrice.value) params.minPrice = minPrice.value
    if (maxPrice.value) params.maxPrice = maxPrice.value

    const res = await getProductList(params)
    products.value = res.data?.records || res.data || []
    total.value = res.data?.total || products.value.length
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const changeSort = (sort) => {
  if (currentSort.value === sort && sort === 'price') {
    sortAsc.value = !sortAsc.value
  }
  currentSort.value = sort
  page.value = 1
  loadProducts()
}

const applyPriceFilter = () => {
  page.value = 1
  loadProducts()
}

const changePage = (p) => {
  page.value = p
  window.scrollTo({ top: 0, behavior: 'smooth' })
  loadProducts()
}

const goToDetail = (id) => router.push(`/product/${id}`)

const searchByTag = (tag) => {
  router.push({ path: '/search', query: { keyword: tag } })
}


watch(() => route.query.keyword, (val) => {
  keyword.value = val || ''
  page.value = 1
  currentSort.value = 'default'
  minPrice.value = null
  maxPrice.value = null
  loadProducts()
}, { immediate: false })

onMounted(() => {
  keyword.value = route.query.keyword || ''
  loadProducts()
})
</script>

<style scoped>
.search-page {
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 24px 60px;
}

.search-header {
  margin-bottom: 24px;
}

.search-header h2 {
  font-size: 28px;
  font-weight: 700;
  color: #111;
  margin: 0 0 6px;
}

.result-info {
  font-size: 15px;
  color: #999;
  margin: 0;
}

.result-info strong {
  color: #c45c3e;
  font-weight: 600;
}

.keyword {
  color: #c45c3e;
  font-weight: 500;
}

/* ===== Filters ===== */
.search-filters {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: #fafaf8;
  border-radius: 8px;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 16px;
}

.filter-tabs {
  display: flex;
  gap: 4px;
}

.filter-tabs button {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 16px;
  border: none;
  background: transparent;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
}

.filter-tabs button:hover {
  background: #fff;
  color: #333;
}

.filter-tabs button.active {
  background: #c45c3e;
  color: #fff;
}

.sort-icon {
  font-size: 12px;
}

.filter-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.price-range {
  display: flex;
  align-items: center;
  gap: 8px;
}

.price-input {
  width: 80px;
  padding: 8px 12px;
  border: 1.5px solid #e5e5e5;
  border-radius: 8px;
  font-size: 13px;
  outline: none;
  transition: border-color 0.2s;
}

.price-input:focus {
  border-color: #c45c3e;
}

.price-input::placeholder {
  color: #bbb;
}

.price-sep {
  color: #999;
}

.price-btn {
  padding: 8px 14px;
  border: 1.5px solid #e5e5e5;
  border-radius: 8px;
  background: #fff;
  font-size: 13px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
}

.price-btn:hover {
  border-color: #c45c3e;
  color: #c45c3e;
}

.result-count {
  font-size: 13px;
  color: #999;
}

/* ===== Popular Tags ===== */
.popular-tags {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}

.tags-label {
  font-size: 14px;
  color: #999;
  font-weight: 500;
}

.tags-list {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.tag-btn {
  padding: 6px 14px;
  border: 1px solid #e5e5e5;
  border-radius: 10px;
  background: #fff;
  font-size: 13px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
}

.tag-btn:hover {
  border-color: #c45c3e;
  color: #c45c3e;
  background: #fafbff;
}

/* ===== Products ===== */
.product-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

/* ===== Empty State ===== */
.empty-state {
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

.search-suggestions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  flex-wrap: wrap;
}

.suggestions-label {
  font-size: 14px;
  color: #999;
}

.suggestion-btn {
  padding: 6px 14px;
  border: 1px dashed #e5e5e5;
  border-radius: 10px;
  background: #fafaf8;
  font-size: 13px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
}

.suggestion-btn:hover {
  border-color: #c45c3e;
  border-style: solid;
  color: #c45c3e;
}

/* ===== Pagination ===== */
.pagination-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 40px;
  padding-bottom: 40px;
}

.page-btn {
  padding: 8px 16px;
  border-radius: 8px;
  border: 1.5px solid #e5e5e5;
  background: #fff;
  color: #333;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.page-btn:hover:not(:disabled) {
  border-color: #c45c3e;
  color: #c45c3e;
}

.page-btn:disabled {
  color: #ccc;
  cursor: not-allowed;
}

.page-numbers {
  display: flex;
  gap: 4px;
}

.page-numbers button {
  min-width: 36px;
  height: 36px;
  border-radius: 8px;
  border: 1.5px solid #e5e5e5;
  background: #fff;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.page-numbers button:hover:not(:disabled):not(.ellipsis) {
  border-color: #c45c3e;
  color: #c45c3e;
}

.page-numbers button.active {
  background: #c45c3e;
  border-color: #c45c3e;
  color: #fff;
}

.page-numbers button.ellipsis {
  border: none;
  background: transparent;
  cursor: default;
}

/* ===== Skeleton ===== */
.skeleton-card {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #f0f0f0;
}

.skeleton-img {
  aspect-ratio: 1/1;
  background: #f0f0f0;
}

.skeleton-body {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.skeleton-line {
  height: 14px;
  border-radius: 4px;
  background: #f0f0f0;
}

.skeleton-line.short {
  width: 40%;
  height: 20px;
}

@keyframes skeleton-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.skeleton-pulse {
  animation: skeleton-pulse 1.5s ease-in-out infinite;
}

@media (max-width: 1024px) {
  .product-grid { grid-template-columns: repeat(3, 1fr); }
}

@media (max-width: 768px) {
  .product-grid { grid-template-columns: repeat(2, 1fr); }
  .filter-right { flex-wrap: wrap; }
}
</style>
