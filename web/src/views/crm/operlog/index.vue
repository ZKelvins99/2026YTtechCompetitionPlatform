<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="操作人" prop="operator">
        <el-input v-model="queryParams.operator" placeholder="请输入操作人" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="请求地址" prop="requestUrl">
        <el-input v-model="queryParams.requestUrl" placeholder="请输入请求地址" clearable style="width: 220px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="操作时间" style="width: 380px">
        <el-date-picker
          v-model="dateRange"
          value-format="YYYY-MM-DD HH:mm:ss"
          type="datetimerange"
          range-separator="-"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          :default-time="[new Date(2000, 1, 1, 0, 0, 0), new Date(2000, 1, 1, 23, 59, 59)]"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="operlogList" @row-dblclick="handleDetail">
      <el-table-column label="编号" align="center" prop="id" width="80" />
      <el-table-column label="操作人" align="center" prop="operator" width="110" show-overflow-tooltip />
      <el-table-column label="请求地址" align="center" prop="requestUrl" min-width="200" show-overflow-tooltip />
      <el-table-column label="请求方式" align="center" prop="requestMethod" width="90" />
      <el-table-column label="TraceId" align="center" prop="traceId" width="140" show-overflow-tooltip />
      <el-table-column label="耗时(ms)" align="center" prop="costMillis" width="100" />
      <el-table-column label="状态" align="center" prop="status" width="80">
        <template #default="scope">
          <el-tag :type="scope.row.status === '0' ? 'success' : 'danger'" size="small">
            {{ scope.row.status === '0' ? '成功' : '失败' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作时间" align="center" prop="operateTime" width="170">
        <template #default="scope">{{ parseTime(scope.row.operateTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="90">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleDetail(scope.row)" v-hasPermi="['crm:operlog:query']">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog title="操作日志详情" v-model="detailOpen" width="780px" append-to-body destroy-on-close>
      <el-descriptions :column="2" border size="small" class="mb12">
        <el-descriptions-item label="操作人">{{ detail.operator }}</el-descriptions-item>
        <el-descriptions-item label="操作时间">{{ parseTime(detail.operateTime) }}</el-descriptions-item>
        <el-descriptions-item label="请求地址" :span="2">{{ detail.requestUrl }}</el-descriptions-item>
        <el-descriptions-item label="请求方式">{{ detail.requestMethod }}</el-descriptions-item>
        <el-descriptions-item label="耗时">{{ detail.costMillis }} ms</el-descriptions-item>
        <el-descriptions-item label="TraceId" :span="2">{{ detail.traceId }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="detail.status === '0' ? 'success' : 'danger'" size="small">
            {{ detail.status === '0' ? '成功' : '失败' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="错误信息">{{ detail.errorMsg || '—' }}</el-descriptions-item>
      </el-descriptions>

      <div class="detail-block">
        <div class="detail-label">请求参数</div>
        <pre class="json-pre">{{ formatJson(detail.requestParams) }}</pre>
      </div>
      <div class="detail-block">
        <div class="detail-label">响应结果</div>
        <pre class="json-pre">{{ formatJson(detail.responseResult) }}</pre>
      </div>
      <div class="detail-block">
        <div class="detail-label">SQL 语句列表（链路追踪）</div>
        <pre class="json-pre">{{ formatSqlList(detail.sqlStatements) }}</pre>
      </div>
    </el-dialog>
  </div>
</template>

<script setup name="CrmOperlog">
import { listOperlog, getOperlog } from '@/api/crm/operlog'

const { proxy } = getCurrentInstance()

const operlogList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const dateRange = ref([])
const detailOpen = ref(false)
const detail = ref({})

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    operator: undefined,
    requestUrl: undefined
  }
})
const { queryParams } = toRefs(data)

function getList() {
  loading.value = true
  listOperlog(proxy.addDateRange(queryParams.value, dateRange.value)).then(res => {
    operlogList.value = res.rows
    total.value = res.total
    loading.value = false
  })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  dateRange.value = []
  proxy.resetForm('queryRef')
  handleQuery()
}

function handleDetail(row) {
  getOperlog(row.id).then(res => {
    detail.value = res.data
    detailOpen.value = true
  })
}

function formatJson(text) {
  if (!text) return '—'
  try {
    return JSON.stringify(JSON.parse(text), null, 2)
  } catch {
    return text
  }
}

function formatSqlList(text) {
  if (!text) return '—'
  try {
    const arr = JSON.parse(text)
    if (Array.isArray(arr)) {
      return arr.map((sql, i) => `${i + 1}. ${sql}`).join('\n\n')
    }
    return formatJson(text)
  } catch {
    return text
  }
}

getList()
</script>

<style scoped>
.mb12 { margin-bottom: 12px; }
.detail-block { margin-top: 12px; }
.detail-label { font-weight: 600; margin-bottom: 6px; color: #303133; }
.json-pre {
  background: #f5f7fa;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 12px;
  max-height: 200px;
  overflow: auto;
  font-size: 12px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-all;
  margin: 0;
}
</style>
