<template>
  <div class="search-bar">
    <el-input v-model="keyword" :placeholder="placeholder" clearable @keyup.enter="handleSearch" size="large">
      <template #append>
        <el-button :icon="Search" @click="handleSearch" />
      </template>
    </el-input>
    <div v-if="displayHotKeywords.length" class="hot-keywords">
      <span class="label">热搜：</span>
      <el-tag v-for="kw in displayHotKeywords" :key="kw" size="small" effect="plain" class="hot-tag" @click="searchByKeyword(kw)">
        {{ kw }}
      </el-tag>
    </div>
    <div v-if="searchHistory.length" class="hot-keywords">
      <span class="label">最近：</span>
      <el-tag v-for="kw in searchHistory" :key="kw" size="small" effect="plain" class="hot-tag" @click="searchByKeyword(kw)">
        {{ kw }}
      </el-tag>
      <button class="clear-history" type="button" @click="clearHistory">清空</button>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { getHotSearchKeywords } from '../api'

const props = defineProps({
  placeholder: { type: String, default: '搜索商品...' },
  hotKeywords: { type: Array, default: () => ['手机', '笔记本', '耳机', '平板'] }
})

const router = useRouter()
const keyword = ref('')
const searchHistory = ref([])
const loadedHotKeywords = ref([])
const SEARCH_HISTORY_KEY = 'searchKeywordHistory'
const displayHotKeywords = computed(() => loadedHotKeywords.value.length ? loadedHotKeywords.value : props.hotKeywords)

const loadHistory = () => {
  try {
    searchHistory.value = JSON.parse(localStorage.getItem(SEARCH_HISTORY_KEY) || '[]').slice(0, 6)
  } catch (error) {
    searchHistory.value = []
  }
}

const saveHistory = (kw) => {
  const text = String(kw || '').trim()
  if (!text) return
  const next = [text, ...searchHistory.value.filter(item => item !== text)].slice(0, 8)
  searchHistory.value = next
  localStorage.setItem(SEARCH_HISTORY_KEY, JSON.stringify(next))
}

const handleSearch = () => {
  if (keyword.value.trim()) {
    saveHistory(keyword.value.trim())
    router.push({ path: '/search', query: { keyword: keyword.value.trim() } })
  }
}

const searchByKeyword = (kw) => {
  keyword.value = kw
  handleSearch()
}

const clearHistory = () => {
  searchHistory.value = []
  localStorage.removeItem(SEARCH_HISTORY_KEY)
}

const normalizeHotKeywords = (items) => (items || [])
  .map(item => typeof item === 'string' ? item : item?.keyword)
  .filter(Boolean)

const loadHotKeywords = async () => {
  try {
    const res = await getHotSearchKeywords({ limit: 8 })
    loadedHotKeywords.value = normalizeHotKeywords(res.data).slice(0, 8)
  } catch (error) {
    loadedHotKeywords.value = []
  }
}

onMounted(() => {
  loadHistory()
  loadHotKeywords()
})
</script>

<style scoped>
.search-bar { max-width: 600px; }
.hot-keywords { margin-top: 8px; display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
.label { font-size: 12px; color: #909399; }
.hot-tag { cursor: pointer; }
.hot-tag:hover { color: #c45c3e; border-color: #c45c3e; }
.clear-history { border: none; background: transparent; color: #909399; font-size: 12px; cursor: pointer; }
.clear-history:hover { color: #c45c3e; }
</style>
