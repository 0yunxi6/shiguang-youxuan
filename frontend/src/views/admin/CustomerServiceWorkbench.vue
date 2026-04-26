<template>
  <div class="service-workbench">
    <aside class="workbench-sidebar">
      <div class="brand-block" @click="router.push('/')">
        <img src="/logo.png" alt="拾光 Logo" />
        <div>
          <strong>拾光</strong>
          <span>AI 客服工作台</span>
        </div>
      </div>

      <nav class="side-menu">
        <button
          v-for="item in menuItems"
          :key="item.key"
          :class="{ active: activeMenu === item.key }"
          @click="setActiveMenu(item.key)"
        >
          <span class="menu-icon">{{ item.icon }}</span>
          <span>{{ item.label }}</span>
        </button>
      </nav>

      <div class="agent-status">
        <span class="online-dot"></span>
        <div>
          <strong>AI 小光在线</strong>
          <small>GLM-5.1 / 本地兜底双通道</small>
        </div>
      </div>
      <button class="back-home" @click="router.push('/')">返回拾光</button>
    </aside>

    <section class="conversation-list">
      <div class="search-box">
        <el-icon><Search /></el-icon>
        <input v-model="keyword" placeholder="搜索订单号 / 商品 / 问题" />
      </div>

      <div class="list-header">
        <div>
          <h2>会话列表</h2>
          <p>参考客服工作台布局，集中处理咨询</p>
        </div>
        <button class="filter-btn">
          <el-icon><Tickets /></el-icon>
          筛选
        </button>
      </div>

      <div class="session-tabs">
        <button v-for="tab in sessionTabs" :key="tab.value" :class="{ active: activeSessionTab === tab.value }" @click="activeSessionTab = tab.value">
          {{ tab.label }}
        </button>
      </div>

      <div class="session-cards">
        <button
          v-for="session in filteredSessions"
          :key="session.id"
          :class="['session-card', { active: selectedSessionId === session.id }]"
          @click="selectedSessionId = session.id"
        >
          <UserAvatar :src="session.avatar" :name="session.name" :size="46" />
          <div class="session-main">
            <div class="session-top">
              <strong>{{ session.name }}</strong>
              <time>{{ session.time }}</time>
            </div>
            <p>{{ session.preview }}</p>
          </div>
          <span v-if="session.unread" class="unread">{{ session.unread }}</span>
          <span :class="['session-state', session.state]">{{ stateText(session.state) }}</span>
        </button>
      </div>
    </section>

    <main class="chat-panel">
      <header class="chat-header">
        <div class="chat-user">
          <UserAvatar :src="userProfile?.avatar" :name="displayName" :size="56" />
          <div>
            <h1>{{ displayName }}</h1>
            <p>
              来源：拾光 · AI 咨询
              <span class="divider"></span>
              会话 ID：{{ sessionNo }}
            </p>
          </div>
          <span class="member-tag">{{ userProfile?.level || '普通会员' }}</span>
        </div>
        <div class="chat-actions">
          <el-switch v-model="soundEnabled" inline-prompt active-text="提示音" inactive-text="静音" />
          <button @click="transferHuman">
            <el-icon><Service /></el-icon>
            转人工
          </button>
          <button @click="endConversation">结束会话</button>
        </div>
      </header>

      <template v-if="activeMenu === 'chat'">
      <section ref="messageListRef" class="message-list">
        <div class="day-divider">今天 {{ todayText }}</div>
        <div
          v-for="message in messages"
          :key="message.id"
          :class="['message-row', message.role === 'user' ? 'from-user' : 'from-ai']"
        >
          <UserAvatar
            v-if="message.role !== 'user'"
            src="/logo.png"
            name="小光"
            :size="38"
          />
          <div class="bubble-wrap">
            <div class="bubble">
              <p>{{ message.content }}</p>
              <span v-if="message.fallback" class="fallback-tag">离线兜底</span>
            </div>
            <small>{{ message.time }}</small>
          </div>
          <UserAvatar
            v-if="message.role === 'user'"
            :src="userProfile?.avatar"
            :name="displayName"
            :size="38"
          />
        </div>
        <div v-if="sending" class="message-row from-ai">
          <UserAvatar src="/logo.png" name="小光" :size="38" />
          <div class="bubble loading-bubble">
            <span></span><span></span><span></span>
          </div>
        </div>
        <div ref="messagesEndRef"></div>
      </section>

      <footer class="composer">
        <div class="shortcut-row">
          <button v-for="item in quickReplies" :key="item" @click="sendQuick(item)">
            {{ item }}
          </button>
        </div>
        <textarea
          v-model="inputText"
          maxlength="1000"
          placeholder="请输入消息，Enter 发送，Shift + Enter 换行"
          @keydown.enter.exact.prevent="sendMessage"
        ></textarea>
        <div class="composer-footer">
          <div class="tool-icons">
            <button title="商品卡片" @click="sendQuick('帮我推荐几款适合日常使用的好物')">
              <el-icon><Goods /></el-icon>
            </button>
            <button title="优惠券" @click="sendQuick('我的优惠券怎么使用？')">
              <el-icon><Ticket /></el-icon>
            </button>
            <button title="复制会话" @click="copyConversation">
              <el-icon><CopyDocument /></el-icon>
            </button>
          </div>
          <div class="send-group">
            <span>{{ inputText.length }}/1000</span>
            <button class="send-btn" :disabled="sending || !inputText.trim()" @click="sendMessage">
              {{ sending ? '发送中...' : '发送' }}
            </button>
          </div>
        </div>
      </footer>
      </template>

      <section v-else class="module-panel">
        <div class="module-hero">
          <span>{{ activeMenuMeta.icon }}</span>
          <div>
            <h2>{{ activeMenuMeta.label }}</h2>
            <p>{{ activeMenuMeta.desc }}</p>
          </div>
        </div>

        <div v-if="activeMenu === 'orders'" class="module-grid">
          <article v-for="order in recentOrders" :key="order.id" class="module-card">
            <div class="module-card-head">
              <h3>{{ order.orderNo }}</h3>
              <em>{{ orderStatusText(order.status) }}</em>
            </div>
            <p>商品：{{ orderItemsText(order.items) }}</p>
            <p>实付：¥{{ formatAmount(order.totalAmount) }}　下单：{{ formatDateTime(order.createTime) }}</p>
            <div class="module-actions">
              <button @click="askOrder(order)">AI 查询订单</button>
              <button @click="askLogistics(order)">物流进度</button>
              <button @click="router.push('/order')">打开订单页</button>
            </div>
          </article>
          <div v-if="!recentOrders.length" class="module-empty">暂无订单，去首页下单后这里会自动展示。</div>
        </div>

        <div v-if="activeMenu === 'goods'" class="module-grid product-grid">
          <article v-for="product in recommendedProducts" :key="product.id" class="module-card product-module-card">
            <img :src="product.imageUrl || '/placeholder.svg'" alt="" />
            <div>
              <h3>{{ product.name }}</h3>
              <p>{{ product.brand || '拾光精选' }} · 库存 {{ product.stock ?? 0 }}</p>
              <strong>¥{{ formatAmount(product.effectivePrice || product.promotionPrice || product.price) }}</strong>
              <div class="module-actions">
                <button @click="router.push(`/product/${product.id}`)">查看商品</button>
                <button @click="sendQuick(`帮我介绍一下商品：${product.name}`)">AI 介绍</button>
              </div>
            </div>
          </article>
          <div v-if="!recommendedProducts.length" class="module-empty">暂无推荐商品，可先去首页浏览。</div>
        </div>

        <div v-if="activeMenu === 'afterSales'" class="module-grid">
          <article v-for="item in afterSales" :key="item.id" class="module-card">
            <div class="module-card-head">
              <h3>{{ item.orderNo }}</h3>
              <em>{{ afterSaleStatusText(item.status) }}</em>
            </div>
            <p>类型：{{ afterSaleTypeText(item.type) }}　金额：¥{{ formatAmount(item.amount) }}</p>
            <p>原因：{{ item.reason }}</p>
            <p v-if="item.auditRemark">处理备注：{{ item.auditRemark }}</p>
            <div class="module-actions">
              <button @click="sendQuick(`帮我查看订单 ${item.orderNo} 的售后进度`)">AI 查询进度</button>
              <button @click="router.push('/profile?tab=afterSales')">售后进度页</button>
            </div>
          </article>
          <article v-if="!afterSales.length" class="module-card">
            <h3>暂无售后记录</h3>
            <p>如商品需要退款、退货或换货，可从订单页发起售后申请。</p>
            <div class="module-actions">
              <button @click="recentOrder ? sendQuick(`我想申请订单 ${recentOrder.orderNo} 的售后`) : router.push('/order')">申请最近订单售后</button>
            </div>
          </article>
        </div>

        <div v-if="activeMenu === 'customers'" class="module-grid">
          <article class="module-card profile-module-card">
            <UserAvatar :src="userProfile?.avatar" :name="displayName" :size="72" />
            <div>
              <h3>{{ displayName }}</h3>
              <p>会员：{{ userProfile?.level || '普通会员' }}　积分：{{ userProfile?.points || 0 }}</p>
              <p>手机：{{ maskPhone(userProfile?.phone) }}　邮箱：{{ userProfile?.email || '未设置' }}</p>
              <p>最近地址：{{ latestAddress }}</p>
              <div class="module-actions">
                <button @click="router.push('/profile')">个人中心</button>
                <button @click="router.push('/profile?tab=messages')">消息中心</button>
                <button @click="sendQuick('帮我检查一下账号安全和会员权益')">AI 账号建议</button>
              </div>
            </div>
          </article>
        </div>

        <div v-if="activeMenu === 'stats'" class="stats-grid">
          <div class="stat-card"><strong>{{ recentOrders.length }}</strong><span>最近订单</span></div>
          <div class="stat-card"><strong>{{ pendingOrderCount }}</strong><span>待处理订单</span></div>
          <div class="stat-card"><strong>{{ availableCouponCount }}</strong><span>可用优惠券</span></div>
          <div class="stat-card"><strong>{{ afterSales.length }}</strong><span>售后记录</span></div>
          <div class="stat-card"><strong>{{ recommendedProducts.length }}</strong><span>推荐商品</span></div>
          <div class="stat-card"><strong>{{ userProfile?.points || 0 }}</strong><span>会员积分</span></div>
        </div>

        <div v-if="activeMenu === 'settings'" class="module-grid">
          <article class="module-card">
            <h3>客服设置</h3>
            <p>AI 模型：GLM-5.1；后端已启用本地兜底，接口异常时仍可回复常见问题。</p>
            <label class="setting-line">
              <span>消息提示音</span>
              <el-switch v-model="soundEnabled" />
            </label>
            <label class="setting-line">
              <span>优先使用订单上下文</span>
              <el-switch :model-value="true" disabled />
            </label>
            <div class="module-actions">
              <button @click="copyConversation">复制当前会话</button>
              <button @click="resetConversation">清空会话</button>
              <button @click="sendQuick('请总结一下当前账号的订单、优惠券和售后状态')">AI 总结</button>
            </div>
          </article>
        </div>
      </section>
    </main>

    <aside class="customer-panel">
      <section class="info-card">
        <div class="card-heading">
          <h3>客户信息</h3>
          <button @click="router.push('/profile')">展开</button>
        </div>
        <div class="customer-main">
          <UserAvatar :src="userProfile?.avatar" :name="displayName" :size="58" />
          <div>
            <strong>{{ displayName }}</strong>
            <span>{{ userProfile?.level || '普通会员' }}</span>
          </div>
        </div>
        <div class="info-lines">
          <p><el-icon><Phone /></el-icon><span>手机：{{ maskPhone(userProfile?.phone) }}</span></p>
          <p><el-icon><Message /></el-icon><span>邮箱：{{ userProfile?.email || '未设置' }}</span></p>
          <p><el-icon><Clock /></el-icon><span>注册：{{ formatDate(userProfile?.createTime) || '—' }}</span></p>
          <p><el-icon><Location /></el-icon><span>地址：{{ latestAddress }}</span></p>
        </div>
      </section>

      <section class="info-card">
        <div class="card-heading">
          <h3>最近订单</h3>
          <button @click="router.push('/order')">查看全部</button>
        </div>
        <div v-if="recentOrder" class="order-card">
          <div class="order-head">
            <span>订单号：{{ recentOrder.orderNo }}</span>
            <strong>{{ orderStatusText(recentOrder.status) }}</strong>
          </div>
          <div v-for="item in previewOrderItems" :key="item.id || item.productId" class="order-product">
            <img :src="item.productImage || '/placeholder.svg'" alt="" />
            <div>
              <strong>{{ item.productName }}</strong>
              <span>{{ item.productSpec || '默认规格' }} · x{{ item.quantity }}</span>
              <em>¥{{ formatAmount(item.price) }}</em>
            </div>
          </div>
          <div class="order-total">
            <span>共 {{ recentOrder.items?.length || 0 }} 件商品</span>
            <strong>合计：¥{{ formatAmount(recentOrder.totalAmount) }}</strong>
          </div>
          <div class="order-actions">
            <button @click="router.push('/order')">查看详情</button>
            <button @click="sendQuick(`帮我查询订单 ${recentOrder.orderNo} 的物流进度`)">物流查询</button>
          </div>
        </div>
        <div v-else class="empty-mini">
          暂无订单记录，可先去首页挑选商品。
        </div>
      </section>

      <section class="info-card">
        <div class="card-heading">
          <h3>快捷操作</h3>
          <span>高频场景</span>
        </div>
        <div class="quick-grid">
          <button v-for="action in quickActions" :key="action.text" @click="handleQuickAction(action)">
            <span>{{ action.icon }}</span>
            {{ action.text }}
          </button>
        </div>
      </section>
    </aside>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Search,
  Tickets,
  Ticket,
  Goods,
  CopyDocument,
  Phone,
  Message,
  Clock,
  Location,
  Service
} from '@element-plus/icons-vue'
import { chatWithAi, getAfterSales, getCoupons, getOrderList, getProductList, getUserProfile } from '../../api'
import UserAvatar from '../../components/UserAvatar.vue'

