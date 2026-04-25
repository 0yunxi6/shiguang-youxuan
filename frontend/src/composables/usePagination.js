import { computed } from 'vue'

export function useVisiblePages(currentPage, totalItems, pageSize) {
  const totalPages = computed(() => Math.ceil(totalItems.value / pageSize))

  const visiblePages = computed(() => {
    const total = totalPages.value
    const page = currentPage.value
    const pages = []
    if (total <= 7) {
      for (let i = 1; i <= total; i++) pages.push(i)
    } else {
      if (page <= 3) {
        for (let i = 1; i <= 5; i++) pages.push(i)
        pages.push('...')
        pages.push(total)
      } else if (page >= total - 2) {
        pages.push(1)
        pages.push('...')
        for (let i = total - 4; i <= total; i++) pages.push(i)
      } else {
        pages.push(1)
        pages.push('...')
        for (let i = page - 1; i <= page + 1; i++) pages.push(i)
        pages.push('...')
        pages.push(total)
      }
    }
    return pages
  })

  return { totalPages, visiblePages }
}
