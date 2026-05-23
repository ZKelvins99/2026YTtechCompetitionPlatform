<template>
  <div class="app-container crm-page">
    <crm-page-header title="发送消息" description="引用已有模板，选择接收人并完成消息发送。" />
    <el-card shadow="never" class="crm-panel">
      <template #header><span>发送 CRM 消息</span></template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" style="max-width: 720px">
        <el-form-item label="选择模板" prop="templateId">
          <el-select v-model="form.templateId" placeholder="请选择消息模板" style="width: 100%" @change="onTemplateChange">
            <el-option v-for="item in templateOptions" :key="item.id" :label="item.templateName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="接收人" prop="receiverId">
          <el-select v-model="form.receiverId" placeholder="请选择接收人" filterable style="width: 100%">
            <el-option v-for="u in userList" :key="u.userId" :label="u.nickName + ' (' + u.userName + ')'" :value="u.userId" />
          </el-select>
        </el-form-item>

        <template v-if="variableKeys.length">
          <el-divider content-position="left">模板变量</el-divider>
          <el-form-item v-for="key in variableKeys" :key="key" :label="key">
            <el-input v-model="form.variables[key]" :placeholder="'请输入 ' + key" @input="updatePreview" />
          </el-form-item>
        </template>

        <el-divider content-position="left">渲染预览</el-divider>
        <el-form-item label="标题预览">
          <el-input :model-value="previewTitle" readonly />
        </el-form-item>
        <el-form-item label="内容预览">
          <el-input :model-value="previewContent" type="textarea" :rows="4" readonly />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="sending" @click="handleSend" v-hasPermi="['crm:message:send']">发送消息</el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup name="CrmMessageSend">
import { getSendTemplateList, sendMessage } from '@/api/crm/message'
import { listUser } from '@/api/system/user'
import useCrmMessageStore from '@/stores/crm/message'

const { proxy } = getCurrentInstance()
const crmMessageStore = useCrmMessageStore()

const templateOptions = ref([])
const userList = ref([])
const selectedTemplate = ref(null)
const variableKeys = ref([])
const previewTitle = ref('')
const previewContent = ref('')
const sending = ref(false)

const data = reactive({
  form: { templateId: undefined, receiverId: undefined, variables: {} },
  rules: {
    templateId: [{ required: true, message: '请选择模板', trigger: 'change' }],
    receiverId: [{ required: true, message: '请选择接收人', trigger: 'change' }]
  }
})
const { form, rules } = toRefs(data)

function parseVariableKeys(tpl) {
  if (!tpl) return []
  if (tpl.variables) {
    try {
      const arr = JSON.parse(tpl.variables)
      if (Array.isArray(arr)) return arr
    } catch { /* fall through */ }
  }
  const matches = (tpl.title + tpl.content).match(/\{([^}]+)\}/g) || []
  return [...new Set(matches.map(m => m.slice(1, -1)))]
}

function renderText(text, vars) {
  if (!text) return ''
  let result = text
  Object.keys(vars || {}).forEach(key => {
    result = result.split('{' + key + '}').join(vars[key] || '')
  })
  return result
}

function updatePreview() {
  if (!selectedTemplate.value) {
    previewTitle.value = ''
    previewContent.value = ''
    return
  }
  previewTitle.value = renderText(selectedTemplate.value.title, form.value.variables)
  previewContent.value = renderText(selectedTemplate.value.content, form.value.variables)
}

function onTemplateChange(id) {
  selectedTemplate.value = templateOptions.value.find(t => t.id === id) || null
  const keys = parseVariableKeys(selectedTemplate.value)
  variableKeys.value = keys
  const vars = {}
  keys.forEach(k => { vars[k] = form.value.variables[k] || '' })
  form.value.variables = vars
  updatePreview()
}

function resetForm() {
  form.value = { templateId: undefined, receiverId: undefined, variables: {} }
  selectedTemplate.value = null
  variableKeys.value = []
  previewTitle.value = ''
  previewContent.value = ''
  proxy.resetForm('formRef')
}

function handleSend() {
  proxy.$refs.formRef.validate(valid => {
    if (!valid) return
    sending.value = true
    sendMessage({
      templateId: form.value.templateId,
      receiverId: form.value.receiverId,
      variables: form.value.variables
    }).then(() => {
      proxy.$modal.msgSuccess('发送成功')
      crmMessageStore.fetchUnread()
      resetForm()
    }).finally(() => { sending.value = false })
  })
}

onMounted(() => {
  getSendTemplateList().then(res => { templateOptions.value = res.data || [] })
  listUser({ pageNum: 1, pageSize: 200, status: '0' }).then(res => { userList.value = res.rows || [] })
})
</script>
