<template>
  <div class="admin-layout">
    <aside class="admin-aside" :class="{ collapsed: isCollapse }">
      <div class="logo-section" @click="$router.push('/admin/dashboard')">
        <div class="logo-icon"><span>拾</span></div>
        <transition name="fade">
          <span v-if="!isCollapse" class="logo-text">拾光优选</span>
        </transition>
      </div>

      <nav class="admin-nav">
        <router-link v-for="item in navItems" :key="item.path" :to="item.path" class="nav-item" :class="{ active: activeMenu === item.path }">
          <component :is="item.icon" class="nav-icon" />
          <transition name="fade">
            <span v-if="!isCollapse" class="nav-label">{{ item.label }}</span>
          </transition>
        </router-link>
      </nav>

      <div class="aside-footer">
        <router-link to="/" class="nav-item back-link">
          <HomeFilled class="nav-icon" />
          <transition name="fade">
            <span v-if="!isCollapse" class="nav-label">返回前台</span>
          </transition>
        </router-link>
      </div>
    </aside>

    <div class="admin-body">
      <header class="admin-header">
        <div class="header-left">
          <button class="collapse-btn" @click="isCollapse = !isCollapse">
            <component :is="isCollapse ? Expand : Fold" />
          </button>
          <div class="breadcrumb">
            <span class="bc-home">管理后台</span>
            <span class="bc-sep"><Right /></span>
            <span class="bc-current">{{ currentTitle }}</span>
          </div>
        </div>
        <div class="header-right">
          <div class="admin-user" @click="showDropdown = !showDropdown">
            <UserAvatar :src="userStore.userInfo?.avatar" :name="userStore.userInfo?.username" :size="30" />
            <span class="admin-name">{{ userStore.userInfo?.username || '管理员' }}</span>
            <component :is="showDropdown ? ArrowUp : ArrowDown" class="dropdown-arrow" />
          </div>
          <transition name="dropdown">
            <div class="user-dropdown" v-if="showDropdown" @click="showDropdown = false">
              <div class="dropdown-item" @click="$router.push('/')"><HomeFilled /> 返回前台</div>
              <div class="dropdown-divider"></div>
              <div class="dropdown-item danger" @click="handleLogout"><SwitchButton /> 退出登录</div>
            </div>
          </transition>
        </div>
      </header>
      <main class="admin-main"><router-view /></main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../../store/user'
import { DataAnalysis, Goods, List, Document, User, HomeFilled, Expand, Fold, Right, ArrowUp, ArrowDown, SwitchButton, Star, Tickets } from '@element-plus/icons-vue'
import UserAvatar from '../../components/UserAvatar.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const isCollapse = ref(false)
const showDropdown = ref(false)

const navItems = [
  { path: '/admin/dashboard', label: '数据概览', icon: DataAnalysis },
  { path: '/admin/products', label: '商品管理', icon: Goods },
  { path: '/admin/categories', label: '分类管理', icon: List },
  { path: '/admin/orders', label: '订单管理', icon: Document },
  { path: '/admin/reviews', label: '评价管理', icon: Document },
  { path: '/admin/favorites', label: '收藏管理', icon: Star },
  { path: '/admin/coupons', label: '优惠券管理', icon: Tickets },
  { path: '/admin/users', label: '用户管理', icon: User }
]
const titleMap = {
  '/admin/dashboard': '数据概览',
  '/admin/products': '商品管理',
  '/admin/categories': '分类管理',
  '/admin/orders': '订单管理',
  '/admin/reviews': '评价管理',
  '/admin/favorites': '收藏管理',
  '/admin/coupons': '优惠券管理',
  '/admin/users': '用户管理'
}
const activeMenu = computed(() => route.path)
const currentTitle = computed(() => titleMap[route.path] || '管理后台')
const handleLogout = () => { userStore.logout(); router.push('/login') }

const closeDropdownOnClickOutside = (e) => {
  if (showDropdown.value && !e.target.closest('.header-right')) {
    showDropdown.value = false
  }
}
onMounted(() => document.addEventListener('click', closeDropdownOnClickOutside))
onUnmounted(() => document.removeEventListener('click', closeDropdownOnClickOutside))
</script>

