<template>
  <div v-loading="loading" class="widget-body-inner">
    <el-table v-if="list.length" :data="list" size="small" class="click-table">
      <el-table-column label="合同名称" prop="contractname" min-width="120" show-overflow-tooltip />
      <el-table-column label="到期日" prop="enddate" width="100" align="center">
        <template #default="scope">{{ parseTime(scope.row.enddate, '{y}-{m}-{d}') }}</template>
      </el-table-column>
      <el-table-column label="剩余" prop="remainingdays" width="70" align="center">
        <template #default="scope">
          <el-tag :type="scope.row.remainingdays <= 7 ? 'danger' : 'warning'" size="small">
            {{ scope.row.remainingdays }}天
          </el-tag>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-else description="近30天无到期合同" :image-size="60" />
  </div>
</template>

<script setup>
import { getWidgetData } from '@/api/crm/workbench'

const list = ref([])
const loading = ref(false)

function normalizeRow(row) {
  const r = {}
  Object.keys(row).forEach(k => { r[k.toLowerCase()] = row[k] })
  return r
}

function loadData() {
  loading.value = true
  getWidgetData('expiring-contracts').then(res => {
    list.value = (res.data || []).map(normalizeRow)
  }).finally(() => { loading.value = false })
}

onMounted(loadData)
defineExpose({ refresh: loadData })
</script>

<style scoped>
.widget-body-inner { min-height: 120px; }
</style>
