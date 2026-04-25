<template>
  <div class="dashboard">
    <div class="page-title-bar">
      <div>
        <h1>数据概览</h1>
        <p class="subtitle">{{ today }}，平台运营数据</p>
      </div>
      <div class="title-actions">
        <button class="btn-action" @click="$router.push('/admin/products')"><Plus /> 新增商品</button>
        <button class="btn-action ghost" @click="$router.push('/admin/orders')"><Document /> 查看订单</button>
      </div>
    </div>

    <div class="stat-grid">
      <StatsCard label="用户总数" :value="stats.userCount" :trend="12.5" accentColor="#1a1a1a">
        <template #icon><User /></template>
      </StatsCard>
      <StatsCard label="商品总数" :value="stats.productCount" :trend="8.2" accentColor="#5a6e5a">
        <template #icon><Goods /></template>
      </StatsCard>
      <StatsCard label="订单总数" :value="stats.orderCount" :trend="23.1" accentColor="#c45c3e">
        <template #icon><Tickets /></template>
      </StatsCard>
      <StatsCard label="分类总数" :value="stats.categoryCount" accentColor="#b08d57">
        <template #icon><List /></template>
      </StatsCard>
      <StatsCard label="销售额" :value="stats.salesAmount" accentColor="#111827" :formatter="formatCurrency">
        <template #icon><TrendCharts /></template>
      </StatsCard>
      <StatsCard label="收藏总数" :value="stats.favoriteCount" accentColor="#c45c3e" class="clickable-card" @click="$router.push('/admin/favorites')">
        <template #icon><Star /></template>
        <template #footer>查看收藏管理</template>
      </StatsCard>
      <StatsCard label="优惠券总数" :value="stats.couponCount" accentColor="#5a7a9a" class="clickable-card" @click="$router.push('/admin/coupons')">
        <template #icon><Tickets /></template>
        <template #footer>查看优惠券管理</template>
      </StatsCard>
      <StatsCard label="待发货订单" :value="stats.pendingShipmentCount" accentColor="#8b5cf6">
        <template #icon><Document /></template>
      </StatsCard>
    </div>

    <div class="dashboard-grid">
      <div class="panel">
        <div class="panel-header">
          <h3>销售趋势</h3>
          <div class="tab-switch">
            <button v-for="t in timeRanges" :key="t.value" :class="{ active: currentRange === t.value }" @click="changeRange(t.value)">{{ t.label }}</button>
          </div>
        </div>
        <div ref="salesChartRef" class="chart-area"></div>
      </div>
      <div class="panel">
        <div class="panel-header"><h3>订单状态</h3></div>
        <div ref="orderChartRef" class="chart-area"></div>
      </div>
    </div>

    <div class="dashboard-grid secondary">
      <div class="panel">
        <div class="panel-header">
          <h3>最近订单</h3>
          <a class="panel-link" @click="$router.push('/admin/orders')">全部 <ArrowRight /></a>
        </div>
        <div class="order-list">
          <div v-for="order in recentOrders" :key="order.id" class="order-row">
            <div class="order-left">
              <span class="order-no">{{ order.orderNo }}</span>
              <span class="order-time">{{ order.createTime }}</span>
            </div>
            <div class="order-right">
              <span class="order-amount">¥{{ order.totalAmount }}</span>
              <span class="order-status" :class="'s' + order.status">{{ statusText[order.status] }}</span>
            </div>
          </div>
          <EmptyState v-if="!recentOrders.length" title="暂无订单" description="还没有新的订单" />
        </div>
      </div>
      <div class="panel">
        <div class="panel-header"><h3>快捷操作</h3></div>
        <div class="shortcut-grid">
          <div class="shortcut-item" v-for="s in shortcuts" :key="s.label" @click="$router.push(s.path)">
            <component :is="s.icon" class="shortcut-icon" />
            <span>{{ s.label }}</span>
          </div>
        </div>
      </div>
    </div>

    <div class="dashboard-grid tertiary">
      <div class="panel">
        <div class="panel-header"><h3>热销商品</h3></div>
        <div class="rank-list">
          <div v-for="item in hotProducts" :key="item.productId" class="rank-item">
            <img :src="item.productImage || '/placeholder.svg'" class="rank-thumb" />
            <div class="rank-copy">
              <strong>{{ item.productName }}</strong>
              <span>销量 {{ item.sales || 0 }} 件</span>
            </div>
          </div>
          <EmptyState v-if="!hotProducts.length" title="暂无热销商品" description="有订单后将自动生成榜单" />
        </div>
      </div>
      <div class="panel">
        <div class="panel-header"><h3>低库存预警</h3></div>
        <div class="rank-list">
          <div v-for="item in lowStockProducts" :key="item.id" class="rank-item warn">
            <img :src="item.imageUrl || '/placeholder.svg'" class="rank-thumb" />
            <div class="rank-copy">
              <strong>{{ item.name }}</strong>
              <span>剩余库存 {{ item.stock }} 件</span>
            </div>
          </div>
          <EmptyState v-if="!lowStockProducts.length" title="库存健康" description="当前没有低库存商品" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, onUnmounted } from 'vue'