const router = useRouter()
const keyword = ref('')
const activeSessionTab = ref('all')
const selectedSessionId = ref(1)
const inputText = ref('')
const sending = ref(false)
const soundEnabled = ref(true)
const activeMenu = ref('chat')
const userProfile = ref(null)
const recentOrders = ref([])
const coupons = ref([])
const afterSales = ref([])
const recommendedProducts = ref([])
const messagesEndRef = ref(null)
const messageListRef = ref(null)
const sessionNo = `SG${new Date().getFullYear()}${String(Date.now()).slice(-8)}`

const menuItems = [
  { key: 'chat', label: '会话', icon: '💬', desc: '继续与 AI 小光沟通' },
  { key: 'orders', label: '订单查询', icon: '📋', desc: '查看当前账号最近订单、金额和状态' },
  { key: 'goods', label: '商品查询', icon: '🛍️', desc: '展示可推荐商品并跳转详情' },
  { key: 'afterSales', label: '售后处理', icon: '🔁', desc: '查看售后记录或引导发起申请' },
  { key: 'customers', label: '客户管理', icon: '👥', desc: '查看账号、会员、积分和联系方式' },
  { key: 'stats', label: '数据统计', icon: '📊', desc: '汇总订单、优惠券、售后和积分' },
  { key: 'settings', label: '系统设置', icon: '⚙️', desc: '管理客服会话和 AI 兜底设置' }
]

