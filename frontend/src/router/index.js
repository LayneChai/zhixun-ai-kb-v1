import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import DashboardView from '../views/DashboardView.vue'
import DatasetView from '../views/DatasetView.vue'
import DocumentView from '../views/DocumentView.vue'
import QaView from '../views/QaView.vue'
import SystemUsersView from '../views/system/SystemUsersView.vue'
import SystemRolesView from '../views/system/SystemRolesView.vue'
import SystemMenusView from '../views/system/SystemMenusView.vue'
import SystemLogsView from '../views/system/SystemLogsView.vue'
import LlmConfigView from '../views/LlmConfigView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/login' },
    { path: '/login', component: LoginView },
    { path: '/dashboard', component: DashboardView, meta: { requiresAuth: true } },
    { path: '/dataset', component: DatasetView, meta: { requiresAuth: true, roles: ['admin', 'super-admin'] } },
    { path: '/document', component: DocumentView, meta: { requiresAuth: true, roles: ['admin', 'super-admin'] } },
    { path: '/qa', component: QaView, meta: { requiresAuth: true } },
    { path: '/system', redirect: '/system/users' },
    { path: '/system/users', component: SystemUsersView, meta: { requiresAuth: true, roles: ['admin', 'super-admin'] } },
    { path: '/system/roles', component: SystemRolesView, meta: { requiresAuth: true, roles: ['admin', 'super-admin'] } },
    { path: '/system/permission', component: SystemRolesView, meta: { requiresAuth: true, roles: ['admin', 'super-admin'] } },
    { path: '/system/menus', component: SystemMenusView, meta: { requiresAuth: true, roles: ['admin', 'super-admin'] } },
    { path: '/system/logs', component: SystemLogsView, meta: { requiresAuth: true, roles: ['admin', 'super-admin'] } },
    { path: '/llm-config', component: LlmConfigView, meta: { requiresAuth: true, roles: ['admin', 'super-admin'] } }
  ]
})

// 全局路由守卫
router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')
  const userRole = localStorage.getItem('role') || 'user'

  if (to.meta.requiresAuth && !token) {
    next('/login')
  } else if (to.meta.roles && !to.meta.roles.includes(userRole)) {
    // 权限不足，回退到智能问答
    next('/qa')
  } else {
    next()
  }
})

export default router
