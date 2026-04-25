<template>
  <div class="profile-page">
    <div class="profile-shell">
      <aside class="sidebar">
        <div class="user-card card">
          <div class="user-card-top">
            <div class="avatar-upload" @click="triggerAvatarUpload">
              <UserAvatar :src="userInfo?.avatar" :name="userInfo?.username" :size="72" />
              <div class="avatar-overlay">
                <el-icon :size="18"><Camera /></el-icon>
              </div>
              <input ref="avatarInput" type="file" accept="image/jpeg,image/png,image/gif,image/webp" hidden @change="handleAvatarChange" />
            </div>
            <div class="user-meta">
              <h2>{{ userInfo?.nickname || userInfo?.username || '用户' }}</h2>
              <p class="join-date">注册于 {{ formatDate(userInfo?.createTime) || '—' }}</p>
            </div>
          </div>

          <div class="account-summary">
            <div class="summary-item">
              <span class="summary-label">账号</span>
              <strong>{{ userInfo?.username || '—' }}</strong>
            </div>
            <div class="summary-item">
              <span class="summary-label">邮箱</span>
              <strong>{{ userInfo?.email || '未设置' }}</strong>
            </div>
          </div>
        </div>

        <nav class="profile-nav card">
          <button
            v-for="tab in tabs"
            :key="tab.key"
            type="button"
            :class="['nav-item', { active: activeTab === tab.key }]"
            @click="activeTab = tab.key"
          >
            <span class="nav-icon">{{ tab.icon }}</span>
            <span class="nav-copy">
              <strong>{{ tab.label }}</strong>
              <small>{{ tab.desc }}</small>
            </span>
          </button>
        </nav>
      </aside>

      <div class="main-area">
        <div class="page-title-row">
          <h1>{{ tabs.find(tab => tab.key === activeTab)?.label }}</h1>
          <p>{{ tabs.find(tab => tab.key === activeTab)?.desc }}</p>
        </div>

        <div v-if="activeTab === 'info'" class="panel card">
          <div class="panel-heading">
            <div>
              <h3 class="panel-title">个人信息</h3>
              <p class="panel-subtitle">修改昵称、邮箱和手机号。</p>
            </div>
          </div>
          <form class="profile-form" @submit.prevent="saveProfile">
            <div class="form-grid single">
              <div class="form-group">
                <label>用户名</label>
                <input :value="userInfo?.username" disabled class="disabled" />
                <span class="field-tip">用户名当前不可修改</span>
              </div>
            </div>
            <div class="form-grid single">
              <div class="form-group">
                <label>昵称</label>
                <input v-model="profileForm.nickname" placeholder="请输入昵称" />
              </div>
            </div>
            <div class="form-grid">
              <div class="form-group">
                <label>邮箱</label>
                <input v-model="profileForm.email" type="email" placeholder="请输入邮箱" />
              </div>
              <div class="form-group">
                <label>手机号</label>
                <input v-model="profileForm.phone" type="tel" placeholder="请输入手机号" />
              </div>
            </div>
            <button type="submit" class="btn-save" :disabled="saving">
              {{ saving ? '保存中...' : '保存修改' }}
            </button>
          </form>
        </div>

        <div v-if="activeTab === 'password'" class="panel glass-card">
          <div class="panel-heading">
            <div>
              <h3 class="panel-title">修改密码</h3>
              <p class="panel-subtitle">修改后请使用新密码登录。</p>
            </div>
          </div>
          <form class="profile-form" @submit.prevent="changePassword">
            <div class="form-grid single">
              <div class="form-group">
                <label>旧密码</label>
                <input v-model="passwordForm.oldPassword" type="password" placeholder="请输入旧密码" required />
              </div>
            </div>
            <div class="form-grid">
              <div class="form-group">
                <label>新密码</label>
                <input v-model="passwordForm.newPassword" type="password" placeholder="请输入新密码（6-20位）" required minlength="6" maxlength="20" />
              </div>
              <div class="form-group">
                <label>确认密码</label>
                <input v-model="passwordForm.confirmPassword" type="password" placeholder="请再次输入新密码" required />
              </div>
            </div>
            <p v-if="pwdError" class="error-msg">{{ pwdError }}</p>
            <button type="submit" class="btn-save" :disabled="changingPwd">
              {{ changingPwd ? '修改中...' : '修改密码' }}
            </button>
          </form>
        </div>

        <div v-if="activeTab === 'orders'" class="panel glass-card">
          <div class="panel-heading">
            <div>
              <h3 class="panel-title">我的订单</h3>
              <p class="panel-subtitle">查看最近订单。</p>
            </div>
            <span class="panel-count">{{ orders.length }} 笔</span>
          </div>
          <div v-if="orders.length === 0" class="empty-state">暂无订单记录</div>
          <div v-else class="mini-order-list">
            <div v-for="order in orders" :key="order.id" class="mini-order">
              <div class="mini-order-left">
                <span :class="['status-dot', `s-${order.status}`]"></span>
                <div class="order-copy">
                  <strong class="order-no">{{ order.orderNo }}</strong>
                  <span class="status-text">{{ ['待支付','已支付','已发货','已完成','已取消'][order.status] }}</span>
                </div>
              </div>
              <span class="mini-order-amount">¥{{ order.totalAmount }}</span>
            </div>
          </div>
        </div>

        <div v-if="activeTab === 'favorites'" class="panel glass-card">
          <div class="panel-heading">
            <div>
              <h3 class="panel-title">我的收藏</h3>
              <p class="panel-subtitle">收藏的商品会显示在这里，方便随时回看。</p>
            </div>
            <span class="panel-count">{{ favorites.length }} 件</span>
          </div>
          <div v-if="favorites.length === 0" class="empty-state">还没有收藏商品，去逛逛吧</div>
          <div v-else class="favorite-grid">
            <div v-for="item in favorites" :key="item.id" class="favorite-card" @click="router.push(`/product/${item.id}`)">
              <img :src="item.imageUrl || '/placeholder.svg'" class="favorite-thumb" />
              <div class="favorite-copy">
                <strong>{{ item.name }}</strong>
                <span>{{ item.categoryName || '未分类' }}</span>
                <div class="favorite-meta">
                  <span>¥{{ Number(item.price || 0).toFixed(2) }}</span>
                  <span>收藏于 {{ formatDate(item.favoriteTime) || '最近' }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div v-if="activeTab === 'coupons'" class="panel glass-card">
          <div class="panel-heading">
            <div>
              <h3 class="panel-title">我的优惠券</h3>
              <p class="panel-subtitle">可在结算页自动选择并抵扣订单金额。</p>
            </div>
            <span class="panel-count">{{ coupons.length }} 张</span>
          </div>
          <div v-if="coupons.length === 0" class="empty-state">暂无可用优惠券</div>
          <div v-else class="coupon-stack">
            <div v-for="coupon in coupons" :key="coupon.id" class="coupon-card" :class="couponStatusClass(coupon)">
              <div class="coupon-amount">¥{{ Number(coupon.discountAmount || 0).toFixed(0) }}</div>
              <div class="coupon-copy">
                <strong>{{ coupon.name }}</strong>
                <span>{{ coupon.description || `满${Number(coupon.minAmount || 0)}可用` }}</span>
                <small>有效期至 {{ formatDate(coupon.endTime) || '长期有效' }}</small>
              </div>
              <div class="coupon-badge">{{ couponStatusText(coupon) }}</div>
            </div>
          </div>
        </div>

        <div v-if="activeTab === 'address'" class="panel glass-card">
          <div class="panel-heading">
            <div>
              <h3 class="panel-title">收货地址</h3>
              <p class="panel-subtitle">地址管理功能开发中。</p>
            </div>
          </div>
          <div class="empty-state">收货地址管理功能开发中</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getUserProfile, updateProfile, updatePassword, getOrderList, uploadFile, getFavorites, getCoupons } from '../api'
