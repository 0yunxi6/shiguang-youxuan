<template>
  <div class="service-page">
    <aside class="service-sidebar">
      <button class="brand" @click="router.push('/')">
        <img src="/logo.png" alt="拾光 Logo" />
        <span>
          <strong>拾光</strong>
          <small>AI 客服小光</small>
        </span>
      </button>

      <div class="service-state">
        <span class="online-dot"></span>
        <div>
          <strong>在线服务中</strong>
          <small>订单、物流、优惠券、售后都可以问</small>
        </div>
      </div>

      <nav class="topic-list">
        <button
          v-for="topic in topics"
          :key="topic.key"
          type="button"
          @click="sendQuick(topic.prompt)"
        >
          <span>{{ topic.icon }}</span>
          <div>
            <strong>{{ topic.title }}</strong>
            <small>{{ topic.desc }}</small>
          </div>
        </button>
      </nav>

      <button class="back-home" @click="router.push('/')">返回拾光首页</button>
    </aside>

    <main class="chat-shell">
      <header class="chat-header">
        <div class="assistant-info">
          <UserAvatar src="/logo.png" name="小光" :size="54" />
          <div>
            <h1>拾光 AI 客服</h1>
            <p>我是小光，正在为 {{ displayName }} 服务。涉及订单时会优先读取你当前账号的真实订单数据。</p>
          </div>
        </div>
        <div class="chat-header-actions">
          <span class="model-tag">GLM-5.1</span>
          <button @click="copyConversation">复制会话</button>
          <button @click="resetConversation">清空</button>
        </div>
      </header>

      <section ref="messageListRef" class="message-list">
        <div class="day-divider">今天 {{ todayText }}</div>
        <div
          v-for="message in messages"
          :key="message.id"
          :class="['message-row', message.role === 'user' ? 'from-user' : 'from-ai']"
        >
          <UserAvatar v-if="message.role !== 'user'" src="/logo.png" name="小光" :size="38" />
          <div class="bubble-wrap">
            <div class="bubble">
              <p>{{ message.content }}</p>
              <span v-if="message.fallback" class="fallback-tag">兜底回复</span>
            </div>
            <small>{{ message.time }}</small>
          </div>
          <UserAvatar v-if="message.role === 'user'" :src="userProfile?.avatar" :name="displayName" :size="38" />
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
          placeholder="请输入你的问题，Enter 发送，Shift + Enter 换行"
          @keydown.enter.exact.prevent="sendMessage"
        ></textarea>
        <div class="composer-footer">
          <div class="tool-icons">
            <button title="推荐商品" @click="sendQuick('帮我推荐几款拾光好物')">🛍️</button>
            <button title="优惠券" @click="sendQuick('我的优惠券怎么使用？')">🎟️</button>
            <button title="复制会话" @click="copyConversation">📋</button>
          </div>
          <div class="send-group">
            <span>{{ inputText.length }}/1000</span>
            <button class="send-btn" :disabled="sending || !inputText.trim()" @click="sendMessage">
              {{ sending ? '发送中...' : '发送' }}
            </button>
          </div>
        </div>
      </footer>
    </main>

    <aside class="self-panel">
      <section class="info-card">
        <div class="card-heading">
          <h3>我的信息</h3>
          <button @click="router.push('/profile')">去个人中心</button>
        </div>
        <div class="profile-row">
          <UserAvatar :src="userProfile?.avatar" :name="displayName" :size="58" />
          <div>
            <strong>{{ displayName }}</strong>
            <span>{{ userProfile?.level || '普通会员' }} · {{ userProfile?.points || 0 }} 积分</span>
          </div>
        </div>
        <div class="info-lines">
          <p>手机：{{ maskPhone(userProfile?.phone) }}</p>
          <p>邮箱：{{ userProfile?.email || '未设置' }}</p>
          <p>注册：{{ formatDate(userProfile?.createTime) || '—' }}</p>
        </div>
      </section>

      <section class="info-card">
        <div class="card-heading">
          <h3>我的最近订单</h3>
          <button @click="router.push('/order')">全部订单</button>
        </div>
        <div v-if="recentOrder" class="order-card">
          <div class="order-head">
            <span>{{ recentOrder.orderNo }}</span>
            <strong>{{ orderStatusText(recentOrder.status) }}</strong>
          </div>
          <div v-for="item in previewOrderItems" :key="item.id || item.productId" class="order-product">
            <img :src="item.productImage || '/placeholder.svg'" alt="" />
            <div>
              <strong>{{ item.productName || '商品' }}</strong>
              <span>{{ item.productSpec || '默认规格' }} · x{{ item.quantity }}</span>
              <em>¥{{ formatAmount(item.price) }}</em>
            </div>
          </div>
          <div class="order-total">
            <span>共 {{ recentOrder.items?.length || 0 }} 件</span>
            <strong>合计：¥{{ formatAmount(recentOrder.totalAmount) }}</strong>
          </div>
          <div class="order-actions">
            <button @click="sendQuick(`帮我查看订单 ${recentOrder.orderNo}`)">查订单</button>
            <button @click="sendQuick(`帮我查询订单 ${recentOrder.orderNo} 的物流进度`)">查物流</button>
          </div>
        </div>
        <div v-else class="empty-mini">暂无订单。下单后我可以帮你查订单和物流。</div>
      </section>

      <section class="info-card">
        <div class="card-heading">
          <h3>自助入口</h3>
          <span>常用功能</span>
        </div>
        <div class="quick-grid">
          <button v-for="action in selfActions" :key="action.text" @click="handleSelfAction(action)">
            <span>{{ action.icon }}</span>
            {{ action.text }}
          </button>
        </div>
      </section>

      <section class="info-card">
        <div class="card-heading">
          <h3>人工工单</h3>
          <button :disabled="ticketSubmitting" @click="createHumanTicket()">
            {{ ticketSubmitting ? '提交中' : '转人工' }}
          </button>
        </div>
        <div v-if="tickets.length" class="ticket-list">
          <div v-for="ticket in tickets.slice(0, 3)" :key="ticket.id" class="ticket-item">
            <strong>{{ ticket.ticketNo }}</strong>
            <span>{{ ticket.title }}</span>
            <em>{{ ticketStatusText(ticket.status) }}</em>
          </div>
        </div>
        <div v-else class="empty-mini">AI 无法解决时，可一键创建人工客服工单。</div>
      </section>

      <section class="info-card">
        <div class="card-heading">
          <h3>当前概览</h3>
          <span>实时读取</span>
        </div>
        <div class="summary-grid">
          <div><strong>{{ recentOrders.length }}</strong><span>订单</span></div>
          <div><strong>{{ availableCouponCount }}</strong><span>可用券</span></div>
          <div><strong>{{ afterSales.length }}</strong><span>售后</span></div>
        </div>
      </section>
    </aside>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { chatWithAi, createTicket, getAfterSales, getCoupons, getMyTickets, getOrderList, getUserProfile } from '../api'
