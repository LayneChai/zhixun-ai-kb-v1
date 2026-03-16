<template>
  <el-main class="page">
    <el-card class="table-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="page-title">菜单管理</span>
        </div>
      </template>
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input v-model="keyword" clearable placeholder="搜索名称/权限" style="width: 280px">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select v-model="typeFilter" clearable placeholder="类型" style="width: 140px">
            <el-option label="目录" value="M" />
            <el-option label="菜单" value="C" />
            <el-option label="按钮" value="F" />
          </el-select>
        </div>
        <div class="toolbar-actions">
          <el-button plain @click="loadMenus"><el-icon><Refresh /></el-icon> 刷新</el-button>
          <el-button type="primary" @click="openMenuCreate"><el-icon><Plus /></el-icon> 新建菜单</el-button>
        </div>
      </div>
      <el-table
        :data="filteredMenus"
        border
        stripe
        v-loading="loading"
        row-key="id"
        :tree-props="{ children: 'children' }"
        default-expand-all
      >
        <el-table-column prop="menuName" label="名称" min-width="200" />
        <el-table-column prop="perms" label="权限标识" min-width="200" />
        <el-table-column prop="menuType" label="类型" width="80">
          <template #default="{ row }">{{ menuTypeLabel(row.menuType) }}</template>
        </el-table-column>
        <el-table-column prop="path" label="路由" min-width="160" />
        <el-table-column prop="component" label="组件" min-width="160" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openMenuCreate(row)">
              新增
            </el-button>
            <el-button link type="primary" @click="openMenuEdit(row)">
              编辑
            </el-button>
            <el-button link type="danger" @click="deleteMenu(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </el-main>

  <el-dialog v-model="menuDialogVisible" :title="menuEditingId ? decodeURIComponent('%E7%BC%96%E8%BE%91%E8%8F%9C%E5%8D%95') : decodeURIComponent('%E6%96%B0%E5%BB%BA%E8%8F%9C%E5%8D%95')" width="600px">
    <el-form label-width="100px">
      <el-form-item label="菜单名称">
        <el-input v-model="menuForm.menuName" />
      </el-form-item>
      <el-form-item label="父级菜单">
        <el-select v-model="menuForm.parentId" style="width: 100%">
          <el-option :label="decodeURIComponent('%E9%A1%B6%E7%BA%A7%E8%8F%9C%E5%8D%95')" :value="0" />
          <el-option
            v-for="item in menuFlat"
            :key="item.id"
            :label="`${item.menuName}`"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="路由路径">
        <el-input v-model="menuForm.path" />
      </el-form-item>
      <el-form-item label="组件路径">
        <el-input v-model="menuForm.component" />
      </el-form-item>
      <el-form-item label="权限标识">
        <el-input v-model="menuForm.perms" />
      </el-form-item>
      <el-form-item label="菜单类型">
        <el-radio-group v-model="menuForm.menuType">
          <el-radio label="M">目录</el-radio>
          <el-radio label="C">菜单</el-radio>
          <el-radio label="F">按钮</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="排序">
        <el-input-number v-model="menuForm.sort" :min="0" />
      </el-form-item>
      <el-form-item label="状态">
        <el-radio-group v-model="menuForm.status">
          <el-radio label="0">{{ decodeURIComponent('%E6%AD%A3%E5%B8%B8') }}</el-radio>
          <el-radio label="1">{{ decodeURIComponent('%E7%A6%81%E7%94%A8') }}</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="menuDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="saveMenu">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import http from '../../api/http'

const menuTree = ref([])
const menuFlat = ref([])
const keyword = ref('')
const typeFilter = ref('')
const loading = ref(false)
const filteredMenus = computed(() => {
  const k = keyword.value.trim().toLowerCase()
  const t = typeFilter.value
  return menuTree.value.filter(m => {
    const matchText = !k || String(m.menuName || '').toLowerCase().includes(k) || String(m.perms || '').toLowerCase().includes(k)
    const matchType = !t || m.menuType === t
    return matchText && matchType
  })
})

