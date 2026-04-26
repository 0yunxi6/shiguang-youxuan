<template>
  <div class="manage-page">
    <div class="page-header">
      <h2>商品管理</h2>
      <div class="header-actions">
        <button class="btn-export" @click="exportProducts" :disabled="!products.length">导出当前页</button>
        <button class="btn-add" @click="openDialog(null)">
          <Plus /> 新增商品
        </button>
      </div>
    </div>

    <div class="filter-bar">
      <div class="search-box">
        <Search class="search-icon" />
        <input
          v-model="keyword"
          placeholder="搜索商品名称"
          class="filter-input"
          @keyup.enter="applyFilters"
        />
      </div>
      <select v-model="categoryId" class="filter-select" @change="applyFilters">
        <option :value="null">全部分类</option>
        <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
      </select>
      <input
        v-model="brandFilter"
        placeholder="品牌筛选"
        class="filter-input compact"
        @keyup.enter="applyFilters"
      />
      <select v-model="statusFilter" class="filter-select" @change="applyFilters">
        <option :value="null">全部状态</option>
        <option :value="1">上架</option>
        <option :value="0">下架</option>
      </select>
      <label class="filter-check">
        <input v-model="lowStockOnly" type="checkbox" @change="applyFilters" />
        库存预警
      </label>
      <button class="btn-search" @click="applyFilters">
        <Search /> 搜索
      </button>
    </div>

    <div class="batch-bar" v-if="selectedProductIds.length">
      <span>已选择 {{ selectedProductIds.length }} 个商品</span>
      <button class="btn-batch" @click="batchUpdateStatus(1)">批量上架</button>
      <button class="btn-batch warn" @click="batchUpdateStatus(0)">批量下架</button>
      <button class="btn-link" @click="selectedProductIds = []">取消选择</button>
    </div>

    <div class="table-wrap">
      <table class="data-table">
        <thead>
          <tr>
            <th style="width:44px">
              <input type="checkbox" :checked="allPageSelected" @change="toggleSelectAll" />
            </th>
            <th style="width:60px">ID</th>
            <th style="width:80px">图片</th>
            <th>商品名称</th>
            <th style="width:100px">品牌</th>
            <th style="width:100px">价格</th>
            <th style="width:80px">库存</th>
            <th style="width:80px">状态</th>
            <th style="width:160px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in products" :key="row.id">
            <td>
              <input v-model="selectedProductIds" type="checkbox" :value="row.id" />
            </td>
            <td>{{ row.id }}</td>
            <td>
              <div class="thumb-wrap">
              <img
                :src="getProductImages(row)[0] || '/placeholder.svg'"
                class="thumb"
                @click="previewImages(getProductImages(row), 0)"
              />
                <span v-if="getProductImages(row).length > 1" class="thumb-count">{{ getProductImages(row).length }}图</span>
              </div>
            </td>
            <td class="name-cell">
              <span class="product-name">{{ row.name }}</span>
              <span class="product-desc" v-if="row.description">{{ row.description }}</span>
            </td>
            <td>{{ row.brand || '—' }}</td>
            <td class="price">¥{{ row.price }}</td>
            <td>
              <span class="stock" :class="{ low: row.stock < 10 }">
                {{ row.stock }}
              </span>
            </td>
            <td>
              <span class="tag" :class="row.status === 1 ? 'tag-green' : 'tag-gray'">
                {{ row.status === 1 ? '上架' : '下架' }}
              </span>
            </td>
            <td class="actions">
              <button class="btn-icon" title="编辑" @click="openDialog(row)">
                <Edit />
              </button>
              <button
                class="btn-icon"
                :class="row.status === 1 ? 'btn-warn' : 'btn-ok'"
                :title="row.status === 1 ? '下架' : '上架'"
                @click="toggleStatus(row)"
              >
                <TurnOff v-if="row.status === 1" />
                <Open />
              </button>
              <button class="btn-icon btn-danger" title="删除" @click="handleDelete(row)">
                <Delete />
              </button>
            </td>
          </tr>
        </tbody>
      </table>

      <EmptyState
        v-if="!products.length && !loading"
        title="暂无商品"
        description="点击右上角按钮添加第一个商品"
      />
    </div>

    <div class="pagination-wrap" v-if="total > pageSize">
      <div class="pagination-info">
        共 {{ total }} 条记录
      </div>
      <div class="pagination">
        <button :disabled="page <= 1" @click="page--; loadProducts()">上一页</button>
        <span class="page-current">{{ page }} / {{ Math.ceil(total / pageSize) }}</span>
        <button :disabled="page >= Math.ceil(total / pageSize)" @click="page++; loadProducts()">下一页</button>
      </div>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑商品' : '新增商品'"
      width="650px"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入商品名称" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="选择分类" style="width: 100%">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="品牌">
          <el-input v-model="form.brand" placeholder="请输入品牌，如 Apple / 华为 / Nike" maxlength="100" show-word-limit />
        </el-form-item>
        <div class="form-row">
          <el-form-item label="价格" prop="price" class="half">
            <el-input-number v-model="form.price" :min="0" :precision="2" style="width: 100%" />
          </el-form-item>
          <el-form-item label="库存" prop="stock" class="half">
            <el-input-number v-model="form.stock" :min="0" style="width: 100%" />
          </el-form-item>
        </div>
        <el-form-item label="图片">
          <div class="image-upload">
            <el-input v-model="form.imageUrl" placeholder="请输入图片URL" />
            <div class="image-preview" v-if="form.imageUrl">
              <img :src="form.imageUrl" @error="form.imageUrl = ''" />
              <button type="button" class="preview-btn" @click="previewImages([form.imageUrl], 0)">
                <View /> 预览
              </button>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="多图">
          <div class="image-upload">
            <el-input
              v-model="form.images"
              type="textarea"
              :rows="4"
              placeholder="请输入图片URL，每行一张"
            />
            <div class="multi-preview" v-if="formImages.length">
              <div v-for="(img, idx) in formImages" :key="img + idx" class="multi-preview-item">
                <img :src="img" @error="() => {}" @click="previewImages(formImages, idx)" />
                <span>第{{ idx + 1 }}张</span>
              </div>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入商品描述"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch
            v-model="form.status"
            :active-value="1"
            :inactive-value="0"
            active-text="上架"
            inactive-text="下架"
          />
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
      title="删除商品"
      :message="`确定删除商品「${deleteTarget?.name}」吗？`"
      detail="删除后，该商品将从系统中移除"
      type="danger"
      confirmText="删除"
      :loading="deleteLoading"
      @confirm="confirmDelete"
    />

    <ImagePreview
      v-if="previewVisible"
      :images="previewImagesList"
      :initial-index="previewIndex"
      @close="previewVisible = false"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import request from '../../utils/request'
