<template>
  <div class="app-container behavior-page crm-page">
    <crm-page-header title="客户行为记录" v-bind="CRM_PAGE_INTRO.behavior" />
    <el-alert
      v-if="responseTime !== null"
      :title="'最近接口响应：' + responseTime + ' ms'"
      type="info"
      :closable="false"
      class="response-time-bar"
    />

    <el-alert
      v-if="importRunning"
      type="warning"
      :closable="false"
      show-icon
      class="import-running-bar"
      @click="openImport = true"
    >
      <template #title>
        Excel 导入进行中（{{ taskStatus?.processed?.toLocaleString() }} / {{ displayTotal }}）
        <span v-if="taskStatus?.fileName" class="import-file-name"> — {{ taskStatus.fileName }}</span>
        <el-link type="primary" :underline="false" class="import-view-link">查看进度</el-link>
      </template>
    </el-alert>

    <div class="data-summary-bar">
      <div class="summary-main">
        <span class="summary-label">数据库总量</span>
        <span class="summary-total">{{ formatCount(dbTotal) }}</span>
        <span class="summary-unit">条</span>
      </div>
      <div class="summary-sub">
        已加载 <strong>{{ formatCount(list.length) }}</strong> 条
        <template v-if="autoLoadingAll">，正在继续加载…</template>
        <template v-else-if="allLoaded">，已全部加载</template>
        <template v-else-if="dbTotal > 0 && list.length < dbTotal">，可继续加载或点击刷新</template>
      </div>
      <el-progress
        v-if="dbTotal > 0"
        :percentage="loadPercent"
        :stroke-width="10"
        :status="allLoaded ? 'success' : undefined"
        class="load-progress"
      />
    </div>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          icon="Upload"
          :disabled="importRunning"
          @click="openImportDialog"
          v-hasPermi="['crm:behavior:import']"
        >{{ importRunning ? '导入进行中…' : 'Excel 导入' }}</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="info" plain icon="Download" @click="downloadTemplate">下载模板</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" :loading="downloadingSample" @click="downloadSample" v-hasPermi="['crm:behavior:import']">下载10万样例</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" @click="handleClear" v-hasPermi="['crm:behavior:remove']">一键清空</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button icon="Refresh" :loading="initialLoading || autoLoadingAll" @click="reloadList">刷新</el-button>
      </el-col>
      <el-col :span="1.5" v-if="hasMore && !autoLoadingAll">
        <el-button type="primary" plain :loading="loadingMore" @click="loadNextBatch">继续加载下一批</el-button>
      </el-col>
    </el-row>

    <el-dialog title="Excel 批量导入行为数据" v-model="openImport" width="560px" append-to-body :close-on-click-modal="!importRunning">
      <el-alert v-if="importRunning" type="info" :closable="false" show-icon class="mb12">
        当前有导入任务正在执行，完成后才能再次导入。关闭本窗口不影响后台导入，再次进入本页可继续查看进度。
      </el-alert>
      <el-upload
        drag
        :auto-upload="false"
        :limit="1"
        accept=".xlsx"
        :disabled="importRunning"
        :on-change="onFileChange"
        :on-remove="() => selectedFile = null"
      >
        <el-icon class="el-icon--upload"><upload-filled /></el-icon>
        <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
        <template #tip>
          <div class="el-upload__tip">仅支持 xlsx；进度保存在 Redis，切换页面后回来仍可查看</div>
        </template>
      </el-upload>
      <div v-if="showImportProgress" class="import-progress-panel">
        <el-progress :percentage="progressPercent" :status="progressStatus" :stroke-width="14" />
        <div class="progress-text" v-if="taskStatus">
          <div class="progress-row">
            <span>进度：{{ taskStatus.processed?.toLocaleString() }} / {{ displayTotal }}</span>
            <span v-if="taskStatus.status === 'RUNNING'" class="text-running">导入中…</span>
            <span v-else-if="taskStatus.status === 'DONE'" class="text-success">已完成</span>
            <span v-else class="text-danger">失败：{{ taskStatus.message }}</span>
          </div>
          <div class="progress-metrics" v-if="taskStatus.status === 'RUNNING' || taskStatus.status === 'DONE'">
            <span>已用时：{{ formatDuration(taskStatus.elapsedMs) }}</span>
            <span v-if="taskStatus.speedPerSec > 0">速度：{{ taskStatus.speedPerSec.toLocaleString() }} 条/秒</span>
            <span v-if="etaText">预计剩余：{{ etaText }}</span>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="closeImportDialog">关闭</el-button>
        <el-button
          type="primary"
          :loading="importSubmitting"
          :disabled="importRunning || !selectedFile"
          @click="startImport"
          v-hasPermi="['crm:behavior:import']"
        >开始导入</el-button>
      </template>
    </el-dialog>

    <div ref="tableWrapRef" class="table-wrap" v-loading="initialLoading && list.length === 0">
      <el-table-v2
        v-if="tableWidth > 0 && tableHeight > 0"
        :columns="tableColumns"
        :data="list"
        :row-key="rowKey"
        :width="tableWidth"
        :height="tableHeight"
        :row-height="36"
        :header-height="40"
        fixed
      />
      <div class="scroll-footer">
        <span>已加载 {{ formatCount(list.length) }} / 数据库共 {{ formatCount(dbTotal) }} 条</span>
        <el-icon v-if="loadingMore || autoLoadingAll" class="is-loading loading-icon"><Loading /></el-icon>
        <span v-else-if="allLoaded && dbTotal > 0" class="no-more">全部数据已展示在本页</span>
        <span v-else-if="hasMore" class="hint-more">可继续加载或点击刷新</span>
      </div>
    </div>
  </div>