import { useUserStore } from '../store/user'
import { ElMessage } from 'element-plus'
import { Camera } from '@element-plus/icons-vue'
import UserAvatar from '../components/UserAvatar.vue'

const router = useRouter()
const userStore = useUserStore()
const activeTab = ref('info')
const userInfo = ref(null)
const saving = ref(false)
const changingPwd = ref(false)
const orders = ref([])
const favorites = ref([])
const coupons = ref([])
const pwdError = ref('')

const avatarInput = ref(null)
const triggerAvatarUpload = () => avatarInput.value?.click()
const handleAvatarChange = async (e) => {
  const file = e.target.files?.[0]
  if (!file) return
  if (file.size > 2 * 1024 * 1024) { ElMessage.error('图片不能超过 2MB'); return }
  const fd = new FormData()
  fd.append('file', file)
  try {
    const res = await uploadFile(fd)
    const avatarUrl = res.data
    await updateProfile({ avatar: avatarUrl })
    userInfo.value.avatar = avatarUrl
    userStore.setUserInfo({ ...userStore.userInfo, avatar: avatarUrl })
    ElMessage.success('头像已更新')
  } catch { ElMessage.error('上传失败') }
  finally { e.target.value = '' }
}

const tabs = [
  { key: 'info', label: '个人信息', icon: '◉', desc: '查看和修改基本资料' },
  { key: 'password', label: '修改密码', icon: '◌', desc: '更新登录密码' },
  { key: 'orders', label: '我的订单', icon: '◎', desc: '查看订单记录' },
  { key: 'favorites', label: '我的收藏', icon: '★', desc: '查看收藏商品' },
  { key: 'coupons', label: '我的优惠券', icon: '◈', desc: '查看可用优惠券' },
  { key: 'address', label: '收货地址', icon: '◍', desc: '管理收货地址' }
]

