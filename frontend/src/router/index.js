import { createRouter, createWebHistory } from 'vue-router'
import { storage } from '../utils/storage'

const Home = () => import('../views/Home.vue')
const Login = () => import('../views/Login.vue')
const ProductDetail = () => import('../views/ProductDetail.vue')
const CategoryProducts = () => import('../views/CategoryProducts.vue')
const Search = () => import('../views/Search.vue')
const Cart = () => import('../views/Cart.vue')
const Checkout = () => import('../views/Checkout.vue')
const Order = () => import('../views/Order.vue')
const UserProfile = () => import('../views/UserProfile.vue')
const AiCustomerService = () => import('../views/AiCustomerService.vue')
const NotFound = () => import('../views/NotFound.vue')
const AdminLayout = () => import('../views/admin/AdminLayout.vue')
const Dashboard = () => import('../views/admin/Dashboard.vue')
const ProductManage = () => import('../views/admin/ProductManage.vue')
const CategoryManage = () => import('../views/admin/CategoryManage.vue')
const OrderManage = () => import('../views/admin/OrderManage.vue')
const UserManage = () => import('../views/admin/UserManage.vue')
const ReviewManage = () => import('../views/admin/ReviewManage.vue')
const FavoriteManage = () => import('../views/admin/FavoriteManage.vue')
const CouponManage = () => import('../views/admin/CouponManage.vue')
const AfterSaleManage = () => import('../views/admin/AfterSaleManage.vue')

const routes = [
  {
    path: '/',
    name: 'Home',
    component: Home,
    meta: { title: '拾光 - 拾取美好，优选生活', description: '精选数码、家居、服饰与生活好物，提供顺手、安心的购物体验。' }
  },
  {
    path: '/product/:id',
    name: 'ProductDetail',
    component: ProductDetail,
    meta: { title: '商品详情 - 拾光' }
  },
  {
    path: '/category/:id',
    name: 'CategoryProducts',
    component: CategoryProducts,
    meta: { title: '分类商品 - 拾光' }
  },
  {
    path: '/search',
    name: 'Search',
    component: Search,
    meta: { title: '搜索 - 拾光' }
  },
  {
    path: '/cart',
    name: 'Cart',
    component: Cart,
    meta: { requiresAuth: true, title: '购物车 - 拾光' }
  },
  {
    path: '/checkout',
    name: 'Checkout',
    component: Checkout,
    meta: { requiresAuth: true, title: '确认订单 - 拾光' }
  },
  {
    path: '/order',
    name: 'Order',
    component: Order,
    meta: { requiresAuth: true, title: '我的订单 - 拾光' }
  },
  {
    path: '/profile',
    name: 'UserProfile',
    component: UserProfile,
    meta: { requiresAuth: true, title: '个人中心 - 拾光' }
  },
  {
    path: '/service',
    name: 'AiCustomerService',
    component: AiCustomerService,
    meta: { requiresAuth: true, title: 'AI 客服 - 拾光', description: '拾光 AI 客服小光，提供商品、订单、优惠券、发票和售后咨询。' }
  },
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { title: '登录 / 注册 - 拾光' }
  },
  {
    path: '/admin',
    component: AdminLayout,
    meta: { requiresAuth: true, requiresAdmin: true, title: '管理后台 - 拾光' },
    redirect: '/admin/dashboard',
    children: [
      { path: 'dashboard', name: 'AdminDashboard', component: Dashboard, meta: { title: '数据看板 - 拾光' } },
      { path: 'products', name: 'AdminProducts', component: ProductManage, meta: { title: '商品管理 - 拾光' } },
      { path: 'categories', name: 'AdminCategories', component: CategoryManage, meta: { title: '分类管理 - 拾光' } },
      { path: 'orders', name: 'AdminOrders', component: OrderManage, meta: { title: '订单管理 - 拾光' } },
      { path: 'reviews', name: 'AdminReviews', component: ReviewManage, meta: { title: '评价管理 - 拾光' } },
      { path: 'favorites', name: 'AdminFavorites', component: FavoriteManage, meta: { title: '收藏管理 - 拾光' } },
      { path: 'coupons', name: 'AdminCoupons', component: CouponManage, meta: { title: '优惠券管理 - 拾光' } },
      { path: 'after-sales', name: 'AdminAfterSales', component: AfterSaleManage, meta: { title: '售后管理 - 拾光' } },
      { path: 'users', name: 'AdminUsers', component: UserManage, meta: { title: '用户管理 - 拾光' } }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: NotFound,
    meta: { title: '页面不存在 - 拾光' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

router.beforeEach((to, from, next) => {
  const token = storage.get('token', '')
  const userInfo = storage.getJSON('userInfo', {}) || {}

  if (to.meta.requiresAuth && !token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }

  if (to.meta.requiresAdmin && !['ADMIN', 'OPERATOR', 'SUPPORT'].includes(userInfo.role)) {
    next('/')
    return
  }

  next()
})

router.afterEach((to) => {
  document.title = to.meta.title || '拾光'
  const description = to.meta.description || '拾光，精选实用好物，让日常生活更顺手。'
  let meta = document.querySelector('meta[name="description"]')
  if (!meta) {
    meta = document.createElement('meta')
    meta.setAttribute('name', 'description')
    document.head.appendChild(meta)
  }
  meta.setAttribute('content', description)
})

export default router
