<template>
  <div class="app-container crm-page">
    <crm-page-header title="消息模板" v-bind="CRM_PAGE_INTRO.messageTemplate" />
    <el-card shadow="never" class="crm-panel crm-search-panel" v-show="showSearch">
    <el-form :model="queryParams" ref="queryRef" :inline="true">
      <el-form-item label="模板名称" prop="templateName">
        <el-input v-model="queryParams.templateName" placeholder="请输入模板名称" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="模板类型" prop="templateType">
        <el-select v-model="queryParams.templateType" placeholder="请选择" clearable style="width: 160px">
          <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
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
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['crm:message:template:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()" v-hasPermi="['crm:message:template:remove']">删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="templateList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="模板名称" prop="templateName" min-width="140" show-overflow-tooltip />
      <el-table-column label="模板类型" prop="templateType" width="120" align="center">
        <template #default="scope">
          <el-tag :type="typeTag(scope.row.templateType)" size="small">{{ scope.row.templateType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="消息标题" prop="title" min-width="160" show-overflow-tooltip />
      <el-table-column label="状态" prop="status" width="80" align="center">
        <template #default="scope">
          <el-tag :type="scope.row.status === '0' ? 'success' : 'info'" size="small">{{ scope.row.status === '0' ? '正常' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" prop="createTime" width="170" align="center">
        <template #default="scope">{{ parseTime(scope.row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="150" align="center">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['crm:message:template:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['crm:message:template:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
    </el-card>

    <el-dialog :title="title" v-model="open" width="640px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="模板名称" prop="templateName">
          <el-input v-model="form.templateName" placeholder="请输入模板名称" />
        </el-form-item>
        <el-form-item label="模板类型" prop="templateType">
          <el-select v-model="form.templateType" placeholder="请选择" style="width: 100%">
            <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="消息标题" prop="title">
          <el-input v-model="form.title" placeholder="支持变量，如 {客户名称}" />
        </el-form-item>
        <el-form-item label="消息内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="5" placeholder="支持变量占位，如 {客户名称}、{合同编号}" />
        </el-form-item>
        <el-form-item label="可用变量" prop="variables">
          <el-input v-model="form.variables" placeholder='JSON 数组，如 ["客户名称","合同编号"]' />
          <div class="form-tip">在标题/内容中使用 {变量名} 占位，发送时填写具体值</div>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio value="0">正常</el-radio>
            <el-radio value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="open = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="CrmMessageTemplate">
import { listTemplate, getTemplate, addTemplate, updateTemplate, delTemplate } from '@/api/crm/message'
import { CRM_PAGE_INTRO } from '@/constants/crmPageIntro'

const { proxy } = getCurrentInstance()

const typeOptions = [
  { label: '合同提醒', value: '合同提醒' },
  { label: '商机提醒', value: '商机提醒' },
  { label: '系统通知', value: '系统通知' }
]

const templateList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const open = ref(false)
const title = ref('')
const ids = ref([])
const multiple = ref(true)

const data = reactive({
  form: {},
  queryParams: { pageNum: 1, pageSize: 10, templateName: undefined, templateType: undefined },
  rules: {
    templateName: [{ required: true, message: '模板名称不能为空', trigger: 'blur' }],
    templateType: [{ required: true, message: '请选择模板类型', trigger: 'change' }],
    title: [{ required: true, message: '消息标题不能为空', trigger: 'blur' }],
    content: [{ required: true, message: '消息内容不能为空', trigger: 'blur' }]
  }
})
const { form, queryParams, rules } = toRefs(data)

function typeTag(type) {
  if (type === '合同提醒') return 'warning'
  if (type === '商机提醒') return 'success'
  return 'primary'
}

function getList() {
  loading.value = true
  listTemplate(queryParams.value).then(res => {
    templateList.value = res.rows
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

function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id)
  multiple.value = !selection.length
}

function resetForm() {
  form.value = { id: undefined, templateName: undefined, templateType: undefined, title: undefined, content: undefined, variables: undefined, status: '0' }
  proxy.resetForm('formRef')
}

function handleAdd() {
  resetForm()
  open.value = true
  title.value = '新增消息模板'
}

function handleUpdate(row) {
  resetForm()
  getTemplate(row.id).then(res => {
    form.value = res.data
    open.value = true
    title.value = '修改消息模板'
  })
}

function submitForm() {
  proxy.$refs.formRef.validate(valid => {
    if (!valid) return
    const req = form.value.id ? updateTemplate(form.value) : addTemplate(form.value)
    req.then(() => {
      proxy.$modal.msgSuccess(form.value.id ? '修改成功' : '新增成功')
      open.value = false
      getList()
    })
  })
}

function handleDelete(row) {
  const delIds = row?.id || ids.value
  proxy.$modal.confirm('是否确认删除所选模板？').then(() => delTemplate(delIds)).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

getList()
</script>

<style scoped>
.form-tip { font-size: 12px; color: #909399; margin-top: 4px; }
</style>
