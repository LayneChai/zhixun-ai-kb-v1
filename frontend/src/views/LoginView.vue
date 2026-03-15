<template>
  <div class="login-page">
    <el-card class="login-card" shadow="always">
      <div class="login-logo">
        <span class="logo-icon">AI</span>
        <h2 class="login-title">智讯 AI 知识库</h2>
        <p class="login-sub">V1.0 企业知识问答平台</p>
      </div>
      <el-form :model="form" label-position="top">
        <el-form-item label="账号">
          <el-input
            v-model="form.username"
            placeholder="请输入账号"
            size="large"
          />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            show-password
            @keyup.enter="login"
          />
        </el-form-item>
        <el-button
          type="primary"
          size="large"
          style="width:100%;margin-top:8px;"
          :loading="loading"
          @click="login"
        >
          登录
        </el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '../api/http'

const router = useRouter()
const loading = ref(false)
const form = reactive({ username: 'admin', password: '123456' })

const login = async () => {
  if (!form.username || !form.password) {
    ElMessage.warning('账号和密码不能为空')
    return
  }

  loading.value = true
  try {
    const res = await http.post('/auth/login', form)
    const data = res.data
    if (data.code === 200 || data.code === 0 || data.data) {
      const payload = data.data || {}
      localStorage.setItem('token', payload.token || 'demo-token')
      localStorage.setItem('userId', payload.user?.id || '1')
      localStorage.setItem('username', payload.user?.username || form.username)
      localStorage.setItem('role', payload.user?.role || 'user')
      ElMessage.success('登录成功')
      const role = payload.user?.role || 'user'
      if (role === 'admin' || role === 'super-admin') {
        router.push('/dashboard')
      } else {
        router.push('/qa')
      }
    } else {
      ElMessage.error(data.message || '用户名或密码错误')
    }
  } catch (e) {
    ElMessage.error('登录失败，请检查网络或账号密码')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #1a1c2e 0%, #2d3561 50%, #1a1c2e 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-card {
  width: 400px;
  border-radius: 16px;
  padding: 16px;
}

.login-logo {
  text-align: center;
  margin-bottom: 28px;
}

.logo-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border-radius: 16px;
  background: #1a1c2e;
  color: #fff;
  font-size: 22px;
  font-weight: 700;
}

.login-title {
  margin: 12px 0 4px;
  font-size: 22px;
  font-weight: 700;
  color: #1a1c2e;
}

.login-sub {
  color: #888;
  font-size: 13px;
  margin: 0;
}
</style>
