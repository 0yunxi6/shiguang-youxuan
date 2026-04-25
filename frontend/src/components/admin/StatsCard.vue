<template>
  <div class="stats-card" :style="{ '--accent': accentColor }">
    <div class="stats-header">
      <span class="stats-label">{{ label }}</span>
      <div class="stats-trend" v-if="trend !== null" :class="trendClass">
        <component :is="trend >= 0 ? ArrowUp : ArrowDown" />
        {{ Math.abs(trend) }}%
      </div>
    </div>
    <div class="stats-body">
      <span class="stats-value">{{ displayValue }}</span>
      <div class="stats-icon"><slot name="icon" /></div>
    </div>
    <div class="stats-footer" v-if="$slots.footer"><slot name="footer" /></div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { ArrowUp, ArrowDown } from '@element-plus/icons-vue'

const props = defineProps({
  label: { type: String, default: '' },
  value: { type: [Number, String], default: 0 },
  trend: { type: Number, default: null },
  accentColor: { type: String, default: '#1a1a1a' },
  formatter: { type: Function, default: null }
})

const displayValue = computed(() => {
  if (props.formatter) return props.formatter(props.value)
  if (typeof props.value === 'number') {
    if (props.value >= 10000) return (props.value / 10000).toFixed(1) + 'w'
    return props.value.toLocaleString()
  }
  return props.value
})
const trendClass = computed(() => props.trend >= 0 ? 'up' : 'down')
</script>

<style scoped>
.stats-card {
  background: #fff; border-radius: 8px; padding: 18px 20px;
  border: 1px solid #eee; position: relative; overflow: hidden;
}
.stats-card::before {
  content: ''; position: absolute; top: 0; left: 0;
  width: 3px; height: 100%; background: var(--accent);
}
.stats-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.stats-label { font-size: 12px; color: #999; font-weight: 500; }
.stats-trend { display: flex; align-items: center; gap: 2px; font-size: 11px; font-weight: 600; padding: 2px 6px; border-radius: 3px; }
.stats-trend svg { width: 10px; height: 10px; }
.stats-trend.up { background: #eef4ee; color: #5a6e5a; }
.stats-trend.down { background: #faf0f0; color: #c45c3e; }
.stats-body { display: flex; justify-content: space-between; align-items: flex-end; }
.stats-value { font-size: 26px; font-weight: 700; color: #1a1a1a; line-height: 1; }
.stats-icon {
  width: 38px; height: 38px; border-radius: 8px;
  background: color-mix(in srgb, var(--accent) 8%, transparent);
  display: flex; align-items: center; justify-content: center;
}
.stats-icon svg { width: 18px; height: 18px; color: var(--accent); }
.stats-footer { margin-top: 10px; padding-top: 10px; border-top: 1px solid #f0f0f0; font-size: 11px; color: #999; }
</style>
