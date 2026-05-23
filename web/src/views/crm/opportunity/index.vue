<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="商机名称" prop="opportunityName">
        <el-input v-model="queryParams.opportunityName" placeholder="请输入商机名称" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="客户" prop="customerId">
        <el-select v-model="queryParams.customerId" placeholder="请选择客户" clearable filterable style="width: 200px">
          <el-option v-for="item in customerOptions" :key="item.id" :label="item.customerName" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="阶段" prop="stageId">
        <el-select v-model="queryParams.stageId" placeholder="请选择阶段" clearable style="width: 160px">
          <el-option v-for="item in stageOptions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['crm:opportunity:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()" v-hasPermi="['crm:opportunity:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="info" plain icon="Upload" @click="handleImport" v-hasPermi="['crm:opportunity:import']">导入 Excel</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="opportunityList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="商机名称" align="center" prop="opportunityName" min-width="140" show-overflow-tooltip />
      <el-table-column label="客户名称" align="center" prop="customerName" min-width="120" show-overflow-tooltip />
      <el-table-column label="阶段" align="center" prop="stageName" width="100" />
      <el-table-column label="预计金额" align="center" prop="estimatedAmount" width="120">
        <template #default="scope">
          <span>{{ scope.row.estimatedAmount != null ? scope.row.estimatedAmount.toLocaleString() : '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="预计成交日期" align="center" prop="expectedCloseDate" width="120">
        <template #default="scope">
          <span>{{ parseTime(scope.row.expectedCloseDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="负责人" align="center" prop="createBy" width="100" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="160">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="150">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['crm:opportunity:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['crm:opportunity:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 新增/编辑 -->
    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="商机名称" prop="opportunityName">
          <el-input v-model="form.opportunityName" placeholder="请输入商机名称" />
        </el-form-item>
        <el-form-item label="关联客户" prop="customerId">
          <el-select v-model="form.customerId" placeholder="请选择客户" filterable style="width: 100%">
            <el-option v-for="item in customerOptions" :key="item.id" :label="item.customerName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="商机阶段" prop="stageId">
          <el-select v-model="form.stageId" placeholder="请选择阶段" style="width: 100%">
            <el-option v-for="item in stageOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="预计金额" prop="estimatedAmount">
          <el-input-number v-model="form.estimatedAmount" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="预计成交日期" prop="expectedCloseDate">
          <el-date-picker v-model="form.expectedCloseDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </template>
    </el-dialog>

    <!-- Excel 导入 -->
    <el-dialog title="导入商机 Excel" v-model="importOpen" width="780px" append-to-body @close="resetImport">
      <el-upload
        ref="uploadRef"
        drag
        :limit="1"
        accept=".xlsx,.xls"
        :auto-upload="false"
        :on-change="handleFileChange"
        :on-remove="handleFileRemove"
      >
        <el-icon class="el-icon--upload"><upload-filled /></el-icon>
        <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
        <template #tip>
          <div class="el-upload__tip">
            仅允许 xls、xlsx 格式。
            <el-link type="primary" underline="never" @click="downloadTemplate">下载模板</el-link>
          </div>
        </template>
      </el-upload>

      <div v-if="importResult" class="import-result-panel">
        <el-row :gutter="16" class="summary-cards">
          <el-col :span="8">
            <el-card shadow="never" class="summary-card success-card">
              <div class="summary-num">{{ importResult.successCount }}</div>
              <div class="summary-label">成功</div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card shadow="never" class="summary-card fail-card">
              <div class="summary-num">{{ importResult.failCount }}</div>
              <div class="summary-label">失败</div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card shadow="never" class="summary-card skip-card">
              <div class="summary-num">{{ importResult.skipCount }}</div>
              <div class="summary-label">跳过(重复)</div>
            </el-card>
          </el-col>
        </el-row>

        <el-table
          v-if="displayErrors.length"
          :data="displayErrors"
          max-height="280"
          :row-class-name="importRowClassName"
          style="margin-top: 16px"
        >
          <el-table-column label="行号" prop="row" width="70" align="center" />
          <el-table-column label="列号" prop="col" width="70" align="center">
            <template #default="scope">{{ scope.row.col || '-' }}</template>
          </el-table-column>
          <el-table-column label="字段名" prop="field" width="140" align="center" />
          <el-table-column label="说明" prop="message" show-overflow-tooltip />
        </el-table>
      </div>

      <template #footer>
        <el-button type="primary" :loading="importLoading" @click="submitImport">开始导入</el-button>
        <el-button @click="importOpen = false">关 闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="CrmOpportunity">
import { UploadFilled } from '@element-plus/icons-vue'
import { listOpportunity, getOpportunity, addOpportunity, updateOpportunity, delOpportunity, importOpportunity } from '@/api/crm/opportunity'
import { getCrmDict } from '@/api/crm/customer'
import { listCustomerOptions } from '@/api/crm/contact'

const { proxy } = getCurrentInstance()

const opportunityList = ref([])
const customerOptions = ref([])
const stageOptions = ref([])
const open = ref(false)
const importOpen = ref(false)
const importLoading = ref(false)
const importResult = ref(null)
const selectedFile = ref(null)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const multiple = ref(true)
const total = ref(0)
const title = ref('')

const displayErrors = computed(() => importResult.value?.errors || [])

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    opportunityName: undefined,
    customerId: undefined,
    stageId: undefined
  },
  rules: {
    opportunityName: [{ required: true, message: '商机名称不能为空', trigger: 'blur' }],
    customerId: [{ required: true, message: '请选择关联客户', trigger: 'change' }]
  }
})

const { queryParams, form, rules } = toRefs(data)

function getList() {
  loading.value = true
  listOpportunity(queryParams.value).then(res => {
    opportunityList.value = res.rows
    total.value = res.total
    loading.value = false
  })
}

function loadOptions() {
  listCustomerOptions().then(res => { customerOptions.value = res.rows || [] })
  getCrmDict('opportunity_stage').then(res => { stageOptions.value = res.data || [] })
}

function cancel() {
  open.value = false
  reset()
}

function reset() {
  form.value = {
    id: undefined,
    opportunityName: undefined,
    customerId: undefined,
    stageId: undefined,
    estimatedAmount: undefined,
    expectedCloseDate: undefined
  }
  proxy.resetForm('formRef')
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  handleQuery()
}

function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id)
  multiple.value = !selection.length
}

function handleAdd() {
  reset()
  open.value = true
  title.value = '新增商机'
}

function handleUpdate(row) {
  reset()
  getOpportunity(row.id).then(res => {
    form.value = res.data
    open.value = true
    title.value = '修改商机'
  })
}

function submitForm() {
  proxy.$refs['formRef'].validate(valid => {
    if (!valid) return
    const action = form.value.id ? updateOpportunity(form.value) : addOpportunity(form.value)
    action.then(() => {
      proxy.$modal.msgSuccess(form.value.id ? '修改成功' : '新增成功')
      open.value = false
      getList()
    })
  })
}

function handleDelete(row) {
  const delIds = row?.id || ids.value
  proxy.$modal.confirm('是否确认删除选中的商机？').then(() => delOpportunity(delIds)).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

function handleImport() {
  resetImport()
  importOpen.value = true
}

function resetImport() {
  importResult.value = null
  selectedFile.value = null
  importLoading.value = false
  proxy.$refs.uploadRef?.clearFiles()
}

function handleFileChange(file) {
  selectedFile.value = file.raw
  importResult.value = null
}

function handleFileRemove() {
  selectedFile.value = null
}

function downloadTemplate() {
  proxy.download('crm/opportunity/importTemplate', {}, `opportunity_template_${Date.now()}.xlsx`)
}

function submitImport() {
  if (!selectedFile.value) {
    proxy.$modal.msgError('请选择 xls 或 xlsx 文件')
    return
  }
  const formData = new FormData()
  formData.append('file', selectedFile.value)
  importLoading.value = true
  importOpportunity(formData).then(res => {
    importResult.value = res.data
    importLoading.value = false
    if (res.data.successCount > 0) {
      getList()
      setTimeout(() => {
        importOpen.value = false
        proxy.$modal.msgSuccess(`成功导入 ${res.data.successCount} 条商机`)
      }, 1500)
    }
  }).catch(() => {
    importLoading.value = false
  })
}

function importRowClassName({ row }) {
  if (row.type === 'skip') return 'import-row-skip'
  if (row.type === 'success') return 'import-row-success'
  return 'import-row-error'
}

loadOptions()
getList()
</script>

<style scoped>
.import-result-panel {
  margin-top: 16px;
}
.summary-cards {
  margin-bottom: 8px;
}
.summary-card {
  text-align: center;
  border: none;
}
.summary-num {
  font-size: 28px;
  font-weight: bold;
  line-height: 1.2;
}
.summary-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}
.success-card .summary-num { color: #67c23a; }
.fail-card .summary-num { color: #f56c6c; }
.skip-card .summary-num { color: #e6a23c; }
:deep(.import-row-error) {
  background-color: #fef0f0 !important;
}
:deep(.import-row-skip) {
  background-color: #fdf6ec !important;
}
:deep(.import-row-success) {
  background-color: #f0f9eb !important;
}
</style>
