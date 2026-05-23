<template>
  <div class="app-container profile-page crm-page">
    <crm-page-header title="CRM 用户画像" :description="`${userStore.nickName}（${userStore.name}）— 标签云、环形图、雷达图，业务数据变更后自动联动刷新。`">
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

function renderRadar(radarChartData) {
  if (!radarChart) radarChart = echarts.init(radarChartRef.value)
  const indicator = radarChartData?.indicator || []
  const values = radarChartData?.data?.[0]?.value || [0, 0, 0, 0, 0, 0]
  radarChart.setOption({
    tooltip: {},
    radar: { indicator, radius: '60%' },
    series: [{
      type: 'radar',
      data: [{ value: values, name: '业绩指标', areaStyle: { opacity: 0.2 } }]
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
