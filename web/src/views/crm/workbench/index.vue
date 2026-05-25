<template>
  <div class="app-container crm-page workbench-page min-h-[calc(100vh-84px)] bg-gradient-to-b from-slate-50 via-brand-50/30 to-slate-50">
    <crm-page-header title="CRM 工作台" v-bind="CRM_PAGE_INTRO.workbench" />
    <div class="mb-4 flex flex-wrap gap-2 rounded-2xl border border-slate-200/80 bg-white p-3 shadow-sm">
      <el-button type="primary" icon="Plus" class="!rounded-xl" @click="openAdd = true">添加组件</el-button>
      <el-button type="success" icon="Check" class="!rounded-xl" @click="handleSave" v-hasPermi="['crm:workbench:save']">保存布局</el-button>
      <el-button icon="RefreshLeft" class="!rounded-xl" @click="handleReset">重置布局</el-button>
    </div>

    <draggable
      v-model="workbenchStore.widgets"
      item-key="id"
      class="grid grid-cols-12 gap-4"
      :animation="200"
      handle=".widget-drag-handle"
      @end="onDragEnd"
    >
      <template #item="{ element }">
        <div
          class="tw-card tw-card-hover flex min-h-[180px] flex-col overflow-hidden"
          :style="{ gridColumn: 'span ' + (element.w || 6) }"
        >
          <div class="flex items-center justify-between border-b border-slate-100 bg-gradient-to-r from-brand-50/80 to-white px-4 py-3">
            <span class="widget-drag-handle flex cursor-move items-center gap-2 text-sm font-semibold text-slate-700">
              <el-icon class="text-brand-500"><Rank /></el-icon>
              {{ element.title }}
            </span>
            <el-button link type="danger" icon="Close" @click="workbenchStore.removeWidget(element.id)" />
          </div>
          <div class="flex-1 p-4">
            <ApprovalWidget v-if="element.id === 'approval'" />
            <CustomerStatsWidget v-else-if="element.id === 'customer-stats'" />
            <OpportunityFunnelWidget v-else-if="element.id === 'opportunity-funnel'" />
            <UnreadMessagesWidget v-else-if="element.id === 'unread-messages'" />
            <QuickActionsWidget v-else-if="element.id === 'quick-actions'" />
            <ExpiringContractsWidget v-else-if="element.id === 'expiring-contracts'" />
          </div>
        </div>
      </template>
    </draggable>

    <el-dialog title="添加小组件" v-model="openAdd" width="480px" append-to-body class="!rounded-2xl">
      <el-table :data="addableWidgets" @row-click="handleAddWidget" highlight-current-row>
        <el-table-column label="组件" prop="title" />
        <el-table-column label="说明" prop="description" show-overflow-tooltip />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup name="CrmWorkbench">
import draggable from 'vuedraggable'
import { Rank } from '@element-plus/icons-vue'
import { CRM_PAGE_INTRO } from '@/constants/crmPageIntro'
import useCrmWorkbenchStore from '@/stores/crm/workbench'
import ApprovalWidget from './components/ApprovalWidget.vue'
import CustomerStatsWidget from './components/CustomerStatsWidget.vue'
import OpportunityFunnelWidget from './components/OpportunityFunnelWidget.vue'
import UnreadMessagesWidget from './components/UnreadMessagesWidget.vue'
import QuickActionsWidget from './components/QuickActionsWidget.vue'
import ExpiringContractsWidget from './components/ExpiringContractsWidget.vue'

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
:deep(.el-table__row) { cursor: pointer; }
</style>