<style scoped>
.admin-layout { display: flex; height: 100vh; background: #f5f5f3; }

/* Sidebar */
.admin-aside {
  width: 220px;
  background: #1a1a1a;
  display: flex;
  flex-direction: column;
  transition: width 0.25s ease;
  flex-shrink: 0;
  overflow: hidden;
}
.admin-aside.collapsed { width: 64px; }

.logo-section {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 18px 16px;
  cursor: pointer;
  border-bottom: 1px solid rgba(255,255,255,0.06);
  height: 60px;
  box-sizing: border-box;
}
.logo-icon {
  width: 32px; height: 32px; border-radius: 6px;
  background: #fff; color: #1a1a1a;
  display: flex; align-items: center; justify-content: center;
  font-size: 14px; font-weight: 800; flex-shrink: 0;
}
.logo-text { font-size: 15px; font-weight: 700; color: #fff; white-space: nowrap; }

.admin-nav {
  flex: 1; padding: 12px 8px;
  display: flex; flex-direction: column; gap: 2px;
  overflow-y: auto;
}
.nav-item {
  display: flex; align-items: center; gap: 12px;
  padding: 10px 14px; border-radius: 6px;
  color: rgba(255,255,255,0.5); text-decoration: none;
  transition: all 0.15s; white-space: nowrap;
  font-size: 13px; font-weight: 500;
}
.nav-item:hover { background: rgba(255,255,255,0.06); color: rgba(255,255,255,0.8); }
.nav-item.active { background: rgba(255,255,255,0.1); color: #fff; }
.nav-icon { width: 16px; height: 16px; flex-shrink: 0; }

.aside-footer { padding: 8px; border-top: 1px solid rgba(255,255,255,0.06); }
.back-link { color: rgba(255,255,255,0.35); }
.back-link:hover { color: rgba(255,255,255,0.6); background: rgba(255,255,255,0.04); }

/* Header */
.admin-body { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
.admin-header {
  height: 60px; background: #fff;
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 20px; flex-shrink: 0;
  border-bottom: 1px solid #eee;
}
.header-left { display: flex; align-items: center; gap: 14px; }
.collapse-btn {
  width: 32px; height: 32px; border: none;
  background: #f5f5f3; border-radius: 6px;
  cursor: pointer; display: flex; align-items: center; justify-content: center;
  color: #666; transition: all 0.15s;
}
.collapse-btn svg { width: 14px; height: 14px; }
.collapse-btn:hover { background: #eee; color: #333; }
.breadcrumb { display: flex; align-items: center; gap: 6px; font-size: 13px; }
.bc-home { color: #999; }
.bc-sep { color: #ddd; display: flex; align-items: center; }
.bc-sep svg { width: 10px; height: 10px; }
.bc-current { color: #333; font-weight: 600; }

/* User dropdown */
.header-right { position: relative; }
.admin-user {
  display: flex; align-items: center; gap: 8px;
  cursor: pointer; padding: 5px 10px; border-radius: 6px;
  transition: background 0.15s;
}
.admin-user:hover { background: #f5f5f3; }
.admin-name { font-size: 13px; color: #333; font-weight: 500; }
.dropdown-arrow { width: 10px; height: 10px; color: #999; }
.user-dropdown {
  position: absolute; top: calc(100% + 6px); right: 0;
  background: #fff; border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.1);
  padding: 6px; min-width: 160px; z-index: 100;
  border: 1px solid #eee;
}
.dropdown-item {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 12px; border-radius: 5px;
  font-size: 13px; color: #555; cursor: pointer;
  transition: background 0.15s;
}
.dropdown-item svg { width: 14px; height: 14px; }
.dropdown-item:hover { background: #f5f5f3; }
.dropdown-item.danger { color: #e74c3c; }
.dropdown-item.danger:hover { background: #fef5f5; }
.dropdown-divider { height: 1px; background: #eee; margin: 4px 0; }

.admin-main { flex: 1; overflow-y: auto; padding: 20px; }

.fade-enter-active, .fade-leave-active { transition: opacity 0.15s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
.dropdown-enter-active { animation: dd-in 0.15s ease-out; }
.dropdown-leave-active { animation: dd-out 0.1s ease-in; }
@keyframes dd-in { from { opacity: 0; transform: translateY(-6px); } to { opacity: 1; transform: translateY(0); } }
@keyframes dd-out { from { opacity: 1; } to { opacity: 0; transform: translateY(-6px); } }
</style>