const profileForm = reactive({ nickname: '', email: '', phone: '' })
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const formatDate = (time) => time ? new Date(time).toLocaleDateString('zh-CN') : ''

const loadProfile = async () => {
  try {
    const res = await getUserProfile()
    userInfo.value = res.data
    profileForm.nickname = res.data.nickname || ''
    profileForm.email = res.data.email || ''
    profileForm.phone = res.data.phone || ''
  } catch (error) { console.error(error) }
}

const saveProfile = async () => {
  saving.value = true
  try {
    await updateProfile(profileForm)
    ElMessage.success('保存成功')
    loadProfile()
  } catch (error) { ElMessage.error('保存失败') }
  finally { saving.value = false }
}

const changePassword = async () => {
  pwdError.value = ''
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    pwdError.value = '两次输入的密码不一致'
    return
  }
  changingPwd.value = true
  try {
    await updatePassword({ oldPassword: passwordForm.oldPassword, newPassword: passwordForm.newPassword })
    ElMessage.success('密码修改成功')
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
  } catch (error) { ElMessage.error('修改失败') }
  finally { changingPwd.value = false }
}

const loadOrders = async () => {
  try {
    const res = await getOrderList()
    orders.value = res.data
  } catch (error) { console.error(error) }
}

const loadFavorites = async () => {
  try {
    const res = await getFavorites()
    favorites.value = res.data || []
  } catch (error) {
    favorites.value = []
  }
}

const loadCoupons = async () => {
  try {
    const res = await getCoupons({ availableOnly: false })
    coupons.value = res.data || []
  } catch (error) {
    coupons.value = []
  }
}

const couponStatusText = (coupon) => {
  if (coupon?.status === 1) return '已使用'
  if (coupon?.status === 2) return '已过期'
  return coupon?.canUse === false ? '暂不可用' : '可使用'
}

const couponStatusClass = (coupon) => {
  if (coupon?.status === 1) return 'used'
  if (coupon?.status === 2) return 'expired'
  return coupon?.canUse === false ? 'disabled' : 'active'
}

onMounted(() => { loadProfile(); loadOrders(); loadFavorites(); loadCoupons() })
</script>

<style scoped>
.profile-page {
  min-height: calc(100vh - 72px);
  padding: 24px;
  background: #ffffff;
}

.profile-shell {
  max-width: 1180px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 20px;
}

.card {
  background: #fff;
  border: 1px solid #ebeef2;
  border-radius: 16px;
  box-shadow: 0 6px 18px rgba(15, 23, 42, 0.04);
}

.sidebar {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.user-card {
  padding: 24px;
}

.user-card-top {
  display: flex;
  align-items: center;
  gap: 14px;
}

.avatar-upload {
  position: relative;
  width: 72px;
  height: 72px;
  flex-shrink: 0;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
}

.avatar-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.45);
  color: #fff;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.avatar-upload:hover .avatar-overlay {
  opacity: 1;
}

.user-meta h2 {
  margin: 0;
  font-size: 20px;
  color: #1f2937;
  word-break: break-all;
}

.join-date {
  margin: 6px 0 0;
  color: #909399;
  font-size: 13px;
}

.account-summary {
  display: grid;
  gap: 10px;
  margin-top: 18px;
}

.summary-item {
  padding: 12px 14px;
  border-radius: 12px;
  background: #f8fafc;
}

.summary-label {
  display: block;
  margin-bottom: 4px;
  font-size: 12px;
  color: #909399;
}

.summary-item strong {
  display: block;
  color: #303133;
  font-size: 14px;
  word-break: break-all;
}

