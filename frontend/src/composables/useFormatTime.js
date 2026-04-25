export function formatTime(time) {
  if (!time) return '-'
  return time.split(' ')[0]
}

export function formatDateTime(time) {
  if (!time) return '-'
  return time
}

export function formatRelativeTime(time) {
  if (!time) return '-'
  const now = new Date()
  const target = new Date(time)
  const diff = now - target
  const minutes = Math.floor(diff / 60000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}小时前`
  const days = Math.floor(hours / 24)
  if (days < 30) return `${days}天前`
  return time.split(' ')[0]
}
