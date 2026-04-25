<template>
  <div class="search-bar">
    <el-input v-model="keyword" :placeholder="placeholder" clearable @keyup.enter="handleSearch" size="large">
      <template #append>
        <el-button :icon="Search" @click="handleSearch" />
      </template>
    </el-input>
    <div v-if="hotKeywords.length" class="hot-keywords">
      <span class="label">热搜：</span>
      <el-tag v-for="kw in hotKeywords" :key="kw" size="small" effect="plain" class="hot-tag" @click="searchByKeyword(kw)">
        {{ kw }}
      </el-tag>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'

const props = defineProps({
  placeholder: { type: String, default: '搜索商品...' },
  hotKeywords: { type: Array, default: () => ['手机', '笔记本', '耳机', '平板'] }
})

const router = useRouter()
const keyword = ref('')

const handleSearch = () => {
  if (keyword.value.trim()) {
    router.push({ path: '/search', query: { keyword: keyword.value.trim() } })
  }
}

const searchByKeyword = (kw) => {
  keyword.value = kw
  handleSearch()
}
</script>

<style scoped>
.search-bar { max-width: 600px; }
.hot-keywords { margin-top: 8px; display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
.label { font-size: 12px; color: #909399; }
.hot-tag { cursor: pointer; }
.hot-tag:hover { color: #c45c3e; border-color: #c45c3e; }
</style>
