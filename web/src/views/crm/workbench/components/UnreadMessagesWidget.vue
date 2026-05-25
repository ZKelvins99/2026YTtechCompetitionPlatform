<template>
  <div v-loading="loading" class="msg-list">
    <div
      v-for="item in list"
      :key="item.id"
      class="msg-item"
      :class="itemClass(item)"
      @click="handleItemClick(item)"
    >
      <div class="msg-title">{{ item.title }}</div>
      <div class="msg-meta">
        <span>{{ item.senderName }} → {{ item.receiverName }}</span>
        <span class="msg-time">{{ parseTime(item.sendTime) }}</span>
      </div>
      <el-tag v-if="item.status === '1'" size="small" type="danger" class="msg-tag">已撤回</el-tag>
      <el-tag v-else-if="isUnreadForMe(item)" size="small" type="warning" class="msg-tag">未读</el-tag>
    </div>
    <el-empty v-if="!loading && !list.length" description="暂无消息" :image-size="50" />
  </div>
</template>

<script setup>
import useCrmMessageStore from '@/stores/crm/message'
import useUserStore from '@/store/modules/user'

const crmMessageStore = useCrmMessageStore()
const userStore = useUserStore()
const list = computed(() => crmMessageStore.inboxList)
const loading = ref(false)

function isUnreadForMe(item) {
  return item.status === '0' && item.receiverId === userStore.id
}

function itemClass(item) {
  if (item.status === '1') return 'msg-recalled'
  if (isUnreadForMe(item)) return 'msg-unread'
  return 'msg-read'
}

async function loadData() {
  loading.value = true
  try {
    await crmMessageStore.fetchInbox(8)
  } finally {
    loading.value = false
  }
}

async function handleItemClick(item) {
  if (!isUnreadForMe(item)) return
  loading.value = true
  try {
    await crmMessageStore.markRead(item.id)
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
defineExpose({ refresh: loadData })
</script>

<style scoped>
.msg-list { max-height: 220px; overflow-y: auto; }
.msg-item {
  padding: 8px 4px;
  border-bottom: 1px solid #f0f0f0;
  cursor: default;
  transition: background 0.15s;
}
.msg-item:last-child { border-bottom: none; }
.msg-item.msg-unread {
  cursor: pointer;
  background: #fdf6ec;
  border-radius: 6px;
  padding: 8px 6px;
  margin-bottom: 4px;
}
.msg-item.msg-unread:hover { background: #faecd8; }
.msg-title { font-size: 13px; line-height: 1.4; }
.msg-unread .msg-title { color: #303133; font-weight: 700; }
.msg-read .msg-title { color: #909399; font-weight: 400; }
.msg-recalled .msg-title { color: #c0c4cc; text-decoration: line-through; }
.msg-meta {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  font-size: 11px;
  color: #909399;
  margin-top: 4px;
}
.msg-unread .msg-meta { color: #606266; }
.msg-time { flex-shrink: 0; }
.msg-tag { margin-top: 4px; }
</style>