import { ElMessage } from 'element-plus'
import { useCategoryStore } from '../../store/category'
import { Plus, Search, Edit, Delete, TurnOff, Open, View } from '@element-plus/icons-vue'
import EmptyState from '../../components/admin/EmptyState.vue'
import ConfirmModal from '../../components/admin/ConfirmModal.vue'
import ImagePreview from '../../components/admin/ImagePreview.vue'

const categoryStore = useCategoryStore()
const products = ref([])
const categories = computed(() => categoryStore.categories)
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const keyword = ref('')
const categoryId = ref(null)
const brandFilter = ref('')
const statusFilter = ref(null)
const lowStockOnly = ref(false)
const page = ref(1)
const pageSize = 10
const total = ref(0)
const formRef = ref(null)
const selectedProductIds = ref([])

const deleteVisible = ref(false)
const deleteTarget = ref(null)
const deleteLoading = ref(false)

const previewVisible = ref(false)
const previewImagesList = ref([])
const previewIndex = ref(0)

const form = reactive({
  id: null,
  name: '',
  categoryId: null,
  brand: '',
  price: 0,
  stock: 0,
  imageUrl: '',
  images: '',
  description: '',
  status: 1
})

const formImages = computed(() => getProductImages(form))
const allPageSelected = computed(() => products.value.length > 0 && products.value.every(row => selectedProductIds.value.includes(row.id)))

