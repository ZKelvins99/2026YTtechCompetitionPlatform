<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="联系人" prop="contactName">
        <el-input v-model="queryParams.contactName" placeholder="请输入联系人姓名" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="所属客户" prop="customerId">
        <el-select v-model="queryParams.customerId" placeholder="请选择客户" clearable filterable style="width: 200px">
          <el-option v-for="item in customerOptions" :key="item.id" :label="item.customerName" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['crm:contact:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()" v-hasPermi="['crm:contact:remove']">删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="contactList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="联系人" align="center" prop="contactName" width="120" />
      <el-table-column label="所属客户" align="center" prop="customerName" min-width="140" show-overflow-tooltip />
      <el-table-column label="联系方式类型" align="center" prop="contactTypeName" width="110" />
      <el-table-column label="联系方式" align="center" prop="contactValue" width="150" />
      <el-table-column label="职位" align="center" prop="position" width="100" />
      <el-table-column label="首要联系人" align="center" prop="isPrimary" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.isPrimary === '1' ? 'success' : 'info'">{{ scope.row.isPrimary === '1' ? '是' : '否' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="150">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['crm:contact:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['crm:contact:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="560px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="所属客户" prop="customerId">
          <el-select v-model="form.customerId" placeholder="请选择客户" filterable style="width: 100%">
            <el-option v-for="item in customerOptions" :key="item.id" :label="item.customerName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="联系人姓名" prop="contactName">
          <el-input v-model="form.contactName" placeholder="请输入联系人姓名" />
        </el-form-item>
        <el-form-item label="联系方式类型" prop="contactTypeId">
          <el-select v-model="form.contactTypeId" placeholder="请选择" style="width: 100%">
            <el-option v-for="item in contactTypeOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="联系方式" prop="contactValue">
          <el-input v-model="form.contactValue" placeholder="请输入联系方式" />
        </el-form-item>
        <el-form-item label="职位" prop="position">
          <el-input v-model="form.position" placeholder="请输入职位" />
        </el-form-item>
        <el-form-item label="首要联系人" prop="isPrimary">
          <el-radio-group v-model="form.isPrimary">
            <el-radio value="1">是</el-radio>
            <el-radio value="0">否</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="CrmContact">
import { listContact, getContact, addContact, updateContact, delContact, listCustomerOptions } from '@/api/crm/contact'
import { getCrmDict } from '@/api/crm/customer'

const { proxy } = getCurrentInstance()

const contactList = ref([])
const customerOptions = ref([])
const contactTypeOptions = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const multiple = ref(true)
const total = ref(0)
const title = ref('')

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    contactName: undefined,
    customerId: undefined
  },
  rules: {
    customerId: [{ required: true, message: '请选择所属客户', trigger: 'change' }],
    contactName: [{ required: true, message: '联系人姓名不能为空', trigger: 'blur' }]
  }
})

const { queryParams, form, rules } = toRefs(data)

function getList() {
  loading.value = true
  listContact(queryParams.value).then(response => {
    contactList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

function loadOptions() {
  listCustomerOptions().then(res => {
    customerOptions.value = res.rows || []
  })
  getCrmDict('contact_type').then(res => {
    contactTypeOptions.value = res.data || []
  })
}

function cancel() {
  open.value = false
  reset()
}

function reset() {
  form.value = {
    id: undefined,
    customerId: undefined,
    contactName: undefined,
    contactTypeId: undefined,
    contactValue: undefined,
    position: undefined,
    isPrimary: '0'
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
  title.value = '新增联系人'
}

function handleUpdate(row) {
  reset()
  getContact(row.id).then(res => {
    form.value = res.data
    open.value = true
    title.value = '修改联系人'
  })
}

function submitForm() {
  proxy.$refs['formRef'].validate(valid => {
    if (!valid) return
    const action = form.value.id ? updateContact(form.value) : addContact(form.value)
    action.then(() => {
      proxy.$modal.msgSuccess(form.value.id ? '修改成功' : '新增成功')
      open.value = false
      getList()
    })
  })
}

function handleDelete(row) {
  const delIds = row?.id || ids.value
  proxy.$modal.confirm('是否确认删除选中的联系人？').then(() => delContact(delIds)).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

loadOptions()
getList()
</script>
