import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getCategories } from '../api'

export const useCategoryStore = defineStore('category', () => {
  const categories = ref([])
  const loaded = ref(false)
  const loading = ref(false)

  const fetchCategories = async (force = false) => {
    if (loaded.value && !force) return categories.value
    if (loading.value) return categories.value

    loading.value = true
    try {
      const res = await getCategories()
      categories.value = res.data || []
      loaded.value = true
    } catch (e) {
      console.error('Failed to load categories:', e)
    } finally {
      loading.value = false
    }
    return categories.value
  }

  return { categories, loaded, loading, fetchCategories }
})
