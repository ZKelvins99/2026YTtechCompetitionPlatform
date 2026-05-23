<template>
  <div class="app-container crm-page workbench-page">
    <crm-page-header
      title="CRM 工作台"
      description="拖拽布局、添加小组件、保存个性化首页；待办审批支持点击跳转办理。"
    />
    <div class="toolbar">
      <el-button type="primary" icon="Plus" @click="openAdd = true">添加组件</el-button>
      <el-button type="success" icon="Check" @click="handleSave" v-hasPermi="['crm:workbench:save']">保存布局</el-button>
      <el-button icon="RefreshLeft" @click="handleReset">重置布局</el-button>
    </div>

    <draggable
      v-model="workbenchStore.widgets"
      item-key="id"
      class="workbench-grid"
      :animation="200"
      handle=".widget-drag-handle"
      @end="onDragEnd"
    >
      <template #item="{ element }">
        <div class="widget-card" :style="{ gridColumn: 'span ' + (element.w || 6) }">
          <div class="widget-header">
            <span class="widget-drag-handle"><el-icon><Rank /></el-icon> {{ element.title }}</span>
            <el-button link type="danger" icon="Close" @click="workbenchStore.removeWidget(element.id)" />
          </div>
          <div class="widget-body">
            <ApprovalWidget v-if="element.id === 'approval'" />
            <CustomerStatsWidget v-else-if="element.id === 'customer-stats'" />
            <OpportunityFunnelWidget v-else-if="element.id === 'opportunity-funnel'" />
            <UnreadMessagesWidget v-else-if="element.id === 'unread-messages'" />
            <QuickActionsWidget v-else-if="element.id === 'quick-actions'" />
          </div>
        </div>
      </template>
    </draggable>

    <el-dialog title="添加小组件" v-model="openAdd" width="480px" append-to-body>
      <el-table :data="addableWidgets" @row-click="handleAddWidget" highlight-current-row class="add-table">
        <el-table-column label="组件" prop="title" />
        <el-table-column label="说明" prop="description" show-overflow-tooltip />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup name="CrmWorkbench">
import draggable from 'vuedraggable'
import { Rank } from '@element-plus/icons-vue'
import useCrmWorkbenchStore from '@/stores/crm/workbench'
import ApprovalWidget from './components/ApprovalWidget.vue'
import CustomerStatsWidget from './components/CustomerStatsWidget.vue'
import OpportunityFunnelWidget from './components/OpportunityFunnelWidget.vue'
import UnreadMessagesWidget from './components/UnreadMessagesWidget.vue'
import QuickActionsWidget from './components/QuickActionsWidget.vue'

const { proxy } = getCurrentInstance()
const workbenchStore = useCrmWorkbenchStore()
const openAdd = ref(false)

const addableWidgets = computed(() => {
  const ids = workbenchStore.widgets.map(w => w.id)
  return workbenchStore.availableWidgets.filter(w => !ids.includes(w.id))
})

function onDragEnd() { /* vuedraggable 已更新顺序 */ }

async function handleSave() {
  await workbenchStore.saveLayout()
  proxy.$modal.msgSuccess('布局已保存')
}

function handleReset() {
  workbenchStore.resetLayout()
  proxy.$modal.msgSuccess('已恢复默认布局')
}

function handleAddWidget(row) {
  if (workbenchStore.addWidget(row)) {
    proxy.$modal.msgSuccess('已添加 ' + row.title)
    openAdd.value = false
  }
}

onMounted(async () => {
  await workbenchStore.loadAvailable()
  await workbenchStore.loadLayout()
})
</script>

<style scoped>
.workbench-page { min-height: calc(100vh - 84px); }
.toolbar { margin-bottom: 16px; display: flex; gap: 8px; }
.workbench-grid {
  display: grid;
  grid-template-columns: repeat(12, 1fr);
  gap: 16px;
}
.widget-card {
  background: #fff;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  overflow: hidden;
  min-height: 180px;
}
.widget-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 14px;
  background: #f7f9fb;
  border-bottom: 1px solid var(--el-border-color-lighter);
  font-weight: 600;
  font-size: 14px;
}
.widget-drag-handle { cursor: move; display: flex; align-items: center; gap: 6px; }
.widget-body { padding: 12px 14px; }
.add-table :deep(.el-table__row) { cursor: pointer; }
</style>
