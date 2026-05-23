<template>
  <div class="app-container workflow-page">
    <el-card shadow="never" class="bpmn-card">
      <template #header>
        <span>合同审批流程 — {{ instance.businessNo }}</span>
        <el-tag class="ml-2" :type="instanceStatusType">{{ instanceStatusText }}</el-tag>
      </template>
      <div ref="bpmnContainer" class="bpmn-container"></div>
    </el-card>

    <el-card v-if="instance.status === '0'" shadow="never" class="action-card">
      <el-form label-width="80px">
        <el-form-item label="审批意见">
          <el-input v-model="opinion" type="textarea" :rows="3" placeholder="请输入审批意见" />
        </el-form-item>
        <el-form-item>
          <el-button type="success" @click="handleApprove" v-hasPermi="['crm:workflow:approve']">通过</el-button>
          <el-button type="danger" @click="handleReject" v-hasPermi="['crm:workflow:approve']">驳回</el-button>
          <el-button @click="openCountersign = true" v-hasPermi="['crm:workflow:approve']">会签</el-button>
          <el-button @click="openTransfer = true" v-hasPermi="['crm:workflow:approve']">转办</el-button>
          <el-dropdown @command="handleRollback" v-hasPermi="['crm:workflow:approve']">
            <el-button class="ml-1">回退<el-icon class="el-icon--right"><arrow-down /></el-icon></el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-for="n in rollbackNodes" :key="n.id" :command="n.id">{{ n.nodeName }}</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-button type="danger" plain @click="handleTerminate" v-hasPermi="['crm:workflow:approve']">终止</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="timeline-card">
      <template #header>审批历史</template>
      <el-timeline>
        <el-timeline-item
          v-for="node in timelineNodes"
          :key="node.id"
          :type="timelineType(node.status)"
          :timestamp="parseTime(node.approveTime) || '待处理'"
          placement="top"
        >
          <p><strong>{{ node.nodeName }}</strong> — {{ node.approverName || '未指定' }}</p>
          <p>{{ actionText(node.status) }} {{ node.opinion ? '：' + node.opinion : '' }}</p>
        </el-timeline-item>
      </el-timeline>
    </el-card>

    <el-dialog title="会签" v-model="openCountersign" width="480px" append-to-body>
      <el-select v-model="countersignUsers" multiple filterable placeholder="选择会签审批人" style="width:100%">
        <el-option v-for="u in userList" :key="u.userId" :label="u.nickName" :value="u.userId" />
      </el-select>
      <template #footer>
        <el-button type="primary" @click="submitCountersign">确 定</el-button>
      </template>
    </el-dialog>

    <el-dialog title="转办" v-model="openTransfer" width="480px" append-to-body>
      <el-select v-model="transferUserId" filterable placeholder="选择转办人" style="width:100%">
        <el-option v-for="u in userList" :key="u.userId" :label="u.nickName" :value="u.userId" />
      </el-select>
      <template #footer>
        <el-button type="primary" @click="submitTransfer">确 定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="CrmWorkflow">
import BpmnViewer from 'bpmn-js/lib/Viewer'
import 'bpmn-js/dist/assets/diagram-js.css'
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn.css'
import { ArrowDown } from '@element-plus/icons-vue'
import { listUser } from '@/api/system/user'
import {
  getWorkflowInstance, getWorkflowBpmn, approveWorkflow, rejectWorkflow,
  countersignWorkflow, transferWorkflow, rollbackWorkflow, terminateWorkflow
} from '@/api/crm/workflow'

const route = useRoute()
const { proxy } = getCurrentInstance()

const instanceId = computed(() => route.params.instanceId)
const bpmnContainer = ref(null)
const instance = ref({ nodes: [], status: '0' })
const opinion = ref('')
const openCountersign = ref(false)
const openTransfer = ref(false)
const countersignUsers = ref([])
const transferUserId = ref(null)
const userList = ref([])
let viewer = null

const instanceStatusText = computed(() => ({ '0': '审批中', '1': '已完成', '2': '已终止' }[instance.value.status] || ''))
const instanceStatusType = computed(() => ({ '0': 'warning', '1': 'success', '2': 'danger' }[instance.value.status] || 'info'))
const timelineNodes = computed(() => instance.value.nodes || [])
const rollbackNodes = computed(() => (instance.value.nodes || []).filter(n => n.approvalType === 'SINGLE'))

