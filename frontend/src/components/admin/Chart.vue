<template>
  <div class="chart-container">
    <div class="chart-header" v-if="title">
      <h3 class="chart-title">{{ title }}</h3>
      <div class="chart-actions" v-if="$slots.actions">
        <slot name="actions" />
      </div>
    </div>
    <div ref="chartRef" class="chart-body"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import * as echarts from '../../utils/echarts'

const props = defineProps({
  title: { type: String, default: '' },
  option: { type: Object, required: true },
  height: { type: String, default: '300px' }
})

const chartRef = ref(null)
let chartInstance = null

const initChart = () => {
  if (!chartRef.value) return

  chartInstance = echarts.init(chartRef.value)
  chartInstance.setOption(props.option)

  window.addEventListener('resize', handleResize)
}

let resizeTicking = false
const handleResize = () => {
  if (resizeTicking) return
  resizeTicking = true
  requestAnimationFrame(() => { chartInstance?.resize(); resizeTicking = false })
}

const updateChart = () => {
  if (chartInstance) {
    chartInstance.setOption(props.option, { notMerge: true })
  }
}

watch(() => props.option, updateChart, { deep: true })

onMounted(async () => {
  await nextTick()
  initChart()
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance?.dispose()
  chartInstance = null
})
</script>

<style scoped>
.chart-container {
  background: #fff;
  border-radius: 14px;
  border: 1px solid #ebeef5;
  padding: 20px;
  height: 100%;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.chart-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0;
}

.chart-body {
  width: 100%;
  height: v-bind(height);
}
</style>
