<template>
  <div class="app-container">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="API 市场" name="market">
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12" :md="8" :lg="6" v-for="item in marketList" :key="item.id" class="mb16">
            <el-card shadow="hover" class="api-card">
              <div class="api-card-title">{{ item.apiName }}</div>
              <p class="api-card-desc">{{ item.apiDesc || '暂无描述' }}</p>
              <div class="api-card-meta">
                <el-tag :type="methodTag(item.apiMethod)" size="small">{{ item.apiMethod }}</el-tag>
                <span class="api-url" :title="item.apiUrl">{{ item.apiUrl }}</span>
              </div>
              <el-tooltip v-if="item.status !== '1'" content="接口已下架，无法调试" placement="top">
                <span>
                  <el-button type="primary" size="small" disabled>在线调试</el-button>
                </span>
              </el-tooltip>
              <el-button v-else type="primary" size="small" @click="openDebug(item)" v-hasPermi="['crm:api:debug']">在线调试</el-button>
            </el-card>
          </el-col>
        </el-row>
        <el-empty v-if="!marketLoading && !marketList.length" description="暂无上架 API" />
      </el-tab-pane>

      <el-tab-pane label="API 管理" name="manage">
        <el-form :model="queryParams" ref="queryRef" :inline="true" class="mb8">
          <el-form-item label="API名称" prop="apiName">
            <el-input v-model="queryParams.apiName" placeholder="请输入名称" clearable style="width: 180px" @keyup.enter="getManageList" />
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 120px">
              <el-option label="上架" value="1" />
              <el-option label="下架" value="0" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="getManageList">搜索</el-button>
            <el-button icon="Refresh" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['crm:api:add']">发布 API</el-button>
          </el-col>
        </el-row>

        <el-table v-loading="manageLoading" :data="manageList">
          <el-table-column label="API名称" prop="apiName" min-width="140" show-overflow-tooltip />
          <el-table-column label="描述" prop="apiDesc" min-width="160" show-overflow-tooltip />
          <el-table-column label="方式" prop="apiMethod" width="80" align="center">
            <template #default="scope">
              <el-tag :type="methodTag(scope.row.apiMethod)" size="small">{{ scope.row.apiMethod }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="地址" prop="apiUrl" min-width="200" show-overflow-tooltip />
          <el-table-column label="状态" width="100" align="center">
            <template #default="scope">
              <el-switch
                v-model="scope.row.status"
                active-value="1"
                inactive-value="0"
                @change="handleStatusChange(scope.row)"
                v-hasPermi="['crm:api:online']"
              />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160" align="center">
            <template #default="scope">
              <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['crm:api:edit']">编辑</el-button>
              <el-popconfirm title="确认删除该 API？" @confirm="handleDelete(scope.row)">
                <template #reference>
                  <el-button link type="danger" icon="Delete" v-hasPermi="['crm:api:remove']">删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
        <pagination v-show="manageTotal > 0" :total="manageTotal" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getManageList" />
      </el-tab-pane>
    </el-tabs>

    <el-dialog :title="formTitle" v-model="formOpen" width="640px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="API名称" prop="apiName">
          <el-input v-model="form.apiName" placeholder="请输入 API 名称" />
        </el-form-item>
        <el-form-item label="API描述" prop="apiDesc">
          <el-input v-model="form.apiDesc" type="textarea" :rows="2" placeholder="请输入描述" />
        </el-form-item>
        <el-form-item label="请求方式" prop="apiMethod">
          <el-select v-model="form.apiMethod" style="width: 100%">
            <el-option label="GET" value="GET" />
            <el-option label="POST" value="POST" />
            <el-option label="PUT" value="PUT" />
            <el-option label="DELETE" value="DELETE" />
          </el-select>
        </el-form-item>
        <el-form-item label="接口地址" prop="apiUrl">
          <el-input v-model="form.apiUrl" placeholder="如 /crm/api/demo/customer-count" />
        </el-form-item>
        <el-form-item label="请求示例" prop="requestExample">
          <el-input v-model="form.requestExample" type="textarea" :rows="3" placeholder="JSON 请求体示例（POST 时使用）" />
        </el-form-item>
        <el-form-item label="响应示例" prop="responseExample">
          <el-input v-model="form.responseExample" type="textarea" :rows="3" placeholder="JSON 响应示例" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio value="1">上架</el-radio>
            <el-radio value="0">下架</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formOpen = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog title="在线调试" v-model="debugOpen" width="720px" append-to-body destroy-on-close>
      <div class="debug-section">
        <div class="debug-label">请求 URL</div>
        <el-input :model-value="currentApi.apiUrl" readonly />
      </div>
      <div class="debug-section">
        <div class="debug-label">请求方式</div>
        <el-tag :type="methodTag(currentApi.apiMethod)">{{ currentApi.apiMethod }}</el-tag>
      </div>
      <div class="debug-section" v-if="currentApi.apiMethod !== 'GET'">
        <div class="debug-label">请求参数</div>
        <el-input v-model="debugBody" type="textarea" :rows="4" placeholder="JSON 请求体" />
      </div>
      <div class="debug-actions">
        <el-button type="primary" :loading="debugLoading" @click="sendDebug">发送请求</el-button>
        <span v-if="debugResult.responseTime != null" class="debug-time">响应时间：{{ debugResult.responseTime }} ms</span>
        <el-tag v-if="debugResult.statusCode" :type="debugResult.statusCode === 200 ? 'success' : 'danger'" class="ml8">
          HTTP {{ debugResult.statusCode }}
        </el-tag>
      </div>
      <div class="debug-section">
        <div class="debug-label">响应结果</div>
        <el-input :model-value="formatJson(debugResult.responseBody)" type="textarea" :rows="10" readonly />
      </div>
    </el-dialog>
  </div>
</template>

<script setup name="CrmApiMarket">
import { listApi, getApi, addApi, updateApi, delApi, onlineApi, offlineApi, debugApi as debugApiRequest } from '@/api/crm/apiMarket'

const { proxy } = getCurrentInstance()

const activeTab = ref('market')
const marketList = ref([])
const marketLoading = ref(false)
const manageList = ref([])
const manageLoading = ref(false)
const manageTotal = ref(0)
const formOpen = ref(false)
const formTitle = ref('')
const debugOpen = ref(false)
const debugLoading = ref(false)
const currentApi = ref({})
const debugBody = ref('')
const debugResult = ref({})

const queryParams = ref({ pageNum: 1, pageSize: 10, apiName: undefined, status: undefined })

const data = reactive({
  form: {},
  rules: {
    apiName: [{ required: true, message: 'API名称不能为空', trigger: 'blur' }],
    apiUrl: [{ required: true, message: '接口地址不能为空', trigger: 'blur' }],
    apiMethod: [{ required: true, message: '请选择请求方式', trigger: 'change' }]
  }
})
const { form, rules } = toRefs(data)

function methodTag(method) {
  if (method === 'GET') return 'success'
  if (method === 'POST') return 'primary'
  if (method === 'PUT') return 'warning'
  return 'danger'
}

function formatJson(text) {
  if (!text) return ''
  try { return JSON.stringify(JSON.parse(text), null, 2) } catch { return text }
}

function getMarketList() {
  marketLoading.value = true
  listApi({ pageNum: 1, pageSize: 100, status: '1' }).then(res => {
    marketList.value = res.rows || []
  }).finally(() => { marketLoading.value = false })
}

function getManageList() {
  manageLoading.value = true
  listApi(queryParams.value).then(res => {
    manageList.value = res.rows || []
    manageTotal.value = res.total
  }).finally(() => { manageLoading.value = false })
}

function resetQuery() {
  proxy.resetForm('queryRef')
  queryParams.value.pageNum = 1
  getManageList()
}

function resetFormData() {
  form.value = {
    id: undefined, apiName: undefined, apiDesc: undefined, apiUrl: undefined,
    apiMethod: 'GET', requestExample: undefined, responseExample: undefined, status: '1'
  }
  proxy.resetForm('formRef')
}

function handleAdd() {
  resetFormData()
  formOpen.value = true
  formTitle.value = '发布 API'
}

function handleUpdate(row) {
  resetFormData()
  getApi(row.id).then(res => {
    form.value = res.data
    formOpen.value = true
    formTitle.value = '编辑 API'
  })
}

function submitForm() {
  proxy.$refs.formRef.validate(valid => {
    if (!valid) return
    const req = form.value.id ? updateApi(form.value) : addApi(form.value)
    req.then(() => {
      proxy.$modal.msgSuccess(form.value.id ? '修改成功' : '发布成功')
      formOpen.value = false
      getManageList()
      getMarketList()
    })
  })
}

function handleDelete(row) {
  delApi(row.id).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    getManageList()
    getMarketList()
  })
}

