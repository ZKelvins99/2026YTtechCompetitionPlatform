<template>
  <div class="app-container workflow-page" v-loading="pageLoading">
    <el-card shadow="never" class="bpmn-card">
      <template #header>
        <span>合同审批流程 — {{ instance.businessNo || '加载中' }}</span>
        <el-tag class="ml-2" :type="instanceStatusType">{{ instanceStatusText }}</el-tag>
        <el-tag v-if="instance.currentStageName && instance.status === '0'" class="ml-2" type="warning">
          当前：{{ instance.currentStageName }}
        </el-tag>
      </template>
      <el-alert v-if="bpmnError" :title="bpmnError" type="warning" show-icon :closable="false" class="mb-2" />
      <div class="bpmn-viewport">
        <div ref="bpmnContainer" class="bpmn-container"></div>
      </div>
      <div class="bpmn-legend">
        <span class="legend-item legend-pass">已通过</span>
        <span class="legend-item legend-current">当前节点</span>
        <span class="legend-item legend-countersign">含会签待办</span>
        <span class="legend-item legend-pending">待处理</span>
        <span class="legend-item legend-reject">驳回</span>
      </div>
    </el-card>

    <el-card v-if="instance.status === '0'" shadow="never" class="stage-card">
      <template #header>当前环节</template>
      <p v-if="instance.pendingHint" class="pending-hint">
        <span class="label">待办：</span>{{ instance.pendingHint }}
      </p>
      <p v-else class="pending-hint text-muted">暂无待办（流程可能已卡住，请联系管理员）</p>
      <div v-if="currentStageNodes.length" class="stage-node-list">
        <div v-for="n in currentStageNodes" :key="n.id" class="stage-node-item" :class="{ 'is-pending': n.status === '0' }">
          <el-tag v-if="n.approvalType === 'COUNTERSIGN'" type="warning" size="small" effect="dark">会签</el-tag>
          <el-tag v-else type="primary" size="small" effect="plain">主审</el-tag>
          <span class="node-name">{{ n.approverName || '未指定' }}</span>
          <el-tag size="small" :type="nodeStatusTag(n.status)">{{ nodeStatusLabel(n.status) }}</el-tag>
        </div>
      </div>
    </el-card>

    <el-alert
      v-if="instance.status === '0' && !instance.canOperate"
      type="info"
      show-icon
      :closable="false"
      class="mb-3"
      :title="waitOperateHint"
    />

    <el-card v-if="instance.status === '0' && instance.canOperate" shadow="never" class="action-card">
      <el-form label-width="80px">
        <el-form-item label="审批意见">
          <el-input v-model="opinion" type="textarea" :rows="3" placeholder="请输入审批意见" />
        </el-form-item>
        <el-form-item>
          <el-button type="success" @click="handleApprove">通过</el-button>
          <el-button type="danger" @click="handleReject">驳回</el-button>
          <el-button @click="openTransfer = true">转办</el-button>
          <el-dropdown @command="handleRollback">
            <el-button class="ml-1">回退<el-icon class="el-icon--right"><arrow-down /></el-icon></el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-for="n in rollbackNodes" :key="n.id" :command="n.id">{{ n.nodeName }}</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-button type="danger" plain @click="handleTerminate">终止</el-button>
        </el-form-item>
        <p v-if="isAdminProxy" class="proxy-tip">您正以管理员身份处理当前环节待办</p>
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
          <p>
            <strong>{{ node.nodeName }}</strong>
            <el-tag v-if="node.approvalType === 'COUNTERSIGN'" type="warning" size="small" class="ml-1">会签</el-tag>
            <el-tag v-else-if="node.nodeOrder > 1" type="info" size="small" class="ml-1">主审</el-tag>
            — {{ approverLabel(node) }}
          </p>
          <p>{{ actionText(node.status) }} {{ node.opinion ? '：' + node.opinion : '' }}</p>
        </el-timeline-item>
      </el-timeline>
    </el-card>

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
import { nextTick } from 'vue'
import BpmnViewer from 'bpmn-js/lib/Viewer'
import 'bpmn-js/dist/assets/diagram-js.css'
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn.css'
import { ArrowDown } from '@element-plus/icons-vue'
import { listUser } from '@/api/system/user'
import { resolveWorkflowBpmnXml } from '@/constants/contractApprovalBpmn'
import useUserStore from '@/store/modules/user'
import {
  getWorkflowInstance, getWorkflowBpmn, approveWorkflow, rejectWorkflow,
  transferWorkflow, rollbackWorkflow, terminateWorkflow
} from '@/api/crm/workflow'

const route = useRoute()
const { proxy } = getCurrentInstance()
const userStore = useUserStore()

