<template>
  <div class="manage-page">
    <div class="page-header">
      <h2>分类管理</h2>
      <button class="btn-add" @click="openDialog(null)">
        <Plus /> 新增分类
      </button>
    </div>

    <div class="filter-bar">
      <div class="search-box">
        <Search class="search-icon" />
        <input
          v-model="keyword"
          placeholder="搜索分类名称"
          class="filter-input"
          @keyup.enter="loadCategories"
        />
      </div>
      <button class="btn-search" @click="loadCategories">
        <Search /> 搜索
      </button>
    </div>

    <div class="table-wrap">
      <table class="data-table">
        <thead>
          <tr>
            <th style="width:80px">ID</th>
            <th>分类名称</th>
            <th style="width:100px">排序</th>
            <th>创建时间</th>
            <th style="width:180px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in categories" :key="row.id">
            <td>{{ row.id }}</td>
            <td class="name-cell">
              <span class="category-name">{{ row.name }}</span>
            </td>
            <td>
              <span class="sort-badge">{{ row.sort }}</span>
            </td>
            <td class="time">{{ formatTime(row.createTime) }}</td>
            <td class="actions">
              <button class="btn-icon" title="编辑" @click="openDialog(row)">
                <Edit />
              </button>
              <button class="btn-icon btn-danger" title="删除" @click="handleDelete(row)">
                <Delete />
              </button>
            </td>
          </tr>
        </tbody>
      </table>

      <EmptyState
        v-if="!categories.length && !loading"
        title="暂无分类"
        description="点击右上角按钮添加第一个分类"
      />
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑分类' : '新增分类'"
      width="450px"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入分类名称" maxlength="20" show-word-limit />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="9999" style="width: 100%" />
          <span class="form-tip">数值越小排序越靠前</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          {{ isEdit ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>

    <ConfirmModal
      v-model:visible="deleteVisible"
      title="删除分类"
      :message="`确定删除分类「${deleteTarget?.name}」吗？`"
      detail="仅当该分类下没有子分类和商品时才允许删除"
      type="danger"
      confirmText="删除"
      :loading="deleteLoading"
      @confirm="confirmDelete"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '../../utils/request'
import { formatTime } from '../../composables/useFormatTime'
import { ElMessage } from 'element-plus'
import { Plus, Search, Edit, Delete } from '@element-plus/icons-vue'
import EmptyState from '../../components/admin/EmptyState.vue'
import ConfirmModal from '../../components/admin/ConfirmModal.vue'

const categories = ref([])
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const keyword = ref('')

const deleteVisible = ref(false)
const deleteTarget = ref(null)
const deleteLoading = ref(false)

const form = reactive({ id: null, name: '', sort: 0 })

const rules = {
  name: [
    { required: true, message: '请输入分类名称', trigger: 'blur' },
    { min: 1, max: 20, message: '名称长度 1-20 个字符', trigger: 'blur' }
  ]
}

const loadCategories = async () => {
  loading.value = true
  try {
    const res = await request.get('/admin/categories', {
      params: { keyword: keyword.value }
    })
    categories.value = res.data || []
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const openDialog = (row) => {
  isEdit.value = !!row
  if (row) {
    Object.assign(form, { id: row.id, name: row.name, sort: row.sort })
  } else {
    Object.assign(form, { id: null, name: '', sort: 0 })
  }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      if (isEdit.value) {
        await request.put(`/admin/categories/${form.id}`, form)
        ElMessage.success('更新成功')
      } else {
        await request.post('/admin/categories', form)
        ElMessage.success('创建成功')
      }
      dialogVisible.value = false
      loadCategories()
    } catch (e) {
      console.error(e)
    } finally {
      submitting.value = false
    }
  })
}

const handleDelete = (row) => {
  deleteTarget.value = row
  deleteVisible.value = true
}

const confirmDelete = async () => {
  deleteLoading.value = true
  try {
    await request.delete(`/admin/categories/${deleteTarget.value.id}`)
    ElMessage.success('删除成功')
    deleteVisible.value = false
    loadCategories()
  } catch (e) {
    console.error(e)
  } finally {
    deleteLoading.value = false
  }
}

onMounted(() => {
  loadCategories()
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

.btn-add {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  border-radius: 8px;
  border: none;
  background: #1a1a1a;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 4px 12px rgba(26, 26, 26, 0.15);
}

.btn-add svg {
  width: 16px;
  height: 16px;
}

.btn-add:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(26, 26, 26, 0.2);
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.search-box {
  position: relative;
  flex: 1;
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
  transition: background 0.15s;
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

.category-name {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.sort-badge {
  display: inline-block;
  padding: 2px 10px;
  background: #f0f0f0;
  border-radius: 6px;
  font-size: 12px;
  color: #666;
}

.time {
  font-size: 13px;
  color: #909399;
}

.actions {
  display: flex;
  gap: 8px;
}

.btn-icon {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  border: 1.5px solid #e5e7eb;
  background: #fff;
  color: #666;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.btn-icon svg {
  width: 14px;
  height: 14px;
}

.btn-icon:hover {
  border-color: #333;
  color: #333;
  background: #fafaf8;
}

.btn-icon.btn-danger:hover {
  border-color: #f56c6c;
  color: #f56c6c;
  background: #fef0f0;
}

.form-tip {
  display: block;
  font-size: 12px;
  color: #909399;
  margin-top: 6px;
}
</style>
