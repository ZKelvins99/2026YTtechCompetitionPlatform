<template>
  <div class="quick-actions">
    <el-button v-for="item in actions" :key="item.path" type="primary" plain @click="go(item)">
      {{ item.label }}
    </el-button>
  </div>
</template>

<script setup>
import { getWidgetData } from '@/api/crm/workbench'

const router = useRouter()
const actions = ref([])

function loadData() {
  getWidgetData('quick-actions').then(res => {
    actions.value = res.data || []
  })
}

function go(item) {
  router.push(item.path)
}

onMounted(loadData)
defineExpose({ refresh: loadData })
</script>

<style scoped>
.quick-actions { display: flex; flex-wrap: wrap; gap: 10px; padding: 8px 0; }
</style>
