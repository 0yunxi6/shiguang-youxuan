<template>
  <div class="manage-page">
    <div class="page-header">
      <h2>收藏管理</h2>
      <div class="header-stats" v-if="total > 0">
        <span class="stat-badge">共 {{ total }} 条收藏记录</span>
      </div>
    </div>

    <div class="filter-bar">
      <div class="search-box">
        <Search class="search-icon" />
        <input
          v-model="keyword"
          placeholder="搜索用户 / 商品"
          class="filter-input"
          @keyup.enter="loadFavorites"
        />
      </div>
      <button class="btn-search" @click="loadFavorites">
        <Search /> 搜索
      </button>
    </div>

    <div class="table-wrap">
      <table class="data-table">
        <thead>
          <tr>
            <th style="width:60px">ID</th>
            <th style="width:180px">用户</th>
            <th>商品</th>
            <th style="width:110px">价格</th>
            <th style="width:140px">收藏时间</th>
            <th style="width:100px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in favorites" :key="row.id">
            <td>{{ row.id }}</td>
            <td>
              <div class="user-cell">
                <strong>{{ row.nickname || row.username || '-' }}</strong>
                <span>{{ row.email || row.username || '-' }}</span>
              </div>
            </td>
            <td>
              <div class="product-cell">
                <img :src="row.productImage || '/placeholder.svg'" class="product-thumb" />
                <div class="product-copy">
                  <strong>{{ row.productName || '商品已删除' }}</strong>
                  <span>ID: {{ row.productId }}</span>
                </div>
              </div>
            </td>
            <td>
              <span class="price">¥{{ formatAmount(row.productPrice) }}</span>
            </td>
            <td class="time">{{ formatTime(row.createTime) }}</td>
            <td class="actions">
              <button class="btn-sm btn-danger" @click="handleDelete(row)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>

      <EmptyState
        v-if="!favorites.length && !loading"
        title="暂无收藏记录"
        description="当前没有用户收藏数据"
      />
    </div>

    <div class="pagination-wrap" v-if="total > pageSize">
      <div class="pagination-info">
        共 {{ total }} 条记录，第 {{ page }} / {{ Math.ceil(total / pageSize) }} 页
      </div>
      <div class="pagination">
        <button :disabled="page <= 1" @click="page--; loadFavorites()">
          <ArrowLeft /> 上一页
        </button>
        <div class="page-numbers">
          <button
            v-for="p in visiblePages"
            :key="p"
            :class="{ active: p === page, ellipsis: p === '...' }"
            @click="p !== '...' && (page = p) && loadFavorites()"
          >
            {{ p }}
          </button>
        </div>
        <button :disabled="page >= Math.ceil(total / pageSize)" @click="page++; loadFavorites()">
          下一页 <ArrowRight />
        </button>
      </div>
    </div>

    <ConfirmModal
      v-model:visible="deleteVisible"
      title="删除收藏"
      :message="`确定删除收藏记录 #${deleteTarget?.id || ''} 吗？`"
      detail="删除后，该收藏记录将从系统中移除"
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

const favorites = ref([])
const loading = ref(false)
const keyword = ref('')
const page = ref(1)
const pageSize = 10
const total = ref(0)

const deleteVisible = ref(false)
const deleteTarget = ref(null)
const deleteLoading = ref(false)

const { visiblePages } = useVisiblePages(page, total, pageSize)

const formatAmount = (value) => Number(value || 0).toFixed(2)

const loadFavorites = async () => {
  loading.value = true
  try {
    const res = await request.get('/admin/favorites', {
      params: {
        page: page.value,
        size: pageSize,
        keyword: keyword.value
      }
    })
    favorites.value = res.data?.records || []
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
    await request.delete(`/admin/favorites/${deleteTarget.value.id}`)
    ElMessage.success('删除成功')
    deleteVisible.value = false
    loadFavorites()
  } catch (e) {
    console.error(e)
  } finally {
    deleteLoading.value = false
  }
}

onMounted(() => {
  loadFavorites()
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

.user-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.user-cell strong,
.product-copy strong {
  color: #303133;
  font-size: 14px;
  font-weight: 500;
}

.user-cell span,
.product-copy span,
.time {
  color: #909399;
  font-size: 12px;
}

.product-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.product-thumb {
  width: 52px;
  height: 52px;
  border-radius: 10px;
  object-fit: cover;
  background: #f5f5f5;
}

.product-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.price {
  color: #f56c6c;
  font-weight: 600;
}

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
  color: #d14343;
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
  display: inline-flex;
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

@media (max-width: 1024px) {
  .table-wrap {
    overflow-x: auto;
  }

  .data-table {
    min-width: 860px;
  }
}
</style>