</template>

<script setup name="CrmBehavior">
import { h, shallowRef } from 'vue'
import { Loading, UploadFilled } from '@element-plus/icons-vue'
import { useElementSize } from '@vueuse/core'
import useUserStore from '@/store/modules/user'
import { CRM_PAGE_INTRO } from '@/constants/crmPageIntro'
import {
  clearBehaviorData,
  getBehaviorTask,
  getBehaviorTotal,
  importBehaviorExcel,
  scrollBehavior
} from '@/api/crm/behavior'
import {
  saveBehaviorListCache,
  readBehaviorListCache,
  clearBehaviorListCache,
  compactBehaviorRows
} from '@/utils/behaviorListCache'

const { proxy } = getCurrentInstance()
const userStore = useUserStore()

/** 每批 1 万条；表格用虚拟滚动避免 DOM 卡顿 */
/** 单批 2000：库内约 2s，减小 JDBC/JSON 体积；多批并行由前端自动加载 */
const PAGE_SIZE = 2000

const cacheKey = computed(() => (userStore.id ? `crm-behavior-list-${userStore.id}` : ''))

const tableWrapRef = ref(null)
const { width: wrapWidth, height: wrapHeight } = useElementSize(tableWrapRef)
const tableWidth = computed(() => Math.max(320, Math.floor(wrapWidth.value || 0)))
const tableHeight = computed(() => Math.max(200, Math.floor((wrapHeight.value || 0) - 44)))

const list = shallowRef([])
const dbTotal = ref(0)
const lastId = ref(0)
const hasMore = ref(true)
const loadingMore = ref(false)
const initialLoading = ref(false)
const autoLoadingAll = ref(false)
const responseTime = ref(null)

const openImport = ref(false)
const selectedFile = ref(null)
const importSubmitting = ref(false)
const downloadingSample = ref(false)
const taskStatus = ref(null)
let pollTimer = null
let pollingTaskId = null
let loadSession = 0
let persistTimer = null

const importRunning = computed(() => taskStatus.value?.status === 'RUNNING')

/** 仅在导入进行中或弹窗内展示刚结束的结果，避免 DONE 后仍锁住界面 */
const showImportProgress = computed(() => {
  if (!taskStatus.value) return false
  if (importRunning.value) return true
  return openImport.value && (taskStatus.value.status === 'DONE' || taskStatus.value.status === 'FAILED')
})

function rowKey(row) {
  return row.id
}

