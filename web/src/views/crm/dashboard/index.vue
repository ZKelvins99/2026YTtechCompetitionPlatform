<template>
  <div class="crm-dashboard">
    <div class="dashboard-header">
      <h2>CRM 数据监控大屏</h2>
      <span class="refresh-tip">每 10 秒自动刷新</span>
    </div>

    <div class="dashboard-grid">
      <div class="panel gauge-panel" ref="cpuGaugeRef"></div>
      <div class="panel gauge-panel" ref="memGaugeRef"></div>
      <div class="panel stat-panel">
        <div class="stat-title">今日操作总次数</div>
        <div class="stat-value">{{ logStats.todayTotal || 0 }}</div>
        <div class="stat-trend" :class="trendClass">
          <el-icon><component :is="trendIcon" /></el-icon>
          较昨日 {{ Math.abs(logStats.todayTrend || 0) }}%
        </div>
      </div>
      <div class="panel stat-panel">
        <div class="stat-title">异常次数 / 异常率</div>
        <div class="stat-value">{{ logStats.errorCount || 0 }} <small>/ {{ logStats.errorRate || 0 }}%</small></div>
        <div class="mini-chart" ref="errorChartRef"></div>
      </div>
      <div class="panel chart-panel" ref="barChartRef"></div>
      <div class="panel chart-panel" ref="pieChartRef"></div>
    </div>

    <div class="panel log-panel" @mouseenter="paused = true" @mouseleave="paused = false">
      <div class="panel-title">最近操作日志（实时滚动）</div>
      <div class="log-scroll-wrap" ref="logScrollRef">
        <el-table :data="logStats.recentLogs" size="small" :show-header="true" class="dark-table">
          <el-table-column label="操作人" prop="operator" width="100" />
          <el-table-column label="请求地址" prop="requestUrl" show-overflow-tooltip />
          <el-table-column label="方式" prop="requestMethod" width="70" align="center" />
          <el-table-column label="耗时(ms)" prop="costMillis" width="90" align="center" />
          <el-table-column label="状态" width="70" align="center">
            <template #default="scope">
              <el-tag :type="scope.row.status === '0' ? 'success' : 'danger'" size="small">
                {{ scope.row.status === '0' ? '成功' : '失败' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="时间" prop="operateTime" width="160">
            <template #default="scope">{{ parseTime(scope.row.operateTime) }}</template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup name="CrmDashboard">
import * as echarts from 'echarts'
import { useIntervalFn } from '@vueuse/core'
import { ArrowDown, ArrowUp, Minus } from '@element-plus/icons-vue'
import { getSystemStatus, getLogStats } from '@/api/crm/dashboard'

const cpuGaugeRef = ref(null)
const memGaugeRef = ref(null)
const barChartRef = ref(null)
const pieChartRef = ref(null)
const errorChartRef = ref(null)
const logScrollRef = ref(null)

const systemStatus = ref({})
const logStats = ref({ recentLogs: [], hourlyStats: [], topUsers: [], moduleStats: [] })

let cpuChart, memChart, barChart, pieChart, errorChart
let rafId = null
let paused = false

const trendClass = computed(() => {
  const t = logStats.value.todayTrend || 0
  return t > 0 ? 'up' : t < 0 ? 'down' : 'flat'
})
const trendIcon = computed(() => {
  const t = logStats.value.todayTrend || 0
  return t > 0 ? ArrowUp : t < 0 ? ArrowDown : Minus
})

function buildGauge(el, title, value, color) {
  const chart = echarts.init(el, 'dark')
  chart.setOption({
    series: [{
      type: 'gauge', startAngle: 200, endAngle: -20, min: 0, max: 100,
      progress: { show: true, width: 14 },
      axisLine: { lineStyle: { width: 14 } },
      axisTick: { show: false }, splitLine: { show: false },
      axisLabel: { show: false }, pointer: { show: false },
      title: { offsetCenter: [0, '70%'], fontSize: 14, color: '#aaa' },
      detail: { valueAnimation: true, fontSize: 28, offsetCenter: [0, '0%'], formatter: '{value}%', color: '#fff' },
      data: [{ value: value || 0, name: title, itemStyle: { color } }]
    }]
  })
  return chart
}

function refreshCharts() {
  if (cpuChart) cpuChart.setOption({ series: [{ data: [{ value: systemStatus.value.cpuUsage || 0 }] }] })
  if (memChart) memChart.setOption({ series: [{ data: [{ value: systemStatus.value.memUsage || 0 }] }] })

  if (barChart && logStats.value.topUsers) {
    barChart.setOption({
      title: { text: '活跃用户 TOP5', left: 'center', textStyle: { color: '#ccc', fontSize: 14 } },
      grid: { left: 60, right: 20, top: 40, bottom: 30 },
      xAxis: { type: 'category', data: logStats.value.topUsers.map(u => u.name), axisLabel: { color: '#aaa' } },
      yAxis: { type: 'value', axisLabel: { color: '#aaa' }, splitLine: { lineStyle: { color: '#333' } } },
      series: [{ type: 'bar', data: logStats.value.topUsers.map(u => u.value), itemStyle: { color: '#409eff' } }]
    })
  }

  if (pieChart && logStats.value.moduleStats) {
    pieChart.setOption({
      title: { text: '模块调用分布', left: 'center', textStyle: { color: '#ccc', fontSize: 14 } },
      tooltip: { trigger: 'item' },
      series: [{
        type: 'pie', radius: ['35%', '65%'], center: ['50%', '55%'],
        data: logStats.value.moduleStats.map(m => ({ name: m.name, value: m.value })),
        label: { color: '#ccc' }
      }]
    })
  }

  if (errorChart && logStats.value.hourlyStats) {
    errorChart.setOption({
      grid: { left: 0, right: 0, top: 5, bottom: 0 },
      xAxis: { type: 'category', show: false, data: logStats.value.hourlyStats.map(h => h.hour) },
      yAxis: { type: 'value', show: false },
      series: [{ type: 'line', smooth: true, symbol: 'none', data: logStats.value.hourlyStats.map(h => h.count), lineStyle: { color: '#f56c6c' }, areaStyle: { color: 'rgba(245,108,108,0.2)' } }]
    })
  }
}

async function loadData() {
  const [sysRes, logRes] = await Promise.all([getSystemStatus(), getLogStats()])
  systemStatus.value = sysRes.data
  logStats.value = logRes.data
  refreshCharts()
}

function initCharts() {
  cpuChart = buildGauge(cpuGaugeRef.value, 'CPU 使用率', 0, '#67c23a')
  memChart = buildGauge(memGaugeRef.value, '内存使用率', 0, '#409eff')
  barChart = echarts.init(barChartRef.value, 'dark')
  pieChart = echarts.init(pieChartRef.value, 'dark')
  errorChart = echarts.init(errorChartRef.value, 'dark')
}


function startLogScroll() {
  const wrap = logScrollRef.value
  if (!wrap) return

  function step() {
    if (!paused) {
      wrap.scrollTop += 0.8
      if (wrap.scrollTop >= wrap.scrollHeight - wrap.clientHeight) {
        wrap.scrollTop = 0
      }
    }
    rafId = requestAnimationFrame(step)
  }
  rafId = requestAnimationFrame(step)
}

function stopLogScroll() {
  if (rafId) {
    cancelAnimationFrame(rafId)
    rafId = null
  }
}

useIntervalFn(loadData, 10000)

onMounted(() => {
  initCharts()
  loadData()
  startLogScroll()
  window.addEventListener('resize', () => {
    cpuChart?.resize(); memChart?.resize(); barChart?.resize(); pieChart?.resize(); errorChart?.resize()
  })
})

onBeforeUnmount(() => {
  stopLogScroll()
  cpuChart?.dispose(); memChart?.dispose(); barChart?.dispose(); pieChart?.dispose(); errorChart?.dispose()
})
</script>

<style scoped>
.crm-dashboard {
  min-height: calc(100vh - 84px);
  background: linear-gradient(180deg, #0d1b2a 0%, #1b263b 100%);
  padding: 16px;
  color: #e0e0e0;
}
.dashboard-header { display: flex; align-items: baseline; gap: 16px; margin-bottom: 16px; }
.dashboard-header h2 { margin: 0; font-size: 22px; color: #fff; }
.refresh-tip { font-size: 12px; color: #888; }
.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  grid-template-rows: 220px 220px;
  gap: 12px;
}
.panel { background: rgba(255,255,255,0.05); border-radius: 8px; border: 1px solid rgba(255,255,255,0.08); }
.gauge-panel, .chart-panel { min-height: 220px; }
.stat-panel { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 16px; }
.stat-title { font-size: 14px; color: #aaa; margin-bottom: 8px; }
.stat-value { font-size: 36px; font-weight: bold; color: #fff; }
.stat-value small { font-size: 18px; color: #f56c6c; }
.stat-trend { margin-top: 8px; font-size: 13px; display: flex; align-items: center; gap: 4px; }
.stat-trend.up { color: #67c23a; }
.stat-trend.down { color: #f56c6c; }
.stat-trend.flat { color: #909399; }
.mini-chart { width: 100%; height: 60px; margin-top: 8px; }
.log-panel { margin-top: 12px; padding: 12px; }
.panel-title { font-size: 14px; margin-bottom: 8px; color: #ccc; }
.log-scroll-wrap { height: 200px; overflow: hidden; }
:deep(.dark-table) { background: transparent; --el-table-bg-color: transparent; --el-table-tr-bg-color: transparent; --el-table-header-bg-color: rgba(255,255,255,0.05); --el-table-text-color: #ccc; --el-table-header-text-color: #aaa; --el-table-border-color: rgba(255,255,255,0.08); }
</style>
