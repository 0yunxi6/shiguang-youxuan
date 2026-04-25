<template>
  <div class="manage-page">
    <div class="page-header">
      <h2>用户管理</h2>
      <div class="header-stats" v-if="total > 0">
        <span class="stat-badge">
          共 {{ total }} 位用户
        </span>
      </div>
    </div>

    <div class="filter-bar">
      <div class="search-box">
        <Search class="search-icon" />
        <input
          v-model="keyword"
          placeholder="搜索用户名/邮箱/手机号"
          class="filter-input"
          @keyup.enter="loadUsers"
        />
      </div>
      <select v-model="roleFilter" class="filter-select" @change="loadUsers">
        <option :value="null">全部角色</option>
        <option value="ADMIN">管理员</option>
        <option value="USER">普通用户</option>
      </select>
      <select v-model="statusFilter" class="filter-select" @change="loadUsers">
        <option :value="null">全部状态</option>
        <option :value="1">正常</option>
        <option :value="0">已禁用</option>
      </select>
      <button class="btn-search" @click="loadUsers">
        <Search /> 搜索
      </button>
    </div>

    <div class="table-wrap">
      <table class="data-table">
        <thead>
          <tr>
            <th style="width:60px">ID</th>
            <th style="width:70px">头像</th>
            <th>用户信息</th>
            <th>联系方式</th>
            <th style="width:80px">角色</th>
            <th style="width:80px">状态</th>
            <th style="width:100px">注册时间</th>
            <th style="width:180px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in users" :key="row.id">
            <td>{{ row.id }}</td>
            <td>
              <UserAvatar :src="row.avatar" :name="row.username" :size="36" />
            </td>
            <td class="name-cell">
              <span class="username">{{ row.username }}</span>
            </td>
            <td class="contact-cell">
              <div class="contact-info">
                <span class="contact-item" v-if="row.email">
                  <Message /> {{ row.email }}
                </span>
                <span class="contact-item" v-if="row.phone">
                  <Phone /> {{ row.phone }}
                </span>
              </div>
            </td>
            <td>
              <span class="tag" :class="row.role === 'ADMIN' ? 'tag-purple' : 'tag-gray'">
                {{ row.role === 'ADMIN' ? '管理员' : '用户' }}
              </span>
            </td>
            <td>
              <span class="tag" :class="row.status === 1 ? 'tag-green' : 'tag-red'">
                {{ row.status === 1 ? '正常' : '禁用' }}
              </span>
            </td>
            <td class="time">{{ formatTime(row.createTime) }}</td>
            <td class="actions">
              <button
                class="btn-sm"
                :class="row.status === 1 ? 'btn-warn' : 'btn-ok'"
                @click="toggleStatus(row)"
                :disabled="row.role === 'ADMIN'"
                :title="row.role === 'ADMIN' ? '管理员无法操作' : ''"
              >
                {{ row.status === 1 ? '禁用' : '启用' }}
              </button>
              <button
                class="btn-sm btn-danger"
                @click="handleDelete(row)"
                :disabled="row.role === 'ADMIN'"
                :title="row.role === 'ADMIN' ? '管理员无法删除' : ''"
              >
                删除
              </button>
            </td>
          </tr>
        </tbody>
      </table>

      <EmptyState
        v-if="!users.length && !loading"
        title="暂无用户"
        description="当前没有注册用户"
      />
    </div>

    <div class="pagination-wrap" v-if="total > pageSize">
      <div class="pagination-info">
        共 {{ total }} 条记录，第 {{ page }} / {{ Math.ceil(total / pageSize) }} 页
      </div>
      <div class="pagination">
        <button :disabled="page <= 1" @click="page--; loadUsers()">
          <ArrowLeft /> 上一页
        </button>
        <div class="page-numbers">
          <button
            v-for="p in visiblePages"
            :key="p"
            :class="{ active: p === page, ellipsis: p === '...' }"
            @click="p !== '...' && (page = p) && loadUsers()"
          >
            {{ p }}
          </button>
        </div>
        <button :disabled="page >= Math.ceil(total / pageSize)" @click="page++; loadUsers()">
          下一页 <ArrowRight />
        </button>
      </div>
    </div>

    <ConfirmModal
      v-model:visible="deleteVisible"
      title="删除用户"
      :message="`确定删除用户「${deleteTarget?.username}」吗？`"
      detail="删除后，该用户的所有数据将被永久移除"
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
import { Search, Message, Phone, ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import UserAvatar from '../../components/UserAvatar.vue'
import EmptyState from '../../components/admin/EmptyState.vue'
import ConfirmModal from '../../components/admin/ConfirmModal.vue'

const users = ref([])
const loading = ref(false)
const keyword = ref('')
const roleFilter = ref(null)
const statusFilter = ref(null)
const page = ref(1)
const pageSize = 10
const total = ref(0)

const deleteVisible = ref(false)
const deleteTarget = ref(null)
const deleteLoading = ref(false)


const { visiblePages } = useVisiblePages(page, total, pageSize)

const loadUsers = async () => {
  loading.value = true
  try {
    const res = await request.get('/admin/users', {
      params: {
        page: page.value,
        size: pageSize,
        keyword: keyword.value,
        role: roleFilter.value,
        status: statusFilter.value
      }
    })
    users.value = res.data?.records || res.data || []
    total.value = res.data?.total || users.value.length
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const toggleStatus = async (row) => {
  if (row.role === 'ADMIN') return
  try {
    await request.put(`/admin/users/${row.id}/status`, null, {
      params: { status: row.status === 1 ? 0 : 1 }
    })
    row.status = row.status === 1 ? 0 : 1
    ElMessage.success(row.status === 1 ? '已启用' : '已禁用')
  } catch (e) {
    console.error(e)
  }
}

const handleDelete = (row) => {
  if (row.role === 'ADMIN') return
  deleteTarget.value = row
  deleteVisible.value = true
}

const confirmDelete = async () => {
  deleteLoading.value = true
  try {
    await request.delete(`/admin/users/${deleteTarget.value.id}`)
    ElMessage.success('删除成功')
    deleteVisible.value = false
    loadUsers()
  } catch (e) {
    console.error(e)
  } finally {
    deleteLoading.value = false
  }
}

onMounted(() => {
  loadUsers()
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
}

.data-table tr:last-child td {
  border-bottom: none;
}

.data-table tr:hover td {
  background: #fafaf8;
}

.name-cell {
  font-weight: 500;
}

.contact-cell {
  max-width: 200px;
}

.contact-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.contact-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #666;
}

.contact-item svg {
  width: 14px;
  height: 14px;
  color: #999;
}

.tag {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

.tag-green { background: #e8f8ee; color: #67c23a; }
.tag-gray { background: #f0f0f0; color: #909399; }
.tag-red { background: #fde8e8; color: #f56c6c; }
.tag-purple { background: #fdf0ec; color: #c45c3e; }

.time {
  font-size: 13px;
  color: #909399;
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
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-sm:hover:not(:disabled) {
  border-color: #333;
  color: #333;
}

.btn-sm:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-sm.btn-warn {
  color: #e6a23c;
  border-color: #fef3e6;
}

.btn-sm.btn-warn:hover:not(:disabled) {
  background: #fef3e6;
  border-color: #e6a23c;
}

.btn-sm.btn-ok {
  color: #67c23a;
  border-color: #e8f8ee;
}

.btn-sm.btn-ok:hover:not(:disabled) {
  background: #e8f8ee;
  border-color: #67c23a;
}

.btn-sm.btn-danger {
  color: #f56c6c;
  border-color: #fde8e8;
}

.btn-sm.btn-danger:hover:not(:disabled) {
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
</style>
