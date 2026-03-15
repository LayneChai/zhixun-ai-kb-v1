<template>
  <router-view v-if="isLoginPage" />
  <el-container v-else style="height:100vh;">
    <el-aside v-if="showSidebar" width="220px" class="sidebar">
      <div class="sidebar-brand">
        <span class="brand-icon">AI</span>
        <span class="brand-name">智询 AI 知识库</span>
      </div>
      <el-menu
        :default-active="$route.path"
        router
        background-color="#1a1c2e"
        text-color="#b8bbd0"
        active-text-color="#409eff"
        :collapse="false"
      >
        <template v-for="item in menuTree" :key="item.id">
          <el-sub-menu v-if="item.children && item.children.length" :index="item.path || String(item.id)">
            <template #title>
              <span>{{ item.menuName }}</span>
            </template>
            <el-menu-item
              v-for="child in (item.children || []).filter(c => c.menuType !== 'F')"
              :key="child.id"
              :index="child.path || String(child.id)"
            >
              <span>{{ child.menuName }}</span>
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else-if="item.menuType !== 'F'" :index="item.path || String(item.id)">
            <span>{{ item.menuName }}</span>
          </el-menu-item>
        </template>
      </el-menu>
      <div class="sidebar-footer">
        <span class="sidebar-user">当前用户: {{ username }}</span>
        <el-button link type="danger" size="small" @click="logout">退出</el-button>
      </div>
    </el-aside>

    <el-main class="main-content" :class="{ 'main-content-full': !showSidebar || $route.path === '/qa' }">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import http from './api/http'
import { setPerms } from './utils/permission'

const route = useRoute()
const router = useRouter()

const isLoginPage = computed(() => route.path === '/login' || route.path === '/')
const username = ref(localStorage.getItem('username') || '')
const role = ref(localStorage.getItem('role') || 'user')
const menuTree = ref([])

const refreshUser = () => {
  username.value = localStorage.getItem('username') || ''
  role.value = localStorage.getItem('role') || 'user'
}

watch(() => route.path, () => {
  refreshUser()
  loadMenus()
  loadPerms()
})

const showSidebar = computed(() => role.value === 'admin' || role.value === 'super-admin')

const loadMenus = async () => {
  if (!showSidebar.value) {
    menuTree.value = []
    return
  }
  try {
    const res = await http.get('/system/menus/my')
    menuTree.value = res.data.data || []
  } catch {
    menuTree.value = []
  }
}

const loadPerms = async () => {
  try {
    const res = await http.get('/system/perms/my')
    setPerms(res.data.data || [])
  } catch {
    setPerms([])
  }
}

onMounted(() => {
  loadMenus()
  loadPerms()
})

const logout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('userId')
  localStorage.removeItem('username')
  localStorage.removeItem('perms')
  localStorage.removeItem('perms_loaded')
  router.push('/login')
}
</script>

<style>
body {
  margin: 0;
  padding: 0;
  font-family: 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  background: #f0f2f5;
}

* {
  box-sizing: border-box;
}
</style>

<style scoped>
.sidebar {
  background: #1a1c2e;
  display: flex;
  flex-direction: column;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.18);
}

.sidebar-brand {
  padding: 20px 16px 16px;
  display: flex;
  align-items: center;
  gap: 10px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.brand-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: #409eff;
  color: #fff;
  font-size: 14px;
  font-weight: 700;
}

.brand-name {
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  white-space: nowrap;
}

.sidebar-footer {
  margin-top: auto;
  padding: 12px 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.sidebar-user {
  color: #b8bbd0;
  font-size: 13px;
}

.main-content {
  background: #f0f2f5;
  overflow-y: auto;
  padding: 24px;
}

.main-content-full {
  padding: 0;
}
</style>
