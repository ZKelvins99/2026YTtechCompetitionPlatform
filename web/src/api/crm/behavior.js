import axios from 'axios'
import { getToken } from '@/utils/auth'
import request from '@/utils/request'

const baseURL = import.meta.env.VITE_APP_BASE_API

/** 异步 Excel 导入，返回 taskId */
export function importBehaviorExcel(formData) {
  return request({
    url: '/crm/behavior/import',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data', repeatSubmit: false },
    timeout: 300000
  })
}

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

/** 当前用户进行中的 Excel 导入任务（Redis），无任务时 data 为 null */
export function getActiveBehaviorImport() {
  return request({
    url: '/crm/behavior/importActive',
    method: 'get'
  })
}

export function clearBehaviorData() {
  return request({
    url: '/crm/behavior/clear',
    method: 'delete'
  })
}

export function getBehaviorTotal(refresh = false) {
  return request({
    url: '/crm/behavior/total',
    method: 'get',
    params: refresh ? { refresh: true } : {}
  })
}

/** 游标分页，返回 data 与响应头 X-Response-Time */
export function scrollBehavior(params) {
  return axios.get(baseURL + '/crm/behavior/scroll', {
    params,
    headers: { Authorization: 'Bearer ' + getToken() },
    timeout: 120000
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
