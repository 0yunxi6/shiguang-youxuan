<template>
  <header class="site-header" :class="{ scrolled: isScrolled }">
    <div class="header-inner">
      <div class="header-left">
        <div class="logo" @click="$router.push('/')">
          <div class="logo-icon">
            <img src="/logo.png" alt="拾光优选 Logo" />
          </div>
          <span class="logo-text">拾光优选</span>
        </div>
        <nav class="main-nav" :class="{ open: mobileMenuOpen }">
          <router-link to="/" class="nav-link" :class="{ active: $route.path === '/' }"
            @click="mobileMenuOpen = false">首页</router-link>
          <router-link v-for="cat in topCategories" :key="cat.id" :to="`/category/${cat.id}`"
            class="nav-link" :class="{ active: $route.path === `/category/${cat.id}` }"
            @click="mobileMenuOpen = false">
            {{ cat.name }}
          </router-link>
        </nav>
      </div>

      <div class="header-center">
        <div class="search-box" :class="{ focused: searchFocused }">
          <input
            v-model="searchKeyword"
            type="text"
            placeholder="搜索商品"
            @focus="handleSearchFocus"
            @blur="handleSearchBlur"
            @keyup.enter="handleSearch"
          />
          <button class="search-btn" @click="handleSearch" aria-label="搜索">
            <el-icon><Search /></el-icon>
          </button>
        </div>

        <!-- Search Dropdown -->
        <transition name="dropdown">
          <div class="search-dropdown" v-if="searchFocused && (searchHistory.length || popularSearches.length)">
            <!-- Search History -->
            <div class="dropdown-section" v-if="searchHistory.length">
              <div class="dropdown-header">
                <span>搜索历史</span>
                <button class="clear-history" @click="clearHistory">清空</button>
              </div>
              <div class="history-list">
                <button
                  v-for="h in searchHistory"
                  :key="h"
                  class="history-item"
                  @mousedown.prevent="searchFromHistory(h)"
                >
                  <el-icon><Clock /></el-icon>
                  {{ h }}
                </button>
              </div>
            </div>

            <!-- Popular Searches -->
            <div class="dropdown-section" v-if="popularSearches.length">
              <div class="dropdown-header">
                <span>热门搜索</span>
              </div>
              <div class="popular-list">
                <button
                  v-for="(item, idx) in popularSearches"
                  :key="item"
                  class="popular-item"
                  :class="{ hot: idx < 3 }"
                  @mousedown.prevent="searchFromHistory(item)"
                >
                  <span class="rank" v-if="idx < 3">{{ idx + 1 }}</span>
                  {{ item }}
                </button>
              </div>
            </div>
          </div>
        </transition>
      </div>

      <div class="header-right">
        <div class="cart-trigger" @click="$router.push('/cart')">
          <el-icon :size="22"><ShoppingCart /></el-icon>
          <span class="cart-badge" v-if="cartCount > 0">{{ cartCount > 99 ? '99+' : cartCount }}</span>
        </div>

        <template v-if="isLogin">
          <el-dropdown trigger="click" @command="handleCommand">
            <div class="user-trigger">
              <UserAvatar :src="userAvatar" :name="username" :size="30" />
              <span class="user-name">{{ username }}</span>
              <el-icon class="dropdown-arrow"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>
                  个人中心
                </el-dropdown-item>
                <el-dropdown-item command="orders">
                  <el-icon><Document /></el-icon>
                  我的订单
                </el-dropdown-item>
                <el-dropdown-item v-if="isAdmin" command="admin" divided>
                  <el-icon><Setting /></el-icon>
                  管理后台
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <router-link to="/login" class="login-btn">
            <el-icon><User /></el-icon>
            登录
          </router-link>
        </template>

        <button class="mobile-menu-btn" @click="mobileMenuOpen = !mobileMenuOpen" aria-label="菜单">
          <span :class="{ open: mobileMenuOpen }"></span>
        </button>
      </div>
    </div>
    <!-- 移动端遮罩 -->
    <div class="mobile-overlay" v-if="mobileMenuOpen" @click="mobileMenuOpen = false"></div>
  </header>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../store/user'
import { useCartStore } from '../store/cart'
import { useCategoryStore } from '../store/category'
import { storage } from '../utils/storage'
import { getHotSearchKeywords } from '../api'
import { ElMessage } from 'element-plus'
import {
  Search, ShoppingCart, User, Document, Setting,
  SwitchButton, ArrowDown, Clock
} from '@element-plus/icons-vue'
import UserAvatar from './UserAvatar.vue'

const router = useRouter()
const userStore = useUserStore()
const cartStore = useCartStore()

const searchKeyword = ref('')
const isScrolled = ref(false)
const categoryStore = useCategoryStore()
const mobileMenuOpen = ref(false)
const searchFocused = ref(false)

