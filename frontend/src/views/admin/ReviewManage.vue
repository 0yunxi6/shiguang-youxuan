<template>
  <div class="manage-page review-manage">
    <div class="page-title-bar">
      <div>
        <h1>评价管理</h1>
        <p class="subtitle">查看买家反馈，及时隐藏不合适内容</p>
      </div>
      <button class="btn-refresh" @click="loadData">刷新</button>
    </div>

    <div class="stat-row">
      <div class="stat-item">
        <span>全部评价</span>
        <strong>{{ stats.total || 0 }}</strong>
      </div>
      <div class="stat-item">
        <span>展示中</span>
        <strong>{{ stats.visible || 0 }}</strong>
      </div>
      <div class="stat-item">
        <span>已隐藏</span>
        <strong>{{ stats.hidden || 0 }}</strong>
      </div>
    </div>

    <div class="toolbar">
      <input v-model.trim="filters.keyword" placeholder="搜索商品、用户或评价内容" @keyup.enter="search" />
      <select v-model="filters.status" @change="search">
        <option value="">全部状态</option>
        <option value="1">展示中</option>
        <option value="0">已隐藏</option>
      </select>
      <button class="btn-primary" @click="search">查询</button>
      <button class="btn-ghost" @click="resetFilters">重置</button>
    </div>

    <div class="table-card">
      <div class="review-row header">
        <span>商品</span>
        <span>用户</span>
        <span>评分</span>
        <span>评价内容</span>
        <span>状态</span>
        <span>时间</span>
        <span>操作</span>
      </div>

      <div v-if="loading" class="empty-line">加载中...</div>
      <div v-else-if="reviews.length === 0" class="empty-line">暂无评价</div>

      <div v-for="item in reviews" :key="item.id" class="review-row">
        <div class="product-cell">
          <img :src="item.productImage || '/placeholder.svg'" alt="" />
          <span>{{ item.productName || '商品已删除' }}</span>
        </div>
        <div class="user-cell">
          <strong>{{ item.nickname || item.username || '用户' }}</strong>
          <span>ID: {{ item.userId }}</span>
        </div>
        <div class="stars">{{ renderStars(item.rating) }}</div>
        <div class="content-cell">
          <p>{{ item.content }}</p>
          <p v-if="item.appendContent" class="append-line">追评：{{ item.appendContent }}</p>
          <p v-if="item.adminReply" class="reply-line">回复：{{ item.adminReply }}</p>
          <div class="review-thumbs" v-if="parseReviewImages(item.images).length">
            <img v-for="(img, idx) in parseReviewImages(item.images)" :key="img + idx" :src="img" />
          </div>
        </div>
        <div>
          <span class="status-pill" :class="{ hidden: item.status === 0 }">
            {{ item.status === 1 ? '展示中' : '已隐藏' }}
          </span>
        </div>
        <div class="time-cell">{{ formatTime(item.createTime) }}</div>
        <div class="actions">
          <button v-if="item.status === 1" @click="changeStatus(item, 0)">隐藏</button>
          <button v-else @click="changeStatus(item, 1)">恢复</button>
          <button @click="replyReview(item)">回复</button>
          <button class="danger" @click="removeReview(item)">删除</button>
        </div>
      </div>
    </div>

    <div class="pagination" v-if="total > pageSize">
      <button :disabled="page <= 1" @click="changePage(page - 1)">上一页</button>
      <span>{{ page }} / {{ totalPages }}</span>
      <button :disabled="page >= totalPages" @click="changePage(page + 1)">下一页</button>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  adminDeleteReview,
  adminGetReviews,
  adminGetReviewStats,
  adminReplyReview,
  adminUpdateReviewStatus
} from '../../api'

const loading = ref(false)
const reviews = ref([])
const stats = ref({})
const page = ref(1)
const pageSize = 10
const total = ref(0)
const filters = reactive({
  keyword: '',
  status: ''
})

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))

const renderStars = (rating = 0) => {
  const value = Math.max(0, Math.min(5, Number(rating) || 0))
  return '★'.repeat(value) + '☆'.repeat(5 - value)
}

const parseReviewImages = (value) => {
  if (!value) return []
  return String(value)
    .split(/[\n\r,，]+/)
    .map(item => item.trim())
    .filter(Boolean)
    .slice(0, 6)
}

const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

const loadStats = async () => {
  try {
    const res = await adminGetReviewStats({ silentError: true })
    stats.value = res.data || {}
  } catch (error) {
    stats.value = {}
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      page: page.value,
      size: pageSize
    }
    if (filters.keyword) params.keyword = filters.keyword
    if (filters.status !== '') params.status = Number(filters.status)
    const res = await adminGetReviews(params, { silentError: true })
    reviews.value = res.data?.records || []
    total.value = res.data?.total || 0
    await loadStats()
  } catch (error) {
    reviews.value = []
    total.value = 0
    if (error.response?.status === 404) {
      ElMessage.warning('评价管理接口尚未生效，请重启后端服务')
    }
  } finally {
    loading.value = false
  }
}

