<template>
  <el-main class="page">
    <el-row :gutter="19">
      <el-col :span="14">
        <el-card class="table-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span class="page-title">角色管理</span>
            </div>
          </template>
          <div class="toolbar">
            <el-input v-model="keyword" clearable placeholder="搜索角色名/标识" style="width: 280px">
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
            <div class="toolbar-actions">
              <el-button plain @click="loadRoles"><el-icon><Refresh /></el-icon> 刷新</el-button>
              <el-button type="primary" v-perm="'system:role:add'" @click="openRoleCreate"><el-icon><Plus /></el-icon> 新建角色</el-button>
            </div>
          </div>
          <el-table :data="filteredRoles" border stripe v-loading="loading" @row-click="selectRole" style="width: 100%" highlight-current-row>
            <el-table-column prop="id" label="ID" width="80" align="center" />
            <el-table-column prop="roleName" label="角色名" min-width="140" />
            <el-table-column prop="roleKey" label="角色标识" min-width="140" />
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === '0' ? 'success' : 'danger'" effect="dark">
                  {{ row.status === '0' ? decodeURIComponent('%E6%AD%A3%E5%B8%B8') : decodeURIComponent('%E7%A6%81%E7%94%A8') }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" v-perm="'system:role:edit'" @click.stop="openRoleEdit(row)">编辑</el-button>
                <el-button link type="danger" v-perm="'system:role:delete'" @click.stop="deleteRole(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="10">
        <el-card class="table-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span class="page-title">角色菜单授权</span>
              <el-button type="primary" v-perm="'system:role:grant'" :disabled="!selectedRoleId" @click="saveRoleMenus">
                保存授权
              </el-button>
            </div>
          </template>
          <div class="role-tip">
            当前角色:
            <el-tag type="info" size="large" effect="light">{{ selectedRoleName || decodeURIComponent('%E6%9C%AA%E9%80%89%E6%8B%A9') }}</el-tag>
          </div>
          <el-tree
            ref="menuTreeRef"
            :data="menuTree"
            node-key="id"
            show-checkbox
            default-expand-all
            :props="{ label: 'menuName', children: 'children' }"
          />
        </el-card>
      </el-col>
    </el-row>
  </el-main>

  <el-dialog v-model="roleDialogVisible" :title="roleEditingId ? decodeURIComponent('%E7%BC%96%E8%BE%91%E8%A7%92%E8%89%B2') : decodeURIComponent('%E6%96%B0%E5%BB%BA%E8%A7%92%E8%89%B2')" width="520px">
    <el-form label-width="100px">
      <el-form-item label="角色名称">
        <el-input v-model="roleForm.roleName" />
      </el-form-item>
      <el-form-item label="角色标识">
        <el-input v-model="roleForm.roleKey" />
      </el-form-item>
      <el-form-item label="状态">
        <el-radio-group v-model="roleForm.status">
          <el-radio label="0">{{ decodeURIComponent('%E6%AD%A3%E5%B8%B8') }}</el-radio>
          <el-radio label="1">{{ decodeURIComponent('%E7%A6%81%E7%94%A8') }}</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="roleDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="saveRole">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import http from '../../api/http'

const roles = ref([])
const keyword = ref('')
const loading = ref(false)
const menuTree = ref([])

const filteredRoles = computed(() => {
  const k = keyword.value.trim().toLowerCase()
  if (!k) return roles.value
  return roles.value.filter(r =>
    String(r.roleName || '').toLowerCase().includes(k) ||
    String(r.roleKey || '').toLowerCase().includes(k)
  )
})
const selectedRoleId = ref(null)
const selectedRoleName = ref('')
const menuTreeRef = ref()

const roleDialogVisible = ref(false)
const roleEditingId = ref(null)
const roleForm = reactive({
  roleName: '',
  roleKey: '',
  status: '0'
})

const loadRoles = async () => {
  loading.value = true
  try {
    const res = await http.get('/system/roles')
    roles.value = res.data.data || []
  } catch (e) {
    ElMessage.error(decodeURIComponent('%E5%8A%A0%E8%BD%BD%E8%A7%92%E8%89%B2%E5%A4%B1%E8%B4%A5') + ': ' + (e.response?.data?.message || e.message))
    roles.value = []
  } finally {
    loading.value = false
  }
}

const loadMenus = async () => {
  try {
    const resTree = await http.get('/system/menus')
    menuTree.value = resTree.data.data || []
  } catch (e) {
    ElMessage.error(decodeURIComponent('%E5%8A%A0%E8%BD%BD%E8%8F%9C%E5%8D%95%E5%A4%B1%E8%B4%A5') + ': ' + (e.response?.data?.message || e.message))
    menuTree.value = []
  }
}

const resetRoleForm = () => {
  roleEditingId.value = null
  roleForm.roleName = ''
  roleForm.roleKey = ''
  roleForm.status = '0'
}

const openRoleCreate = () => {
  resetRoleForm()
  roleDialogVisible.value = true
}

const openRoleEdit = (row) => {
  roleEditingId.value = row.id
  roleForm.roleName = row.roleName
  roleForm.roleKey = row.roleKey
  roleForm.status = row.status
  roleDialogVisible.value = true
}

const saveRole = async () => {
  if (!roleForm.roleName || !roleForm.roleKey) {
    ElMessage.warning(decodeURIComponent('%E8%AF%B7%E8%BE%93%E5%85%A5%E8%A7%92%E8%89%B2%E5%90%8D%E7%A7%B0%E5%92%8C%E6%A0%87%E8%AF%86'))
    return
  }
  const payload = {
    roleName: roleForm.roleName,
    roleKey: roleForm.roleKey,
    status: roleForm.status
  }
  const res = roleEditingId.value
    ? await http.put(`/system/roles/${roleEditingId.value}`, payload)
    : await http.post('/system/roles', payload)
  if (res.data.code !== 0) {
    ElMessage.error(res.data.message || decodeURIComponent('%E4%BF%9D%E5%AD%98%E5%A4%B1%E8%B4%A5'))
    return
  }
  ElMessage.success(decodeURIComponent('%E4%BF%9D%E5%AD%98%E6%88%90%E5%8A%9F'))
  roleDialogVisible.value = false
  await loadRoles()
}

const deleteRole = async (row) => {
  try {
    await ElMessageBox.confirm(
      `${decodeURIComponent('%E7%A1%AE%E8%AE%A4%E5%88%A0%E9%99%A4%E8%A7%92%E8%89%B2')} ${row.roleName} ${decodeURIComponent('%E5%90%97')}`,
      decodeURIComponent('%E6%8F%90%E7%A4%BA'),
      { type: 'warning' }
    )
    const res = await http.delete(`/system/roles/${row.id}`)
    if (res.data.code !== 0) {
      ElMessage.error(res.data.message || decodeURIComponent('%E5%88%A0%E9%99%A4%E5%A4%B1%E8%B4%A5'))
      return
    }
    ElMessage.success(decodeURIComponent('%E5%88%A0%E9%99%A4%E6%88%90%E5%8A%9F'))
    if (selectedRoleId.value === row.id) {
      selectedRoleId.value = null
      selectedRoleName.value = ''
      menuTreeRef.value?.setCheckedKeys([])
    }
    await loadRoles()
  } catch (_e) {
    // canceled
  }
}

const selectRole = async (row) => {
  selectedRoleId.value = row.id
  selectedRoleName.value = row.roleName
  await nextTick()
  menuTreeRef.value?.setCheckedKeys(row.menuIds || [])
}

const saveRoleMenus = async () => {
  if (!selectedRoleId.value) {
    ElMessage.warning(decodeURIComponent('%E8%AF%B7%E5%85%88%E9%80%89%E6%8B%A9%E8%A7%92%E8%89%B2'))
    return
  }
  const checked = menuTreeRef.value?.getCheckedKeys(false) || []
  const halfChecked = menuTreeRef.value?.getHalfCheckedKeys() || []
  const payload = [...new Set([...checked, ...halfChecked])]
  const res = await http.post(`/system/roles/${selectedRoleId.value}/menus`, payload)
  if (res.data.code !== 0) {
    ElMessage.error(res.data.message || decodeURIComponent('%E4%BF%9D%E5%AD%98%E5%A4%B1%E8%B4%A5'))
    return
  }
  ElMessage.success(decodeURIComponent('%E6%8E%88%E6%9D%83%E6%88%90%E5%8A%9F'))
  await loadRoles()
}

onMounted(async () => {
  await Promise.all([loadRoles(), loadMenus()])
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
.role-tip {
  margin-bottom: 16px;
  font-size: 15px;
  font-weight: 500;
}

:deep(.el-table__header-wrapper),
:deep(.el-table__body-wrapper) {
  overflow-x: hidden;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.toolbar-actions {
  display: flex;
  gap: 8px;
}

:deep(.el-card__body) {
  overflow-x: hidden !important;
  overflow-y: auto;
}

.tree-container {
  overflow-x: hidden;
}

:deep(.el-tree) {
  min-width: 100%;
  display: inline-block;
}

:deep(.el-table) {
  width: 100% !important;
  max-width: 100%;
}

:deep(.el-table__header-wrapper) {
  overflow: hidden !important;
}

:deep(.el-table th.el-table__cell) {
  background-color: #f8f9fa !important;
  color: #606266;
  font-weight: 700;
}
</style>
