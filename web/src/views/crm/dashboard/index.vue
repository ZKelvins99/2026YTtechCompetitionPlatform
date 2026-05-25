<template>
  <div class="crm-dashboard relative min-h-[calc(100vh-84px)] overflow-hidden bg-gradient-to-b from-slate-50 via-white to-slate-100/80 px-4 py-5 text-slate-700">
    <div class="pointer-events-none absolute inset-0">
      <div class="absolute -right-20 top-0 h-72 w-72 rounded-full bg-sky-100/80 blur-3xl" />
      <div class="absolute bottom-0 left-10 h-64 w-64 rounded-full bg-indigo-100/60 blur-3xl" />
    </div>

    <div class="relative z-10">
      <header class="mb-5 flex flex-wrap items-center justify-between gap-4">
        <div>
          <p class="mb-1 text-xs font-medium tracking-wide text-brand-600">Operations Monitor</p>
          <h2 class="m-0 text-2xl font-bold tracking-tight text-slate-800 sm:text-3xl">CRM 数据监控大屏</h2>
        </div>
        <div class="flex items-center gap-3">
          <span class="inline-flex items-center gap-2 rounded-full border border-emerald-200 bg-emerald-50 px-4 py-1.5 text-xs font-medium text-emerald-700">
            <span class="relative flex h-2 w-2">
              <span class="absolute inline-flex h-full w-full animate-ping rounded-full bg-emerald-400 opacity-50" />
              <span class="relative inline-flex h-2 w-2 rounded-full bg-emerald-500" />
            </span>
            每 10 秒自动刷新
          </span>
          <span class="hidden text-xs text-slate-400 sm:inline">{{ lastRefreshText }}</span>
        </div>
      </header>

      <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-3">
        <section class="dash-card min-h-[240px]">
          <div class="dash-card__inner flex min-h-[232px] flex-col p-3">
            <h3 class="dash-card__title">CPU 使用率</h3>
            <div class="min-h-0 flex-1" ref="cpuGaugeRef" />
          </div>
        </section>
        <section class="dash-card min-h-[240px]">
          <div class="dash-card__inner flex min-h-[232px] flex-col p-3">
            <h3 class="dash-card__title">内存使用率</h3>
            <div class="min-h-0 flex-1" ref="memGaugeRef" />
          </div>
        </section>
        <section class="dash-card sm:col-span-2 xl:col-span-1">
          <div class="dash-card__inner flex min-h-[232px] flex-col items-center justify-center p-6 text-center">
            <p class="text-sm text-slate-500">今日操作总次数</p>
            <p class="mt-2 font-mono text-5xl font-bold tabular-nums text-slate-800">{{ logStats.todayTotal ?? 0 }}</p>
            <p
              class="mt-3 inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-sm font-medium"
              :class="trendClass === 'up' ? 'bg-emerald-50 text-emerald-700' : trendClass === 'down' ? 'bg-rose-50 text-rose-600' : 'bg-slate-100 text-slate-500'"
            >
              <el-icon><component :is="trendIcon" /></el-icon>
              较昨日 {{ Math.abs(logStats.todayTrend || 0) }}%
            </p>
          </div>
        </section>
        <section class="dash-card sm:col-span-2 xl:col-span-1">
          <div class="dash-card__inner flex min-h-[232px] flex-col items-center justify-center p-6">
            <p class="text-sm text-slate-500">异常次数 / 异常率</p>
            <p class="mt-2 font-mono text-4xl font-bold text-slate-800">
              {{ logStats.errorCount ?? 0 }}
              <span class="text-xl font-semibold text-rose-500">/ {{ logStats.errorRate ?? 0 }}%</span>
            </p>
            <div class="mt-3 h-14 w-full max-w-xs" ref="errorChartRef" />
          </div>
        </section>
        <section class="dash-card min-h-[240px]">
          <div class="dash-card__inner min-h-[232px] p-2" ref="barChartRef" />
        </section>
        <section class="dash-card min-h-[240px]">
          <div class="dash-card__inner min-h-[232px] p-2" ref="pieChartRef" />
        </section>
      </div>

      <section class="log-panel mt-4">
        <div class="log-panel__header">
          <div class="flex items-center gap-2">
            <span class="log-panel__dot" />
            <h3 class="m-0 text-base font-semibold text-slate-800">最近操作日志</h3>
            <span class="rounded-md bg-sky-50 px-2 py-0.5 text-xs font-medium text-sky-700">实时滚动</span>
          </div>
          <span class="text-xs text-slate-400">悬停暂停 · {{ logCount }} 条</span>
        </div>

        <div class="log-table">
          <div class="log-table__head grid-cols-log">
            <span>操作人</span>
            <span>请求地址</span>
            <span class="text-center">方式</span>
            <span class="text-center">耗时</span>
            <span class="text-center">状态</span>
            <span>时间</span>
          </div>

          <div
            class="log-table__viewport"
            @mouseenter="scrollPaused = true"
            @mouseleave="scrollPaused = false"
          >
            <div v-if="!logList.length" class="log-table__empty">暂无操作日志</div>
            <div
              v-else
              class="log-table__track"
              :class="{ 'is-paused': scrollPaused }"
              :style="{ '--scroll-duration': `${scrollDuration}s` }"
            >
              <div class="log-table__chunk">
                <div
                  v-for="(row, idx) in logList"
                  :key="'a-' + idx"
                  class="log-table__row grid-cols-log"
                >
                  <span class="truncate font-medium text-slate-700">{{ row.operator || '-' }}</span>
                  <span class="truncate text-slate-500" :title="row.requestUrl">{{ row.requestUrl || '-' }}</span>
                  <span class="text-center">
                    <span class="log-method">{{ row.requestMethod || '-' }}</span>
                  </span>
                  <span class="text-center font-mono text-xs text-slate-500">{{ row.costMillis ?? '-' }}</span>
                  <span class="flex justify-center">
                    <span class="log-status" :class="row.status === '0' ? 'is-ok' : 'is-fail'">
                      {{ row.status === '0' ? '成功' : '失败' }}
                    </span>
                  </span>
                  <span class="truncate font-mono text-xs text-slate-400">{{ formatLogTime(row.operateTime) }}</span>
                </div>
              </div>
              <div class="log-table__chunk" aria-hidden="true">
                <div
                  v-for="(row, idx) in logList"
                  :key="'b-' + idx"
                  class="log-table__row grid-cols-log"
                >
                  <span class="truncate font-medium text-slate-700">{{ row.operator || '-' }}</span>
                  <span class="truncate text-slate-500" :title="row.requestUrl">{{ row.requestUrl || '-' }}</span>
                  <span class="text-center">
                    <span class="log-method">{{ row.requestMethod || '-' }}</span>
                  </span>
                  <span class="text-center font-mono text-xs text-slate-500">{{ row.costMillis ?? '-' }}</span>
                  <span class="flex justify-center">
                    <span class="log-status" :class="row.status === '0' ? 'is-ok' : 'is-fail'">
                      {{ row.status === '0' ? '成功' : '失败' }}
                    </span>
                  </span>
                  <span class="truncate font-mono text-xs text-slate-400">{{ formatLogTime(row.operateTime) }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup name="CrmDashboard">
import * as echarts from 'echarts'
import { useIntervalFn } from '@vueuse/core'
import { ArrowDown, ArrowUp, Minus } from '@element-plus/icons-vue'
import { getSystemStatus, getLogStats } from '@/api/crm/dashboard'
import { parseTime } from '@/utils/ruoyi'

const cpuGaugeRef = ref(null)
const memGaugeRef = ref(null)
const barChartRef = ref(null)
const pieChartRef = ref(null)
const errorChartRef = ref(null)

const systemStatus = ref({})
const logStats = ref({ recentLogs: [], hourlyStats: [], topUsers: [], moduleStats: [] })
const scrollPaused = ref(false)
const lastRefreshAt = ref(Date.now())

let cpuChart, memChart, barChart, pieChart, errorChart

const logList = computed(() => logStats.value.recentLogs || [])
const logCount = computed(() => logList.value.length)
const scrollDuration = computed(() => {
  const n = logCount.value
  if (n <= 1) return 12
  return Math.max(18, n * 2.8)
})

const lastRefreshText = computed(() => {
  const d = new Date(lastRefreshAt.value)
  return `更新于 ${d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })}`
})

const trendClass = computed(() => {
  const t = logStats.value.todayTrend || 0
  return t > 0 ? 'up' : t < 0 ? 'down' : 'flat'
})
const trendIcon = computed(() => {
  const t = logStats.value.todayTrend || 0
  return t > 0 ? ArrowUp : t < 0 ? ArrowDown : Minus
})

function formatLogTime(time) {
  return time ? parseTime(time) : '-'
}

function buildGauge(el, value, color, trackColor) {
  const chart = echarts.init(el)
  chart.setOption({
    series: [{
      type: 'gauge',
      startAngle: 200,
      endAngle: -20,
      min: 0,
      max: 100,
      progress: { show: true, width: 12, itemStyle: { color } },
      axisLine: { lineStyle: { width: 12, color: [[1, trackColor]] } },
      axisTick: { show: false },
      splitLine: { show: false },
      axisLabel: { show: false },
      pointer: { show: false },
      title: { show: false },
      detail: {
        valueAnimation: true,
        fontSize: 32,
        fontWeight: 700,
        offsetCenter: [0, '0%'],
        formatter: '{value}%',
        color: '#1e293b'
      },
      data: [{ value: value || 0 }]
    }]
  })
  return chart
}

function refreshCharts() {
  if (cpuChart) {
    cpuChart.setOption({ series: [{ data: [{ value: systemStatus.value.cpuUsage || 0 }] }] })
  }
  if (memChart) {
    memChart.setOption({ series: [{ data: [{ value: systemStatus.value.memUsage || 0 }] }] })
  }

  if (barChart && logStats.value.topUsers?.length) {
    barChart.setOption({
      backgroundColor: 'transparent',
      title: { text: '活跃用户 TOP5', left: 12, top: 8, textStyle: { color: '#475569', fontSize: 13, fontWeight: 600 } },
      grid: { left: 52, right: 16, top: 44, bottom: 28 },
      xAxis: {
        type: 'category',
        data: logStats.value.topUsers.map(u => u.name),
        axisLabel: { color: '#64748b', fontSize: 11 },
        axisLine: { lineStyle: { color: '#e2e8f0' } }
      },
      yAxis: {
        type: 'value',
        axisLabel: { color: '#64748b', fontSize: 11 },
        splitLine: { lineStyle: { color: '#f1f5f9' } }
      },
      series: [{
        type: 'bar',
        barWidth: '50%',
        data: logStats.value.topUsers.map(u => u.value),
        itemStyle: {
          borderRadius: [6, 6, 0, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#60a5fa' },
            { offset: 1, color: '#2563eb' }
          ])
        }
      }]
    })
  }

  if (pieChart && logStats.value.moduleStats?.length) {
    pieChart.setOption({
      backgroundColor: 'transparent',
      title: { text: '模块调用分布', left: 12, top: 8, textStyle: { color: '#475569', fontSize: 13, fontWeight: 600 } },
      tooltip: { trigger: 'item' },
      color: ['#3b82f6', '#6366f1', '#10b981', '#f59e0b', '#ec4899', '#94a3b8'],
      series: [{
        type: 'pie',
        radius: ['42%', '68%'],
        center: ['50%', '58%'],
        data: logStats.value.moduleStats.map(m => ({ name: m.name, value: m.value })),
        label: { color: '#64748b', fontSize: 11 },
        itemStyle: { borderColor: '#fff', borderWidth: 2 }
      }]
    })
  }

  if (errorChart && logStats.value.hourlyStats?.length) {
    errorChart.setOption({
      grid: { left: 4, right: 4, top: 4, bottom: 4 },
      xAxis: { type: 'category', show: false, data: logStats.value.hourlyStats.map(h => h.hour) },
      yAxis: { type: 'value', show: false },
      series: [{
        type: 'line',
        smooth: true,
        symbol: 'none',
        data: logStats.value.hourlyStats.map(h => h.count),
        lineStyle: { color: '#f43f5e', width: 2 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(244, 63, 94, 0.25)' },
            { offset: 1, color: 'rgba(244, 63, 94, 0)' }
          ])
        }
      }]
    })
  }
}

