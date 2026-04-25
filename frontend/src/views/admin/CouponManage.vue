<template>
  <div class="manage-page">
    <div class="page-header">
      <h2>优惠券管理</h2>
      <div class="header-stats" v-if="total > 0">
        <span class="stat-badge">共 {{ total }} 张优惠券</span>
      </div>
    </div>

    <div class="filter-bar">
      <div class="search-box">
        <Search class="search-icon" />
        <input
          v-model="keyword"
          placeholder="搜索券码 / 券名 / 用户"
          class="filter-input"
          @keyup.enter="loadCoupons"
        />
      </div>
      <select v-model="statusFilter" class="filter-select" @change="loadCoupons">
        <option :value="null">全部状态</option>
        <option :value="0">未使用</option>
        <option :value="1">已使用</option>
        <option :value="2">已过期</option>
      </select>
      <button class="btn-search" @click="loadCoupons">
        <Search /> 搜索
      </button>
    </div>

    <div class="table-wrap">
      <table class="data-table">
        <thead>
          <tr>
            <th style="width:60px">ID</th>
            <th style="width:180px">用户</th>
            <th>优惠券</th>
            <th style="width:130px">优惠信息</th>
            <th style="width:90px">状态</th>
            <th style="width:220px">有效期</th>
            <th style="width:150px">使用时间</th>
            <th style="width:100px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in coupons" :key="row.id">
            <td>{{ row.id }}</td>
            <td>
              <div class="user-cell">
                <strong>{{ row.nickname || row.username || '-' }}</strong>
                <span>{{ row.email || row.username || '-' }}</span>
              </div>
            </td>
            <td>
              <div class="coupon-cell">
                <strong>{{ row.name }}</strong>
                <span>{{ row.couponCode }}</span>
                <small>{{ formatCouponDesc(row) }}</small>
              </div>
            </td>
            <td>
              <div class="amount-cell">
                <span class="price">¥{{ formatAmount(row.discountAmount) }}</span>
                <small>满 ¥{{ formatAmount(row.minAmount) }} 可用</small>
              </div>
            </td>
            <td>
              <span
                class="tag"
                :class="{
                  'tag-blue': row.status === 0,
                  'tag-green': row.status === 1,
                  'tag-red': row.status === 2
                }"
              >
                {{ row.statusText }}
              </span>
            </td>
            <td class="time">{{ formatTime(row.startTime) }} - {{ formatTime(row.endTime) }}</td>
            <td class="time">{{ row.useTime ? formatTime(row.useTime) : '-' }}</td>
            <td class="actions">
              <button class="btn-sm btn-danger" @click="handleDelete(row)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>

      <EmptyState
        v-if="!coupons.length && !loading"
        title="暂无优惠券"
        description="当前没有可管理的优惠券记录"
      />
    </div>

    <div class="pagination-wrap" v-if="total > pageSize">
      <div class="pagination-info">
        共 {{ total }} 条记录，第 {{ page }} / {{ Math.ceil(total / pageSize) }} 页
      </div>
      <div class="pagination">
        <button :disabled="page <= 1" @click="page--; loadCoupons()">
          <ArrowLeft /> 上一页
        </button>
        <div class="page-numbers">
          <button
            v-for="p in visiblePages"
            :key="p"
            :class="{ active: p === page, ellipsis: p === '...' }"
            @click="p !== '...' && (page = p) && loadCoupons()"
          >
            {{ p }}
          </button>
        </div>
        <button :disabled="page >= Math.ceil(total / pageSize)" @click="page++; loadCoupons()">
          下一页 <ArrowRight />
        </button>
      </div>
    </div>

    <ConfirmModal
      v-model:visible="deleteVisible"
      title="删除优惠券"
      :message="`确定删除优惠券「${deleteTarget?.name || ''}」吗？`"
      detail="删除后，该券记录将从系统中移除"
      type="danger"
      confirmText="删除"
      :loading="deleteLoading"
      @confirm="confirmDelete"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '../../utils/request'
