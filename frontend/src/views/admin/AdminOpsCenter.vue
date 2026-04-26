<template>
  <div class="ops-page">
    <section class="ops-hero">
      <div>
        <p>运营增强中心</p>
        <h1>知识库 / 工单 / 存储 / 活动 / 风控</h1>
        <span>把 AI 客服、人工客服、营销统计和风险检测集中到一个后台页面，方便当前阶段直接使用。</span>
      </div>
      <div class="hero-stats">
        <div><strong>{{ ticketRows.length }}</strong><span>当前工单</span></div>
        <div><strong>{{ riskOverview.open || 0 }}</strong><span>待处理风险</span></div>
        <div><strong>{{ campaignOverview.conversionRate || 0 }}%</strong><span>活动转化</span></div>
      </div>
    </section>

    <el-tabs v-model="activeTab" class="ops-tabs" @tab-change="handleTabChange">
      <el-tab-pane label="AI 知识库" name="knowledge">
        <div class="toolbar">
          <el-input v-model="knowledgeQuery.keyword" placeholder="搜索标题/问题/关键词" clearable @keyup.enter="loadKnowledge" />
          <el-select v-model="knowledgeQuery.status" placeholder="状态" clearable>
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
          <el-button @click="loadKnowledge">查询</el-button>
          <el-button type="primary" @click="openKnowledgeDialog()">新增知识</el-button>
        </div>
        <el-table :data="knowledgeRows" border stripe>
          <el-table-column prop="category" label="分类" width="110" />
          <el-table-column prop="title" label="标题" width="180" />
          <el-table-column prop="question" label="问题" min-width="180" />
          <el-table-column prop="keywords" label="关键词" min-width="160" />
          <el-table-column label="状态" width="90">
            <template #default="{ row }"><el-tag :type="Number(row.status) === 1 ? 'success' : 'info'">{{ Number(row.status) === 1 ? '启用' : '停用' }}</el-tag></template>
          </el-table-column>
          <el-table-column label="操作" width="170" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openKnowledgeDialog(row)">编辑</el-button>
              <el-button link type="danger" @click="removeKnowledge(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="人工工单" name="tickets">
        <div class="toolbar">
          <el-input v-model="ticketQuery.keyword" placeholder="工单号/订单号/问题" clearable @keyup.enter="loadTickets" />
          <el-select v-model="ticketQuery.status" placeholder="状态" clearable>
            <el-option label="待处理" :value="0" />
            <el-option label="处理中" :value="1" />
            <el-option label="已解决" :value="2" />
            <el-option label="已关闭" :value="3" />
          </el-select>
          <el-button @click="loadTickets">查询</el-button>
        </div>
        <el-table :data="ticketRows" border stripe>
          <el-table-column prop="ticketNo" label="工单号" width="170" />
          <el-table-column prop="nickname" label="用户" width="120" />
          <el-table-column prop="title" label="标题" min-width="180" />
          <el-table-column prop="orderNo" label="订单号" min-width="150" />
          <el-table-column label="状态" width="95">
            <template #default="{ row }"><el-tag :type="ticketTagType(row.status)">{{ row.statusText || ticketStatusText(row.status) }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="satisfaction" label="满意度" width="90" />
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="assignTicket(row)">接单</el-button>
              <el-button link type="success" @click="openTicketDialog(row)">回复/解决</el-button>
              <el-button link @click="closeTicket(row)">关闭</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="活动数据统计" name="campaigns">
        <div class="stat-row">
          <div><span>领取</span><strong>{{ campaignOverview.issueCount || 0 }}</strong></div>
          <div><span>使用</span><strong>{{ campaignOverview.useCount || 0 }}</strong></div>
          <div><span>下单</span><strong>{{ campaignOverview.orderCount || 0 }}</strong></div>
          <div><span>转化率</span><strong>{{ campaignOverview.conversionRate || 0 }}%</strong></div>
        </div>
        <div class="toolbar">
          <el-select v-model="campaignQuery.campaignType" placeholder="活动类型" clearable>
            <el-option label="优惠券" value="coupon" />
            <el-option label="满减" value="full_reduction" />
            <el-option label="限时活动" value="limited_promotion" />
          </el-select>
          <el-button @click="loadCampaignStats">刷新统计</el-button>
        </div>
        <el-table :data="campaignRows" border stripe>
          <el-table-column prop="campaignType" label="类型" width="140" />
          <el-table-column prop="campaignName" label="活动名称" min-width="180" />
          <el-table-column prop="issueCount" label="领取" width="90" />
          <el-table-column prop="useCount" label="使用" width="90" />
          <el-table-column prop="orderCount" label="下单" width="90" />
          <el-table-column prop="amount" label="金额" width="110" />
          <el-table-column label="转化率" width="110"><template #default="{ row }">{{ row.conversionRate || 0 }}%</template></el-table-column>
          <el-table-column prop="lastTime" label="最近事件" width="180" />
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="风控检测" name="risk">
        <div class="stat-row risk">
          <div><span>待处理</span><strong>{{ riskOverview.open || 0 }}</strong></div>
          <div><span>高风险</span><strong>{{ riskOverview.high || 0 }}</strong></div>
          <div><span>今日事件</span><strong>{{ riskOverview.today || 0 }}</strong></div>
        </div>
        <div class="toolbar">
          <el-input v-model="riskQuery.keyword" placeholder="用户/IP/路径/说明" clearable @keyup.enter="loadRiskEvents" />
          <el-select v-model="riskQuery.riskLevel" placeholder="风险等级" clearable>
            <el-option label="高" value="high" />
            <el-option label="中" value="medium" />
            <el-option label="低" value="low" />
          </el-select>
          <el-select v-model="riskQuery.status" placeholder="处理状态" clearable>
            <el-option label="待处理" :value="0" />
            <el-option label="已处理" :value="1" />
            <el-option label="已忽略" :value="2" />
          </el-select>
          <el-button @click="loadRiskEvents">查询</el-button>
        </div>
        <el-table :data="riskRows" border stripe>
          <el-table-column prop="eventType" label="类型" width="145" />
          <el-table-column prop="riskLevel" label="等级" width="90">
            <template #default="{ row }"><el-tag :type="riskTagType(row.riskLevel)">{{ riskLevelText(row.riskLevel) }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="username" label="用户" width="120" />
          <el-table-column prop="ip" label="IP" width="140" />
          <el-table-column prop="description" label="说明" min-width="220" />
          <el-table-column prop="path" label="路径" min-width="160" />
          <el-table-column prop="createTime" label="时间" width="180" />
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="updateRisk(row, 1)">标记处理</el-button>
              <el-button link @click="updateRisk(row, 2)">忽略</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="对象存储" name="media">
        <div class="storage-card">
          <strong>当前存储：{{ mediaConfig.activeProvider || mediaConfig.provider || 'local' }}</strong>
          <span>基础版已统一记录 provider、bucket、objectKey、url；当前本地可用，后续接 R2/OSS/COS/S3 时可沿用同一资源表。</span>
        </div>
        <div class="toolbar">
          <el-input v-model="mediaQuery.keyword" placeholder="文件名/objectKey/url" clearable @keyup.enter="loadMediaAssets" />
          <el-button @click="loadMediaAssets">查询</el-button>
        </div>
        <el-table :data="mediaRows" border stripe>
          <el-table-column label="预览" width="90">
            <template #default="{ row }"><img v-if="isImage(row.contentType)" class="media-thumb" :src="row.url" alt="" /><span v-else>文件</span></template>
          </el-table-column>
          <el-table-column prop="originalName" label="原文件名" min-width="170" />
          <el-table-column prop="bizType" label="业务" width="110" />
          <el-table-column prop="provider" label="存储" width="90" />
          <el-table-column prop="objectKey" label="对象 Key" min-width="260" />
          <el-table-column label="大小" width="100"><template #default="{ row }">{{ formatSize(row.size) }}</template></el-table-column>
          <el-table-column prop="createTime" label="上传时间" width="180" />
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="knowledgeDialogVisible" :title="knowledgeForm.id ? '编辑知识库' : '新增知识库'" width="720px">
      <el-form label-width="90px">
        <el-form-item label="分类"><el-input v-model="knowledgeForm.category" placeholder="售后政策/优惠券规则" /></el-form-item>
        <el-form-item label="标题"><el-input v-model="knowledgeForm.title" /></el-form-item>
        <el-form-item label="问题"><el-input v-model="knowledgeForm.question" /></el-form-item>
        <el-form-item label="关键词"><el-input v-model="knowledgeForm.keywords" placeholder="多个关键词用逗号分隔" /></el-form-item>
        <el-form-item label="答案"><el-input v-model="knowledgeForm.answer" type="textarea" :rows="6" /></el-form-item>
        <el-form-item label="排序/状态">
          <el-input-number v-model="knowledgeForm.sortOrder" :min="0" />
          <el-switch v-model="knowledgeForm.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" style="margin-left: 18px" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="knowledgeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveKnowledge">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="ticketDialogVisible" title="回复/解决工单" width="680px">
      <div v-if="activeTicket" class="ticket-detail">
        <p><strong>{{ activeTicket.ticketNo }}</strong> {{ activeTicket.title }}</p>
        <p>{{ activeTicket.content }}</p>
      </div>
      <el-input v-model="ticketResolution" type="textarea" :rows="5" placeholder="填写处理结果，会同步给用户消息中心" />
      <template #footer>
        <el-button @click="ticketDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="replyTicket">保存并解决</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  adminAssignTicket,
  adminCreateKnowledge,
  adminDeleteKnowledge,
  adminGetCampaignStats,
  adminGetKnowledge,
  adminGetMediaAssets,
  adminGetRiskEvents,
  adminGetTickets,
  adminReplyTicket,
  adminUpdateKnowledge,
  adminUpdateRiskStatus,
  adminUpdateTicketStatus
} from '../../api'

