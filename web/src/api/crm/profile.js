import request from '@/utils/request'

export function getUserProfile(userId) {
  return request({ url: '/crm/profile/' + userId, method: 'get' })
}
