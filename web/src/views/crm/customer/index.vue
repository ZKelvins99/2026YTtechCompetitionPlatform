<template>
  <div class="app-container crm-page">
    <crm-page-header title="客户管理" v-bind="CRM_PAGE_INTRO.customer" />
    <el-card shadow="never" class="crm-panel crm-search-panel" v-show="showSearch">
    <el-form :model="queryParams" ref="queryRef" :inline="true">
      <el-form-item label="客户名称" prop="customerName">
        <el-input v-model="queryParams.customerName" placeholder="请输入客户名称" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="客户等级" prop="levelId">
        <el-select v-model="queryParams.levelId" placeholder="请选择等级" clearable style="width: 160px">
          <el-option v-for="item in levelOptions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="行业" prop="industry">
        <el-input v-model="queryParams.industry" placeholder="请输入行业" clearable style="width: 160px" @keyup.enter="handleQuery" />
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
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['crm:customer:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()" v-hasPermi="['crm:customer:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['crm:customer:export']">导出</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-dropdown trigger="click" @command="handleColumnCommand">
          <el-button plain icon="Setting">列设置</el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-checkbox-group v-model="visibleColumns" @change="saveColumnSettings">
                <div class="column-setting-panel">
                  <el-checkbox v-for="col in allColumns" :key="col.prop" :label="col.prop">{{ col.label }}</el-checkbox>
                </div>
              </el-checkbox-group>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table ref="tableRef" v-loading="loading" :data="customerList" @selection-change="handleSelectionChange" @sort-change="handleSortChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column v-if="isColVisible('customerNo')" label="客户编号" align="center" prop="customerNo" width="160" sortable="custom" :sort-orders="['descending', 'ascending']" />
      <el-table-column v-if="isColVisible('customerName')" label="客户名称" align="center" prop="customerName" min-width="140" sortable="custom" :sort-orders="['descending', 'ascending']" show-overflow-tooltip />
      <el-table-column v-if="isColVisible('levelName')" label="客户等级" align="center" prop="levelName" width="100" />
      <el-table-column v-if="isColVisible('industry')" label="行业" align="center" prop="industry" width="120" sortable="custom" :sort-orders="['descending', 'ascending']" />
      <el-table-column v-if="isColVisible('province')" label="省份" align="center" prop="province" width="90" />
      <el-table-column v-if="isColVisible('city')" label="城市" align="center" prop="city" width="90" />
      <el-table-column v-if="isColVisible('phone')" label="联系电话" align="center" prop="phone" width="130" />
      <el-table-column v-if="isColVisible('email')" label="邮箱" align="center" prop="email" width="160" show-overflow-tooltip />
      <el-table-column v-if="isColVisible('createTime')" label="创建时间" align="center" prop="createTime" width="160" sortable="custom" :sort-orders="['descending', 'ascending']">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="150" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['crm:customer:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['crm:customer:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
    </el-card>

    <el-dialog :title="title" v-model="open" width="680px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="客户名称" prop="customerName">
              <el-input v-model="form.customerName" placeholder="请输入客户名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="客户等级" prop="levelId">
              <el-select v-model="form.levelId" placeholder="请选择" style="width: 100%">
                <el-option v-for="item in levelOptions" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属行业" prop="industry">
              <el-input v-model="form.industry" placeholder="请输入行业" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话" prop="phone">
              <el-input v-model="form.phone" placeholder="请输入联系电话" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="省份" prop="province">
              <el-input v-model="form.province" placeholder="请输入省份" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="城市" prop="city">
              <el-input v-model="form.city" placeholder="请输入城市" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="详细地址" prop="address">
              <el-input v-model="form.address" placeholder="请输入详细地址" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="请输入邮箱" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="CrmCustomer">
import { listCustomer, getCustomer, addCustomer, updateCustomer, delCustomer, getCrmDict } from '@/api/crm/customer'
import { CRM_PAGE_INTRO } from '@/constants/crmPageIntro'

const { proxy } = getCurrentInstance()
const COLUMN_STORAGE_KEY = 'crm_customer_columns'

const customerList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const multiple = ref(true)
const total = ref(0)
const title = ref('')
const levelOptions = ref([])

const allColumns = [
  { prop: 'customerNo', label: '客户编号' },
  { prop: 'customerName', label: '客户名称' },
  { prop: 'levelName', label: '客户等级' },
  { prop: 'industry', label: '行业' },
  { prop: 'province', label: '省份' },
  { prop: 'city', label: '城市' },
  { prop: 'phone', label: '联系电话' },
  { prop: 'email', label: '邮箱' },
  { prop: 'createTime', label: '创建时间' }
]

const defaultVisible = allColumns.map(c => c.prop)
const visibleColumns = ref(loadColumnSettings())

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    customerName: undefined,
    levelId: undefined,
    industry: undefined,
    orderByColumn: undefined,
    isAsc: undefined
  },
  rules: {
    customerName: [{ required: true, message: '客户名称不能为空', trigger: 'blur' }]
  }
})

const { queryParams, form, rules } = toRefs(data)

function loadColumnSettings() {
  try {
    const saved = localStorage.getItem(COLUMN_STORAGE_KEY)
    if (saved) {
      return JSON.parse(saved)
    }
  } catch (e) { /* ignore */ }
  return [...defaultVisible]
}

function saveColumnSettings() {
  localStorage.setItem(COLUMN_STORAGE_KEY, JSON.stringify(visibleColumns.value))
}

function isColVisible(prop) {
  return visibleColumns.value.includes(prop)
}

function handleColumnCommand() {}

function getList() {
  loading.value = true
  listCustomer(queryParams.value).then(response => {
    customerList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

function loadLevelOptions() {
  getCrmDict('customer_level').then(res => {
    levelOptions.value = res.data || []
  })
}

function cancel() {
  open.value = false
  reset()
}

function reset() {
  form.value = {
    id: undefined,
    customerName: undefined,
    levelId: undefined,
    industry: undefined,
    province: undefined,
    city: undefined,
    address: undefined,
    phone: undefined,
    email: undefined,
    remark: undefined
  }
  proxy.resetForm('formRef')
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  queryParams.value.orderByColumn = undefined
  queryParams.value.isAsc = undefined
  handleQuery()
}

function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id)
  multiple.value = !selection.length
}

function handleSortChange(column) {
  queryParams.value.orderByColumn = column.prop
  queryParams.value.isAsc = column.order
  getList()
}

function handleAdd() {
  reset()
  open.value = true
  title.value = '新增客户'
}

function handleUpdate(row) {
  reset()
  getCustomer(row.id).then(res => {
    form.value = res.data
    open.value = true
    title.value = '修改客户'
  })
}

function submitForm() {
  proxy.$refs['formRef'].validate(valid => {
    if (!valid) return
    const action = form.value.id ? updateCustomer(form.value) : addCustomer(form.value)
    action.then(() => {
      proxy.$modal.msgSuccess(form.value.id ? '修改成功' : '新增成功')
      open.value = false
      getList()
    })
  })
}

function handleDelete(row) {
  const delIds = row?.id || ids.value
  proxy.$modal.confirm('是否确认删除选中的客户数据？').then(() => delCustomer(delIds)).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

function handleExport() {
  proxy.download('crm/customer/export', { ...queryParams.value }, `customer_${Date.now()}.xlsx`)
}

loadLevelOptions()
getList()
</script>

<style scoped>
.column-setting-panel {
  padding: 8px 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 140px;
}
</style>
