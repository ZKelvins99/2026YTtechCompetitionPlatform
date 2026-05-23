<template>
  <div v-loading="loading" class="stats-wrap">
    <div class="stat-item">
      <div class="stat-num">{{ data.total || 0 }}</div>
      <div class="stat-label">客户总数</div>
    </div>
    <div class="stat-item">
      <div class="stat-num accent">{{ data.monthNew || 0 }}</div>
      <div class="stat-label">本月新增</div>
    </div>
  </div>
</template>

<script setup>
import { getWidgetData } from '@/api/crm/workbench'

const data = ref({})
const loading = ref(false)

function loadData() {
  loading.value = true
  getWidgetData('customer-stats').then(res => {
    data.value = res.data || {}
  }).finally(() => { loading.value = false })
}

onMounted(loadData)
defineExpose({ refresh: loadData })
</script>

<style scoped>
.stats-wrap { display: flex; gap: 24px; justify-content: center; padding: 16px 0; }
.stat-item { text-align: center; }
.stat-num { font-size: 32px; font-weight: bold; color: #303133; }
.stat-num.accent { color: #409eff; }
.stat-label { font-size: 13px; color: #909399; margin-top: 4px; }
</style>
