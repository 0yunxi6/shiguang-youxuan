<template>
  <div class="manage-page">
    <div class="page-header">
      <h2>订单管理</h2>
      <div class="header-actions">
        <button class="btn-export" @click="exportOrders" :disabled="!orders.length">导出当前页</button>
        <div class="header-stats">
          <span class="stat-badge warning" v-if="pendingCount > 0">
            {{ pendingCount }} 待处理
          </span>
        </div>
      </div>
    </div>

    <div class="filter-bar">
      <div class="search-box">
        <Search class="search-icon" />
        <input
          v-model="keyword"
          placeholder="搜索订单号/收货人/手机号"
          class="filter-input"
          @keyup.enter="applyFilters"
        />
      </div>
      <select v-model="statusFilter" class="filter-select" @change="applyFilters">
        <option :value="null">全部状态</option>
        <option v-for="(text, idx) in statusText" :key="idx" :value="idx">{{ text }}</option>
      </select>
      <button class="btn-search" @click="applyFilters">
        <Search /> 搜索
      </button>
    </div>

    <div class="batch-bar" v-if="selectedOrderIds.length">
      <span>已选择 {{ selectedOrderIds.length }} 个订单</span>
      <button class="btn-batch" @click="batchUpdateStatus(2)">批量发货</button>
      <button class="btn-batch ok" @click="batchUpdateStatus(3)">批量完成</button>
      <button class="btn-batch danger" @click="batchUpdateStatus(4)">批量取消</button>
      <button class="btn-link" @click="selectedOrderIds = []">取消选择</button>
    </div>

    <div class="table-wrap">
      <table class="data-table">
        <thead>
          <tr>
            <th style="width:44px">
              <input type="checkbox" :checked="allPageSelected" @change="toggleSelectAll" />
            </th>
            <th style="width:60px">ID</th>
            <th>订单号</th>
            <th style="width:100px">金额</th>
            <th style="width:90px">状态</th>
            <th>收货人</th>
            <th>联系方式</th>
            <th style="width:150px">下单时间</th>
            <th style="width:200px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in orders" :key="row.id">
            <td>
              <input v-model="selectedOrderIds" type="checkbox" :value="row.id" />
            </td>
            <td>{{ row.id }}</td>
            <td class="mono">{{ row.orderNo }}</td>
            <td class="price">¥{{ row.totalAmount }}</td>
            <td>
              <span class="tag" :class="'s' + row.status">
                {{ statusText[row.status] }}
              </span>
            </td>
            <td class="name-cell">{{ row.receiverName }}</td>
            <td class="contact-cell">
              <div class="contact-info">
                <span>{{ row.receiverPhone }}</span>
                <span class="address" :title="row.receiverAddress">{{ row.receiverAddress }}</span>
              </div>
            </td>
            <td class="time">{{ row.createTime }}</td>
            <td class="actions">
              <button class="btn-sm" @click="viewDetail(row)">
                <View /> 详情
              </button>
              <select
                v-if="row.status < 3"
                v-model="row.status"
                class="status-select"
                @change="updateStatus(row)"
              >
                <option v-for="(text, idx) in statusText" :key="idx" :value="idx">
                  {{ idx > row._originalStatus ? `改为：${text}` : text }}
                </option>
              </select>
            </td>
          </tr>
        </tbody>
      </table>

      <EmptyState
        v-if="!orders.length && !loading"
        title="暂无订单"
        description="还没有订单数据"
      />
    </div>

    <div class="pagination-wrap" v-if="total > pageSize">
      <div class="pagination-info">
        共 {{ total }} 条记录
      </div>
      <div class="pagination">
        <button :disabled="page <= 1" @click="page--; loadOrders()">上一页</button>
        <span class="page-current">{{ page }} / {{ Math.ceil(total / pageSize) }}</span>
        <button :disabled="page >= Math.ceil(total / pageSize)" @click="page++; loadOrders()">下一页</button>
      </div>
    </div>

    <el-dialog
      v-model="detailVisible"
      title="订单详情"
      width="650px"
      :close-on-click-modal="false"
    >
      <div class="order-detail" v-if="currentOrder">
        <div class="detail-section">
          <h4 class="section-title">订单信息</h4>
          <div class="detail-grid">
            <div class="detail-item">
              <span class="label">订单号</span>
              <span class="value mono">{{ currentOrder.orderNo }}</span>
            </div>
            <div class="detail-item">
              <span class="label">订单金额</span>
              <span class="value price">¥{{ currentOrder.totalAmount }}</span>
            </div>
            <div class="detail-item">
              <span class="label">订单状态</span>
              <span class="value">
                <span class="tag" :class="'s' + currentOrder.status">
                  {{ statusText[currentOrder.status] }}
                </span>
              </span>
            </div>
            <div class="detail-item">
              <span class="label">下单时间</span>
              <span class="value">{{ currentOrder.createTime }}</span>
            </div>
          </div>
        </div>

        <div class="detail-section">
          <h4 class="section-title">收货信息</h4>
          <div class="detail-grid">
            <div class="detail-item">
              <span class="label">收货人</span>
              <span class="value">{{ currentOrder.receiverName }}</span>
            </div>
            <div class="detail-item">
              <span class="label">联系电话</span>
              <span class="value">{{ currentOrder.receiverPhone }}</span>
            </div>
            <div class="detail-item full">
              <span class="label">收货地址</span>
              <span class="value">{{ currentOrder.receiverAddress }}</span>
            </div>
            <div class="detail-item full" v-if="currentOrder.remark">
              <span class="label">订单备注</span>
              <span class="value">{{ currentOrder.remark }}</span>
            </div>
          </div>
        </div>
        <div class="detail-section" v-if="currentOrder.items?.length">
          <h4 class="section-title">商品明细</h4>
          <div class="detail-items">
            <div v-for="item in currentOrder.items" :key="item.id" class="detail-product">
              <img :src="item.productImage || '/placeholder.svg'" />
              <div class="detail-product-copy">
                <strong>{{ item.productName }}</strong>
                <span>¥{{ item.price }} × {{ item.quantity }}</span>
              </div>
              <span class="price">¥{{ item.totalAmount }}</span>
            </div>
          </div>
          <div class="detail-totals">
            <span v-if="Number(currentOrder.originalAmount || 0) > 0">商品总价：¥{{ formatAmount(currentOrder.originalAmount) }}</span>
            <span v-if="Number(currentOrder.discountAmount || 0) > 0">优惠：-¥{{ formatAmount(currentOrder.discountAmount) }}</span>
            <span>运费：{{ Number(currentOrder.shippingFee || 0) === 0 ? '免运费' : '¥' + formatAmount(currentOrder.shippingFee) }}</span>
            <strong>实付：¥{{ formatAmount(currentOrder.totalAmount) }}</strong>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button
          v-if="currentOrder && currentOrder.status === 1"
          type="primary"
          @click="quickShip(currentOrder)"
        >
          确认发货
        </el-button>
      </template>
    </el-dialog>

    <ConfirmModal
      v-model:visible="shipVisible"
      title="确认发货"
      message="确定要发货此订单吗？"
      detail="发货后订单状态将变更为「已发货」"
      type="info"
      confirmText="确认发货"
      :loading="shipLoading"
      @confirm="confirmShip"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import request from '../../utils/request'
