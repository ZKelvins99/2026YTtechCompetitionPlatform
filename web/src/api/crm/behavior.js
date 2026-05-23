import axios from 'axios'
import { getToken } from '@/utils/auth'
import request from '@/utils/request'

const baseURL = import.meta.env.VITE_APP_BASE_API

export function generateBehavior(count) {
  return request({
    url: '/crm/behavior/generate',
    method: 'post',
    data: { count },
    headers: { repeatSubmit: false }
  })
}

export function getBehaviorTask(taskId) {
  return request({
    url: '/crm/behavior/task/' + taskId,
    method: 'get'
  })
}

export function getBehaviorTotal() {
  return request({
    url: '/crm/behavior/total',
    method: 'get'
  })
}

/** 游标分页，返回 data 与响应头 X-Response-Time */
export function scrollBehavior(params) {
  return axios.get(baseURL + '/crm/behavior/scroll', {
    params,
    headers: { Authorization: 'Bearer ' + getToken() },
    timeout: 30000
  }).then(res => {
    const code = res.data?.code || 200
    if (code !== 200) {
      return Promise.reject(new Error(res.data?.msg || '请求失败'))
    }
    return {
      data: res.data.data,
      responseTime: res.headers['x-response-time'] || res.headers['X-Response-Time'] || '-'
    }
  })
}
