<template>
  <div class="app-container crm-page">
    <crm-page-header title="消息记录" v-bind="CRM_PAGE_INTRO.messageRecord" />
    <el-card shadow="never" class="crm-panel crm-search-panel" v-show="showSearch">
    <el-form :model="queryParams" ref="queryRef" :inline="true">
      <el-form-item label="消息标题" prop="title">
        <el-input v-model="queryParams.title" placeholder="请输入标题" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 140px">
          <el-option label="已发送(未读)" value="0" />
          <el-option label="已撤回" value="1" />
          <el-option label="已读" value="2" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>
    </el-card>

    <el-card shadow="never" class="crm-panel">
    <el-row :gutter="10" class="mb8">
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="recordList" :row-class-name="tableRowClassName">
      <el-table-column label="标题" prop="title" min-width="180" show-overflow-tooltip>
        <template #default="scope">
          <span :class="titleClass(scope.row)">{{ scope.row.title }}</span>
        </template>
      </el-table-column>
      <el-table-column label="模板" prop="templateName" width="120" show-overflow-tooltip />
      <el-table-column label="发送人" prop="senderName" width="100" align="center" />
      <el-table-column label="接收人" prop="receiverName" width="100" align="center" />
      <el-table-column label="发送时间" prop="sendTime" width="170" align="center">
        <template #default="scope">{{ parseTime(scope.row.sendTime) }}</template>
      </el-table-column>
      <el-table-column label="状态" prop="status" width="100" align="center">
        <template #default="scope">
          <el-tag :type="statusTag(scope.row.status)" size="small">{{ statusLabel(scope.row) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" align="center">
        <template #default="scope">
          <el-button
            v-if="canMarkRead(scope.row)"
            link
            type="primary"
            @click="handleMarkRead(scope.row)"
          >标为已读</el-button>
          <el-button v-if="scope.row.status === '0' && isSender(scope.row)" link type="warning" @click="handleRecall(scope.row)" v-hasPermi="['crm:message:recall']">撤回</el-button>
          <el-button v-if="scope.row.status === '1' && isSender(scope.row)" link type="primary" @click="handleResend(scope.row)" v-hasPermi="['crm:message:resend']">重发</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
    </el-card>
  </div>
</template>

<script setup name="CrmMessageRecord">
import { listRecord, recallMessage, resendMessage, markMessageRead } from '@/api/crm/message'
import useCrmMessageStore from '@/stores/crm/message'
import useUserStore from '@/store/modules/user'
import { CRM_PAGE_INTRO } from '@/constants/crmPageIntro'

const { proxy } = getCurrentInstance()
const crmMessageStore = useCrmMessageStore()
const userStore = useUserStore()

const recordList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)

const queryParams = ref({ pageNum: 1, pageSize: 10, title: undefined, status: undefined })

function isSender(row) {
  return row.senderId === userStore.id
}

function isReceiver(row) {
  return row.receiverId === userStore.id
}

function canMarkRead(row) {
  return row.status === '0' && isReceiver(row)
}

function statusLabel(row) {
  if (row.status === '0') return isReceiver(row) ? '未读' : '已发送'
  if (row.status === '1') return '已撤回'
  return '已读'
}

function statusTag(s) {
  return s === '0' ? 'warning' : s === '1' ? 'danger' : 'info'
}

function isUnreadRow(row) {
  return row.status === '0' && isReceiver(row)
}

function titleClass(row) {
  if (row.status === '1') return 'msg-title-recalled'
  if (isUnreadRow(row)) return 'msg-title-unread'
  return 'msg-title-read'
}

function tableRowClassName({ row }) {
  if (isUnreadRow(row)) return 'msg-row-unread'
  if (row.status === '2' || row.status === '1') return 'msg-row-read'
  return ''
}

function getList() {
  loading.value = true
  listRecord(queryParams.value).then(res => {
    recordList.value = res.rows
    total.value = res.total
    loading.value = false
  })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  handleQuery()
}

function afterMessageAction() {
  crmMessageStore.refreshAll()
  getList()
}

function handleMarkRead(row) {
  markMessageRead(row.id).then(() => {
    proxy.$modal.msgSuccess('已标记为已读')
    afterMessageAction()
  })
}

function handleRecall(row) {
  proxy.$modal.confirm('确认撤回该消息？').then(() => recallMessage(row.id)).then(() => {
    proxy.$modal.msgSuccess('撤回成功')
    afterMessageAction()
  }).catch(() => {})
}

function handleResend(row) {
  proxy.$modal.confirm('确认重新发送该消息？').then(() => resendMessage(row.id)).then(() => {
    proxy.$modal.msgSuccess('重发成功')
    afterMessageAction()
  }).catch(() => {})
}

getList()
</script>

<style scoped>
:deep(.msg-row-unread .msg-title-unread) {
  color: #303133;
  font-weight: 700;
}
:deep(.msg-title-read) {
  color: #909399;
  font-weight: 400;
}
:deep(.msg-title-recalled) {
  color: #c0c4cc;
}
</style>
