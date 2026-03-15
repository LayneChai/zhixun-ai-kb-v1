import axios from 'axios'
import router from '../router'

const http = axios.create({ baseURL: '/api' })

// 请求拦截器：自动携带 token
http.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers['Authorization'] = `Bearer ${token}`
  }
  return config
})

// 响应拦截器：401 自动跳转登录页
http.interceptors.response.use(
  res => res,
  err => {
    if (err.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('userId')
      router.push('/login')
    }
    return Promise.reject(err)
  }
)

export default http
