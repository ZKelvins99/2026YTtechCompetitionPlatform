<template>
  <div v-loading="loading" ref="chartRef" class="funnel-chart"></div>
</template>

<script setup>
import * as echarts from 'echarts'
import { getWidgetData } from '@/api/crm/workbench'

const chartRef = ref(null)
const loading = ref(false)
let chart

function loadData() {
  loading.value = true
  getWidgetData('opportunity-funnel').then(res => {
    const data = (res.data || []).map(item => ({
      name: item.name || item.NAME,
      value: item.value || item.VALUE || 0
    }))
    renderChart(data.length ? data : [{ name: '暂无', value: 0 }])
  }).finally(() => { loading.value = false })
}

function renderChart(data) {
  if (!chart) chart = echarts.init(chartRef.value)
  chart.setOption({
    tooltip: { trigger: 'item' },
    series: [{
      type: 'funnel', left: '10%', width: '80%', minSize: '20%', maxSize: '100%',
      sort: 'descending', gap: 2, label: { show: true, position: 'inside', fontSize: 11 },
      data
    }]
  })
}

onMounted(loadData)
onBeforeUnmount(() => chart?.dispose())
defineExpose({ refresh: loadData })
</script>

<style scoped>
.funnel-chart { height: 200px; width: 100%; }
</style>