const tableColumns = [
  {
    key: 'rowIndex',
    title: '序号',
    width: 72,
    align: 'center',
    cellRenderer: ({ rowIndex }) => h('span', { class: 'cell-index' }, rowIndex + 1)
  },
  { key: 'customerId', dataKey: 'customerId', title: '客户ID', width: 100, align: 'center' },
  { key: 'behaviorType', dataKey: 'behaviorType', title: '行为类型', width: 120, align: 'center' },
  {
    key: 'description',
    dataKey: 'description',
    title: '描述',
    width: 280,
    cellRenderer: ({ cellData }) => h('span', { class: 'cell-ellipsis', title: cellData }, cellData)
  },
  {
    key: 'behaviorTime',
    dataKey: 'behaviorTimeText',
    title: '行为时间',
    width: 170,
    align: 'center'
  }
]

const allLoaded = computed(() => dbTotal.value > 0 && list.value.length >= dbTotal.value && !hasMore.value)
const loadPercent = computed(() => {
  if (!dbTotal.value) return list.value.length > 0 ? 1 : 0
  return Math.min(100, Math.round((list.value.length / dbTotal.value) * 100))
})

const displayTotal = computed(() => {
  const t = taskStatus.value?.total
  if (!t) return '估算中…'
  return Number(t).toLocaleString()
})

const progressPercent = computed(() => {
  if (!taskStatus.value || !taskStatus.value.total) return 0
  return Math.min(100, Math.round(taskStatus.value.processed / taskStatus.value.total * 100))
})

const etaText = computed(() => {
  const s = taskStatus.value
  if (!s || s.status !== 'RUNNING' || !s.total || !s.speedPerSec || s.processed >= s.total) return ''
  const remain = s.total - s.processed
  const sec = Math.ceil(remain / s.speedPerSec)
  return formatDuration(sec * 1000)
})

const progressStatus = computed(() => {
  if (!taskStatus.value) return undefined
  if (taskStatus.value.status === 'FAILED') return 'exception'
  if (taskStatus.value.status === 'DONE' && (taskStatus.value.processed || 0) > 0) return 'success'
  if (taskStatus.value.status === 'DONE') return 'exception'
  return undefined
})

function formatCount(n) {
  return (Number(n) || 0).toLocaleString()
}

function formatDuration(ms) {
  if (ms == null || ms < 0) return '-'
  const sec = Math.floor(ms / 1000)
  if (sec < 60) return `${sec} 秒`
  const min = Math.floor(sec / 60)
  const rs = sec % 60
  if (min < 60) return `${min} 分 ${rs} 秒`
  const hr = Math.floor(min / 60)
  return `${hr} 时 ${min % 60} 分`
}

function normalizeRows(rows) {
  return rows.map(row => ({
    ...row,
    behaviorTimeText: proxy.parseTime(row.behaviorTime)
  }))
}

function setListRows(rows, reset) {
  const normalized = normalizeRows(rows)
  if (reset) {
    list.value = normalized
  } else if (normalized.length) {
    list.value = list.value.concat(normalized)
  }
}

function schedulePersistCache() {
  if (!cacheKey.value || !list.value.length) return
  if (persistTimer) clearTimeout(persistTimer)
  persistTimer = setTimeout(() => {
    persistTimer = null
    persistCacheNow()
  }, 600)
}

async function persistCacheNow() {
  const key = cacheKey.value
  if (!key || !list.value.length) return
  await saveBehaviorListCache(key, {
    list: compactBehaviorRows(list.value),
    lastId: lastId.value,
    hasMore: hasMore.value,
    dbTotal: dbTotal.value
  })
}

async function restoreFromCache() {
  const key = cacheKey.value
  if (!key) return false
  const snap = await readBehaviorListCache(key)
  if (!snap?.list?.length) return false
  const rows = dedupeRowsById(snap.list)
  list.value = normalizeRows(rows)
  lastId.value = snap.lastId ?? (rows.length ? rows[rows.length - 1].id : 0)
  hasMore.value = !!snap.hasMore
  syncLoadState()
  return true
}

function dedupeRowsById(rows) {
  const seen = new Set()
  const out = []
  for (const row of rows) {
    const id = row?.id
    if (id == null || seen.has(id)) continue
    seen.add(id)
    out.push(row)
  }
  return out
}

function syncLoadState() {
  if (dbTotal.value > 0 && list.value.length >= dbTotal.value) {
    hasMore.value = false
  }
}

