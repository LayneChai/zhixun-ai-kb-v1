<template>
  <el-main class="dataset-page">
    <div class="page-header">
      <h3 class="page-title">知识库管理</h3>
      <el-button type="primary" @click="openCreateDialog">
        <el-icon><Plus /></el-icon> 新增知识库
      </el-button>
    </div>
    
    <el-card class="table-card" shadow="never">
      <el-table :data="rows" style="width: 100%" border stripe>
        <el-table-column prop="id" label="ID" width="80" align="center"/>
        <el-table-column prop="name" label="名称">
          <template #default="scope">
            <span class="ds-name">{{ scope.row.name }}</span>
            <el-tag v-if="scope.row.isDefault === 1" size="small" type="success" style="margin-left:8px" effect="light">默认</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" show-overflow-tooltip/>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="scope">
            <el-tag v-if="scope.row.status === 1" type="success" size="small" effect="dark">启用</el-tag>
            <el-tag v-else type="danger" size="small" effect="dark">停用</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="380" fixed="right">
          <template #default="scope">
            <el-button size="small" plain @click="editDataset(scope.row)">编辑</el-button>
            <el-button size="small" :type="scope.row.status === 1 ? 'warning' : 'success'" plain @click="toggle(scope.row)">
              {{ scope.row.status === 1 ? '停用' : '启用' }}
            </el-button>
            <el-button size="small" type="primary" plain @click="showContent(scope.row)">查看内容</el-button>
            <el-button size="small" type="danger" plain @click="remove(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="editVisible" title="编辑知识库" width="520px">
      <el-form label-width="80px">
        <el-form-item label="名称"><el-input v-model="editForm.name" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="editForm.description" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible=false">取消</el-button>
        <el-button type="primary" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="createVisible" title="新增知识库" width="520px">
      <el-form label-width="80px">
        <el-form-item label="名称">
          <el-input v-model="createForm.name" placeholder="请输入名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="createForm.description" type="textarea" placeholder="请输入描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible=false">取消</el-button>
        <el-button type="primary" @click="create">创建</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="contentVisible" title="知识库内容" size="60%">
      <div v-if="contentData">
        <p><b>知识库：</b>{{ contentData.dataset.name }}</p>
        <p><b>描述：</b>{{ contentData.dataset.description }}</p>
        <el-divider />
        <div v-for="item in contentData.documents" :key="item.document.id" style="margin-bottom: 16px;">
          <el-card>
            <template #header>
              <b>{{ item.document.filename }}</b>
              <span style="margin-left:8px;color:#888;">状态: {{ item.document.status }}</span>
            </template>
            <el-empty v-if="!item.chunks || item.chunks.length===0" description="暂无解析片段" />
            <el-collapse v-else>
              <el-collapse-item v-for="c in item.chunks" :key="c.id" :title="`片段 #${c.id}`">
                <pre style="white-space: pre-wrap;word-break: break-word;">{{ c.content }}</pre>
              </el-collapse-item>
            </el-collapse>
          </el-card>
        </div>
      </div>
    </el-drawer>
  </el-main>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import http from '../api/http'

const rows = ref([])
const editVisible = ref(false)
const createVisible = ref(false)
const contentVisible = ref(false)
const contentData = ref(null)
const editForm = reactive({ id: null, name: '', description: '' })
const createForm = reactive({ name: '', description: '' })

const load = async () => { rows.value = (await http.get('/datasets')).data.data.records || [] }

const openCreateDialog = () => {
  createForm.name = ''
  createForm.description = ''
  createVisible.value = true
}

const create = async () => {
  if (!createForm.name.trim()) {
    ElMessage.warning('请输入名称')
    return
  }
  await http.post('/datasets', {
    name: createForm.name.trim(),
    description: createForm.description.trim(),
    createdBy: 1,
    status: 0
  })
  ElMessage.success('创建成功')
  createVisible.value = false
  await load()
}


const editDataset = (row) => {
  editForm.id = row.id
  editForm.name = row.name
  editForm.description = row.description
  editVisible.value = true
}

const saveEdit = async () => {
  await http.put(`/datasets/${editForm.id}`, { name: editForm.name, description: editForm.description })
  ElMessage.success('保存成功')
  editVisible.value = false
  await load()
}

const toggle = async (row) => {
  const status = row.status === 1 ? 0 : 1
  const res = await http.post(`/datasets/${row.id}/toggle`, null, { params: { status } })
  if (res.data?.code !== 0) {
    ElMessage.error(res.data?.message || '操作失败')
    return
  }
  ElMessage.success('状态已更新')
  await load()
}

const remove = async (id) => {
  await http.delete(`/datasets/${id}`)
  ElMessage.success('删除成功')
  await load()
}

const showContent = async (row) => {
  const res = await http.get(`/datasets/${row.id}/content`)
  contentData.value = res.data.data
  contentVisible.value = true
}

onMounted(load)
</script>
<style scoped>
.dataset-page {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 60px);
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.page-title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #303133;
}
.table-card {
  border-radius: 8px;
  border: none;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05) !important;
}
.ds-name {
  font-weight: 600;
  color: #409EFF;
}
/* 列表与表格的基础优化 */
:deep(.el-table th.el-table__cell) {
  background-color: #f8f9fa !important;
  color: #606266;
  font-weight: 700;
}
</style>
