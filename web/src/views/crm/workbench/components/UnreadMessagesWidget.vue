<template>
  <div v-loading="loading" class="msg-list">
    <div v-for="item in list" :key="item.id" class="msg-item">
      <div class="msg-title">{{ item.title }}</div>
      <div class="msg-time">{{ parseTime(item.sendTime) }}</div>
    </div>
    <el-empty v-if="!loading && !list.length" description="暂无未读" :image-size="50" />
  </div>
</template>

<script setup>
import { getWidgetData } from '@/api/crm/workbench'

const list = ref([])
const loading = ref(false)

function loadData() {
  loading.value = true
  getWidgetData('unread-messages').then(res => {
    list.value = res.data || []
  }).finally(() => { loading.value = false })
}

onMounted(loadData)
defineExpose({ refresh: loadData })
</script>

<style scoped>
.msg-list { max-height: 200px; overflow-y: auto; }
.msg-item { padding: 8px 0; border-bottom: 1px solid #f0f0f0; }
.msg-item:last-child { border-bottom: none; }
.msg-title { font-size: 13px; color: #303133; }
.msg-time { font-size: 11px; color: #909399; margin-top: 2px; }
</style>