async function fetchDbTotal(refresh = false) {
  try {
    const res = await getBehaviorTotal(refresh)
    const payload = res.data
    if (payload != null && typeof payload === 'object') {
      dbTotal.value = Number(payload.total) || 0
      applyActiveImportTask(payload.activeImport)
    } else {
      dbTotal.value = Number(payload) || 0
    }
  } catch {
    dbTotal.value = 0
  }
}

function applyActiveImportTask(task) {
  if (!task || !task.taskId || task.status !== 'RUNNING') {
    return
  }
  taskStatus.value = task
  if (pollingTaskId !== task.taskId) {
    beginPollTask(task.taskId)
  }
}

function clearImportTaskState() {
  stopPoll()
  taskStatus.value = null
}

let loadLock = false
async function loadMore(reset = false) {
  if (loadLock) return false
  if (!reset && !hasMore.value) return false
  loadLock = true
  if (reset) {
    initialLoading.value = true
    list.value = []
    lastId.value = 0
    hasMore.value = true
  } else {
    loadingMore.value = true
  }
  try {
    const res = await scrollBehavior({ lastId: lastId.value, pageSize: PAGE_SIZE })
    responseTime.value = res.responseTime
    const data = res.data
    const rows = data.list || []
    if (data.total != null && data.total > 0 && (reset || lastId.value === 0)) {
      dbTotal.value = data.total
    }
    setListRows(rows, reset)
    lastId.value = data.lastId ?? lastId.value
    hasMore.value = !!data.hasMore
    syncLoadState()
    schedulePersistCache()
    return rows.length > 0
  } finally {
    loadingMore.value = false
    initialLoading.value = false
    loadLock = false
  }
}

function yieldToUi() {
  return new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve)))
}

/** 首批后自动逐批加载直至 hasMore 为 false */
async function loadAllSequential(reset = false) {
  const session = ++loadSession
  if (reset) {
    await fetchDbTotal(true)
    if (session !== loadSession) return
  }
  await loadMore(reset)
  if (session !== loadSession) return
  if (!hasMore.value) return

  autoLoadingAll.value = true
  try {
    while (hasMore.value && session === loadSession) {
      await yieldToUi()
      const ok = await loadMore(false)
      if (session !== loadSession) break
      if (!ok && !hasMore.value) break
    }
  } finally {
    if (session === loadSession) {
      autoLoadingAll.value = false
      schedulePersistCache()
    }
  }
}

function loadNextBatch() {
  loadMore(false)
}

async function reloadList() {
  loadSession++
  await clearBehaviorListCache(cacheKey.value)
  await loadAllSequential(true)
}

function openImportDialog() {
  if (!importRunning.value) {
    clearImportTaskState()
  }
  openImport.value = true
}

function onFileChange(uploadFile) {
  selectedFile.value = uploadFile.raw
}

function downloadTemplate() {
  proxy.download('crm/behavior/importTemplate', {}, `behavior_template_${Date.now()}.xlsx`)
}

function downloadSample() {
  downloadingSample.value = true
  proxy.download('crm/behavior/sampleExcel', { count: 100000 }, `behavior_sample_100000.xlsx`).finally(() => {
    downloadingSample.value = false
  })
}

function startImport() {
  if (importRunning.value) {
    proxy.$modal.msgWarning('已有导入任务进行中，请等待完成')
    return
  }
  if (taskStatus.value && taskStatus.value.status !== 'RUNNING') {
    clearImportTaskState()
  }
  if (!selectedFile.value) {
    proxy.$modal.msgError('请选择 Excel 文件')
    return
  }
  importSubmitting.value = true
  const formData = new FormData()
  formData.append('file', selectedFile.value)
  importBehaviorExcel(formData).then(res => {
    selectedFile.value = null
    beginPollTask(res.data.taskId)
  }).catch(err => {
    proxy.$modal.msgError(err?.message || '导入启动失败')
  }).finally(() => {
    importSubmitting.value = false
  })
}

