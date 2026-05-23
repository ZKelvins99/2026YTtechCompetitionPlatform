import { defineStore } from 'pinia'
import { getWorkbenchLayout, saveWorkbenchLayout, getWorkbenchWidgets } from '@/api/crm/workbench'

const DEFAULT_WIDGETS = [
  { id: 'approval', title: '待办审批', x: 0, y: 0, w: 6, h: 4 },
  { id: 'customer-stats', title: '客户统计', x: 6, y: 0, w: 6, h: 2 },
  { id: 'opportunity-funnel', title: '商机漏斗', x: 0, y: 4, w: 6, h: 4 },
  { id: 'unread-messages', title: '未读消息', x: 6, y: 2, w: 6, h: 3 }
]

const useCrmWorkbenchStore = defineStore('crmWorkbench', {
  state: () => ({
    widgets: [],
    availableWidgets: []
  }),
  actions: {
    async loadLayout() {
      const res = await getWorkbenchLayout()
      this.widgets = res.data.widgets || [...DEFAULT_WIDGETS]
    },
    async loadAvailable() {
      const res = await getWorkbenchWidgets()
      this.availableWidgets = res.data || []
    },
    async saveLayout() {
      await saveWorkbenchLayout({ widgets: this.widgets })
    },
    resetLayout() {
      this.widgets = JSON.parse(JSON.stringify(DEFAULT_WIDGETS))
    },
    addWidget(meta) {
      if (this.widgets.some(w => w.id === meta.id)) return false
      this.widgets.push({
        id: meta.id,
        title: meta.title,
        x: 0,
        y: 0,
        w: 6,
        h: meta.id === 'quick-actions' ? 2 : 3
      })
      return true
    },
    removeWidget(id) {
      this.widgets = this.widgets.filter(w => w.id !== id)
    }
  }
})

export default useCrmWorkbenchStore
