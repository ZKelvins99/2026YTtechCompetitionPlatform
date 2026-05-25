<template>
  <div class="app-container profile-page crm-page">
    <crm-page-header
      title="用户画像"
      :tagline="`${userStore.nickName || '当前用户'} · 业绩与分布一览`"
      :features="CRM_PAGE_INTRO.profile.features"
    >
      <template #extra>
        <el-button icon="Refresh" @click="loadProfile">刷新数据</el-button>
      </template>
    </crm-page-header>

    <el-row :gutter="16" v-loading="loading">
      <el-col :span="8">
        <el-card shadow="hover" class="chart-card">
          <template #header><span>行业标签云</span></template>
          <div class="tag-cloud" v-if="tagCloud.length">
            <el-tag
              v-for="item in tagCloud"
              :key="item.name"
              :size="tagSize(item.value)"
              :type="tagType(item.name)"
              class="cloud-tag"
            >{{ item.name }} ({{ item.value }})</el-tag>
          </div>
          <el-empty v-else description="暂无行业数据" :image-size="80" />
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card shadow="hover" class="chart-card">
          <template #header><span>商机阶段分布</span></template>
          <div ref="ringChartRef" class="chart-box"></div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card shadow="hover" class="chart-card">
          <template #header><span>业绩雷达图</span></template>
          <div ref="radarChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup name="CrmUserProfile">
import * as echarts from 'echarts'
import { getUserProfile } from '@/api/crm/profile'
import useUserStore from '@/store/modules/user'
import { CRM_PAGE_INTRO } from '@/constants/crmPageIntro'

const userStore = useUserStore()
const loading = ref(false)
const tagCloud = ref([])
const ringChartRef = ref(null)
const radarChartRef = ref(null)
let ringChart, radarChart

function tagSize(value) {
  if (value >= 15) return 'large'
  if (value >= 8) return 'default'
  return 'small'
}

function tagType(name) {
  const map = { '制造业': 'warning', '金融': 'success', '电商': 'primary' }
  return map[name] || 'info'
}

function renderRing(data) {
  if (!ringChart) ringChart = echarts.init(ringChartRef.value)
  ringChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, type: 'scroll' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['50%', '45%'],
      data: data.length ? data : [{ name: '暂无数据', value: 1 }],
      label: { formatter: '{b}: {c}' }
    }]
  })
}

function formatContractAmount(value) {
  const n = Number(value)
  if (!Number.isFinite(n)) return '0 元'
  if (Math.abs(n) >= 10000) {
    return `${(n / 10000).toLocaleString('zh-CN', { maximumFractionDigits: 2 })} 万元`
  }
  return `${n.toLocaleString('zh-CN', { maximumFractionDigits: 2 })} 元`
}

function formatRadarMetricLine(metric) {
  if (!metric) return ''
  const name = metric.name
  const score = metric.score ?? 0
  if (name === '合同额') {
    return `${name}: ${formatContractAmount(metric.value)}（得分 ${score}）`
  }
  if (name === '赢单率' || name === '回款率') {
    return `${name}: ${metric.value ?? 0}%（得分 ${score}）`
  }
  if (name === '活跃度') {
    return `${name}: ${metric.value ?? 0} 分（得分 ${score}）`
  }
  const unit = metric.unit || ''
  return `${name}: ${metric.value ?? 0}${unit}（得分 ${score}）`
}

function renderRadar(radarChartData) {
  if (!radarChart) radarChart = echarts.init(radarChartRef.value)
  const indicator = radarChartData?.indicator || []
  const values = radarChartData?.data?.[0]?.value || [0, 0, 0, 0, 0, 0]
  const rawMetrics = radarChartData?.rawMetrics || []
  radarChart.setOption({
    tooltip: {
      trigger: 'item',
      formatter(params) {
        const lines = rawMetrics.length
          ? rawMetrics.map(formatRadarMetricLine)
          : indicator.map((item, i) => `${item.name}: 得分 ${values[i] ?? 0}`)
        return `${params.name}<br/>${lines.join('<br/>')}`
      }
    },
    radar: {
      indicator,
      radius: '58%',
      splitNumber: 4,
      axisName: { color: '#64748b' },
      splitLine: { lineStyle: { color: '#e2e8f0' } },
      splitArea: { areaStyle: { color: ['#fff', '#f8fafc'] } },
      axisLine: { lineStyle: { color: '#cbd5e1' } }
    },
    series: [{
      type: 'radar',
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: { width: 2, color: '#3b82f6' },
      itemStyle: { color: '#3b82f6' },
      areaStyle: { color: 'rgba(59, 130, 246, 0.15)' },
      data: [{ value: values, name: '业绩指标' }]
    }]
  })
}

async function loadProfile() {
  loading.value = true
  try {
    const res = await getUserProfile(userStore.id)
    const data = res.data
    tagCloud.value = data.tagCloud || []
    await nextTick()
    renderRing(data.ringChart || [])
    renderRadar(data.radarChart)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadProfile()
  window.addEventListener('resize', () => {
    ringChart?.resize()
    radarChart?.resize()
  })
})

onActivated(() => loadProfile())

onBeforeUnmount(() => {
  ringChart?.dispose()
  radarChart?.dispose()
})
</script>

<style scoped>
.profile-page { min-height: calc(100vh - 84px); }
.profile-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.page-title { margin: 0 0 4px; font-size: 20px; }
.page-sub { margin: 0; color: #909399; font-size: 14px; }
.chart-card { min-height: 420px; }
.chart-box { height: 340px; }
.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  justify-content: center;
  min-height: 340px;
  padding: 16px;
}
.cloud-tag { margin: 4px; }
</style>