import * as echarts from '../../utils/echarts'
import request from '../../utils/request'
import { User, Goods, Tickets, List, Plus, Document, ArrowRight, TrendCharts, Star } from '@element-plus/icons-vue'
import StatsCard from '../../components/admin/StatsCard.vue'
import EmptyState from '../../components/admin/EmptyState.vue'

const stats = reactive({
  userCount: 0,
  productCount: 0,
  orderCount: 0,
  categoryCount: 0,
  salesAmount: 0,
  favoriteCount: 0,
  couponCount: 0,
  pendingShipmentCount: 0
})
const recentOrders = ref([])
const salesTrend = ref({ labels: [], values: [] })
const orderStatusCounts = ref([0, 0, 0, 0, 0])
const hotProducts = ref([])
const lowStockProducts = ref([])
const statusText = ['待支付', '已支付', '已发货', '已完成', '已取消']
const currentRange = ref('7d')
const timeRanges = [{ label: '近7天', value: '7d' }, { label: '近30天', value: '30d' }]
const salesChartRef = ref(null)
const orderChartRef = ref(null)
let salesChart = null, orderChart = null

const shortcuts = [
  { label: '管理商品', icon: Goods, path: '/admin/products' },
  { label: '处理订单', icon: Tickets, path: '/admin/orders' },
  { label: '管理分类', icon: List, path: '/admin/categories' },
  { label: '管理用户', icon: User, path: '/admin/users' }
]

const today = computed(() => { const n = new Date(); return `${n.getFullYear()}年${n.getMonth()+1}月${n.getDate()}日` })
const formatCurrency = (value) => `¥${Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`

const initSalesChart = () => {
  if (!salesChartRef.value) return
  if (salesChart) salesChart.dispose()
  salesChart = echarts.init(salesChartRef.value)
  const dates = salesTrend.value.labels || []
  const data = salesTrend.value.values || []
  salesChart.setOption({
    tooltip: { trigger: 'axis', backgroundColor: '#fff', borderColor: '#eee', borderWidth: 1, textStyle: { color: '#333', fontSize: 12 }, formatter: '{b}<br/>销售额: ¥{c}' },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '8%', containLabel: true },
    xAxis: { type: 'category', boundaryGap: false, data: dates, axisLine: { lineStyle: { color: '#eee' } }, axisLabel: { color: '#999', fontSize: 11 } },
    yAxis: { type: 'value', axisLine: { show: false }, splitLine: { lineStyle: { color: '#f5f5f3' } }, axisLabel: { color: '#999', fontSize: 11, formatter: v => v >= 1000 ? (v/1000).toFixed(1)+'k' : v } },
    series: [{ type: 'line', smooth: true, symbol: 'circle', symbolSize: 5, lineStyle: { color: '#1a1a1a', width: 2 }, itemStyle: { color: '#1a1a1a' },
      areaStyle: { color: new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:'rgba(26,26,26,0.08)'},{offset:1,color:'rgba(26,26,26,0.01)'}]) }, data }]
  })
}
const initOrderChart = () => {
  if (!orderChartRef.value) return
  if (orderChart) orderChart.dispose()
  orderChart = echarts.init(orderChartRef.value)
  const colors = ['#d4a843', '#5a7a9a', '#999', '#5a6e5a', '#c45c3e']
  orderChart.setOption({
    tooltip: { trigger: 'item', backgroundColor: '#fff', borderColor: '#eee', textStyle: { color: '#333', fontSize: 12 } },
    legend: { orient: 'vertical', right: '5%', top: 'center', itemWidth: 8, itemHeight: 8, textStyle: { color: '#666', fontSize: 12 } },
    series: [{ type: 'pie', radius: ['42%','68%'], center: ['35%','50%'], avoidLabelOverlap: false,
      itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 }, label: { show: false },
      emphasis: { label: { show: true, fontSize: 13, fontWeight: '600' } },
      data: statusText.map((name, index) => ({ value: orderStatusCounts.value[index] || 0, name, itemStyle: { color: colors[index] } })) }]
  })
}
const loadOverview = async () => {
  const days = currentRange.value === '30d' ? 30 : 7
  const r = await request.get('/admin/dashboard/overview', { params: { days } })
  Object.assign(stats, r.data?.stats || {})
  salesTrend.value = r.data?.salesTrend || { labels: [], values: [] }
  orderStatusCounts.value = r.data?.orderStatusCounts || [0, 0, 0, 0, 0]
  recentOrders.value = r.data?.recentOrders || []
  hotProducts.value = r.data?.hotProducts || []
  lowStockProducts.value = r.data?.lowStockProducts || []
}
const changeRange = async (range) => {
  currentRange.value = range
  await loadOverview()
  initSalesChart()
  initOrderChart()
}
let resizeTicking = false
const handleResize = () => {
  if (resizeTicking) return
  resizeTicking = true
  requestAnimationFrame(() => { salesChart?.resize(); orderChart?.resize(); resizeTicking = false })
}
onMounted(async () => { await loadOverview(); initSalesChart(); initOrderChart(); window.addEventListener('resize', handleResize) })
onUnmounted(() => { window.removeEventListener('resize', handleResize); salesChart?.dispose(); orderChart?.dispose() })
</script>

