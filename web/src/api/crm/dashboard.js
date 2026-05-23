import request from '@/utils/request'

export function getSystemStatus() {
  return request({ url: '/crm/dashboard/system-status', method: 'get' })
}

export function getLogStats() {
  return request({ url: '/crm/dashboard/log-stats', method: 'get' })
}