function actionText(status) {
  return { '0': '待审批', '1': '通过', '2': '驳回', '3': '转办', '4': '回退' }[status] || ''
}
function timelineType(status) {
  return { '1': 'success', '2': 'danger', '3': 'warning', '4': 'info' }[status] || 'primary'
}

async function loadData() {
  const [instRes, bpmnRes] = await Promise.all([
    getWorkflowInstance(instanceId.value),
    getWorkflowBpmn(instanceId.value)
  ])
  instance.value = instRes.data
  await renderBpmn(bpmnRes.data)
}

async function renderBpmn(xml) {
  if (viewer) { viewer.destroy(); viewer = null }
  viewer = new BpmnViewer({ container: bpmnContainer.value })
  await viewer.importXML(xml)
  applyNodeColors()
  const canvas = viewer.get('canvas')
  canvas.zoom('fit-viewport')
}

function applyNodeColors() {
  if (!viewer || !instance.value.nodes) return
  const canvas = viewer.get('canvas')
  const registry = viewer.get('elementRegistry')
  const orderStatus = {}
  instance.value.nodes.forEach(n => {
    const id = n.bpmnElementId
    if (!id) return
    const cur = orderStatus[n.nodeOrder] || 'pending'
    if (n.status === '2') orderStatus[n.nodeOrder] = 'reject'
    else if (n.status === '1' && cur !== 'reject') orderStatus[n.nodeOrder] = 'pass'
    else if (n.id === instance.value.currentNodeId) orderStatus[n.nodeOrder] = 'current'
  })
  const bpmnIds = ['Task_Submit', 'Task_Dept', 'Task_Finance', 'Task_Archive']
  bpmnIds.forEach((id, idx) => {
    const el = registry.get(id)
    if (!el) return
    const st = orderStatus[idx + 1] || 'pending'
    const cls = { pass: 'highlight-green', current: 'highlight-blue', reject: 'highlight-red', pending: 'highlight-gray' }[st]
    canvas.addMarker(el.id, cls)
  })
}

function basePayload() {
  return { instanceId: Number(instanceId.value), opinion: opinion.value }
}

function handleApprove() {
  approveWorkflow(basePayload()).then(() => { proxy.$modal.msgSuccess('审批通过'); refresh() })
}
function handleReject() {
  rejectWorkflow(basePayload()).then(() => { proxy.$modal.msgSuccess('已驳回'); refresh() })
}
function submitCountersign() {
  countersignWorkflow({ ...basePayload(), countersignUserIds: countersignUsers.value }).then(() => {
    openCountersign.value = false; proxy.$modal.msgSuccess('会签人已添加'); refresh()
  })
}
function submitTransfer() {
  transferWorkflow({ ...basePayload(), approverId: transferUserId.value }).then(() => {
    openTransfer.value = false; proxy.$modal.msgSuccess('转办成功'); refresh()
  })
}
function handleRollback(targetNodeId) {
  rollbackWorkflow(targetNodeId, basePayload()).then(() => { proxy.$modal.msgSuccess('已回退'); refresh() })
}
function handleTerminate() {
  proxy.$modal.confirm('确认终止该审批流程？').then(() => {
    return terminateWorkflow(basePayload())
  }).then(() => { proxy.$modal.msgSuccess('流程已终止'); refresh() }).catch(() => {})
}

function refresh() {
  opinion.value = ''
  loadData()
}

function loadUsers() {
  listUser({ pageNum: 1, pageSize: 200, status: '0' }).then(res => { userList.value = res.rows || [] })
}

onMounted(() => { loadUsers(); loadData() })
onBeforeUnmount(() => { if (viewer) viewer.destroy() })
</script>

<style scoped>
.workflow-page .bpmn-container { height: 280px; }
.bpmn-card, .action-card, .timeline-card { margin-bottom: 16px; }
.ml-2 { margin-left: 8px; }
:deep(.highlight-green .djs-visual > :nth-child(1)) { stroke: #67c23a !important; fill: #f0f9eb !important; }
:deep(.highlight-blue .djs-visual > :nth-child(1)) { stroke: #409eff !important; fill: #ecf5ff !important; }
:deep(.highlight-red .djs-visual > :nth-child(1)) { stroke: #f56c6c !important; fill: #fef0f0 !important; }
:deep(.highlight-gray .djs-visual > :nth-child(1)) { stroke: #dcdfe6 !important; fill: #f5f7fa !important; }
</style>
