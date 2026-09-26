import axios from 'axios'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code === 200) {
      return res
    } else {
      // 并发冲突：携带最新记录内容，调用方据此提示并刷新
      const error = new Error(res.message || '请求失败')
      error.code = res.code
      error.latestData = res.data
      return Promise.reject(error)
    }
  },
  error => {
    console.error('请求错误:', error)
    const res = error.response
    if (res && res.data) {
      error.message = res.data.message || error.message
      error.code = res.data.code || res.status
      error.latestData = res.data.data
    }
    return Promise.reject(error)
  }
)

export default request