const activeTab = ref('knowledge')
const knowledgeRows = ref([])
const ticketRows = ref([])
const campaignRows = ref([])
const riskRows = ref([])
const mediaRows = ref([])
const campaignOverview = ref({})
const riskOverview = ref({})
const mediaConfig = ref({})
const knowledgeDialogVisible = ref(false)
const ticketDialogVisible = ref(false)
const activeTicket = ref(null)
const ticketResolution = ref('')

const knowledgeQuery = reactive({ page: 1, size: 50, keyword: '', status: undefined })
const ticketQuery = reactive({ page: 1, size: 50, keyword: '', status: undefined })
const campaignQuery = reactive({ campaignType: '' })
const riskQuery = reactive({ page: 1, size: 50, keyword: '', riskLevel: '', status: 0 })
const mediaQuery = reactive({ page: 1, size: 50, keyword: '' })
const knowledgeForm = reactive({ id: null, category: '', title: '', question: '', answer: '', keywords: '', status: 1, sortOrder: 100 })

function pageRecords(res) {
  return Array.isArray(res.data?.records) ? res.data.records : []
}

async function loadKnowledge() {
  const res = await adminGetKnowledge(knowledgeQuery)
  knowledgeRows.value = pageRecords(res)
}

function openKnowledgeDialog(row) {
  Object.assign(knowledgeForm, row ? { ...row } : { id: null, category: '', title: '', question: '', answer: '', keywords: '', status: 1, sortOrder: 100 })
  knowledgeDialogVisible.value = true
}