function beginPollTask(taskId) {
  stopPoll()
  pollingTaskId = taskId
  const fetchOnce = () => {
    getBehaviorTask(taskId).then(r => {
      const data = r.data
      if (!data) {
        clearImportTaskState()
        return
      }
      if (data.status === 'RUNNING') {
        taskStatus.value = data
        return
      }
      stopPoll()
      taskStatus.value = data
      if (data.status === 'DONE') {
        const imported = Number(data.processed) || 0
        if (imported <= 0) {
          proxy.$modal.msgError('导入完成但未写入任何数据')
        } else {
          proxy.$modal.msgSuccess(`成功导入 ${imported.toLocaleString()} 条数据`)
          reloadList()
        }
        scheduleClearImportState()
      } else if (data.status === 'FAILED') {
        proxy.$modal.msgError(data.message || '导入失败')
        scheduleClearImportState()
      }
    }).catch(() => {})
  }
  fetchOnce()
  pollTimer = setInterval(fetchOnce, 500)
}

let clearImportTimer = null
function scheduleClearImportState() {
  if (clearImportTimer) {
    clearTimeout(clearImportTimer)
  }
  clearImportTimer = setTimeout(() => {
    clearImportTimer = null
    if (!importRunning.value) {
      taskStatus.value = null
    }
  }, 4000)
}

function stopPoll() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
  pollingTaskId = null
}

function closeImportDialog() {
  openImport.value = false
  if (!importRunning.value) {
    clearImportTaskState()
  }
}

function handleClear() {
  proxy.$modal.confirm('确认清空全部客户行为数据？此操作不可恢复。').then(() => {
    return clearBehaviorData()
  }).then(async res => {
    proxy.$modal.msgSuccess(`已清空 ${res.data} 条数据`)
    dbTotal.value = 0
    await clearBehaviorListCache(cacheKey.value)
    await loadAllSequential(true)
  }).catch(() => {})
}

async function initPage() {
  const restored = await restoreFromCache()
  if (restored) {
    await fetchDbTotal(true)
    syncLoadState()
    return
  }
  await loadAllSequential(true)
}

onMounted(() => initPage())

onBeforeUnmount(() => {
  loadSession++
  if (persistTimer) {
    clearTimeout(persistTimer)
    persistTimer = null
  }
  if (clearImportTimer) {
    clearTimeout(clearImportTimer)
    clearImportTimer = null
  }
  persistCacheNow()
  clearImportTaskState()
})
</script>

<style scoped>
.behavior-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 84px);
}
.response-time-bar,
.import-running-bar {
  margin-bottom: 12px;
  flex-shrink: 0;
}
.import-running-bar {
  cursor: pointer;
}
.import-running-bar .import-file-name {
  font-weight: normal;
  color: #606266;
}
.import-view-link {
  margin-left: 8px;
  font-size: 13px;
}
.mb12 {
  margin-bottom: 12px;
}
.data-summary-bar {
  margin-bottom: 12px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #f0f7ff 0%, #f8fafc 100%);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  flex-shrink: 0;
}
.summary-main {
  display: flex;
  align-items: baseline;
  gap: 8px;
  flex-wrap: wrap;
}
.summary-label {
  font-size: 14px;
  color: #606266;
}
.summary-total {
  font-size: 28px;
  font-weight: 700;
  color: #409eff;
  line-height: 1.2;
}
.summary-unit {
  font-size: 14px;
  color: #909399;
}
.summary-sub {
  margin-top: 6px;
  font-size: 13px;
  color: #606266;
}
.summary-sub strong {
  color: #303133;
}
.load-progress {
  margin-top: 10px;
  max-width: 480px;
}
.table-wrap {
  flex: 1;
  min-height: 0;
  height: calc(100vh - 320px);
  border: 1px solid var(--el-border-color-light);
  border-radius: 4px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.table-wrap :deep(.cell-index) {
  color: #909399;
  font-variant-numeric: tabular-nums;
}
.table-wrap :deep(.cell-ellipsis) {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.scroll-footer {
  padding: 10px;
  text-align: center;
  color: #909399;
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border-top: 1px solid var(--el-border-color-lighter);
  flex-shrink: 0;
}
.loading-icon {
  font-size: 16px;
}
.no-more {
  color: #67c23a;
}
.hint-more {
  color: #409eff;
}
.import-progress-panel {
  margin-top: 16px;
}
.progress-text {
  margin-top: 10px;
  font-size: 13px;
  color: #606266;
}
.progress-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 12px;
}
.progress-metrics {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px 20px;
  color: #909399;
  font-size: 12px;
}
.text-running {
  color: #409eff;
}
.text-success {
  color: #67c23a;
}
.text-danger {
  color: #f56c6c;
}
</style>