import { useVisiblePages } from '../../composables/usePagination'
import { formatTime } from '../../composables/useFormatTime'
import { ElMessage } from 'element-plus'
import { Search, ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import EmptyState from '../../components/admin/EmptyState.vue'
import ConfirmModal from '../../components/admin/ConfirmModal.vue'

const coupons = ref([])
const loading = ref(false)
const keyword = ref('')
const statusFilter = ref(null)
const page = ref(1)
const pageSize = 10
const total = ref(0)

const deleteVisible = ref(false)
const deleteTarget = ref(null)
const deleteLoading = ref(false)

const { visiblePages } = useVisiblePages(page, total, pageSize)

const formatAmount = (value) => Number(value || 0).toFixed(2)
const formatCouponDesc = (row) => row.description || `满${formatAmount(row.minAmount)}减${formatAmount(row.discountAmount)}`

const loadCoupons = async () => {
  loading.value = true
  try {
    const res = await request.get('/admin/coupons', {
      params: {
        page: page.value,
        size: pageSize,
        keyword: keyword.value,
        status: statusFilter.value
      }
    })
    coupons.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const handleDelete = (row) => {
  deleteTarget.value = row
  deleteVisible.value = true
}

const confirmDelete = async () => {
  deleteLoading.value = true
  try {
    await request.delete(`/admin/coupons/${deleteTarget.value.id}`)
    ElMessage.success('删除成功')
    deleteVisible.value = false
    loadCoupons()
  } catch (e) {
    console.error(e)
  } finally {
    deleteLoading.value = false
  }
}

onMounted(() => {
  loadCoupons()
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

.header-stats {
  display: flex;
  gap: 12px;
}

.stat-badge {
  padding: 6px 14px;
  background: #f0f0f0;
  border-radius: 20px;
  font-size: 13px;
  color: #666;
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
  max-width: 320px;
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
  transition: border-color 0.2s;
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
  vertical-align: middle;
}

.data-table tr:last-child td {
  border-bottom: none;
}

.data-table tr:hover td {
  background: #fafaf8;
}

.user-cell,
.coupon-cell,
.amount-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.user-cell strong,
.coupon-cell strong {
  color: #303133;
  font-size: 14px;
  font-weight: 500;
}

.user-cell span,
.coupon-cell span,
.coupon-cell small,
.amount-cell small {
  color: #909399;
  font-size: 12px;
}

.price {
  color: #f56c6c;
  font-weight: 600;
}

.time {
  font-size: 13px;
  color: #909399;
  white-space: nowrap;
}

.tag {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

.tag-blue { background: #e8f0fe; color: #409eff; }
.tag-green { background: #e8f8ee; color: #67c23a; }
.tag-red { background: #fde8e8; color: #f56c6c; }

.actions {
  display: flex;
  gap: 8px;
}

.btn-sm {
  padding: 6px 14px;
  border-radius: 6px;
  border: 1.5px solid #e5e7eb;
  background: #fff;
  cursor: pointer;
  font-size: 12px;
  font-weight: 500;
  transition: all 0.2s;
}

.btn-sm:hover:not(:disabled) {
  border-color: #333;
  color: #333;
}

.btn-danger {
  color: #f56c6c;
  border-color: #fde8e8;
}

.btn-danger:hover:not(:disabled) {
  background: #fde8e8;
  border-color: #f56c6c;
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
  gap: 8px;
}

.pagination button {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 14px;
  border-radius: 6px;
  border: 1.5px solid #e5e7eb;
  background: #fff;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.pagination button svg {
  width: 14px;
  height: 14px;
}

.pagination button:hover:not(:disabled) {
  border-color: #333;
  color: #333;
}

.pagination button:disabled {
  color: #ccc;
  cursor: not-allowed;
}

.page-numbers {
  display: flex;
  gap: 4px;
}

.page-numbers button {
  min-width: 36px;
  justify-content: center;
}

.page-numbers button.active {
  background: #c45c3e;
  border-color: #333;
  color: #fff;
}

.page-numbers button.ellipsis {
  border: none;
  background: transparent;
  cursor: default;
}

@media (max-width: 1200px) {
  .table-wrap {
    overflow-x: auto;
  }

  .data-table {
    min-width: 1120px;
  }
}
</style>
