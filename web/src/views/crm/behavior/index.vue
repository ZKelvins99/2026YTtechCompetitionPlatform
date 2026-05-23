<template>
  <div class="app-container behavior-page">
    <el-alert
      v-if="responseTime !== null"
      :title="'接口响应时间：' + responseTime + ' ms'"
      type="info"
      :closable="false"
      class="response-time-bar"
    />

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" icon="Upload" @click="openImport = true" v-hasPermi="['crm:behavior:import']">Excel 导入</el-button>
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
        <el-button icon="Refresh" @click="reloadList">刷新列表</el-button>
      </el-col>
    </el-row>

    <el-dialog title="Excel 批量导入行为数据" v-model="openImport" width="520px" append-to-body :close-on-click-modal="!importing">
      <el-upload drag :auto-upload="false" :limit="1" accept=".xlsx,.xls" :on-change="onFileChange" :on-remove="() => selectedFile = null">
        <el-icon class="el-icon--upload"><upload-filled /></el-icon>
        <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
        <template #tip>
          <div class="el-upload__tip">支持 xls/xlsx，建议 10 万级以上数据使用本功能异步导入</div>
        </template>
      </el-upload>
      <el-form-item v-if="importing || taskStatus" label-width="0" style="margin-top: 16px">
        <el-progress :percentage="progressPercent" :status="progressStatus" />
        <div class="progress-text" v-if="taskStatus">
          {{ taskStatus.processed }} / {{ taskStatus.total || '统计中…' }}
          <span v-if="taskStatus.status === 'RUNNING'">（导入中…）</span>
          <span v-else-if="taskStatus.status === 'DONE'" class="text-success">（完成）</span>
          <span v-else class="text-danger">（失败：{{ taskStatus.message }}）</span>
        </div>
      </el-form-item>
      <template #footer>
        <el-button @click="closeImportDialog" :disabled="importing && taskStatus?.status === 'RUNNING'">关闭</el-button>
        <el-button type="primary" :loading="importing" @click="startImport" v-hasPermi="['crm:behavior:import']">开始导入</el-button>
      </template>
    </el-dialog>

    <div ref="containerRef" class="scroll-container" v-loading="initialLoading">
      <el-table :data="list" size="small" stripe>
        <el-table-column label="ID" prop="id" width="100" align="center" />
        <el-table-column label="客户ID" prop="customerId" width="100" align="center" />
        <el-table-column label="行为类型" prop="behaviorType" width="120" align="center" />
        <el-table-column label="描述" prop="description" min-width="200" show-overflow-tooltip />
        <el-table-column label="行为时间" prop="behaviorTime" width="170" align="center">
          <template #default="scope">{{ parseTime(scope.row.behaviorTime) }}</template>
        </el-table-column>
      </el-table>

      <div class="scroll-footer">
        <span>已加载 {{ list.length }} / 共 {{ total }} 条</span>
        <el-icon v-if="loadingMore" class="is-loading loading-icon"><Loading /></el-icon>
        <span v-else-if="!hasMore && list.length > 0" class="no-more">没有更多了</span>
      </div>
    </div>
  </div>
</template>

<script setup name="CrmBehavior">
import { Loading, UploadFilled } from '@element-plus/icons-vue'
import { useScroll } from '@vueuse/core'
import { clearBehaviorData, getBehaviorTask, importBehaviorExcel, scrollBehavior } from '@/api/crm/behavior'

const { proxy } = getCurrentInstance()

const containerRef = ref(null)
const list = ref([])
const total = ref(0)
const lastId = ref(0)
const hasMore = ref(true)
const loadingMore = ref(false)
const initialLoading = ref(false)
const responseTime = ref(null)

const openImport = ref(false)
const selectedFile = ref(null)
const importing = ref(false)
const downloadingSample = ref(false)
const taskStatus = ref(null)
let pollTimer = null

const { arrivedState } = useScroll(containerRef)

const progressPercent = computed(() => {
  if (!taskStatus.value || !taskStatus.value.total) return 0
  return Math.min(100, Math.round(taskStatus.value.processed / taskStatus.value.total * 100))
})

const progressStatus = computed(() => {
  if (!taskStatus.value) return undefined
  if (taskStatus.value.status === 'FAILED') return 'exception'
  if (taskStatus.value.status === 'DONE') return 'success'
  return undefined
})

let loadLock = false
async function loadMore(reset = false) {
  if (loadLock) return
  if (!reset && (!hasMore.value || loadingMore.value)) return
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
    const res = await scrollBehavior({ lastId: lastId.value, pageSize: 100 })
    responseTime.value = res.responseTime
    const data = res.data
    total.value = data.total || 0
    const rows = data.list || []
    if (reset) {
      list.value = rows
    } else {
      list.value.push(...rows)
    }
    lastId.value = data.lastId ?? lastId.value
    hasMore.value = !!data.hasMore
  } finally {
    loadingMore.value = false
    initialLoading.value = false
    loadLock = false
  }
}

function reloadList() {
  loadMore(true)
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
  if (!selectedFile.value) {
    proxy.$modal.msgError('请选择 Excel 文件')
    return
  }
  importing.value = true
  taskStatus.value = null
  const formData = new FormData()
  formData.append('file', selectedFile.value)
  importBehaviorExcel(formData).then(res => {
  pollTask(res.data.taskId)
  }).catch(() => { importing.value = false })
}

function pollTask(taskId) {
  pollTimer = setInterval(() => {
    getBehaviorTask(taskId).then(r => {
      taskStatus.value = r.data
      if (r.data.status === 'DONE') {
        stopPoll()
        importing.value = false
        proxy.$modal.msgSuccess(`成功导入 ${r.data.processed} 条数据`)
        reloadList()
      } else if (r.data.status === 'FAILED') {
        stopPoll()
        importing.value = false
      }
    }).catch(() => {
      stopPoll()
      importing.value = false
    })
  }, 1000)
}

function stopPoll() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

function closeImportDialog() {
  if (importing.value && taskStatus.value?.status === 'RUNNING') return
  openImport.value = false
  selectedFile.value = null
  stopPoll()
}

function handleClear() {
  proxy.$modal.confirm('确认清空全部客户行为数据？此操作不可恢复。').then(() => {
    return clearBehaviorData()
  }).then(res => {
    proxy.$modal.msgSuccess(`已清空 ${res.data} 条数据`)
    reloadList()
  }).catch(() => {})
}

watch(() => arrivedState.bottom, (v) => {
  if (v && hasMore.value && !loadingMore.value && !initialLoading.value) {
    loadMore(false)
  }
})

onMounted(() => loadMore(true))

onBeforeUnmount(() => stopPoll())
</script>

<style scoped>
.behavior-page { display: flex; flex-direction: column; height: calc(100vh - 84px); }
.response-time-bar { margin-bottom: 12px; flex-shrink: 0; }
.scroll-container {
  flex: 1;
  height: calc(100vh - 200px);
  overflow-y: auto;
  border: 1px solid var(--el-border-color-light);
  border-radius: 4px;
  display: flex;
  flex-direction: column;
}
.scroll-container :deep(.el-table) { flex: 1; }
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
}
.loading-icon { font-size: 16px; }
.no-more { color: #c0c4cc; }
.progress-text { margin-top: 8px; font-size: 13px; color: #606266; }
.text-success { color: #67c23a; }
.text-danger { color: #f56c6c; }
</style>
