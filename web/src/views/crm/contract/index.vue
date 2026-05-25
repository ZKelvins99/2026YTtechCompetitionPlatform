<template>
  <div class="app-container crm-page">
    <crm-page-header title="合同管理" v-bind="CRM_PAGE_INTRO.contract" />
    <el-card shadow="never" class="crm-panel crm-search-panel" v-show="showSearch">
    <el-form :model="queryParams" ref="queryRef" :inline="true">
      <el-form-item label="合同名称" prop="contractName">
        <el-input v-model="queryParams.contractName" placeholder="请输入合同名称" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="合同编号" prop="contractNo">
        <el-input v-model="queryParams.contractNo" placeholder="请输入合同编号" clearable style="width: 160px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="statusId">
        <el-select v-model="queryParams.statusId" placeholder="请选择" clearable style="width: 140px">
          <el-option v-for="item in statusOptions" :key="item.id" :label="item.name" :value="item.id" />
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
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['crm:contract:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()" v-hasPermi="['crm:contract:remove']">删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="contractList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="合同编号" prop="contractNo" width="160" show-overflow-tooltip />
      <el-table-column label="合同名称" prop="contractName" min-width="140" show-overflow-tooltip />
      <el-table-column label="客户" prop="customerName" width="120" show-overflow-tooltip />
      <el-table-column label="金额" prop="contractAmount" width="120" align="right">
        <template #default="scope">{{ scope.row.contractAmount?.toLocaleString() }}</template>
      </el-table-column>
      <el-table-column label="状态" prop="statusName" width="100" align="center">
        <template #default="scope">
          <el-tag :type="statusTagType(scope.row.statusCode)">{{ scope.row.statusName }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" prop="createTime" width="160" align="center">
        <template #default="scope">{{ parseTime(scope.row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="220" align="center" fixed="right">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['crm:contract:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['crm:contract:remove']">删除</el-button>
          <el-button v-if="scope.row.statusCode === 'DRAFT'" link type="success" icon="Promotion" @click="handleStartApproval(scope.row)" v-hasPermi="['crm:workflow:start']">发起审批</el-button>
          <el-button v-if="scope.row.workflowInstanceId" link type="warning" icon="View" @click="goWorkflow(scope.row.workflowInstanceId)" v-hasPermi="['crm:workflow:query']">审批流程</el-button>
          <el-button v-if="scope.row.statusCode === 'APPROVING' && !scope.row.workflowInstanceId" link type="warning" icon="View" @click="findAndGoWorkflow(scope.row)" v-hasPermi="['crm:workflow:query']">审批流程</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
    </el-card>

    <el-dialog title="发起合同审批" v-model="approvalOpen" width="520px" append-to-body>
      <el-form ref="approvalRef" :model="approvalForm" :rules="approvalRules" label-width="110px">
        <el-form-item label="合同">
          <span>{{ approvalContractName }}</span>
        </el-form-item>
        <el-form-item label="部门审批人" prop="deptApproverId">
          <el-select v-model="approvalForm.deptApproverId" filterable placeholder="请选择部门审批人" style="width:100%">
            <el-option v-for="u in userOptions" :key="u.userId" :label="u.nickName" :value="u.userId" />
          </el-select>
        </el-form-item>
        <el-form-item label="财务审批人" prop="financeApproverId">
          <el-select v-model="approvalForm.financeApproverId" filterable placeholder="请选择财务审批人" style="width:100%">
            <el-option v-for="u in userOptions" :key="u.userId" :label="u.nickName" :value="u.userId" />
          </el-select>
        </el-form-item>
        <el-form-item label="归档审批人" prop="archiveApproverId">
          <el-select v-model="approvalForm.archiveApproverId" filterable placeholder="请选择归档审批人" style="width:100%">
            <el-option v-for="u in userOptions" :key="u.userId" :label="u.nickName" :value="u.userId" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" :loading="approvalSubmitting" @click="submitStartApproval">确 定</el-button>
        <el-button @click="approvalOpen = false">取 消</el-button>
      </template>
    </el-dialog>

    <el-dialog :title="title" v-model="open" width="640px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="合同名称" prop="contractName">
          <el-input v-model="form.contractName" placeholder="请输入合同名称" />
        </el-form-item>
        <el-form-item label="关联客户" prop="customerId">
          <el-select v-model="form.customerId" filterable placeholder="请选择客户" style="width:100%">
            <el-option v-for="item in customerOptions" :key="item.id" :label="item.customerName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="合同金额" prop="contractAmount">
          <el-input-number v-model="form.contractAmount" :min="0" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="签订日期" prop="signDate">
          <el-date-picker v-model="form.signDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="生效日期" prop="startDate">
          <el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="到期日期" prop="endDate">
          <el-date-picker v-model="form.endDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="合同内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="4" placeholder="请输入合同内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="CrmContract">
import { listContract, getContract, addContract, updateContract, delContract } from '@/api/crm/contract'
import { getCrmDict } from '@/api/crm/customer'
import { CRM_PAGE_INTRO } from '@/constants/crmPageIntro'
import { listCustomerOptions } from '@/api/crm/contact'
import { startWorkflow } from '@/api/crm/workflow'
import { listUser } from '@/api/system/user'

const { proxy } = getCurrentInstance()
const router = useRouter()

const contractList = ref([])
const customerOptions = ref([])
const statusOptions = ref([])
const loading = ref(true)
const showSearch = ref(true)
const open = ref(false)
const title = ref('')
const ids = ref([])
const multiple = ref(true)
const total = ref(0)
const approvalOpen = ref(false)
const approvalSubmitting = ref(false)
const approvalContractName = ref('')
const approvalContractId = ref(null)
const userOptions = ref([])
const approvalForm = ref({
  deptApproverId: undefined,
  financeApproverId: undefined,
  archiveApproverId: undefined
})
const approvalRules = {
  deptApproverId: [{ required: true, message: '请选择部门审批人', trigger: 'change' }],
  financeApproverId: [{ required: true, message: '请选择财务审批人', trigger: 'change' }],
  archiveApproverId: [{ required: true, message: '请选择归档审批人', trigger: 'change' }]
}

const data = reactive({
  form: {},
  queryParams: { pageNum: 1, pageSize: 10, contractName: undefined, contractNo: undefined, statusId: undefined },
  rules: {
    contractName: [{ required: true, message: '合同名称不能为空', trigger: 'blur' }],
    customerId: [{ required: true, message: '请选择客户', trigger: 'change' }],
    contractAmount: [{ required: true, message: '请输入合同金额', trigger: 'blur' }]
  }
})
const { queryParams, form, rules } = toRefs(data)

function statusTagType(code) {
  const map = { DRAFT: 'info', APPROVING: 'warning', ACTIVE: 'success', TERMINATED: 'danger', ARCHIVED: '' }
  return map[code] || ''
}

function getList() {
  loading.value = true
  listContract(queryParams.value).then(res => {
    contractList.value = res.rows
    total.value = res.total
    loading.value = false
  })
}

function loadOptions() {
  listCustomerOptions().then(res => { customerOptions.value = res.rows || [] })
  getCrmDict('contract_status').then(res => { statusOptions.value = res.data || [] })
}

function cancel() { open.value = false; reset() }
function reset() {
  form.value = { id: undefined, contractName: undefined, customerId: undefined, contractAmount: undefined, signDate: undefined, startDate: undefined, endDate: undefined, content: undefined }
  proxy.resetForm('formRef')
}
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function handleSelectionChange(sel) { ids.value = sel.map(i => i.id); multiple.value = !sel.length }
function handleAdd() { reset(); open.value = true; title.value = '新增合同' }
function handleUpdate(row) {
  reset()
  getContract(row.id).then(res => { form.value = res.data; open.value = true; title.value = '修改合同' })
}
function submitForm() {
  proxy.$refs.formRef.validate(valid => {
    if (!valid) return
    const action = form.value.id ? updateContract(form.value) : addContract(form.value)
    action.then(() => { proxy.$modal.msgSuccess('保存成功'); open.value = false; getList() })
  })
}
function handleDelete(row) {
  const delIds = row?.id || ids.value
  proxy.$modal.confirm('是否确认删除？').then(() => delContract(delIds)).then(() => { getList(); proxy.$modal.msgSuccess('删除成功') }).catch(() => {})
}
function loadUserOptions() {
  if (userOptions.value.length) return
  listUser({ pageNum: 1, pageSize: 200, status: '0' }).then(res => {
    userOptions.value = res.rows || []
  })
}

function handleStartApproval(row) {
  loadUserOptions()
  approvalContractId.value = row.id
  approvalContractName.value = row.contractName
  approvalForm.value = { deptApproverId: undefined, financeApproverId: undefined, archiveApproverId: undefined }
  approvalOpen.value = true
}

function submitStartApproval() {
  proxy.$refs.approvalRef.validate(valid => {
    if (!valid) return
    approvalSubmitting.value = true
    startWorkflow({
      contractId: approvalContractId.value,
      deptApproverId: approvalForm.value.deptApproverId,
      financeApproverId: approvalForm.value.financeApproverId,
      archiveApproverId: approvalForm.value.archiveApproverId
    }).then(res => {
      proxy.$modal.msgSuccess('审批流程已发起')
      approvalOpen.value = false
      getList()
      goWorkflow(res.data.id)
    }).finally(() => { approvalSubmitting.value = false })
  })
}
function goWorkflow(instanceId) {
  router.push('/crm/workflow/index/' + instanceId)
}
function findAndGoWorkflow(row) {
  getContract(row.id).then(res => {
    if (res.data.workflowInstanceId) goWorkflow(res.data.workflowInstanceId)
    else proxy.$modal.msgWarning('未找到进行中的审批流程')
  })
}

loadOptions()
getList()
</script>
