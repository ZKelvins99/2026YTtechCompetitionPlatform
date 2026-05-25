import { defineStore } from 'pinia'
import { getUnreadCount, getUnreadList, getInboxList, markMessageRead } from '@/api/crm/message'

const useCrmMessageStore = defineStore('crmMessage', {
  state: () => ({
    unreadCount: 0,
    unreadList: [],
    inboxList: []
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
    },
    async fetchInbox(limit = 8) {
      try {
        const res = await getInboxList(limit)
        this.inboxList = res.data || []
      } catch {
        this.inboxList = []
      }
    },
    async refreshAll(limit = 8) {
      await Promise.all([this.fetchUnread(), this.fetchInbox(limit)])
    },
    async markRead(id) {
      await markMessageRead(id)
      await this.refreshAll()
    }
  }
})

export default useCrmMessageStore