import UserAvatar from '../components/UserAvatar.vue'

const router = useRouter()
const inputText = ref('')
const sending = ref(false)
const userProfile = ref(null)
const recentOrders = ref([])
const coupons = ref([])
const afterSales = ref([])
const tickets = ref([])
const ticketSubmitting = ref(false)
const messagesEndRef = ref(null)
const messageListRef = ref(null)

const topics = [
  { key: 'order', icon: '📦', title: '订单查询', desc: '订单状态、金额明细', prompt: () => recentOrder.value ? `帮我查看订单 ${recentOrder.value.orderNo}` : '我想查询我的订单' },
  { key: 'logistics', icon: '🚚', title: '物流进度', desc: '发货和配送进度', prompt: () => recentOrder.value ? `帮我查询订单 ${recentOrder.value.orderNo} 的物流进度` : '怎么查询物流进度？' },
  { key: 'coupon', icon: '🎟️', title: '优惠券', desc: '可用券和使用规则', prompt: '我的优惠券怎么使用？' },
  { key: 'afterSale', icon: '🔁', title: '售后退换', desc: '退款、退货、换货', prompt: () => recentOrder.value ? `我想申请订单 ${recentOrder.value.orderNo} 的售后` : '如何申请售后？' },
  { key: 'invoice', icon: '🧾', title: '发票信息', desc: '抬头、税号维护', prompt: '发票信息在哪里维护？' },
  { key: 'account', icon: '🛡️', title: '账号安全', desc: '密码、邮箱、登录提醒', prompt: '帮我看看账号安全怎么设置？' }
]

const quickReplies = [
  '这款商品有货吗？',
  '什么时候能发货？',
  '优惠券怎么使用？',
  '如何申请售后？',
  '发票信息在哪里维护？'
]

const selfActions = [
  { text: '我的订单', icon: '📦', route: '/order' },
  { text: '优惠券', icon: '🎟️', route: '/profile?tab=coupons' },
  { text: '售后进度', icon: '🔁', route: '/profile?tab=afterSales' },
  { text: '发票信息', icon: '🧾', route: '/profile?tab=invoices' },
  { text: '消息中心', icon: '🔔', route: '/profile?tab=messages' },
  { text: '人工协助', icon: '👩‍💼', ticket: true, prompt: '我需要人工客服协助处理问题' }
]

