<template>
  <div v-loading="loading" class="widget-body-inner">
    <el-table v-if="list.length" :data="list" size="small" @row-click="handleRowClick" class="click-table">
      <el-table-column label="合同" prop="contractname" min-width="120" show-overflow-tooltip />
      <el-table-column label="节点" prop="nodename" width="90" />
      <el-table-column label="编号" prop="businessno" width="120" show-overflow-tooltip />
    </el-table>
    <el-empty v-else description="暂无待办" :image-size="60" />
  </div>
</template>

<script setup>
import { getWidgetData } from '@/api/crm/workbench'

const router = useRouter()
const list = ref([])
const loading = ref(false)

function normalizeRow(row) {
  const r = {}
  Object.keys(row).forEach(k => { r[k.toLowerCase()] = row[k] })
  return r
}

function loadData() {
  loading.value = true
  getWidgetData('approval').then(res => {
    list.value = (res.data || []).map(normalizeRow)
  }).finally(() => { loading.value = false })
}

function handleRowClick(row) {
  const id = row.instanceid || row.instanceId
  if (id) router.push('/crm/workflow/index/' + id)
}

onMounted(loadData)
defineExpose({ refresh: loadData })
</script>

<style scoped>
.click-table :deep(.el-table__row) { cursor: pointer; }
.widget-body-inner { min-height: 120px; }
</style>