import { ElMessage } from 'element-plus'
import { Search, View } from '@element-plus/icons-vue'
import EmptyState from '../../components/admin/EmptyState.vue'
import ConfirmModal from '../../components/admin/ConfirmModal.vue'

const orders = ref([])
const loading = ref(false)
const keyword = ref('')
const statusFilter = ref(null)
const page = ref(1)
const pageSize = 10
const total = ref(0)
const selectedOrderIds = ref([])

const detailVisible = ref(false)
const currentOrder = ref(null)
const shipVisible = ref(false)
const shipTarget = ref(null)
const shipLoading = ref(false)

const statusText = ['待支付', '已支付', '已发货', '已完成', '已取消']

const pendingCount = computed(() => {
  return orders.value.filter(o => o.status === 1).length
})

const allPageSelected = computed(() => orders.value.length > 0 && orders.value.every(row => selectedOrderIds.value.includes(row.id)))

const loadOrders = async () => {
  loading.value = true
  try {
    const res = await request.get('/admin/orders', {
      params: {
        page: page.value,
        size: pageSize,
        keyword: keyword.value,
        status: statusFilter.value
      }
    })
    orders.value = (res.data?.records || res.data || []).map(o => ({
      ...o,
      _originalStatus: o.status
    }))
    total.value = res.data?.total || orders.value.length
    selectedOrderIds.value = selectedOrderIds.value.filter(id => orders.value.some(row => row.id === id))
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const toggleSelectAll = (event) => {
  if (event.target.checked) {
    selectedOrderIds.value = Array.from(new Set([...selectedOrderIds.value, ...orders.value.map(row => row.id)]))
  } else {
    const pageIds = new Set(orders.value.map(row => row.id))
    selectedOrderIds.value = selectedOrderIds.value.filter(id => !pageIds.has(id))
  }
}

const applyFilters = () => {
  page.value = 1
  loadOrders()
}

const viewDetail = async (row) => {
  currentOrder.value = { ...row, items: [] }
  detailVisible.value = true
  try {
    const res = await request.get(`/admin/orders/${row.id}`)
    currentOrder.value = {
      ...(res.data?.order || row),
      items: res.data?.items || []
    }
  } catch (error) {
    currentOrder.value = row
  }
}

const updateStatus = async (row) => {
  if (row.status === row._originalStatus) return
  try {
    await request.put(`/admin/orders/${row.id}/status`, null, {
      params: { status: row.status }
    })
    row._originalStatus = row.status
    ElMessage.success('状态更新成功')
  } catch (e) {
    console.error(e)
    row.status = row._originalStatus
  }
}

const quickShip = (order) => {
  shipTarget.value = order
  shipVisible.value = true
}

const confirmShip = async () => {
  shipLoading.value = true
  try {
    await request.put(`/admin/orders/${shipTarget.value.id}/status`, null, {
      params: { status: 2 }
    })
    shipTarget.value.status = 2
    shipTarget.value._originalStatus = 2
    ElMessage.success('发货成功')
    shipVisible.value = false
    detailVisible.value = false
  } catch (e) {
    console.error(e)
  } finally {
    shipLoading.value = false
  }
}

const batchUpdateStatus = async (status) => {
  if (!selectedOrderIds.value.length) return
  try {
    await request.put('/admin/orders/status/batch', selectedOrderIds.value, { params: { status } })
    ElMessage.success('批量更新成功')
    selectedOrderIds.value = []
    loadOrders()
  } catch (e) {
    console.error(e)
  }
}

const formatAmount = (amount) => Number(amount || 0).toFixed(2)
const csvCell = (value) => `"${String(value ?? '').replace(/"/g, '""')}"`
const downloadCsv = (filename, rows) => {
  const content = '\uFEFF' + rows.map(row => row.map(csvCell).join(',')).join('\n')
  const blob = new Blob([content], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  URL.revokeObjectURL(url)
}

const exportOrders = () => {
  const rows = [
    ['ID', '订单号', '金额', '状态', '收货人', '手机号', '地址', '下单时间'],
    ...orders.value.map(row => [
      row.id,
      row.orderNo,
      row.totalAmount,
      statusText[row.status] || '未知',
      row.receiverName,
      row.receiverPhone,
      row.receiverAddress,
      row.createTime
    ])
  ]
  downloadCsv(`orders-page-${page.value}.csv`, rows)
}

onMounted(() => {
  loadOrders()
})
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  font-size: 20px;
  font-weight: 700;
  color: #111;
  margin: 0;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-stats {
  display: flex;
  gap: 12px;
}

.btn-export {
  padding: 10px 16px;
  border-radius: 8px;
  border: 1.5px solid #e5e7eb;
  background: #fff;
  color: #666;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.btn-export:hover:not(:disabled) {
  border-color: #333;
  color: #333;
}

.btn-export:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.stat-badge {
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
}

.stat-badge.warning {
  background: #fef3e6;
  color: #e6a23c;
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.search-box {
  position: relative;
  flex: 1;
  min-width: 240px;
  max-width: 360px;
}

.search-icon {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: #999;
  width: 16px;
  height: 16px;
}

.filter-input {
  width: 100%;
  padding: 10px 14px 10px 38px;
  border: 1.5px solid #e5e7eb;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  background: #fff;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.filter-input:focus {
  border-color: #333;
  box-shadow: 0 0 0 3px rgba(196, 92, 62, 0.1);
}

.filter-select {
  padding: 10px 32px 10px 14px;
  border: 1.5px solid #e5e7eb;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  background: #fff url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%23999' d='M6 8L1 3h10z'/%3E%3C/svg%3E") no-repeat right 12px center;
  appearance: none;
  cursor: pointer;
}

.filter-select:focus {
  border-color: #333;
}

.btn-search {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  border-radius: 8px;
  border: 1.5px solid #e5e7eb;
  background: #fff;
  font-size: 14px;
  font-weight: 500;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-search svg {
  width: 14px;
  height: 14px;
}

.btn-search:hover {
  border-color: #333;
  color: #333;
}

.batch-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding: 12px 16px;
  background: #fafaf8;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  color: #666;
  font-size: 13px;
}

.btn-batch {
  padding: 6px 14px;
  border-radius: 6px;
  border: 1px solid #409eff;
  background: #ecf5ff;
  color: #409eff;
  cursor: pointer;
}

.btn-batch.ok {
  border-color: #67c23a;
  background: #f0faf4;
  color: #67c23a;
}

.btn-batch.danger {
  border-color: #f56c6c;
  background: #fef0f0;
  color: #f56c6c;
}

.btn-link {
  border: none;
  background: transparent;
  color: #999;
  cursor: pointer;
}

.table-wrap {
  background: #fff;
  border-radius: 8px;
  border: 1px solid #ebeef5;
  overflow: hidden;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th {
  padding: 14px 16px;
  text-align: left;
  font-size: 12px;
  font-weight: 600;
  color: #909399;
  background: #fafaf8;
  border-bottom: 1px solid #ebeef5;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.data-table td {
  padding: 14px 16px;
  font-size: 14px;
  color: #303133;
  border-bottom: 1px solid #f0f0f0;
}

.data-table tr:last-child td {
  border-bottom: none;
}

.data-table tr:hover td {
  background: #fafaf8;
}

.mono {
  font-family: 'SF Mono', Monaco, monospace;
  font-size: 12px;
  color: #666;
}

.price {
  color: #f56c6c;
  font-weight: 600;
}

.name-cell {
  font-weight: 500;
}

.contact-cell {
  max-width: 180px;
}

.contact-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.address {
  font-size: 12px;
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 160px;
  display: inline-block;
}

.time {
  font-size: 13px;
  color: #909399;
}

.tag {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

.tag.s0 { background: #fef3e6; color: #e6a23c; }
.tag.s1 { background: #e8f0fe; color: #409eff; }
.tag.s2 { background: #f0f0f0; color: #666; }
.tag.s3 { background: #e8f8ee; color: #67c23a; }
.tag.s4 { background: #fde8e8; color: #f56c6c; }

.actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.btn-sm {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border-radius: 6px;
  border: 1.5px solid #e5e7eb;
  background: #fff;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-sm svg {
  width: 14px;
  height: 14px;
}

.btn-sm:hover {
  border-color: #333;
  color: #333;
}

.status-select {
  padding: 6px 10px;
  border: 1.5px solid #e5e7eb;
  border-radius: 6px;
  font-size: 12px;
  outline: none;
  cursor: pointer;
  transition: border-color 0.2s;
}

.status-select:focus {
  border-color: #333;
}

.pagination-wrap {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 20px;
  padding: 16px 20px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #ebeef5;
}

.pagination-info {
  font-size: 13px;
  color: #909399;
}

.pagination {
  display: flex;
  align-items: center;
  gap: 12px;
}

.pagination button {
  padding: 8px 16px;
  border-radius: 6px;
  border: 1.5px solid #e5e7eb;
  background: #fff;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.pagination button:hover:not(:disabled) {
  border-color: #333;
  color: #333;
}

.pagination button:disabled {
  color: #ccc;
  cursor: not-allowed;
}

.page-current {
  font-size: 13px;
  color: #666;
}

.order-detail {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.detail-section {
  padding-bottom: 20px;
  border-bottom: 1px dashed #ebeef5;
}

.detail-section:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 16px;
  padding-bottom: 8px;
  border-bottom: 2px solid #c45c3e;
  display: inline-block;
}

.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.detail-item.full {
  grid-column: span 2;
}

.label {
  font-size: 12px;
  color: #909399;
}

.value {
  font-size: 14px;
  color: #303133;
}

.value.mono {
  font-family: 'SF Mono', Monaco, monospace;
  font-size: 13px;
}

.value.price {
  color: #f56c6c;
  font-weight: 600;
}

.detail-items {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.detail-product {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  background: #fafaf8;
}

.detail-product img {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  object-fit: cover;
  background: #fff;
}

.detail-product-copy {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.detail-product-copy strong {
  color: #303133;
  font-size: 14px;
}

.detail-product-copy span {
  color: #909399;
  font-size: 12px;
}

.detail-totals {
  display: flex;
  justify-content: flex-end;
  gap: 14px;
  flex-wrap: wrap;
  margin-top: 12px;
  color: #606266;
  font-size: 13px;
}

.detail-totals strong {
  color: #f56c6c;
}
</style>