.profile-nav {
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.nav-item {
  border: none;
  width: 100%;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 12px;
  background: transparent;
  color: #4b5563;
  cursor: pointer;
  transition: background 0.2s ease, color 0.2s ease;
  text-align: left;
}

.nav-item:hover {
  background: #f5f7fa;
}

.nav-item.active {
  background: #111827;
  color: #fff;
}

.nav-icon {
  width: 30px;
  height: 30px;
  border-radius: 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #fdf0ec;
  color: #c45c3e;
  font-size: 14px;
  flex-shrink: 0;
}

.nav-item.active .nav-icon {
  background: rgba(255, 255, 255, 0.14);
  color: #fff;
}

.nav-copy {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.nav-copy strong {
  font-size: 14px;
  font-weight: 600;
}

.nav-copy small {
  font-size: 12px;
  opacity: 0.78;
}

.main-area {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
}

.page-title-row h1 {
  margin: 0;
  font-size: 28px;
  color: #1f2937;
}

.page-title-row p {
  margin: 6px 0 0;
  color: #6b7280;
  font-size: 14px;
}

.panel {
  padding: 24px;
}

.glass-card {
  background: #fff;
  border: 1px solid #ebeef2;
  border-radius: 16px;
  box-shadow: 0 6px 18px rgba(15, 23, 42, 0.04);
}

.panel-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.panel-title {
  margin: 0;
  font-size: 22px;
  color: #1f2937;
}

.panel-subtitle {
  margin: 6px 0 0;
  color: #909399;
  font-size: 14px;
}

.panel-count {
  padding: 6px 10px;
  border-radius: 999px;
  background: #fdf0ec;
  color: #c45c3e;
  font-size: 12px;
  font-weight: 600;
}

.profile-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.form-grid.single {
  grid-template-columns: 1fr;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-group label {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.form-group input {
  min-height: 48px;
  padding: 0 14px;
  border-radius: 10px;
  border: 1px solid #dcdfe6;
  background: #fff;
  font-size: 14px;
  color: #303133;
  outline: none;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.form-group input:focus {
  border-color: #c45c3e;
  box-shadow: 0 0 0 3px rgba(196, 92, 62, 0.1);
}

.form-group input.disabled {
  background: #f5f7fa;
  color: #909399;
  cursor: not-allowed;
}

.form-group input::placeholder {
  color: #b6bcc6;
}

.field-tip {
  font-size: 12px;
  color: #909399;
}

.error-msg {
  margin: 0;
  color: #f56c6c;
  font-size: 13px;
}

.btn-save {
  min-width: 120px;
  height: 44px;
  border: none;
  border-radius: 10px;
  background: #111827;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  align-self: flex-start;
  transition: background 0.2s ease;
}

.btn-save:hover:not(:disabled) {
  background: #1f2937;
}

.btn-save:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.mini-order-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.mini-order {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  border-radius: 12px;
  background: #f8fafc;
  border: 1px solid #edf0f5;
}

.mini-order-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.order-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.order-no {
  color: #303133;
  font-size: 14px;
  word-break: break-all;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.s-0 { background: #e6a23c; }
.s-1 { background: #67c23a; }
.s-2 { background: #409eff; }
.s-3 { background: #909399; }
.s-4 { background: #f56c6c; }

.status-text {
  color: #909399;
  font-size: 12px;
}

.mini-order-amount {
  color: #f56c6c;
  font-size: 16px;
  font-weight: 700;
  white-space: nowrap;
}

.favorite-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
}

.favorite-card {
  border: 1px solid #edf0f5;
  border-radius: 14px;
  overflow: hidden;
  background: #fff;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.favorite-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.08);
}

.favorite-thumb {
  width: 100%;
  height: 180px;
  object-fit: cover;
  background: #f5f7fa;
}

.favorite-copy {
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.favorite-copy strong {
  color: #303133;
  font-size: 15px;
}

.favorite-copy span {
  color: #909399;
  font-size: 13px;
}

.favorite-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.coupon-stack {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.coupon-card {
  display: grid;
  grid-template-columns: 120px 1fr auto;
  gap: 16px;
  align-items: center;
  padding: 18px 20px;
  border-radius: 16px;
  border: 1px solid #edf0f5;
  background: linear-gradient(135deg, #fffaf5, #fff);
}

.coupon-card.active {
  border-color: #f3d0b9;
}

.coupon-card.disabled,
.coupon-card.used,
.coupon-card.expired {
  opacity: 0.7;
  background: #fafafa;
}

.coupon-amount {
  font-size: 32px;
  font-weight: 700;
  color: #c45c3e;
}

.coupon-copy {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.coupon-copy strong {
  font-size: 16px;
  color: #303133;
}

.coupon-copy span,
.coupon-copy small {
  color: #909399;
  font-size: 13px;
}

.coupon-badge {
  padding: 8px 12px;
  border-radius: 999px;
  background: #111827;
  color: #fff;
  font-size: 12px;
  font-weight: 600;
}

.empty-state {
  padding: 48px 20px;
  text-align: center;
  color: #909399;
  font-size: 14px;
  background: #fafafa;
  border-radius: 12px;
  border: 1px dashed #e5e7eb;
}

@media (max-width: 1024px) {
  .profile-shell {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .profile-page {
    padding: 16px;
  }

  .panel-heading,
  .mini-order,
  .coupon-card {
    flex-direction: column;
    align-items: flex-start;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .favorite-grid {
    grid-template-columns: 1fr;
  }

  .coupon-card {
    grid-template-columns: 1fr;
  }

  .profile-nav {
    overflow-x: auto;
  }

  .nav-item {
    min-width: 220px;
  }

  .btn-save {
    width: 100%;
  }
}
</style>