const rules = {
  name: [
    { required: true, message: '请输入商品名称', trigger: 'blur' },
    { min: 1, max: 100, message: '名称长度 1-100 个字符', trigger: 'blur' }
  ],
  categoryId: [
    { required: true, message: '请选择分类', trigger: 'change' }
  ],
  price: [
    { required: true, message: '请输入价格', trigger: 'blur' }
  ]
}

const loadProducts = async () => {
  loading.value = true
  try {
    const res = await request.get('/admin/products', {
      params: {
        page: page.value,
        size: pageSize,
        keyword: keyword.value,
        categoryId: categoryId.value,
        brand: brandFilter.value,
        status: statusFilter.value,
        lowStockOnly: lowStockOnly.value,
        maxStock: 10
      }
    })
    products.value = res.data?.records || res.data || []
    total.value = res.data?.total || products.value.length
    selectedProductIds.value = selectedProductIds.value.filter(id => products.value.some(row => row.id === id))
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const applyFilters = () => {
  page.value = 1
  loadProducts()
}

const toggleSelectAll = (event) => {
  if (event.target.checked) {
    selectedProductIds.value = Array.from(new Set([...selectedProductIds.value, ...products.value.map(row => row.id)]))
  } else {
    const pageIds = new Set(products.value.map(row => row.id))
    selectedProductIds.value = selectedProductIds.value.filter(id => !pageIds.has(id))
  }
}

const loadCategories = () => categoryStore.fetchCategories()

const openDialog = (row) => {
  isEdit.value = !!row
  if (row) {
    Object.assign(form, row)
    form.images = getProductImages(row).join('\n')
  } else {
    Object.assign(form, {
      id: null,
      name: '',
      categoryId: null,
      brand: '',
      price: 0,
      stock: 0,
      imageUrl: '',
      images: '',
      description: '',
      status: 1
    })
  }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const normalizedImages = getProductImages(form)
      const payload = {
        ...form,
        imageUrl: normalizedImages[0] || '',
        images: normalizedImages.join(','),
        imageList: normalizedImages
      }
      if (isEdit.value) {
        await request.put(`/admin/products/${form.id}`, payload)
        ElMessage.success('更新成功')
      } else {
        await request.post('/admin/products', payload)
        ElMessage.success('创建成功')
      }
      dialogVisible.value = false
      loadProducts()
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
    await request.delete(`/admin/products/${deleteTarget.value.id}`)
    ElMessage.success('删除成功')
    deleteVisible.value = false
    loadProducts()
  } catch (e) {
    console.error(e)
  } finally {
    deleteLoading.value = false
  }
}

const toggleStatus = async (row) => {
  const newStatus = row.status === 1 ? 0 : 1
  try {
    await request.put(`/admin/products/${row.id}/status`, null, { params: { status: newStatus } })
    row.status = newStatus
    ElMessage.success(newStatus === 1 ? '已上架' : '已下架')
  } catch (e) {
    console.error(e)
  }
}

const batchUpdateStatus = async (status) => {
  if (!selectedProductIds.value.length) return
  try {
    await request.put('/admin/products/status/batch', selectedProductIds.value, { params: { status } })
    ElMessage.success(status === 1 ? '批量上架成功' : '批量下架成功')
    selectedProductIds.value = []
    loadProducts()
  } catch (e) {
    console.error(e)
  }
}

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

const exportProducts = () => {
  const rows = [
    ['ID', '商品名称', '分类', '品牌', '价格', '库存', '状态', '图片'],
    ...products.value.map(row => [
      row.id,
      row.name,
      categories.value.find(c => c.id === row.categoryId)?.name || row.categoryName || '',
      row.brand || '',
      row.price,
      row.stock,
      row.status === 1 ? '上架' : '下架',
      getProductImages(row).join(' ')
    ])
  ]
  downloadCsv(`products-page-${page.value}.csv`, rows)
}

const previewImages = (images, index) => {
  previewImagesList.value = images?.length ? images : ['/placeholder.svg']
  previewIndex.value = index
  previewVisible.value = true
}

const getProductImages = (product) => {
  const result = []
  const pushImage = (value) => {
    const url = String(value || '').trim()
    if (url && !result.includes(url)) result.push(url)
  }
  if (Array.isArray(product?.imageList)) {
    product.imageList.forEach(pushImage)
  }
  String(product?.images || '')
    .split(/[\n\r,，]+/)
    .forEach(pushImage)
  pushImage(product?.imageUrl)
  return result
}

onMounted(() => {
  loadProducts()
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

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
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

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.search-box {
  position: relative;
  flex: 1;
  min-width: 200px;
  max-width: 280px;
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

.filter-input.compact {
  width: 150px;
  padding-left: 14px;
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

.filter-check {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 0 10px;
  color: #666;
  font-size: 13px;
  user-select: none;
  cursor: pointer;
}

.filter-check input {
  accent-color: #c45c3e;
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
  border: 1px solid #67c23a;
  background: #f0faf4;
  color: #67c23a;
  cursor: pointer;
}

.btn-batch.warn {
  border-color: #e6a23c;
  background: #fef9f0;
  color: #e6a23c;
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

.thumb {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  object-fit: cover;
  background: #f5f5f5;
  cursor: pointer;
  transition: transform 0.2s;
}

.thumb-wrap {
  position: relative;
  width: fit-content;
}

.thumb-count {
  position: absolute;
  right: -8px;
  bottom: -8px;
  padding: 2px 6px;
  border-radius: 999px;
  background: rgba(17, 24, 39, 0.88);
  color: #fff;
  font-size: 10px;
}

.thumb:hover {
  transform: scale(1.1);
}

.name-cell {
  max-width: 300px;
}

.product-name {
  display: block;
  font-weight: 500;
  margin-bottom: 2px;
}

.product-desc {
  display: block;
  font-size: 12px;
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.price {
  color: #f56c6c;
  font-weight: 600;
}

.stock {
  display: inline-block;
  padding: 2px 10px;
  background: #f0f0f0;
  border-radius: 6px;
  font-size: 12px;
}

.stock.low {
  background: #fef0f0;
  color: #f56c6c;
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

.btn-icon.btn-warn:hover {
  border-color: #e6a23c;
  color: #e6a23c;
  background: #fef9f0;
}

.btn-icon.btn-ok:hover {
  border-color: #67c23a;
  color: #67c23a;
  background: #f0faf4;
}

.btn-icon.btn-danger:hover {
  border-color: #f56c6c;
  color: #f56c6c;
  background: #fef0f0;
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

.form-row {
  display: flex;
  gap: 16px;
}

.form-row .half {
  flex: 1;
}

.image-upload {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.image-preview {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #fafaf8;
  border-radius: 8px;
}

.image-preview img {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  object-fit: cover;
}

.multi-preview {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(88px, 1fr));
  gap: 10px;
}

.multi-preview-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  align-items: center;
  padding: 10px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fafaf8;
}

.multi-preview-item img {
  width: 68px;
  height: 68px;
  border-radius: 8px;
  object-fit: cover;
  cursor: pointer;
}

.multi-preview-item span {
  font-size: 12px;
  color: #909399;
}

.preview-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border-radius: 6px;
  border: 1.5px solid #e5e7eb;
  background: #fff;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.preview-btn svg {
  width: 14px;
  height: 14px;
}

.preview-btn:hover {
  border-color: #333;
  color: #333;
}
</style>
