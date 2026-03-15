<template>
  <el-main class="page">
    <el-card class="table-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="page-title">用户列表</span>
        </div>
      </template>
      <div class="toolbar">
        <el-input v-model="keyword" clearable placeholder="搜索用户名" style="width: 280px" >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <div class="toolbar-actions">
          <el-button plain @click="loadUsers"><el-icon><Refresh /></el-icon> 刷新</el-button>
          <el-button type="primary" v-perm="'system:user:add'" @click="openUserCreate">
            <el-icon><Plus /></el-icon> 新建用户
          </el-button>
        </div>
      </div>
      <el-table :data="filteredUsers" border stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="username" label="用户名" min-width="160" />
        <el-table-column label="角色" min-width="200">
          <template #default="{ row }">
            <el-tag v-for="role in (row.roleNames || [])" :key="role" class="tag-item" effect="light">{{ role }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" effect="dark">
              {{ row.status === 1 ? decodeURIComponent('%E6%AD%A3%E5%B8%B8') : decodeURIComponent('%E7%A6%81%E7%94%A8') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" v-perm="'system:user:edit'" @click="openUserEdit(row)">编辑</el-button>
            <el-button link type="danger" v-perm="'system:user:delete'" @click="deleteUser(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </el-main>

  <el-dialog v-model="userDialogVisible" :title="userEditingId ? decodeURIComponent('%E7%BC%96%E8%BE%91%E7%94%A8%E6%88%B7') : decodeURIComponent('%E6%96%B0%E5%BB%BA%E7%94%A8%E6%88%B7')" width="520px">
    <el-form label-width="90px">
      <el-form-item label="用户名">
        <el-input v-model="userForm.username" />
      </el-form-item>
      <el-form-item label="密码">
        <el-input v-model="userForm.password" type="password" show-password :placeholder="decodeURIComponent('%E7%BC%96%E8%BE%91%E6%97%B6%E7%95%99%E7%A9%BA%E5%88%99%E4%B8%8D%E4%BF%AE%E6%94%B9')" />
      </el-form-item>
      <el-form-item label="状态">
        <el-radio-group v-model="userForm.status">
          <el-radio :label="1">{{ decodeURIComponent('%E6%AD%A3%E5%B8%B8') }}</el-radio>
          <el-radio :label="0">{{ decodeURIComponent('%E7%A6%81%E7%94%A8') }}</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="角色">
        <el-select v-model="userForm.roleIds" multiple style="width: 100%">
          <el-option v-for="role in roles" :key="role.id" :label="role.roleName" :value="role.id" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="userDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="saveUser">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import http from '../../api/http'

const users = ref([])
const keyword = ref('')
const loading = ref(false)
const roles = ref([])

const filteredUsers = computed(() => {
  const k = keyword.value.trim().toLowerCase()
  if (!k) return users.value
  return users.value.filter(u => String(u.username || '').toLowerCase().includes(k))
})
const userDialogVisible = ref(false)
const userEditingId = ref(null)
const userForm = reactive({
  username: '',
  password: '',
  status: 1,
  roleIds: []
})

const loadUsers = async () => {
  loading.value = true
  try {
    const res = await http.get('/system/users')
    users.value = res.data.data || []
  } catch (e) {
    ElMessage.error(decodeURIComponent('%E5%8A%A0%E8%BD%BD%E7%94%A8%E6%88%B7%E5%A4%B1%E8%B4%A5') + ': ' + (e.response?.data?.message || e.message))
    users.value = []
  } finally {
    loading.value = false
  }
}

const loadRoles = async () => {
  try {
    const res = await http.get('/system/roles')
    roles.value = res.data.data || []
  } catch {
    roles.value = []
  }
}

const resetUserForm = () => {
  userEditingId.value = null
  userForm.username = ''
  userForm.password = ''
  userForm.status = 1
  userForm.roleIds = []
}

const openUserCreate = () => {
  resetUserForm()
  userDialogVisible.value = true
}

const openUserEdit = (row) => {
  userEditingId.value = row.id
  userForm.username = row.username
  userForm.password = ''
  userForm.status = row.status
  userForm.roleIds = [...(row.roleIds || [])]
  userDialogVisible.value = true
}

const saveUser = async () => {
  if (!userForm.username) {
    ElMessage.warning(decodeURIComponent('%E8%AF%B7%E8%BE%93%E5%85%A5%E7%94%A8%E6%88%B7%E5%90%8D'))
    return
  }
  if (!userEditingId.value && !userForm.password) {
    ElMessage.warning(decodeURIComponent('%E6%96%B0%E5%BB%BA%E7%94%A8%E6%88%B7%E5%BF%85%E9%A1%BB%E5%A1%AB%E5%86%99%E5%AF%86%E7%A0%81'))
    return
  }
  const payload = {
    username: userForm.username,
    password: userForm.password,
    status: userForm.status,
    roleIds: userForm.roleIds
  }
  const res = userEditingId.value
    ? await http.put(`/system/users/${userEditingId.value}`, payload)
    : await http.post('/system/users', payload)
  if (res.data.code !== 0) {
    ElMessage.error(res.data.message || decodeURIComponent('%E4%BF%9D%E5%AD%98%E5%A4%B1%E8%B4%A5'))
    return
  }
  ElMessage.success(decodeURIComponent('%E4%BF%9D%E5%AD%98%E6%88%90%E5%8A%9F'))
  userDialogVisible.value = false
  await loadUsers()
}

const deleteUser = async (row) => {
  try {
    await ElMessageBox.confirm(
      `${decodeURIComponent('%E7%A1%AE%E8%AE%A4%E5%88%A0%E9%99%A4%E7%94%A8%E6%88%B7')} ${row.username} ${decodeURIComponent('%E5%90%97')}`,
      decodeURIComponent('%E6%8F%90%E7%A4%BA'),
      { type: 'warning' }
    )
    const res = await http.delete(`/system/users/${row.id}`)
    if (res.data.code !== 0) {
      ElMessage.error(res.data.message || decodeURIComponent('%E5%88%A0%E9%99%A4%E5%A4%B1%E8%B4%A5'))
      return
    }
    ElMessage.success(decodeURIComponent('%E5%88%A0%E9%99%A4%E6%88%90%E5%8A%9F'))
    await loadUsers()
  } catch (_e) {
    // canceled
  }
}

onMounted(async () => {
  await Promise.all([loadUsers(), loadRoles()])
})
</script>

<style scoped>
.page {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 60px);
}

.table-card {
  border-radius: 8px;
  border: none;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05) !important;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.page-title {
  font-size: 18px;
  font-weight: 700;
  color: #303133;
}

.tag-item {
  margin-right: 6px;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.toolbar-actions {
  display: flex;
  gap: 8px;
}

/* 列表与表格的基础优化 */
:deep(.el-table th.el-table__cell) {
  background-color: #f8f9fa !important;
  color: #606266;
  font-weight: 700;
}
</style>
