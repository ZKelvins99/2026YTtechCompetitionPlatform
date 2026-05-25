<template>
  <el-popover placement="bottom-end" :width="340" trigger="click" @show="onShow">
    <template #reference>
      <div class="right-menu-item hover-effect crm-msg-trigger">
        <el-badge :value="crmMessageStore.unreadCount" :hidden="crmMessageStore.unreadCount === 0" :max="99">
          <el-icon :size="18"><Bell /></el-icon>
        </el-badge>
      </div>
    </template>
    <div class="crm-msg-popover">
      <div class="crm-msg-header">
        <span>CRM 消息</span>
        <router-link to="/crm/message/record" class="crm-msg-more">查看全部</router-link>
      </div>
      <div v-if="loading" class="crm-msg-empty">加载中...</div>
      <div v-else-if="!crmMessageStore.unreadList.length" class="crm-msg-empty">暂无未读消息</div>
      <div v-else>
        <div
          v-for="item in crmMessageStore.unreadList"
          :key="item.id"
          class="crm-msg-item crm-msg-unread"
          @click="handleRead(item)"
        >
          <div class="crm-msg-title">{{ item.title }}</div>
          <div class="crm-msg-meta">{{ item.senderName }} · {{ parseTime(item.sendTime) }}</div>
        </div>
      </div>
    </div>
  </el-popover>
</template>

<script setup>
import { Bell } from '@element-plus/icons-vue'
import { useIntervalFn } from '@vueuse/core'
import useCrmMessageStore from '@/stores/crm/message'

const crmMessageStore = useCrmMessageStore()
const loading = ref(false)

async function onShow() {
  loading.value = true
  await crmMessageStore.fetchUnread()
  loading.value = false
}

async function handleRead(item) {
  loading.value = true
  try {
    await crmMessageStore.markRead(item.id)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  crmMessageStore.fetchUnread()
})

useIntervalFn(() => crmMessageStore.fetchUnread(), 30000)
</script>

<style scoped>
.crm-msg-trigger {
  display: flex;
  align-items: center;
  height: 100%;
  padding: 0 8px;
}
.crm-msg-popover { margin: -12px; }
.crm-msg-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 14px;
  border-bottom: 1px solid #eee;
  font-weight: 600;
  font-size: 13px;
}
.crm-msg-more { font-size: 12px; color: var(--el-color-primary); text-decoration: none; }
.crm-msg-empty { padding: 24px; text-align: center; color: #999; font-size: 12px; }
.crm-msg-item {
  padding: 10px 14px;
  border-bottom: 1px solid #f5f5f5;
}
.crm-msg-item:last-child { border-bottom: none; }
.crm-msg-unread {
  cursor: pointer;
}
.crm-msg-unread:hover {
  background: #f5f7fa;
}
.crm-msg-title { font-size: 13px; color: #303133; font-weight: 700; margin-bottom: 4px; }
.crm-msg-meta { font-size: 11px; color: #606266; }
</style>