const sessionTabs = [
  { value: 'all', label: '全部(12)' },
  { value: 'active', label: '接待中(5)' },
  { value: 'waiting', label: '等待中(2)' },
  { value: 'done', label: '已结束(5)' }
]

const sessions = ref([
  { id: 1, name: '当前用户', state: 'active', time: '刚刚', unread: 0, preview: '正在咨询拾光 AI 客服小光', avatar: '' },
  { id: 2, name: '爱吃草莓的猫', state: 'waiting', time: '10:28', unread: 2, preview: '我的订单什么时候发货？', avatar: '/default-avatar.png' },
  { id: 3, name: '快乐的一只鱼', state: 'active', time: '10:25', unread: 0, preview: '商品支持七天无理由吗？', avatar: '/default-avatar.png' },
  { id: 4, name: '小太阳', state: 'done', time: '昨天', unread: 0, preview: '优惠券怎么使用？', avatar: '/default-avatar.png' }
])

const quickReplies = [
  '这款商品有货吗？',
  '什么时候能发货？',
  '优惠券怎么使用？',
  '如何申请售后？',
  '发票信息在哪里维护？'
]

const quickActions = [
  { text: '发送订单', icon: '📦', prompt: () => recentOrder.value ? `帮我查看订单 ${recentOrder.value.orderNo}` : '我想查询我的订单' },
  { text: '发送商品', icon: '🛍️', prompt: () => '帮我推荐几款拾光热卖好物' },
  { text: '发送优惠券', icon: '🎟️', prompt: () => '我的优惠券怎么使用，结算时会自动选择吗？' },
  { text: '物流查询', icon: '🚚', prompt: () => recentOrder.value ? `帮我查询订单 ${recentOrder.value.orderNo} 的物流进度` : '如何查询物流进度？' },
  { text: '售后申请', icon: '🔁', prompt: () => '我想申请退换货售后，需要怎么操作？' },
  { text: '结束会话', icon: '✅', action: 'end' }
]

