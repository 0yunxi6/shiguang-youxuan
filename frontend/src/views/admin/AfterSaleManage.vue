<template>
  <div class="manage-page">
    <div class="page-header">
      <h2>售后管理</h2>
      <div class="header-stats" v-if="total > 0"><span class="stat-badge">共 {{ total }} 条申请</span></div>
    </div>

    <div class="filter-bar">
      <input v-model="keyword" class="filter-input" placeholder="搜索订单号/原因" @keyup.enter="loadList" />
      <select v-model="statusFilter" class="filter-select" @change="loadList">
        <option :value="null">全部状态</option>
        <option :value="0">待审核</option>
        <option :value="1">已同意</option>
        <option :value="2">已拒绝</option>
        <option :value="3">已完成</option>
      </select>
      <button class="btn-search" @click="loadList">搜索</button>
    </div>

    <div class="table-wrap">
      <table class="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>订单号</th>
            <th>类型</th>
            <th>金额</th>
            <th>原因</th>
            <th>状态</th>
            <th>审核备注</th>
            <th>申请时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in list" :key="row.id">
            <td>{{ row.id }}</td>
            <td class="mono">{{ row.orderNo }}</td>
            <td>{{ typeText(row.type) }}</td>
            <td class="price">¥{{ Number(row.amount || 0).toFixed(2) }}</td>
            <td class="reason">{{ row.reason }}</td>
            <td><span class="tag" :class="'s' + row.status">{{ statusText[row.status] }}</span></td>
            <td>{{ row.auditRemark || '-' }}</td>
            <td class="time">{{ formatTime(row.createTime) }}</td>
            <td class="actions">
              <button class="btn-sm ok" @click="audit(row, 1)" :disabled="row.status !== 0">同意</button>
              <button class="btn-sm danger" @click="audit(row, 2)" :disabled="row.status !== 0">拒绝</button>
              <button class="btn-sm" @click="audit(row, 3)" :disabled="row.status === 3">完成</button>
            </td>
          </tr>
        </tbody>
      </table>
      <EmptyState v-if="!list.length && !loading" title="暂无售后申请" description="用户提交退款/退货后会显示在这里" />
    </div>

    <div class="pagination-wrap" v-if="total > pageSize">
      <span>共 {{ total }} 条，第 {{ page }} / {{ Math.ceil(total / pageSize) }} 页</span>
      <div>
        <button :disabled="page <= 1" @click="page--; loadList()">上一页</button>
        <button :disabled="page >= Math.ceil(total / pageSize)" @click="page++; loadList()">下一页</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { adminGetAfterSales, adminUpdateAfterSaleStatus } from '../../api'
import { ElMessage } from 'element-plus'
import EmptyState from '../../components/admin/EmptyState.vue'

const list = ref([])
const loading = ref(false)
const keyword = ref('')
const statusFilter = ref(null)
const page = ref(1)
const pageSize = 10
const total = ref(0)
const statusText = ['待审核', '已同意', '已拒绝', '已完成']
const typeText = (type) => ({ refund: '退款', return: '退货退款', exchange: '换货' }[type] || type || '-')
const formatTime = (time) => time ? new Date(time).toLocaleString('zh-CN') : '-'

const loadList = async () => {
  loading.value = true
  try {
    const res = await adminGetAfterSales({ page: page.value, size: pageSize, status: statusFilter.value, keyword: keyword.value })
    list.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

const audit = async (row, status) => {
  const remark = window.prompt('请输入审核备注（可选）', row.auditRemark || '')
  if (remark === null) return
  await adminUpdateAfterSaleStatus(row.id, status, remark)
  ElMessage.success('售后状态已更新')
  loadList()
}

onMounted(loadList)
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h2 { margin: 0; font-size: 20px; color: #111; }
.stat-badge { padding: 6px 14px; background: #f0f0f0; border-radius: 999px; color: #666; font-size: 13px; }
.filter-bar { display: flex; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; }
.filter-input, .filter-select { padding: 10px 14px; border: 1.5px solid #e5e7eb; border-radius: 8px; background: #fff; outline: none; }
.filter-input { min-width: 260px; }
.btn-search, .pagination-wrap button { padding: 9px 16px; border: 1.5px solid #e5e7eb; border-radius: 8px; background: #fff; cursor: pointer; }
.table-wrap { background: #fff; border: 1px solid #ebeef5; border-radius: 8px; overflow-x: auto; }
.data-table { width: 100%; min-width: 1080px; border-collapse: collapse; }
.data-table th, .data-table td { padding: 13px 14px; border-bottom: 1px solid #f0f0f0; text-align: left; font-size: 13px; }
.data-table th { background: #fafaf8; color: #909399; font-weight: 700; }
.mono { font-family: 'SF Mono', Monaco, monospace; color: #666; }
.price { color: #f56c6c; font-weight: 700; }
.reason { max-width: 260px; }
.time { color: #909399; white-space: nowrap; }
.tag { padding: 4px 10px; border-radius: 999px; font-size: 12px; }
.tag.s0 { background: #fef3e6; color: #e6a23c; }
.tag.s1 { background: #ecf5ff; color: #409eff; }
.tag.s2 { background: #fde8e8; color: #f56c6c; }
.tag.s3 { background: #e8f8ee; color: #67c23a; }
.actions { display: flex; gap: 6px; }
.btn-sm { padding: 6px 10px; border: 1px solid #ddd; border-radius: 6px; background: #fff; cursor: pointer; }
.btn-sm.ok { color: #409eff; }
.btn-sm.danger { color: #f56c6c; }
.btn-sm:disabled { opacity: .45; cursor: not-allowed; }
.pagination-wrap { display: flex; justify-content: space-between; margin-top: 16px; padding: 14px; background: #fff; border-radius: 8px; }
</style>