async function loadData() {
  const [sysRes, logRes] = await Promise.all([getSystemStatus(), getLogStats()])
  systemStatus.value = sysRes.data
  logStats.value = logRes.data
  lastRefreshAt.value = Date.now()
  refreshCharts()
}

function initCharts() {
  cpuChart = buildGauge(cpuGaugeRef.value, 0, '#10b981', '#e2e8f0')
  memChart = buildGauge(memGaugeRef.value, 0, '#3b82f6', '#e2e8f0')
  barChart = echarts.init(barChartRef.value)
  pieChart = echarts.init(pieChartRef.value)
  errorChart = echarts.init(errorChartRef.value)
}

function handleResize() {
  cpuChart?.resize()
  memChart?.resize()
  barChart?.resize()
  pieChart?.resize()
  errorChart?.resize()
}

useIntervalFn(loadData, 10000)

onMounted(() => {
  initCharts()
  loadData()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  cpuChart?.dispose()
  memChart?.dispose()
  barChart?.dispose()
  pieChart?.dispose()
  errorChart?.dispose()
})
</script>

<style scoped>
.crm-dashboard {
  --log-row-h: 44px;
  --log-viewport-h: calc(var(--log-row-h) * 5);
}

.dash-card {
  border-radius: 1rem;
  border: 1px solid #e2e8f0;
  background: #fff;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.06);
  transition: box-shadow 0.2s ease, border-color 0.2s ease;
}
.dash-card:hover {
  border-color: #bfdbfe;
  box-shadow: 0 8px 24px rgba(37, 99, 235, 0.08);
}
.dash-card__inner {
  height: 100%;
  border-radius: inherit;
}
.dash-card__title {
  margin: 0 0 4px 8px;
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
}

