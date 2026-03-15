<template>
  <div class="login-page">
    <div class="background-animation"></div>
    <el-card class="login-card" shadow="never">
      <div class="login-logo">
        <div class="logo-icon-wrapper">
          <span class="logo-icon">AI</span>
        </div>
        <h2 class="login-title">智询知识库问答系统</h2>
        <p class="login-sub">V1.0 企业级知识问答平台</p>
      </div>
      <el-form :model="form" label-position="top" class="login-form">
        <el-form-item label="账号">
          <el-input
            v-model="form.username"
            placeholder="请输入账号"
            size="large"
            clearable
          >
            <template #prefix>
              <el-icon><User /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            show-password
            clearable
            @keyup.enter="login"
          >
            <template #prefix>
              <el-icon><Lock /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-button
          type="primary"
          size="large"
          class="login-btn"
          :loading="loading"
          @click="login"
        >
          立即登录
        </el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
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
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  overflow: hidden;
}

/* 动态背景修饰 */
.background-animation {
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(64,158,255,0.1) 0%, rgba(255,255,255,0) 60%),
              radial-gradient(circle, rgba(103,194,58,0.05) 0%, rgba(255,255,255,0) 60%);
  background-position: 0 0, 50% 50%;
  background-size: 50% 50%, 40% 40%;
  animation: bg-shift 20s infinite alternate cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 0;
}

@keyframes bg-shift {
  0% { transform: scale(1) translate(0, 0) rotate(0deg); }
  100% { transform: scale(1.1) translate(5%, 5%) rotate(5deg); }
}

/* 毛玻璃风格卡片 */
.login-card {
  width: 440px;
  border-radius: 20px;
  padding: 30px 20px;
  background: rgba(255, 255, 255, 0.85) !important;
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.5) !important;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.08), 0 1px 3px rgba(0,0,0,0.05) !important;
  z-index: 1;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.login-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 25px 50px rgba(0, 0, 0, 0.12), 0 2px 5px rgba(0,0,0,0.08) !important;
}

.login-logo {
  text-align: center;
  margin-bottom: 32px;
}

.logo-icon-wrapper {
  display: inline-flex;
  padding: 8px;
  border-radius: 20px;
  background: linear-gradient(135deg, #e6f1fc 0%, #ffffff 100%);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.2);
  margin-bottom: 16px;
}

.logo-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border-radius: 16px;
  background: linear-gradient(135deg, #409eff 0%, #3a8ee6 100%);
  color: #fff;
  font-size: 24px;
  font-weight: 800;
  letter-spacing: 1px;
}

.login-title {
  margin: 0 0 8px;
  font-size: 26px;
  font-weight: 800;
  color: #1f2235;
  letter-spacing: 0.5px;
}

.login-sub {
  color: #909399;
  font-size: 14px;
  margin: 0;
  letter-spacing: 0.5px;
}

.login-form {
  padding: 0 10px;
}

:deep(.el-form-item__label) {
  font-weight: 600;
  color: #303133;
  padding-bottom: 4px;
}

:deep(.el-input__wrapper) {
  background-color: #f5f7fa !important;
  border-radius: 8px;
  box-shadow: 0 0 0 1px transparent inset !important;
  transition: all 0.2s cubic-bezier(0.645, 0.045, 0.355, 1);
}

:deep(.el-input__wrapper:hover), :deep(.el-input__wrapper.is-focus) {
  background-color: #ffffff !important;
  box-shadow: 0 0 0 1px #409eff inset !important;
}

.login-btn {
  width: 100%;
  margin-top: 16px;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 1px;
  background: linear-gradient(135deg, #409eff 0%, #3a8ee6 100%);
  border: none;
  box-shadow: 0 4px 12px rgba(64,158,255,0.4);
  transition: all 0.3s;
}

.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(64,158,255,0.5);
  background: linear-gradient(135deg, #66b1ff 0%, #3a8ee6 100%);
}
</style>