const DEFAULT_POPULAR_SEARCHES = ['手机', '电脑', '运动鞋', '连衣裙', '零食', '护肤品']
const SEARCH_HISTORY_KEY = 'searchKeywordHistory'
const legacyHistory = storage.getJSON('searchHistory', [])
const popularSearches = ref([...DEFAULT_POPULAR_SEARCHES])

const searchHistory = ref(storage.getJSON(SEARCH_HISTORY_KEY, legacyHistory))

const isLogin = computed(() => userStore.isLogin)
const username = computed(() => userStore.userInfo?.username || '用户')
const userAvatar = computed(() => userStore.userInfo?.avatar || '')
const isAdmin = computed(() => userStore.userInfo?.role === 'ADMIN')
const cartCount = computed(() => cartStore.cartCount)

const handleSearchFocus = () => {
  searchFocused.value = true
}

const handleSearchBlur = () => {
  setTimeout(() => {
    searchFocused.value = false
  }, 200)
}

const handleSearch = () => {
  if (searchKeyword.value.trim()) {
    addToHistory(searchKeyword.value.trim())
    router.push({ path: '/search', query: { keyword: searchKeyword.value.trim() } })
    mobileMenuOpen.value = false
    searchFocused.value = false
  }
}

const addToHistory = (keyword) => {
  const history = searchHistory.value.filter(h => h !== keyword)
  history.unshift(keyword)
  searchHistory.value = history.slice(0, 10)
  storage.setJSON(SEARCH_HISTORY_KEY, searchHistory.value)
}

const searchFromHistory = (keyword) => {
  searchKeyword.value = keyword
  handleSearch()
}

const clearHistory = () => {
  searchHistory.value = []
  storage.remove(SEARCH_HISTORY_KEY)
  storage.remove('searchHistory')
}

const normalizeHotKeywords = (items) => (items || [])
  .map(item => typeof item === 'string' ? item : item?.keyword)
  .filter(Boolean)

const loadPopularSearches = async () => {
  try {
    const res = await getHotSearchKeywords({ limit: 8 })
    const keywords = normalizeHotKeywords(res.data)
    if (keywords.length) {
      popularSearches.value = keywords
    }
  } catch (error) {
    popularSearches.value = [...DEFAULT_POPULAR_SEARCHES]
  }
}

const handleCommand = (cmd) => {
  const map = { profile: '/profile', orders: '/order', admin: '/admin' }
  if (cmd === 'logout') {
    userStore.logout()
    cartStore.clearCart()
    ElMessage.success('已退出登录')
    router.push('/')
  } else if (map[cmd]) {
    router.push(map[cmd])
  }
}

// 节流 scroll 监听
let ticking = false
const onScroll = () => {
  if (!ticking) {
    requestAnimationFrame(() => {
      isScrolled.value = window.scrollY > 10
      ticking = false
    })
    ticking = true
  }
}

const topCategories = computed(() => categoryStore.categories.slice(0, 5))

onMounted(async () => {
  window.addEventListener('scroll', onScroll, { passive: true })
  categoryStore.fetchCategories()
  loadPopularSearches()
})

onUnmounted(() => window.removeEventListener('scroll', onScroll))
</script>

<style scoped>
.site-header {
  position: sticky;
  top: 0;
  z-index: 1000;
  background: rgba(255,255,255,0.95);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  transition: box-shadow 0.3s;
  border-bottom: 1px solid transparent;
}

.site-header.scrolled {
  box-shadow: 0 2px 20px rgba(0,0,0,0.06);
  border-bottom-color: #f0f0f0;
}

.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 60px;
  padding: 0 20px;
  position: relative;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 28px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  user-select: none;
}

.logo-icon {
  width: 36px;
  height: 36px;
  border-radius: 9px;
  background: #fff;
  border: 1px solid #eee;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  box-shadow: 0 2px 10px rgba(0,0,0,0.06);
}

.logo-icon img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.logo-text {
  font-size: 16px;
  font-weight: 700;
  color: #111;
}

.main-nav {
  display: flex;
  gap: 4px;
}

.nav-link {
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 14px;
  color: #555;
  text-decoration: none;
  transition: all 0.15s;
  font-weight: 500;
}

.nav-link:hover {
  background: #f5f5f5;
  color: #111;
}

.nav-link.active {
  background: #1a1a1a;
  color: #fff;
}

/* ===== Search ===== */
.header-center {
  flex: 1;
  max-width: 420px;
  margin: 0 24px;
  position: relative;
}

.search-box {
  display: flex;
  align-items: center;
  background: #f5f5f5;
  border-radius: 8px;
  overflow: hidden;
  border: 1.5px solid transparent;
  transition: all 0.2s;
}

.search-box.focused {
  background: #fff;
  border-color: #1a1a1a;
  box-shadow: 0 0 0 3px rgba(0,0,0,0.04);
}

.search-box input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  padding: 10px 14px;
  font-size: 13px;
  color: #333;
}

.search-box input::placeholder {
  color: #bbb;
}

.search-btn {
  border: none;
  background: #1a1a1a;
  padding: 0 16px;
  height: 40px;
  cursor: pointer;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: opacity 0.15s;
}