.log-panel {
  border-radius: 1rem;
  border: 1px solid #e2e8f0;
  background: #fff;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.06);
}
.log-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 18px;
  border-bottom: 1px solid #f1f5f9;
  background: linear-gradient(180deg, #f8fafc 0%, #fff 100%);
}
.log-panel__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #0ea5e9;
  box-shadow: 0 0 0 3px rgba(14, 165, 233, 0.2);
  animation: pulse-dot 2s ease-in-out infinite;
}
@keyframes pulse-dot {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.55; }
}

.log-table__head,
.log-table__row {
  display: grid;
  align-items: center;
  gap: 8px;
  padding: 0 18px;
}
.grid-cols-log {
  grid-template-columns: 88px minmax(0, 1fr) 56px 72px 64px 148px;
}

.log-table__head {
  height: 40px;
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}

.log-table__viewport {
  height: var(--log-viewport-h);
  overflow: hidden;
  position: relative;
  background: #fff;
  mask-image: linear-gradient(to bottom, transparent 0%, #000 5%, #000 95%, transparent 100%);
}

.log-table__empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #94a3b8;
  font-size: 14px;
}

.log-table__track {
  display: flex;
  flex-direction: column;
  will-change: transform;
  animation: log-scroll linear infinite;
  animation-duration: var(--scroll-duration, 24s);
}
.log-table__track.is-paused {
  animation-play-state: paused;
}

@keyframes log-scroll {
  from { transform: translate3d(0, 0, 0); }
  to { transform: translate3d(0, -50%, 0); }
}

.log-table__row {
  height: var(--log-row-h);
  font-size: 13px;
  border-bottom: 1px solid #f1f5f9;
  transition: background 0.15s ease;
}
.log-table__row:hover {
  background: #f0f9ff;
}

.log-method {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
  font-family: ui-monospace, monospace;
  color: #0369a1;
  background: #e0f2fe;
}

.log-status {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 9999px;
  font-size: 11px;
  font-weight: 500;
}
.log-status.is-ok {
  color: #047857;
  background: #d1fae5;
}
.log-status.is-fail {
  color: #be123c;
  background: #ffe4e6;
}

@media (max-width: 1024px) {
  .grid-cols-log {
    grid-template-columns: 72px minmax(0, 1fr) 48px 60px 56px 120px;
  }
}
</style>