const messages = ref([
  {
    id: Date.now(),
    role: 'assistant',
    content: '你好，我是拾光 AI 客服小光。可以帮你处理商品推荐、优惠券、下单支付、物流、发票和售后问题；涉及真实订单状态时，请以订单页展示为准。',
    time: formatClock(new Date())
  }
])

const todayText = computed(() => formatClock(new Date()))
const displayName = computed(() => userProfile.value?.nickname || userProfile.value?.username || '拾光用户')
const filteredSessions = computed(() => {
  const words = keyword.value.trim().toLowerCase()
  return sessions.value.filter(item => {
    const tabOk = activeSessionTab.value === 'all' || item.state === activeSessionTab.value
    const keywordOk = !words || `${item.name}${item.preview}`.toLowerCase().includes(words)
    return tabOk && keywordOk
  })
})
const recentOrder = computed(() => recentOrders.value[0] || null)
const previewOrderItems = computed(() => (recentOrder.value?.items || []).slice(0, 2))
const latestAddress = computed(() => recentOrder.value?.receiverAddress || '暂未读取到地址')
const activeMenuMeta = computed(() => menuItems.find(item => item.key === activeMenu.value) || menuItems[0])
const pendingOrderCount = computed(() => recentOrders.value.filter(order => [0, 1, 2].includes(Number(order.status))).length)
const availableCouponCount = computed(() => coupons.value.filter(coupon => Number(coupon.status) === 0).length)

const stateText = (state) => ({ active: '接待中', waiting: '等待中', done: '已结束' }[state] || '接待中')
const orderStatusText = (status) => ({
  0: '待付款',
  1: '待发货',
  2: '待收货',
  3: '已完成',
  4: '已取消'
}[status] || '未知')
const afterSaleStatusText = (status) => ({
  0: '待审核',
  1: '已同意',
  2: '已拒绝',
  3: '已完成'
}[status] || '待审核')
const afterSaleTypeText = (type) => ({
  refund: '退款',
  return: '退货',
  exchange: '换货'
}[type] || '退款')