const instanceId = computed(() => route.params.instanceId)
const bpmnContainer = ref(null)
const instance = ref({ nodes: [], status: '0', canOperate: false })
const opinion = ref('')
const openTransfer = ref(false)
const transferUserId = ref(null)
const userList = ref([])
const pageLoading = ref(false)
const bpmnError = ref('')
let viewer = null

const instanceStatusText = computed(() => ({ '0': '审批中', '1': '已完成', '2': '已终止' }[instance.value.status] || ''))
const instanceStatusType = computed(() => ({ '0': 'warning', '1': 'success', '2': 'danger' }[instance.value.status] || 'info'))
const timelineNodes = computed(() => instance.value.nodes || [])
const rollbackNodes = computed(() => (instance.value.nodes || []).filter(n => n.approvalType === 'SINGLE' && n.nodeOrder > 1))
const activeNodeOrder = computed(() => {
  const pending = (instance.value.nodes || []).filter(n => n.status === '0' && n.nodeOrder > 1)
  if (!pending.length) return instance.value.activeNodeOrder
  return Math.min(...pending.map(n => n.nodeOrder))
})
const currentStageNodes = computed(() => {
  const order = activeNodeOrder.value
  if (!order) return []
  return (instance.value.nodes || []).filter(n => n.nodeOrder === order).sort((a, b) => {
    if (a.approvalType === 'COUNTERSIGN' && b.approvalType !== 'COUNTERSIGN') return 1
    if (b.approvalType === 'COUNTERSIGN' && a.approvalType !== 'COUNTERSIGN') return -1
    return (a.id || 0) - (b.id || 0)
  })
})
const isAdminProxy = computed(() => {
  if (!instance.value.canOperate) return false
  const uid = Number(userStore.id)
  return currentStageNodes.value.some(n => n.status === '0' && Number(n.approverId) !== uid)
})
const waitOperateHint = computed(() => {
  if (instance.value.pendingHint) {
    return `当前环节待 ${instance.value.pendingHint} 处理；请使用对应账号登录，或由系统管理员代审。`
  }
  return '当前环节待指定审批人处理；系统管理员可代为审批。'
})

function actionText(status) {
  return { '0': '待审批', '1': '通过', '2': '驳回', '3': '转办', '4': '回退' }[status] || ''
}
function nodeStatusLabel(status) {
  return actionText(status)
}
function nodeStatusTag(status) {
  return { '0': 'warning', '1': 'success', '2': 'danger', '3': 'info', '4': 'info' }[status] || 'info'
}
function approverLabel(node) {
  if (node.approverName) return node.approverName
  if (node.status === '1' || node.status === '2') return '管理员代审'
  return '未指定'
}
function timelineType(status) {
  return { '1': 'success', '2': 'danger', '3': 'warning', '4': 'info' }[status] || 'primary'
}

async function loadData() {
  pageLoading.value = true
  bpmnError.value = ''
  try {
    const instRes = await getWorkflowInstance(instanceId.value)
    instance.value = instRes.data || { nodes: [], status: '0' }
    let bpmnRes = null
    try {
      bpmnRes = await getWorkflowBpmn(instanceId.value)
    } catch (e) {
      console.warn('BPMN 接口不可用，使用实例内嵌或本地模板', e)
    }
    const xml = resolveWorkflowBpmnXml(instance.value, bpmnRes)
    await renderBpmn(xml)
  } catch (e) {
    bpmnError.value = e?.message || '加载流程失败'
  } finally {
    pageLoading.value = false
  }
}

async function renderBpmn(xml) {
  if (viewer) {
    viewer.destroy()
    viewer = null
  }
  await nextTick()
  if (!bpmnContainer.value) return
  if (!xml || !String(xml).includes('bpmn:definitions')) {
    bpmnError.value = '未获取到流程图数据'
    return
  }
  try {
    viewer = new BpmnViewer({ container: bpmnContainer.value })
    const { warnings } = await viewer.importXML(xml)
    if (warnings?.length) {
      console.warn('BPMN import warnings', warnings)
    }
    applyNodeColors()
    fitDiagramViewport()
  } catch (e) {
    console.error(e)
    bpmnError.value = '流程图渲染失败，请刷新重试'
  }
}

function fitDiagramViewport() {
  const canvas = viewer.get('canvas')
  canvas.resized()
  canvas.zoom('fit-viewport')
  const vb = canvas.viewbox()
  const pad = 48
  canvas.viewbox({
    x: vb.x - pad,
    y: vb.y - pad,
    width: vb.width + pad * 2,
    height: vb.height + pad * 2
  })
}