function handleStatusChange(row) {
  const req = row.status === '1' ? onlineApi(row.id) : offlineApi(row.id)
  req.then(() => {
    proxy.$modal.msgSuccess(row.status === '1' ? '已上架' : '已下架')
    getMarketList()
  }).catch(() => {
    row.status = row.status === '1' ? '0' : '1'
  })
}

function openDebug(item) {
  currentApi.value = item
  debugBody.value = item.requestExample || ''
  debugResult.value = {}
  debugOpen.value = true
}

function sendDebug() {
  debugLoading.value = true
  debugApiRequest(currentApi.value.id, { requestBody: debugBody.value || undefined }).then(res => {
    debugResult.value = res.data
  }).finally(() => { debugLoading.value = false })
}

watch(activeTab, tab => {
  if (tab === 'market') getMarketList()
  else getManageList()
})

onMounted(() => {
  getMarketList()
})
</script>

<style scoped>
.mb16 { margin-bottom: 16px; }
.api-card-title { font-weight: 600; font-size: 15px; margin-bottom: 8px; }
.api-card-desc { color: #909399; font-size: 13px; min-height: 40px; margin: 0 0 12px; }
.api-card-meta { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; }
.api-url { font-size: 12px; color: #606266; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; flex: 1; }
.debug-section { margin-bottom: 14px; }
.debug-label { font-weight: 600; margin-bottom: 6px; font-size: 13px; }
.debug-actions { display: flex; align-items: center; margin-bottom: 16px; gap: 8px; }
.debug-time { color: #909399; font-size: 13px; margin-left: 12px; }
.ml8 { margin-left: 8px; }
</style>