function formatClock(date) {
  return new Intl.DateTimeFormat('zh-CN', { hour: '2-digit', minute: '2-digit', hour12: false }).format(date)
}

function formatDate(value) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  return new Intl.DateTimeFormat('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' }).format(date)
}

function formatDateTime(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  }).format(date)
}

function formatAmount(value) {
  const amount = Number(value || 0)
  return amount.toFixed(2)
}

function maskPhone(phone) {
  if (!phone) return '未设置'
  const raw = String(phone)
  if (raw.length < 7) return raw
  return `${raw.slice(0, 3)}****${raw.slice(-4)}`
}

function scrollToBottom() {
  nextTick(() => {
    messagesEndRef.value?.scrollIntoView({ behavior: 'smooth', block: 'end' })
  })
}

function orderItemsText(items = []) {
  if (!items.length) return '暂无商品明细'
  return items.map(item => `${item.productName || '商品'}×${item.quantity || 1}`).join('、')
}

function setActiveMenu(key) {
  activeMenu.value = key
  if (key === 'chat') {
    scrollToBottom()
  }
}

async function loadContext() {
  try {
    const profileRes = await getUserProfile()
    userProfile.value = profileRes.data || null
    if (userProfile.value) {
      sessions.value[0] = {
        ...sessions.value[0],
        name: displayName.value,
        avatar: userProfile.value.avatar || ''
      }
    }
  } catch (error) {
    console.error(error)
  }

  try {
    const orderRes = await getOrderList()
    const payload = orderRes.data
    const list = Array.isArray(payload?.records) ? payload.records : (Array.isArray(payload) ? payload : [])
    recentOrders.value = list.slice(0, 8)
  } catch (error) {
    console.error(error)
  }

  try {
    const couponRes = await getCoupons()
    coupons.value = Array.isArray(couponRes.data) ? couponRes.data : []
  } catch (error) {
    console.error(error)
  }

  try {
    const afterSaleRes = await getAfterSales()
    afterSales.value = Array.isArray(afterSaleRes.data) ? afterSaleRes.data : []
  } catch (error) {
    console.error(error)
  }

  try {
    const productRes = await getProductList({ page: 1, size: 8, sort: 'sales' })
    recommendedProducts.value = productRes.data?.records || []
  } catch (error) {
    console.error(error)
  }
}

function appendMessage(role, content, extra = {}) {
  messages.value.push({
    id: `${Date.now()}-${Math.random().toString(16).slice(2)}`,
    role,
    content,
    time: formatClock(new Date()),
    ...extra
  })
  scrollToBottom()
}

function sendQuick(text) {
  activeMenu.value = 'chat'
  inputText.value = text
  sendMessage()
}

function askOrder(order) {
  if (!order?.orderNo) return
  sendQuick(`帮我查看订单 ${order.orderNo}`)
}

function askLogistics(order) {
  if (!order?.orderNo) return
  sendQuick(`帮我查询订单 ${order.orderNo} 的物流进度`)
}

async function sendMessage() {
  const text = inputText.value.trim()
  if (!text || sending.value) return

  const history = messages.value.slice(-8).map(item => ({ role: item.role, content: item.content }))
  appendMessage('user', text)
  inputText.value = ''
  sending.value = true

  try {
    const res = await chatWithAi({ message: text, history })
    const data = res.data || {}
    appendMessage('assistant', data.reply || '我已收到你的问题，请稍后再试。', { fallback: Boolean(data.fallback) })
  } catch (error) {
    console.error(error)
    appendMessage('assistant', '暂时无法连接 AI 服务，我先给你兜底答复：可以在订单页查看订单状态，在个人中心查看优惠券、消息、售后和发票信息。', { fallback: true })
  } finally {
    sending.value = false
  }
}

function transferHuman() {
  appendMessage('assistant', '已为你记录转人工需求。当前版本先由 AI 小光接待，后续可接入客服坐席排队、工单和在线客服系统。')
}

function endConversation() {
  appendMessage('assistant', '本次会话已结束。后续仍可继续输入问题，我会重新为你接待。')
  ElMessage.success('会话已标记结束')
}

function resetConversation() {
  messages.value = [{
    id: Date.now(),
    role: 'assistant',
    content: '会话已清空。我是拾光 AI 客服小光，可以继续帮你查询订单、物流、优惠券、售后、发票和商品推荐。',
    time: formatClock(new Date())
  }]
  activeMenu.value = 'chat'
  scrollToBottom()
}

function handleQuickAction(action) {
  if (action.action === 'end') {
    endConversation()
    return
  }
  sendQuick(action.prompt())
}

async function copyConversation() {
  const text = messages.value.map(item => `${item.role === 'user' ? '我' : '小光'}：${item.content}`).join('\n')
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('会话已复制')
  } catch (error) {
    console.error(error)
    ElMessage.info('当前浏览器不支持自动复制')
  }
}

