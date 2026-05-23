import { defineStore } from 'pinia'
import { getUnreadCount, getUnreadList } from '@/api/crm/message'

const useCrmMessageStore = defineStore('crmMessage', {
  state: () => ({
    unreadCount: 0,
    unreadList: []
  }),
  actions: {
    async fetchUnread() {
      try {
        const [countRes, listRes] = await Promise.all([getUnreadCount(), getUnreadList()])
        this.unreadCount = countRes.data || 0
        this.unreadList = listRes.data || []
      } catch {
        // 未登录或无权限时静默失败
      }
    }
  }
})

export default useCrmMessageStore
