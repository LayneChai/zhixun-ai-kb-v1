<template>
  <div class="doc-page">
    <div class="page-header">
      <h2 class="page-title">文档管理</h2>
      <div class="header-actions">
        <!-- 知识库选择器 -->
        <el-select v-model="selectedDatasetId" placeholder="选择知识库" style="width:220px;margin-right:16px"
          @change="load" clearable size="large">
          <el-option v-for="ds in datasets" :key="ds.id" :label="ds.name" :value="ds.id" />
        </el-select>
        <el-upload :http-request="upload" :show-file-list="false" :disabled="!selectedDatasetId">
          <el-button type="primary" size="large" :disabled="!selectedDatasetId">
            <el-icon><Upload /></el-icon> 上传文档
          </el-button>
        </el-upload>
      </div>
    </div>

    <div v-if="!datasets.length" class="empty-prompt">
      <el-alert
        title="暂无知识库"
        type="warning"
        description="请先前往“知识库管理”页面创建一个知识库，以便上传和管理文档。"
        show-icon
        :closable="false"
      >
        <template #default>
          <div style="margin-top: 10px">
            <el-button type="primary" size="small" @click="$router.push('/dataset')">前往创建</el-button>
          </div>
        </template>
      </el-alert>
    </div>
    <el-empty v-else-if="!selectedDatasetId" description="请选择知识库以查看文档" />
    <el-card v-else class="table-card" shadow="never">
      <el-table :data="rows" style="width: 100%" border stripe>
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="filename" label="文件名">
          <template #default="scope">
            <span class="doc-name">{{ scope.row.filename }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="fileType" label="类型" width="120" align="center" />
        <el-table-column prop="status" label="状态" width="120" align="center">
          <template #default="scope">
            <el-tag :type="statusType(scope.row.status)" size="small" effect="dark">
              {{ statusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="上传时间" width="180" align="center" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="scope">
            <el-button size="small" type="primary" plain @click="parse(scope.row.id)">解析切片</el-button>
            <el-button size="small" type="success" plain @click="reindex(scope.row.id)">重建索引</el-button>
            <el-button size="small" type="danger"  plain @click="remove(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload } from '@element-plus/icons-vue'
import http from '../api/http'

const rows              = ref([])
const datasets          = ref([])
const selectedDatasetId = ref(null)

const loadDatasets = async () => {
  try {
    const res = await http.get('/datasets', { params: { page: 1, size: 100 } })
    datasets.value = res.data.data?.records || res.data.data || []
    if (datasets.value.length) {
      if (!selectedDatasetId.value) {
        selectedDatasetId.value = datasets.value[0].id
        await load()
      }
    } else {
      ElMessage.warning('检测到暂无知识库，请先前往“知识库管理”创建')
    }
  } catch (_) {
    ElMessage.error('加载知识库列表失败')
  }
}

const load = async () => {
  if (!selectedDatasetId.value) { rows.value = []; return }
  const res = await http.get('/documents', { params: { datasetId: selectedDatasetId.value } })
  rows.value = res.data.data || []
}

const getSelectedDatasetName = () => {
  const ds = datasets.value.find(d => d.id === selectedDatasetId.value)
  return ds ? ds.name : '未知知识库'
}

const upload = async (params) => {
  const fd = new FormData()
  fd.append('datasetId', selectedDatasetId.value)
  fd.append('file', params.file)
  try {
    const res = await http.post('/documents/upload', fd)
    if (res.data.code !== 0) {
      ElMessage.error(res.data.message || '上传失败')
      return
    }
    ElMessage.success(`文件已上传并归类至知识库：${getSelectedDatasetName()}`)
    await load()
  } catch (e) {
    ElMessage.error('上传失败')
  }
}

const parse = async (id) => {
  try {
    const res = await http.post(`/documents/${id}/parse`)
    if (res.data.code !== 0) {
      ElMessage.error(res.data.message || '解析失败')
      return
    }
    ElMessage.success(`文档已在知识库 [${getSelectedDatasetName()}] 中解析完成`)
    await load()
  } catch (e) {
    ElMessage.error('解析失败')
  }
}

const reindex = async (id) => {
  try {
    const res = await http.post(`/documents/${id}/reindex`)
    if (res.data.code !== 0) {
      ElMessage.error(res.data.message || '重建索引失败')
      return
    }
    ElMessage.success('重建索引完成')
    await load()
  } catch (e) {
    ElMessage.error('重建索引失败')
  }
}

const remove = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该文档吗？', '提示', { type: 'warning' })
    const res = await http.delete(`/documents/${id}`)
    if (res.data.code !== 0) {
      ElMessage.error(res.data.message || '删除失败')
      return
    }
    ElMessage.success('删除成功')
    await load()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

const statusText = (s) => ({ 0: '已上传', 1: '已解析', 2: '已重建索引' }[s] || '未知')
const statusType = (s) => ({ 0: 'info',   1: 'success', 2: 'warning' }[s] || '')

onMounted(loadDatasets)
</script>

<style scoped>
.doc-page { 
  width: 100%; 
  max-width: 1600px; 
  margin: 0 auto; 
  padding: 20px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 60px);
  box-sizing: border-box;
}
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}
.page-title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #303133;
}
.header-actions { display: flex; align-items: center; }

.table-card {
  border-radius: 8px;
  border: none;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05) !important;
}

.doc-name {
  font-weight: 500;
  color: #303133;
}

.empty-prompt { padding: 40px; text-align: center; }

/* 列表与表格的基础优化 */
:deep(.el-table th.el-table__cell) {
  background-color: #f8f9fa !important;
  color: #606266;
  font-weight: 700;
}
</style>