onMounted(() => {
  loadContext()
  scrollToBottom()
})
</script>

<style scoped>
.service-workbench {
  min-height: calc(100vh - 100px);
  background: #f5f7fb;
  display: grid;
  grid-template-columns: minmax(280px, 326px) minmax(480px, 1fr) minmax(290px, 340px);
  color: #172033;
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid #e6ebf3;
}

.workbench-sidebar {
  display: none;
}

.brand-block {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}

.brand-block img {
  width: 42px;
  height: 42px;
  border-radius: 14px;
  background: #fff;
  object-fit: cover;
}

.brand-block strong {
  display: block;
  font-size: 18px;
  letter-spacing: 0.04em;
}

.brand-block span,
.agent-status small {
  color: rgba(255, 255, 255, 0.68);
  font-size: 12px;
}

.side-menu {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.side-menu button {
  border: none;
  border-radius: 10px;
  padding: 13px 14px;
  background: transparent;
  color: rgba(255, 255, 255, 0.86);
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 15px;
  cursor: pointer;
  transition: all 0.18s ease;
}

.side-menu button.active,
.side-menu button:hover {
  background: linear-gradient(135deg, #2d8cff, #1664ff);
  color: #fff;
  box-shadow: 0 12px 24px rgba(22, 100, 255, 0.26);
}

.menu-icon {
  width: 24px;
  text-align: center;
}

.agent-status {
  margin-top: auto;
  padding: 14px;
  border-top: 1px solid rgba(255, 255, 255, 0.12);
  display: flex;
  gap: 10px;
  align-items: center;
}

.online-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: #34d399;
  box-shadow: 0 0 0 5px rgba(52, 211, 153, 0.16);
}

.back-home {
  height: 36px;
  border: 1px solid rgba(255, 255, 255, 0.22);
  background: rgba(255, 255, 255, 0.06);
  color: #fff;
  border-radius: 8px;
  cursor: pointer;
}

.conversation-list,
.chat-panel,
.customer-panel {
  border-right: 1px solid #e6ebf3;
}

.conversation-list {
  background: #fff;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.search-box {
  margin: 16px 20px 14px;
  height: 42px;
  border: 1px solid #dce4ef;
  border-radius: 10px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 12px;
  color: #8a97ad;
}

.search-box input {
  border: none;
  outline: none;
  flex: 1;
  min-width: 0;
  color: #172033;
}

.list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 20px 6px;
}

.list-header h2 {
  font-size: 18px;
  margin-bottom: 4px;
}

.list-header p {
  color: #8a97ad;
  font-size: 12px;
}

.filter-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  border: none;
  background: #f4f7fb;
  color: #53627a;
  border-radius: 999px;
  padding: 7px 10px;
  cursor: pointer;
}

.session-tabs {
  display: flex;
  gap: 14px;
  padding: 0 20px;
  border-bottom: 1px solid #eef2f7;
}

.session-tabs button {
  position: relative;
  border: none;
  background: transparent;
  padding: 12px 0;
  color: #53627a;
  cursor: pointer;
}

.session-tabs button.active {
  color: #1664ff;
  font-weight: 700;
}

.session-tabs button.active::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: -1px;
  height: 2px;
  background: #1664ff;
  border-radius: 2px;
}

.session-cards {
  overflow-y: auto;
  flex: 1;
}

.session-card {
  position: relative;
  width: 100%;
  border: none;
  border-bottom: 1px solid #eef2f7;
  background: #fff;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 17px 20px;
  text-align: left;
  cursor: pointer;
  transition: background 0.18s ease;
}

.session-card.active,
.session-card:hover {
  background: #eef6ff;
}

.session-main {
  min-width: 0;
  flex: 1;
}

.session-top {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 6px;
}

.session-top strong {
  font-size: 15px;
}

.session-top time,
.session-card p {
  color: #728098;
  font-size: 12px;
}