function applyNodeColors() {
  if (!viewer || !instance.value.nodes) return
  const canvas = viewer.get('canvas')
  const registry = viewer.get('elementRegistry')
  const order = activeNodeOrder.value
  const orderStatus = {}
  for (let o = 2; o <= 4; o++) {
    const stageNodes = instance.value.nodes.filter(n => n.nodeOrder === o)
    if (!stageNodes.length) continue
    if (stageNodes.some(n => n.status === '2')) orderStatus[o] = 'reject'
    else if (stageNodes.every(n => n.status === '1')) orderStatus[o] = 'pass'
    else orderStatus[o] = 'pending'
  }
  if (instance.value.status === '0' && order) {
    const stageNodes = instance.value.nodes.filter(n => n.nodeOrder === order)
    const hasPending = stageNodes.some(n => n.status === '0')
    if (hasPending) {
      const hasCountersignPending = stageNodes.some(n => n.status === '0' && n.approvalType === 'COUNTERSIGN')
      orderStatus[order] = hasCountersignPending ? 'countersign' : 'current'
    }
  }
  if (instance.value.status === '1') {
    for (let i = 1; i <= 4; i++) {
      if (orderStatus[i] !== 'reject') orderStatus[i] = 'pass'
    }
  }
  const bpmnIds = ['Task_Submit', 'Task_Dept', 'Task_Finance', 'Task_Archive']
  bpmnIds.forEach((id, idx) => {
    const el = registry.get(id)
    if (!el) return
    const st = orderStatus[idx + 1] || 'pending'
    const cls = {
      pass: 'highlight-green',
      current: 'highlight-blue',
      countersign: 'highlight-orange',
      reject: 'highlight-red',
      pending: 'highlight-gray'
    }[st]
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
.bpmn-card, .action-card, .timeline-card, .stage-card { margin-bottom: 16px; }
.ml-2 { margin-left: 8px; }
.ml-1 { margin-left: 4px; }
.mb-2 { margin-bottom: 8px; }
.mb-3 { margin-bottom: 12px; }

.pending-hint { margin: 0 0 12px; font-size: 14px; color: #303133; }
.pending-hint .label { color: #909399; }
.text-muted { color: #909399; }

.stage-node-list { display: flex; flex-direction: column; gap: 8px; }
.stage-node-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 8px;
  background: #f5f7fa;
}
.stage-node-item.is-pending { background: #fdf6ec; border: 1px solid #faecd8; }
.node-name { flex: 1; font-weight: 500; }
.proxy-tip { margin: 0; font-size: 12px; color: #e6a23c; }

.bpmn-viewport {
  border-radius: 10px;
  border: 1px solid #e4e7ed;
  background: linear-gradient(180deg, #f8fafc 0%, #f1f5f9 100%);
  overflow: hidden;
}
.workflow-page .bpmn-container {
  height: 400px;
  min-height: 280px;
}

.bpmn-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed #e4e7ed;
  font-size: 12px;
  color: #606266;
}
.legend-item::before {
  content: '';
  display: inline-block;
  width: 12px;
  height: 12px;
  margin-right: 6px;
  border-radius: 3px;
  vertical-align: -2px;
  border: 2px solid transparent;
}
.legend-pass::before { background: #f0f9eb; border-color: #67c23a; }
.legend-current::before { background: #ecf5ff; border-color: #409eff; }
.legend-countersign::before { background: #fdf6ec; border-color: #e6a23c; }
.legend-pending::before { background: #f5f7fa; border-color: #dcdfe6; }
.legend-reject::before { background: #fef0f0; border-color: #f56c6c; }

:deep(.bpmn-container .djs-container) {
  background: transparent !important;
}
:deep(.bpmn-container .djs-element .djs-label) {
  font-family: 'PingFang SC', 'Microsoft YaHei', system-ui, sans-serif !important;
  font-size: 13px !important;
  font-weight: 500 !important;
  fill: #303133 !important;
}
:deep(.bpmn-container .djs-connection .djs-visual > path) {
  stroke: #94a3b8 !important;
  stroke-width: 2px !important;
}
:deep(.bpmn-container .djs-visual circle) {
  stroke-width: 2px !important;
}

:deep(.highlight-green .djs-visual rect),
:deep(.highlight-green .djs-visual circle) {
  stroke: #67c23a !important;
  fill: #f0f9eb !important;
}
:deep(.highlight-blue .djs-visual rect),
:deep(.highlight-blue .djs-visual circle) {
  stroke: #409eff !important;
  fill: #ecf5ff !important;
  stroke-width: 2.5px !important;
}
:deep(.highlight-orange .djs-visual rect),
:deep(.highlight-orange .djs-visual circle) {
  stroke: #e6a23c !important;
  fill: #fdf6ec !important;
  stroke-width: 2.5px !important;
}
:deep(.highlight-red .djs-visual rect),
:deep(.highlight-red .djs-visual circle) {
  stroke: #f56c6c !important;
  fill: #fef0f0 !important;
}
:deep(.highlight-gray .djs-visual rect),
:deep(.highlight-gray .djs-visual circle) {
  stroke: #c0c4cc !important;
  fill: #ffffff !important;
}
</style>