const messages = ref([
  {
    id: Date.now(),
    role: 'assistant',
    content: '你好，我是拾光 AI 客服小光。你可以直接问我订单、物流、优惠券、售后、发票和商品推荐问题；如果发送订单号，我会优先基于你当前账号的真实订单数据回答。',
    time: formatClock(new Date())
  }
])

const displayName = computed(() => userProfile.value?.nickname || userProfile.value?.username || '你')
const todayText = computed(() => formatClock(new Date()))
const recentOrder = computed(() => recentOrders.value[0] || null)
const previewOrderItems = computed(() => (recentOrder.value?.items || []).slice(0, 2))
const availableCouponCount = computed(() => coupons.value.filter(coupon => Number(coupon.status) === 0).length)

async function loadContext() {
  try {
    const profileRes = await getUserProfile()
    userProfile.value = profileRes.data || null
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
    const ticketRes = await getMyTickets()
    tickets.value = Array.isArray(ticketRes.data) ? ticketRes.data : []
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

function resolvePrompt(prompt) {
  return typeof prompt === 'function' ? prompt() : prompt
}

function sendQuick(prompt) {
  inputText.value = resolvePrompt(prompt)
  sendMessage()
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
    appendMessage('assistant', '暂时无法连接 AI 服务。我先给你兜底说明：订单和物流可以在“我的订单”查看；优惠券、售后、发票可以在个人中心对应栏目处理。', { fallback: true })
  } finally {
    sending.value = false
  }
}

function handleSelfAction(action) {
  if (action.route) {
    router.push(action.route)
    return
  }
  if (action.ticket) {
    createHumanTicket(action.prompt)
    return
  }
  sendQuick(action.prompt)
}

async function createHumanTicket(prompt) {
  if (ticketSubmitting.value) return
  const lastQuestion = [...messages.value].reverse().find(item => item.role === 'user')?.content
  const content = resolvePrompt(prompt) || lastQuestion || '我需要人工客服协助处理问题'
  ticketSubmitting.value = true
  try {
    const res = await createTicket({
      source: 'ai',
      type: 'consult',
      title: content.length > 28 ? `${content.slice(0, 28)}...` : content,
      content,
      orderNo: recentOrder.value?.orderNo || ''
    })
    const ticket = res.data || {}
    appendMessage('assistant', `已为你创建人工客服工单 ${ticket.ticketNo || ''}。客服会在后台工作台处理，你也可以在右侧查看工单进度。`)
    ElMessage.success('人工客服工单已创建')
    const ticketRes = await getMyTickets()
    tickets.value = Array.isArray(ticketRes.data) ? ticketRes.data : []
  } catch (error) {
    console.error(error)
    ElMessage.error('工单创建失败，请稍后再试')
  } finally {
    ticketSubmitting.value = false
  }
}

function resetConversation() {
  messages.value = [{
    id: Date.now(),
    role: 'assistant',
    content: '会话已清空。我是拾光 AI 客服小光，可以继续帮你查询订单、物流、优惠券、售后、发票和商品推荐。',
    time: formatClock(new Date())
  }]
  scrollToBottom()
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

function scrollToBottom() {
  nextTick(() => {
    messagesEndRef.value?.scrollIntoView({ behavior: 'smooth', block: 'end' })
  })
}

function formatClock(date) {
  return new Intl.DateTimeFormat('zh-CN', { hour: '2-digit', minute: '2-digit', hour12: false }).format(date)
}

function formatDate(value) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  return new Intl.DateTimeFormat('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' }).format(date)
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

function orderStatusText(status) {
  return {
    0: '待付款',
    1: '待发货',
    2: '待收货',
    3: '已完成',
    4: '已取消'
  }[status] || '未知'
}

function ticketStatusText(status) {
  return {
    0: '待处理',
    1: '处理中',
    2: '已解决',
    3: '已关闭'
  }[Number(status)] || '待处理'
}

onMounted(() => {
  loadContext()
  scrollToBottom()
})
</script>

<style scoped>
.service-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 260px minmax(520px, 1fr) 340px;
  background: #f5f7fb;
  color: #172033;
}