async function saveKnowledge() {
  if (!knowledgeForm.category || !knowledgeForm.title || !knowledgeForm.answer) {
    ElMessage.warning('请填写分类、标题和答案')
    return
  }
  if (knowledgeForm.id) {
    await adminUpdateKnowledge(knowledgeForm.id, knowledgeForm)
  } else {
    await adminCreateKnowledge(knowledgeForm)
  }
  ElMessage.success('知识库已保存')
  knowledgeDialogVisible.value = false
  loadKnowledge()
}

async function removeKnowledge(row) {
  await ElMessageBox.confirm(`确认删除「${row.title}」？`, '删除知识库', { type: 'warning' })
  await adminDeleteKnowledge(row.id)
  ElMessage.success('已删除')
  loadKnowledge()
}

async function loadTickets() {
  const res = await adminGetTickets(ticketQuery)
  ticketRows.value = pageRecords(res)
}

async function assignTicket(row) {
  await adminAssignTicket(row.id)
  ElMessage.success('已接单')
  loadTickets()
}

function openTicketDialog(row) {
  activeTicket.value = row
  ticketResolution.value = row.resolution || ''
  ticketDialogVisible.value = true
}

async function replyTicket() {
  if (!activeTicket.value || !ticketResolution.value.trim()) {
    ElMessage.warning('请填写处理结果')
    return
  }
  await adminReplyTicket(activeTicket.value.id, { resolution: ticketResolution.value, status: 2 })
  ElMessage.success('工单已解决')
  ticketDialogVisible.value = false
  loadTickets()
}

async function closeTicket(row) {
  await adminUpdateTicketStatus(row.id, { status: 3, resolution: row.resolution || '工单已关闭' })
  ElMessage.success('工单已关闭')
  loadTickets()
}