.search-btn:hover {
  opacity: 0.85;
}

/* ===== Search Dropdown ===== */
.search-dropdown {
  position: absolute;
  top: calc(100% + 6px);
  left: 0;
  right: 0;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.1);
  border: 1px solid #eee;
  padding: 14px;
  z-index: 100;
}

.dropdown-section {
  margin-bottom: 16px;
}

.dropdown-section:last-child {
  margin-bottom: 0;
}

.dropdown-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.dropdown-header span {
  font-size: 13px;
  font-weight: 600;
  color: #909399;
}

.clear-history {
  border: none;
  background: transparent;
  font-size: 12px;
  color: #999;
  cursor: pointer;
  transition: color 0.2s;
}

.clear-history:hover {
  color: #333;
}

.history-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.history-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: #f5f5f5;
  border: none;
  border-radius: 20px;
  font-size: 13px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
}

.history-item:hover {
  background: #f2efe9;
  color: #333;
}

.popular-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.popular-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: #fafafa;
  border: none;
  border-radius: 20px;
  font-size: 13px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
}

.popular-item:hover {
  background: #f2efe9;
  color: #333;
}

.popular-item.hot {
  background: #f2efe9;
}

.rank {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  background: #1a1a1a;
  color: #fff;
  border-radius: 3px;
  font-size: 10px;
  font-weight: 700;
}

.popular-item:nth-child(2) .rank {
  background: #666;
}

.popular-item:nth-child(3) .rank {
  background: #999;
}

/* ===== Header Right ===== */
.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.cart-trigger {
  position: relative;
  cursor: pointer;
  padding: 8px;
  border-radius: 10px;
  transition: background 0.2s;
  color: #555;
  display: flex;
  align-items: center;
}

.cart-trigger:hover {
  background: #f5f5f5;
  color: #111;
}

.cart-badge {
  position: absolute;
  top: 2px;
  right: 2px;
  background: #c45c3e;
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  min-width: 16px;
  height: 16px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
  line-height: 1;
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 6px 10px;
  border-radius: 10px;
  transition: background 0.2s;
}

.user-trigger:hover {
  background: #f5f5f5;
}

.user-name {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.dropdown-arrow {
  font-size: 12px;
  color: #999;
}

.login-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 18px;
  border-radius: 8px;
  background: #1a1a1a;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  text-decoration: none;
  transition: all 0.2s;
}

.login-btn:hover {
  background: #333;
}

/* ===== Mobile Menu ===== */
.mobile-menu-btn {
  display: none;
  width: 36px;
  height: 36px;
  border: none;
  background: transparent;
  cursor: pointer;
  position: relative;
}

.mobile-menu-btn span,
.mobile-menu-btn span::before,
.mobile-menu-btn span::after {
  display: block;
  width: 22px;
  height: 2px;
  background: #333;
  border-radius: 1px;
  transition: all 0.3s;
  position: absolute;
  left: 7px;
}

.mobile-menu-btn span {
  top: 17px;
}

.mobile-menu-btn span::before {
  content: '';
  top: -7px;
}

.mobile-menu-btn span::after {
  content: '';
  top: 7px;
}

.mobile-menu-btn span.open {
  background: transparent;
}

.mobile-menu-btn span.open::before {
  transform: rotate(45deg);
  top: 0;
}

.mobile-menu-btn span.open::after {
  transform: rotate(-45deg);
  top: 0;
}

.mobile-overlay {
  display: none;
}

/* ===== Dropdown Animation ===== */
.dropdown-enter-active {
  animation: dropdown-in 0.2s ease-out;
}

.dropdown-leave-active {
  animation: dropdown-out 0.15s ease-in;
}

@keyframes dropdown-in {
  from {
    opacity: 0;
    transform: translateY(-8px) scale(0.98);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@keyframes dropdown-out {
  from {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
  to {
    opacity: 0;
    transform: translateY(-8px) scale(0.98);
  }
}

@media (max-width: 1024px) {
  .header-center {
    max-width: 400px;
  }

  .main-nav {
    gap: 0;
  }

  .nav-link {
    padding: 8px 12px;
    font-size: 14px;
  }
}

@media (max-width: 768px) {
  .header-center {
    display: none;
  }

  .main-nav {
    display: none;
    position: fixed;
    top: 60px;
    left: 0;
    right: 0;
    background: #fff;
    flex-direction: column;
    padding: 16px;
    box-shadow: 0 8px 24px rgba(0,0,0,0.08);
    z-index: 999;
  }

  .main-nav.open {
    display: flex;
  }

  .nav-link {
    padding: 14px 16px;
    font-size: 16px;
  }

  .mobile-menu-btn {
    display: block;
  }

  .mobile-overlay {
    display: block;
    position: fixed;
    inset: 0;
    top: 60px;
    background: rgba(0,0,0,0.2);
    z-index: 998;
  }

  .user-name {
    display: none;
  }
}
</style>