.service-sidebar {
  padding: 22px 18px;
  background: linear-gradient(180deg, #071b3a 0%, #02132b 100%);
  color: #fff;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.brand {
  border: none;
  background: transparent;
  display: flex;
  align-items: center;
  gap: 12px;
  color: #fff;
  text-align: left;
  cursor: pointer;
}

.brand img {
  width: 46px;
  height: 46px;
  border-radius: 15px;
  background: #fff;
  object-fit: cover;
}

.brand strong,
.brand small {
  display: block;
}

.brand strong {
  font-size: 20px;
}

.brand small,
.service-state small,
.topic-list small {
  color: rgba(255, 255, 255, 0.68);
}

.service-state {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  padding: 14px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.06);
}

.online-dot {
  width: 10px;
  height: 10px;
  margin-top: 5px;
  border-radius: 50%;
  background: #34d399;
  box-shadow: 0 0 0 5px rgba(52, 211, 153, 0.16);
}

.topic-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.topic-list button {
  border: none;
  border-radius: 13px;
  padding: 13px 12px;
  background: transparent;
  color: rgba(255, 255, 255, 0.9);
  display: flex;
  gap: 11px;
  text-align: left;
  cursor: pointer;
  transition: all 0.18s ease;
}

.topic-list button:hover {
  background: linear-gradient(135deg, #2d8cff, #1664ff);
}

.topic-list button > span {
  width: 24px;
  text-align: center;
}

.topic-list strong {
  display: block;
  margin-bottom: 3px;
}

.back-home {
  margin-top: auto;
  height: 38px;
  border: 1px solid rgba(255, 255, 255, 0.22);
  background: rgba(255, 255, 255, 0.06);
  color: #fff;
  border-radius: 10px;
  cursor: pointer;
}

.chat-shell {
  min-width: 0;
  background: #fff;
  display: grid;
  grid-template-rows: auto 1fr auto;
  border-right: 1px solid #e6ebf3;
}

.chat-header {
  min-height: 86px;
  padding: 18px 24px;
  border-bottom: 1px solid #e6ebf3;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 18px;
}

.assistant-info {
  display: flex;
  align-items: center;
  gap: 13px;
}

.assistant-info h1 {
  font-size: 21px;
  margin-bottom: 5px;
}

.assistant-info p {
  color: #728098;
  font-size: 13px;
}

.chat-header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.chat-header-actions button {
  height: 34px;
  padding: 0 12px;
  border: 1px solid #dce4ef;
  border-radius: 8px;
  background: #fff;
  color: #53627a;
  cursor: pointer;
}

.model-tag {
  padding: 6px 10px;
  border-radius: 999px;
  background: #eaf2ff;
  color: #1664ff;
  font-weight: 700;
  font-size: 12px;
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
  max-width: min(680px, 74%);
}

.bubble {
  position: relative;
  padding: 13px 16px;
  border-radius: 12px;
  background: #f3f5f8;
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

.loading-bubble span:nth-child(2) { animation-delay: 0.15s; }
.loading-bubble span:nth-child(3) { animation-delay: 0.3s; }

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
.order-actions button:hover,
.chat-header-actions button:hover {
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

.self-panel {
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

.profile-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.profile-row strong {
  display: block;
  margin-bottom: 4px;
  font-size: 17px;
}

.profile-row span {
  color: #ff3154;
  font-size: 12px;
}

.info-lines {
  display: flex;
  flex-direction: column;
  gap: 9px;
  color: #53627a;
  font-size: 13px;
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
  min-height: 42px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.summary-grid div {
  padding: 14px 8px;
  border-radius: 10px;
  background: #f3f7ff;
  text-align: center;
}

.summary-grid strong {
  display: block;
  color: #1664ff;
  font-size: 22px;
}

.summary-grid span {
  color: #64748b;
  font-size: 12px;
}

.empty-mini {
  padding: 24px 12px;
  text-align: center;
  color: #8a97ad;
  background: #f8fafd;
  border-radius: 10px;
}

.ticket-list {
  display: grid;
  gap: 9px;
}

.ticket-item {
  padding: 10px;
  border: 1px solid #edf1f6;
  border-radius: 10px;
  background: #fbfdff;
}

.ticket-item strong,
.ticket-item span {
  display: block;
}

.ticket-item strong {
  color: #1664ff;
  font-size: 12px;
}

.ticket-item span {
  margin: 4px 0;
  color: #53627a;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ticket-item em {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 999px;
  background: #edf5ff;
  color: #1664ff;
  font-style: normal;
  font-size: 12px;
}

@media (max-width: 1180px) {
  .service-page {
    grid-template-columns: 210px minmax(460px, 1fr);
  }

  .self-panel {
    display: none;
  }
}

@media (max-width: 820px) {
  .service-page {
    display: flex;
    flex-direction: column;
  }

  .service-sidebar {
    display: none;
  }

  .chat-shell {
    min-height: 100vh;
  }

  .chat-header {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