async function loadCampaignStats() {
  const params = {}
  if (campaignQuery.campaignType) params.campaignType = campaignQuery.campaignType
  const res = await adminGetCampaignStats(params)
  campaignRows.value = pageRecords(res)
  campaignOverview.value = res.data?.overview || {}
}

async function loadRiskEvents() {
  const params = { ...riskQuery }
  if (params.status === '') delete params.status
  const res = await adminGetRiskEvents(params)
  riskRows.value = pageRecords(res)
  riskOverview.value = res.data?.overview || {}
}

async function updateRisk(row, status) {
  await adminUpdateRiskStatus(row.id, status)
  ElMessage.success('风控状态已更新')
  loadRiskEvents()
}

async function loadMediaAssets() {
  const res = await adminGetMediaAssets(mediaQuery)
  mediaRows.value = pageRecords(res)
  mediaConfig.value = res.data?.config || {}
}

function handleTabChange(name) {
  if (name === 'knowledge') loadKnowledge()
  if (name === 'tickets') loadTickets()
  if (name === 'campaigns') loadCampaignStats()
  if (name === 'risk') loadRiskEvents()
  if (name === 'media') loadMediaAssets()
}

function ticketStatusText(status) {
  return { 0: '待处理', 1: '处理中', 2: '已解决', 3: '已关闭' }[Number(status)] || '待处理'
}

function ticketTagType(status) {
  return { 0: 'warning', 1: 'primary', 2: 'success', 3: 'info' }[Number(status)] || 'warning'
}

function riskLevelText(level) {
  return { high: '高', medium: '中', low: '低' }[level] || level || '中'
}

function riskTagType(level) {
  return { high: 'danger', medium: 'warning', low: 'info' }[level] || 'warning'
}

function isImage(contentType) {
  return String(contentType || '').startsWith('image/')
}

function formatSize(size) {
  const value = Number(size || 0)
  if (value > 1024 * 1024) return `${(value / 1024 / 1024).toFixed(1)} MB`
  if (value > 1024) return `${(value / 1024).toFixed(1)} KB`
  return `${value} B`
}

onMounted(() => {
  loadKnowledge()
  loadTickets()
  loadCampaignStats()
  loadRiskEvents()
})
</script>

<style scoped>
.ops-page { display: grid; gap: 16px; }
.ops-hero { display: flex; justify-content: space-between; gap: 20px; padding: 22px; border-radius: 16px; background: linear-gradient(135deg, #0f172a, #1d4ed8); color: #fff; }
.ops-hero p { opacity: 0.75; margin-bottom: 6px; }
.ops-hero h1 { font-size: 24px; margin-bottom: 8px; }
.ops-hero span { color: rgba(255,255,255,0.75); }
.hero-stats { display: grid; grid-template-columns: repeat(3, 110px); gap: 12px; align-items: stretch; }
.hero-stats div, .stat-row div { border-radius: 14px; background: rgba(255,255,255,0.12); padding: 14px; text-align: center; }
.hero-stats strong, .stat-row strong { display: block; font-size: 24px; }
.hero-stats span, .stat-row span { font-size: 12px; color: rgba(255,255,255,0.72); }
.ops-tabs { padding: 18px; border: 1px solid #e5e7eb; border-radius: 16px; background: #fff; }
.toolbar { display: flex; gap: 10px; align-items: center; margin-bottom: 14px; }
.toolbar .el-input { max-width: 320px; }
.stat-row { display: grid; grid-template-columns: repeat(4, minmax(130px, 1fr)); gap: 12px; margin-bottom: 14px; }
.stat-row div { background: linear-gradient(135deg, #eef6ff, #f8fbff); color: #1e293b; border: 1px solid #dbeafe; }
.stat-row span { color: #64748b; }
.stat-row.risk { grid-template-columns: repeat(3, minmax(130px, 1fr)); }
.storage-card { display: flex; flex-direction: column; gap: 6px; padding: 14px; border: 1px solid #dbeafe; border-radius: 12px; background: #f8fbff; color: #334155; margin-bottom: 14px; }
.media-thumb { width: 54px; height: 54px; object-fit: cover; border-radius: 8px; background: #f1f5f9; }
.ticket-detail { padding: 12px; border-radius: 10px; background: #f8fafc; margin-bottom: 12px; color: #334155; }
.ticket-detail p + p { margin-top: 8px; line-height: 1.7; }
@media (max-width: 980px) { .ops-hero { flex-direction: column; } .hero-stats, .stat-row { grid-template-columns: 1fr; } .toolbar { flex-wrap: wrap; } }
</style>