const menuDialogVisible = ref(false)
const menuEditingId = ref(null)
const menuForm = reactive({
  parentId: 0,
  menuName: '',
  path: '',
  component: '',
  perms: '',
  menuType: 'C',
  sort: 0,
  status: '0'
})

const menuTypeLabel = (t) => ({ M: decodeURIComponent('%E7%9B%AE%E5%BD%95'), C: decodeURIComponent('%E8%8F%9C%E5%8D%95'), F: decodeURIComponent('%E6%8C%89%E9%92%AE') }[t] || t)

const loadMenus = async () => {
  loading.value = true
  try {
    const [resTree, resFlat] = await Promise.all([
      http.get('/system/menus'),
      http.get('/system/menus', { params: { mode: 'flat' } })
    ])
    menuTree.value = resTree.data.data || []
    menuFlat.value = resFlat.data.data || []
  } catch (e) {
    ElMessage.error(decodeURIComponent('%E5%8A%A0%E8%BD%BD%E8%8F%9C%E5%8D%95%E5%A4%B1%E8%B4%A5') + ': ' + (e.response?.data?.message || e.message))
    menuTree.value = []
    menuFlat.value = []
  } finally {
    loading.value = false
  }
}

const resetMenuForm = () => {
  menuEditingId.value = null
  menuForm.parentId = 0
  menuForm.menuName = ''
  menuForm.path = ''
  menuForm.component = ''
  menuForm.perms = ''
  menuForm.menuType = 'C'
  menuForm.sort = 0
  menuForm.status = '0'
}

const openMenuCreate = (row) => {
  resetMenuForm()
  if (row && row.id) {
    menuForm.parentId = row.id
  }
  menuDialogVisible.value = true
}

const openMenuEdit = (row) => {
  menuEditingId.value = row.id
  menuForm.parentId = row.parentId ?? 0
  menuForm.menuName = row.menuName || ''
  menuForm.path = row.path || ''
  menuForm.component = row.component || ''
  menuForm.perms = row.perms || ''
  menuForm.menuType = row.menuType || 'C'
  menuForm.sort = row.sort ?? 0
  menuForm.status = row.status || '0'
  menuDialogVisible.value = true
}

const saveMenu = async () => {
  if (!menuForm.menuName) {
    ElMessage.warning(decodeURIComponent('%E8%AF%B7%E8%BE%93%E5%85%A5%E8%8F%9C%E5%8D%95%E5%90%8D%E7%A7%B0'))
    return
  }
  const payload = { ...menuForm }
  const res = menuEditingId.value
    ? await http.put(`/system/menus/${menuEditingId.value}`, payload)
    : await http.post('/system/menus', payload)
  if (res.data.code !== 0) {
    ElMessage.error(res.data.message || decodeURIComponent('%E4%BF%9D%E5%AD%98%E5%A4%B1%E8%B4%A5'))
    return
  }
  ElMessage.success(decodeURIComponent('%E4%BF%9D%E5%AD%98%E6%88%90%E5%8A%9F'))
  menuDialogVisible.value = false
  await loadMenus()
}

const deleteMenu = async (row) => {
  try {
    await ElMessageBox.confirm(
      `${decodeURIComponent('%E7%A1%AE%E8%AE%A4%E5%88%A0%E9%99%A4%E8%8F%9C%E5%8D%95')} ${row.menuName} ${decodeURIComponent('%E5%90%97')}`,
      decodeURIComponent('%E6%8F%90%E7%A4%BA'),
      { type: 'warning' }
    )
    const res = await http.delete(`/system/menus/${row.id}`)
    if (res.data.code !== 0) {
      ElMessage.error(res.data.message || decodeURIComponent('%E5%88%A0%E9%99%A4%E5%A4%B1%E8%B4%A5'))
      return
    }
    ElMessage.success(decodeURIComponent('%E5%88%A0%E9%99%A4%E6%88%90%E5%8A%9F'))
    await loadMenus()
  } catch (_e) {
    // canceled
  }
}

onMounted(loadMenus)
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
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.toolbar-left {
  display: flex;
  gap: 8px;
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