.session-card p {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.unread {
  position: absolute;
  right: 18px;
  bottom: 18px;
  min-width: 17px;
  height: 17px;
  line-height: 17px;
  text-align: center;
  border-radius: 999px;
  background: #ff3154;
  color: #fff;
  font-size: 11px;
}

.session-state {
  position: absolute;
  right: 52px;
  top: 17px;
  padding: 2px 7px;
  border-radius: 999px;
  font-size: 11px;
}

.session-state.active {
  background: #e8f7ef;
  color: #0a9b52;
}

.session-state.waiting {
  background: #fff4e5;
  color: #df8a00;
}

.session-state.done {
  background: #eef2f7;
  color: #718096;
}

.chat-panel {
  min-width: 0;
  background: #fff;
  display: grid;
  grid-template-rows: auto 1fr auto;
}

.chat-header {
  min-height: 86px;
  padding: 18px 24px;
  border-bottom: 1px solid #e6ebf3;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}

.chat-user {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.chat-user h1 {
  font-size: 20px;
  margin-bottom: 4px;
}

.chat-user p {
  color: #728098;
  font-size: 13px;
}

.divider {
  display: inline-block;
  width: 1px;
  height: 12px;
  margin: 0 10px;
  background: #dce4ef;
  vertical-align: middle;
}

.member-tag {
  padding: 3px 8px;
  border-radius: 999px;
  background: #eaf2ff;
  color: #1664ff;
  font-size: 12px;
  font-weight: 700;
}

.chat-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.chat-actions button {
  height: 34px;
  padding: 0 12px;
  border: 1px solid #dce4ef;
  border-radius: 8px;
  background: #fff;
  color: #53627a;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
}

.message-list {
  min-height: 0;
  overflow-y: auto;
  padding: 24px 28px;
  background:
    radial-gradient(circle at top left, rgba(22, 100, 255, 0.06), transparent 28%),
    #fff;
}

.day-divider {
  width: max-content;
  margin: 0 auto 22px;
  color: #8a97ad;
  font-size: 12px;
}

.message-row {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  margin-bottom: 20px;
}

.message-row.from-user {
  justify-content: flex-end;
}

.bubble-wrap {
  max-width: min(640px, 72%);
}

.bubble {
  position: relative;
  padding: 13px 16px;
  border-radius: 12px;
  background: #f3f5f8;
  color: #172033;
  line-height: 1.75;
  box-shadow: 0 6px 18px rgba(15, 23, 42, 0.04);
}

.from-user .bubble {
  background: linear-gradient(135deg, #eaf3ff, #dfeeff);
}

.bubble p {
  white-space: pre-wrap;
  word-break: break-word;
}

.bubble-wrap small {
  display: block;
  margin-top: 5px;
  color: #9aa6b8;
  font-size: 12px;
}

.from-user .bubble-wrap small {
  text-align: right;
}

.fallback-tag {
  display: inline-block;
  margin-top: 8px;
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(244, 156, 43, 0.12);
  color: #c97913;
  font-size: 12px;
}

.loading-bubble {
  display: flex;
  gap: 5px;
  align-items: center;
  width: 74px;
}

.loading-bubble span {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #93a4bd;
  animation: typing 1s infinite ease-in-out;
}

.loading-bubble span:nth-child(2) {
  animation-delay: 0.15s;
}

.loading-bubble span:nth-child(3) {
  animation-delay: 0.3s;
}

@keyframes typing {
  0%, 80%, 100% { transform: translateY(0); opacity: 0.5; }
  40% { transform: translateY(-4px); opacity: 1; }
}

.composer {
  margin: 0 24px 20px;
  border: 1px solid #dce4ef;
  border-radius: 12px;
  background: #fff;
  overflow: hidden;
}

.shortcut-row {
  display: flex;
  gap: 10px;
  padding: 12px 14px;
  border-bottom: 1px solid #edf1f6;
  overflow-x: auto;
}

.shortcut-row button,
.quick-grid button,
.order-actions button {
  border: 1px solid #dce4ef;
  border-radius: 8px;
  background: #fff;
  color: #53627a;
  cursor: pointer;
  transition: all 0.18s ease;
}

.shortcut-row button {
  white-space: nowrap;
  padding: 6px 10px;
}

.shortcut-row button:hover,
.quick-grid button:hover,
.order-actions button:hover {
  color: #1664ff;
  border-color: #9bc4ff;
  background: #f3f8ff;
}

.composer textarea {
  width: 100%;
  height: 110px;
  border: none;
  outline: none;
  resize: none;
  padding: 16px;
  color: #172033;
  font-family: inherit;
  line-height: 1.6;
}

.composer-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
}

.tool-icons {
  display: flex;
  gap: 10px;
}

.tool-icons button {
  width: 32px;
  height: 32px;
  border: none;
  background: #f3f6fa;
  border-radius: 8px;
  color: #53627a;
  cursor: pointer;
}

.send-group {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #9aa6b8;
  font-size: 12px;
}

.send-btn {
  min-width: 86px;
  height: 36px;
  border: none;
  border-radius: 8px;
  background: linear-gradient(135deg, #2d8cff, #1664ff);
  color: #fff;
  font-weight: 700;
  cursor: pointer;
}

.send-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.customer-panel {
  padding: 18px 16px;
  background: #f8fafd;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.info-card {
  border: 1px solid #e1e7f0;
  border-radius: 12px;
  background: #fff;
  padding: 16px;
  box-shadow: 0 10px 26px rgba(15, 23, 42, 0.04);
}

.card-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.card-heading h3 {
  font-size: 16px;
}

.card-heading button,
.card-heading span {
  border: none;
  background: transparent;
  color: #1664ff;
  font-size: 12px;
  cursor: pointer;
}

.customer-main {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.customer-main strong {
  display: block;
  margin-bottom: 4px;
  font-size: 17px;
}

.customer-main span {
  color: #ff3154;
  font-size: 12px;
}

.info-lines {
  display: flex;
  flex-direction: column;
  gap: 10px;
  color: #53627a;
  font-size: 13px;
}

.info-lines p {
  display: flex;
  align-items: flex-start;
  gap: 9px;
  line-height: 1.55;
}

.order-card {
  border: 1px solid #edf1f6;
  border-radius: 10px;
  padding: 12px;
}

.order-head,
.order-total,
.order-actions {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
}

.order-head {
  font-size: 12px;
  color: #728098;
  margin-bottom: 12px;
}

.order-head strong {
  color: #ff6b35;
}

.order-product {
  display: flex;
  gap: 10px;
  padding: 10px 0;
  border-top: 1px solid #f0f3f8;
}

.order-product img {
  width: 58px;
  height: 58px;
  border-radius: 8px;
  object-fit: cover;
  background: #f5f7fb;
}

.order-product div {
  min-width: 0;
}

.order-product strong {
  display: block;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.order-product span,
.order-total span {
  display: block;
  color: #728098;
  font-size: 12px;
  margin: 4px 0;
}

.order-product em {
  color: #172033;
  font-style: normal;
  font-weight: 700;
}

.order-total {
  padding-top: 10px;
  border-top: 1px solid #f0f3f8;
  margin-top: 6px;
}

.order-total strong {
  color: #ff5a1f;
}

.order-actions {
  margin-top: 12px;
}

.order-actions button {
  flex: 1;
  height: 34px;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.quick-grid button {
  height: 42px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.empty-mini {
  padding: 24px 12px;
  text-align: center;
  color: #8a97ad;
  background: #f8fafd;
  border-radius: 10px;
}

.module-panel {
  min-height: 0;
  overflow-y: auto;
  padding: 24px;
  background: linear-gradient(180deg, #f7faff 0%, #fff 42%);
}

.module-hero {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  border: 1px solid #e1e9f5;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.05);
  margin-bottom: 18px;
}

.module-hero > span {
  width: 48px;
  height: 48px;
  border-radius: 16px;
  display: grid;
  place-items: center;
  background: #edf5ff;
  font-size: 24px;
}

.module-hero h2 {
  font-size: 22px;
  margin-bottom: 6px;
}

.module-hero p,
.module-card p {
  color: #64748b;
  line-height: 1.7;
}

.module-grid {
  display: grid;
  gap: 14px;
}

.module-card {
  padding: 18px;
  border: 1px solid #e1e9f5;
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.04);
}

.module-card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.module-card h3 {
  font-size: 17px;
  word-break: break-all;
}

.module-card em {
  flex-shrink: 0;
  padding: 3px 9px;
  border-radius: 999px;
  background: #fff4e8;
  color: #f97316;
  font-style: normal;
  font-size: 12px;
  font-weight: 700;
}

.module-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 14px;
}

.module-actions button {
  min-height: 34px;
  border: 1px solid #cfe0f7;
  border-radius: 8px;
  background: #fff;
  color: #1664ff;
  padding: 0 12px;
  cursor: pointer;
}

.module-actions button:hover {
  background: #edf5ff;
}

.product-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.product-module-card {
  display: grid;
  grid-template-columns: 86px 1fr;
  gap: 14px;
}

.product-module-card img {
  width: 86px;
  height: 86px;
  border-radius: 12px;
  object-fit: cover;
  background: #f5f7fb;
}

.product-module-card strong {
  display: block;
  margin-top: 6px;
  color: #ff5a1f;
  font-size: 18px;
}

.profile-module-card {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.stat-card {
  min-height: 118px;
  border: 1px solid #e1e9f5;
  border-radius: 16px;
  background: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  box-shadow: 0 12px 26px rgba(15, 23, 42, 0.05);
}

.stat-card strong {
  color: #1664ff;
  font-size: 34px;
}

.stat-card span {
  color: #64748b;
  margin-top: 6px;
}

.setting-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 14px;
  padding: 12px 0;
  border-top: 1px solid #eef2f7;
}

.module-empty {
  padding: 34px 20px;
  border: 1px dashed #cfe0f7;
  border-radius: 14px;
  text-align: center;
  color: #64748b;
  background: #fff;
}

@media (max-width: 1280px) {
  .service-workbench {
    grid-template-columns: 300px minmax(420px, 1fr);
  }

  .brand-block div,
  .side-menu button span:last-child,
  .agent-status div,
  .back-home,
  .customer-panel {
    display: none;
  }

  .workbench-sidebar {
    align-items: center;
  }

  .side-menu button {
    justify-content: center;
    width: 52px;
  }
}

@media (max-width: 900px) {
  .service-workbench {
    display: flex;
    flex-direction: column;
  }

  .workbench-sidebar {
    display: none;
  }

  .conversation-list {
    max-height: 280px;
  }

  .chat-panel {
    min-height: 680px;
  }

  .chat-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .chat-actions {
    flex-wrap: wrap;
  }
}
</style>