const search = () => {
  page.value = 1
  loadData()
}

const resetFilters = () => {
  filters.keyword = ''
  filters.status = ''
  search()
}

const changePage = (nextPage) => {
  page.value = nextPage
  loadData()
}

const changeStatus = async (item, status) => {
  await adminUpdateReviewStatus(item.id, status)
  ElMessage.success(status === 1 ? '评价已恢复展示' : '评价已隐藏')
  loadData()
}

const removeReview = async (item) => {
  await ElMessageBox.confirm('确定删除这条评价吗？删除后不可恢复。', '删除确认', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await adminDeleteReview(item.id)
  ElMessage.success('评价已删除')
  loadData()
}

const replyReview = async (item) => {
  const { value } = await ElMessageBox.prompt('请输入商家回复内容', '回复评价', {
    confirmButtonText: '保存回复',
    cancelButtonText: '取消',
    inputType: 'textarea',
    inputValue: item.adminReply || '',
    inputValidator: (val) => String(val || '').trim().length >= 2 || '回复至少2个字'
  })
  await adminReplyReview(item.id, { reply: value })
  ElMessage.success('回复已保存')
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.review-manage { animation: fadeIn 0.2s ease; }
.page-title-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
}
.page-title-bar h1 { margin: 0; font-size: 24px; color: #111; }
.subtitle { margin: 6px 0 0; color: #888; font-size: 13px; }
.btn-refresh,
.btn-primary,
.btn-ghost {
  border: none;
  border-radius: 8px;
  padding: 9px 16px;
  cursor: pointer;
  font-weight: 600;
}
.btn-refresh,
.btn-primary { background: #1a1a1a; color: #fff; }
.btn-ghost { background: #f3f2ef; color: #555; }
.stat-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 16px;
}
.stat-item {
  background: #fff;
  border: 1px solid #eee;
  border-radius: 12px;
  padding: 18px;
}
.stat-item span { color: #888; font-size: 13px; }
.stat-item strong { display: block; margin-top: 8px; font-size: 28px; color: #111; }
.toolbar {
  display: flex;
  gap: 10px;
  background: #fff;
  border: 1px solid #eee;
  border-radius: 12px;
  padding: 14px;
  margin-bottom: 16px;
}
.toolbar input,
.toolbar select {
  height: 38px;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  padding: 0 12px;
  outline: none;
}
.toolbar input { flex: 1; }
.table-card {
  background: #fff;
  border: 1px solid #eee;
  border-radius: 12px;
  overflow: hidden;
}
.review-row {
  display: grid;
  grid-template-columns: 1.4fr 0.9fr 0.7fr 2fr 0.7fr 1fr 1fr;
  gap: 12px;
  align-items: center;
  padding: 14px 16px;
  border-bottom: 1px solid #f2f2f0;
  font-size: 13px;
}
.review-row.header {
  background: #fafaf8;
  color: #888;
  font-weight: 700;
}
.review-row:last-child { border-bottom: none; }
.product-cell {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}
.product-cell img {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  object-fit: cover;
  background: #f5f5f5;
}
.product-cell span,
.content-cell {
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
}
.content-cell {
  display: block;
}
.content-cell p {
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
}

.content-cell .append-line {
  margin-top: 6px;
  color: #c45c3e;
}

.content-cell .reply-line {
  margin-top: 6px;
  color: #5a6e5a;
}
.review-thumbs {
  display: flex;
  gap: 6px;
  margin-top: 8px;
  flex-wrap: wrap;
}
.review-thumbs img {
  width: 42px;
  height: 42px;
  border-radius: 6px;
  object-fit: cover;
  background: #f5f5f5;
  border: 1px solid #eee;
}
.user-cell strong { display: block; color: #333; }
.user-cell span { color: #aaa; font-size: 12px; }
.stars { color: #e6a23c; letter-spacing: 1px; white-space: nowrap; }
.status-pill {
  display: inline-flex;
  padding: 4px 9px;
  border-radius: 999px;
  background: #e8f8ee;
  color: #67c23a;
  font-weight: 700;
}
.status-pill.hidden {
  background: #f4f4f4;
  color: #999;
}
.time-cell { color: #888; }
.actions {
  display: flex;
  gap: 8px;
}
.actions button {
  border: none;
  background: #f3f2ef;
  color: #444;
  border-radius: 7px;
  padding: 7px 10px;
  cursor: pointer;
}
.actions .danger {
  background: #fff1f0;
  color: #e74c3c;
}
.empty-line {
  text-align: center;
  color: #999;
  padding: 38px 0;
}
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 14px;
  margin-top: 16px;
}
.pagination button {
  border: 1px solid #e5e5e5;
  background: #fff;
  border-radius: 8px;
  padding: 8px 14px;
  cursor: pointer;
}
.pagination button:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
@media (max-width: 1100px) {
  .review-row { grid-template-columns: 1fr; }
  .review-row.header { display: none; }
  .toolbar { flex-wrap: wrap; }
  .stat-row { grid-template-columns: 1fr; }
}
</style>
