<template>
  <div class="category-page">
    <div class="breadcrumb">
      <router-link to="/">首页</router-link>
      <span class="sep">/</span>
      <span>{{ categoryName || '分类' }}</span>
    </div>

    <div class="category-layout">
      <!-- 侧边栏 -->
      <aside class="sidebar">
        <h3 class="sidebar-title">商品分类</h3>
        <nav class="cat-nav">
          <a v-for="cat in categories" :key="cat.id"
            :class="['cat-link', { active: categoryId === cat.id }]"
            @click="switchCategory(cat)">
            <span class="cat-dot"></span>
            {{ cat.name }}
          </a>
        </nav>
      </aside>

      <!-- 主内容 -->
      <div class="main-area">
        <div class="sort-bar">
          <button v-for="s in sortOptions" :key="s.value"
            :class="['sort-btn', { active: sortBy === s.value }]"
            @click="sortBy = s.value; loadProducts()">
            {{ s.label }}
          </button>
          <label class="stock-filter">
            <input v-model="inStockOnly" type="checkbox" @change="page = 1; loadProducts()" />
            仅看有货
          </label>
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
          <ProductCard v-for="product in products" :key="product.id" :product="product"
            @click="goToDetail(product.id)" @add-cart="handleAddCart" />
        </div>

        <div v-else class="empty-state">
          <div class="empty-icon">暂无</div>
          <p>该分类暂无商品</p>
        </div>

        <div class="pagination-wrap" v-if="total > pageSize">
          <button class="page-btn" :disabled="page <= 1" @click="changePage(page - 1)">上一页</button>
          <span class="page-info">{{ page }} / {{ Math.ceil(total / pageSize) }}</span>
          <button class="page-btn" :disabled="page >= Math.ceil(total / pageSize)" @click="changePage(page + 1)">下一页</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getProductList } from '../api'
import { useCategoryStore } from '../store/category'
import { useAddToCart } from '../composables/useAddToCart'
import ProductCard from '../components/ProductCard.vue'

const route = useRoute()
const router = useRouter()
const categoryStore = useCategoryStore()
const { handleAddCart } = useAddToCart()
const products = ref([])
const categories = computed(() => categoryStore.categories)
const categoryId = ref(null)
const categoryName = ref('')
const sortBy = ref('default')
const page = ref(1)
const pageSize = 12
const total = ref(0)
const loading = ref(true)
const inStockOnly = ref(false)

const sortOptions = [
  { label: '默认', value: 'default' },
  { label: '价格升序', value: 'price_asc' },
  { label: '价格降序', value: 'price_desc' },
  { label: '最新', value: 'newest' }
]

const loadProducts = async () => {
  loading.value = true
  try {
    const params = {
      page: page.value,
      size: pageSize,
      categoryId: categoryId.value
    }
    if (sortBy.value !== 'default') params.sort = sortBy.value
    if (inStockOnly.value) params.inStockOnly = true
    const res = await getProductList(params)
    products.value = res.data.records || res.data
    total.value = res.data.total || products.value.length
  } catch (error) { console.error(error) }
  finally { loading.value = false }
}

const loadCategories = async () => {
  await categoryStore.fetchCategories()
  const current = categories.value.find(c => c.id === categoryId.value)
  if (current) categoryName.value = current.name
}

const switchCategory = (cat) => {
  categoryId.value = cat.id
  categoryName.value = cat.name
  page.value = 1
  router.push(`/category/${cat.id}`)
  loadProducts()
}

const changePage = (p) => {
  page.value = p
  window.scrollTo({ top: 0, behavior: 'smooth' })
  loadProducts()
}

const goToDetail = (id) => router.push(`/product/${id}`)


watch(() => route.params.id, (val) => {
  if (val) {
    categoryId.value = Number(val)
    const current = categories.value.find(c => c.id === categoryId.value)
    if (current) categoryName.value = current.name
    loadProducts()
  }
})

onMounted(() => {
  categoryId.value = Number(route.params.id)
  loadCategories()
  loadProducts()
})
</script>

<style scoped>
.category-page { max-width: 1280px; margin: 0 auto; padding: 0 24px; }

.breadcrumb {
  padding: 20px 0; font-size: 13px; color: #999;
  display: flex; align-items: center; gap: 8px;
}
.breadcrumb a { color: #666; transition: color 0.2s; text-decoration: none; }
.breadcrumb a:hover { color: #111; }
.sep { color: #ddd; }

.category-layout { display: flex; gap: 32px; }

/* 侧边栏 */
.sidebar {
  width: 200px; flex-shrink: 0; position: sticky; top: 84px; align-self: flex-start;
}
.sidebar-title { font-size: 16px; font-weight: 700; color: #111; margin: 0 0 16px; }
.cat-nav { display: flex; flex-direction: column; gap: 2px; }
.cat-link {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 14px; border-radius: 8px; font-size: 14px; color: #555;
  cursor: pointer; transition: all 0.2s; font-weight: 500;
}
.cat-link:hover { background: #f5f5f5; color: #111; }
.cat-link.active { background: #111; color: #fff; }
.cat-dot {
  width: 6px; height: 6px; border-radius: 50%; background: #ccc;
  transition: background 0.2s;
}
.cat-link.active .cat-dot { background: #fff; }

/* 主内容 */
.main-area { flex: 1; min-width: 0; }

.sort-bar { display: flex; gap: 8px; margin-bottom: 24px; }
.sort-btn {
  padding: 6px 16px; border-radius: 6px; border: 1.5px solid #e5e5e5;
  background: #fff; color: #666; font-size: 13px; font-weight: 500;
  cursor: pointer; transition: all 0.2s;
}
.sort-btn:hover { border-color: #111; color: #111; }
.sort-btn.active { background: #111; color: #fff; border-color: #111; }
.stock-filter {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-left: auto;
  font-size: 13px;
  color: #666;
  cursor: pointer;
  user-select: none;
}
.stock-filter input { accent-color: #111; }

.product-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; }

.empty-state { text-align: center; padding: 60px 0; }
.empty-icon { font-size: 48px; margin-bottom: 12px; }
.empty-state p { font-size: 15px; color: #999; }

.pagination-wrap {
  display: flex; align-items: center; justify-content: center;
  gap: 16px; margin-top: 40px; padding-bottom: 40px;
}
.page-btn {
  padding: 8px 20px; border-radius: 8px; border: 1.5px solid #e5e5e5;
  background: #fff; color: #333; font-size: 14px; font-weight: 500;
  cursor: pointer; transition: all 0.2s;
}
.page-btn:hover:not(:disabled) { border-color: #111; color: #111; }
.page-btn:disabled { color: #ccc; cursor: not-allowed; }
.page-info { font-size: 14px; color: #666; font-weight: 500; }

/* 骨架屏 */
.skeleton-card { background: #fff; border-radius: 8px; overflow: hidden; border: 1px solid #f0f0f0; }
.skeleton-img { aspect-ratio: 1/1; background: #f0f0f0; }
.skeleton-body { padding: 16px; display: flex; flex-direction: column; gap: 10px; }
.skeleton-line { height: 14px; border-radius: 4px; background: #f0f0f0; }
.skeleton-line.short { width: 40%; height: 20px; }
@keyframes skeleton-pulse { 0%,100%{opacity:1} 50%{opacity:0.4} }
.skeleton-pulse { animation: skeleton-pulse 1.5s ease-in-out infinite; }

@media (max-width: 768px) {
  .category-layout { flex-direction: column; }
  .sidebar { width: 100%; position: static; }
  .cat-nav { flex-direction: row; overflow-x: auto; gap: 8px; padding-bottom: 8px; }
  .product-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
