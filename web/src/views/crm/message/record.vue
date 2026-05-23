<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="消息标题" prop="title">
        <el-input v-model="queryParams.title" placeholder="请输入标题" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 140px">
          <el-option label="已发送" value="0" />
          <el-option label="已撤回" value="1" />
          <el-option label="已读" value="2" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="recordList">
      <el-table-column label="标题" prop="title" min-width="180" show-overflow-tooltip />
      <el-table-column label="模板" prop="templateName" width="120" show-overflow-tooltip />
      <el-table-column label="发送人" prop="senderName" width="100" align="center" />
      <el-table-column label="接收人" prop="receiverName" width="100" align="center" />
      <el-table-column label="发送时间" prop="sendTime" width="170" align="center">
        <template #default="scope">{{ parseTime(scope.row.sendTime) }}</template>
      </el-table-column>
      <el-table-column label="状态" prop="status" width="90" align="center">
        <template #default="scope">
          <el-tag :type="statusTag(scope.row.status)" size="small">{{ statusLabel(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" align="center">
        <template #default="scope">
          <el-button v-if="scope.row.status === '0'" link type="warning" @click="handleRecall(scope.row)" v-hasPermi="['crm:message:recall']">撤回</el-button>
          <el-button v-if="scope.row.status === '1'" link type="primary" @click="handleResend(scope.row)" v-hasPermi="['crm:message:resend']">重发</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script setup name="CrmMessageRecord">
import { listRecord, recallMessage, resendMessage } from '@/api/crm/message'
import useCrmMessageStore from '@/stores/crm/message'

const { proxy } = getCurrentInstance()
const crmMessageStore = useCrmMessageStore()

const recordList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)

const queryParams = ref({ pageNum: 1, pageSize: 10, title: undefined, status: undefined })

function statusLabel(s) {
  return s === '0' ? '已发送' : s === '1' ? '已撤回' : '已读'
}
function statusTag(s) {
  return s === '0' ? 'success' : s === '1' ? 'danger' : 'info'
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

function handleRecall(row) {
  proxy.$modal.confirm('确认撤回该消息？').then(() => recallMessage(row.id)).then(() => {
    proxy.$modal.msgSuccess('撤回成功')
    crmMessageStore.fetchUnread()
    getList()
  }).catch(() => {})
}

function handleResend(row) {
  proxy.$modal.confirm('确认重新发送该消息？').then(() => resendMessage(row.id)).then(() => {
    proxy.$modal.msgSuccess('重发成功')
    crmMessageStore.fetchUnread()
    getList()
  }).catch(() => {})
}

getList()
</script>