<style scoped>
.page-title-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-title-bar h1 { font-size: 20px; font-weight: 700; color: #1a1a1a; margin: 0; }
.subtitle { font-size: 13px; color: #999; margin: 4px 0 0; }
.title-actions { display: flex; gap: 8px; }
.btn-action { display: flex; align-items: center; gap: 5px; padding: 8px 16px; border-radius: 6px; border: 1px solid #1a1a1a; background: #1a1a1a; color: #fff; font-size: 13px; font-weight: 500; cursor: pointer; transition: opacity 0.15s; }
.btn-action svg { width: 14px; height: 14px; }
.btn-action:hover { opacity: 0.85; }
.btn-action.ghost { background: #fff; color: #333; border-color: #ddd; }
.btn-action.ghost:hover { border-color: #bbb; opacity: 1; }
.stat-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 14px; margin-bottom: 20px; }
.dashboard-grid { display: grid; grid-template-columns: 1fr 360px; gap: 14px; margin-bottom: 14px; }
.dashboard-grid.secondary { grid-template-columns: 1fr 320px; }
.dashboard-grid.tertiary { grid-template-columns: 1fr 1fr; }
.panel { background: #fff; border-radius: 8px; border: 1px solid #eee; overflow: hidden; }
.panel-header { display: flex; justify-content: space-between; align-items: center; padding: 14px 18px; border-bottom: 1px solid #f0f0f0; }
.panel-header h3 { font-size: 14px; font-weight: 600; color: #1a1a1a; margin: 0; }
.tab-switch { display: flex; gap: 2px; background: #f5f5f3; padding: 2px; border-radius: 5px; }
.tab-switch button { padding: 4px 10px; border: none; background: transparent; border-radius: 4px; font-size: 12px; color: #888; cursor: pointer; transition: all 0.15s; }
.tab-switch button.active { background: #fff; color: #1a1a1a; font-weight: 500; box-shadow: 0 1px 2px rgba(0,0,0,0.06); }
.chart-area { height: 260px; padding: 12px; }
.order-list { padding: 4px 0; }
.order-row { display: flex; justify-content: space-between; align-items: center; padding: 12px 18px; border-bottom: 1px solid #f5f5f3; transition: background 0.1s; }
.order-row:last-child { border-bottom: none; }
.order-row:hover { background: #fafaf8; }
.order-left { display: flex; flex-direction: column; gap: 2px; }
.order-no { font-size: 12px; font-weight: 600; color: #333; font-family: 'SF Mono', Monaco, monospace; }
.order-time { font-size: 11px; color: #bbb; }
.order-right { display: flex; flex-direction: column; align-items: flex-end; gap: 2px; }
.order-amount { font-size: 13px; font-weight: 600; color: #c45c3e; }
.order-status { font-size: 11px; font-weight: 500; padding: 2px 8px; border-radius: 3px; }
.order-status.s0 { background: #fef8ee; color: #b08d57; }
.order-status.s1 { background: #eef3f8; color: #5a7a9a; }
.order-status.s2 { background: #f0f0f0; color: #666; }
.order-status.s3 { background: #eef4ee; color: #5a6e5a; }
.order-status.s4 { background: #faf0f0; color: #c45c3e; }
.panel-link { display: flex; align-items: center; gap: 3px; font-size: 12px; color: #999; cursor: pointer; transition: color 0.15s; }
.panel-link svg { width: 12px; height: 12px; }
.panel-link:hover { color: #333; }
.shortcut-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; padding: 14px; }
.shortcut-item { display: flex; flex-direction: column; align-items: center; gap: 8px; padding: 20px 14px; border-radius: 6px; background: #fafaf8; cursor: pointer; transition: background 0.15s; }
.shortcut-item:hover { background: #f2efe9; }
.shortcut-icon { width: 20px; height: 20px; color: #666; }
.shortcut-item span { font-size: 12px; font-weight: 500; color: #555; }
.rank-list { padding: 8px 0; }
.rank-item {
  display: flex; align-items: center; gap: 12px; padding: 12px 18px;
  border-bottom: 1px solid #f5f5f3;
}
.rank-item:last-child { border-bottom: none; }
.rank-item.warn .rank-copy span { color: #c45c3e; }
.rank-thumb {
  width: 52px; height: 52px; border-radius: 10px; object-fit: cover; background: #f5f5f5;
}
.rank-copy {
  display: flex; flex-direction: column; gap: 4px;
}
.rank-copy strong { font-size: 14px; color: #1a1a1a; }
.rank-copy span { font-size: 12px; color: #909399; }
.clickable-card { cursor: pointer; transition: transform 0.15s ease, box-shadow 0.15s ease; }
.clickable-card:hover { transform: translateY(-2px); box-shadow: 0 8px 20px rgba(15, 23, 42, 0.08); }

@media (max-width: 1024px) {
  .dashboard-grid,
  .dashboard-grid.secondary,
  .dashboard-grid.tertiary {
    grid-template-columns: 1fr;
  }
}
</style>
