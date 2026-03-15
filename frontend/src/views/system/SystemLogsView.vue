<template>
  <el-main class="page">
    <el-card class="table-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="page-title">操作日志</span>
        </div>
      </template>
      <div class="toolbar">
        <el-input v-model="keyword" clearable placeholder="搜索模块/行为/IP" style="width: 280px">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <div class="toolbar-actions">
          <el-button plain @click="loadLogs"><el-icon><Refresh /></el-icon> 刷新</el-button>
        </div>
      </div>
      <el-table :data="filteredLogs" border stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="module" label="模块" min-width="120" />
        <el-table-column prop="action" label="行为" min-width="220" />
        <el-table-column prop="operatorId" label="操作人ID" width="120" />
        <el-table-column prop="ip" label="IP" width="140" />
        <el-table-column prop="createdAt" label="时间" min-width="180" />
      </el-table>
    </el-card>
  </el-main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import http from '../../api/http'

const logs = ref([])
const keyword = ref('')
const loading = ref(false)

const filteredLogs = computed(() => {
  const k = keyword.value.trim().toLowerCase()
  if (!k) return logs.value
  return logs.value.filter(l =>
    String(l.module || '').toLowerCase().includes(k) ||
    String(l.action || '').toLowerCase().includes(k) ||
    String(l.ip || '').toLowerCase().includes(k)
  )
})

const loadLogs = async () => {
  loading.value = true
  try {
    const res = await http.get('/system/logs')
    logs.value = res.data.data || []
  } catch (e) {
    ElMessage.error(decodeURIComponent('%E5%8A%A0%E8%BD%BD%E6%97%A5%E5%BF%97%E5%A4%B1%E8%B4%A5') + ': ' + (e.response?.data?.message || e.message))
    logs.value = []
  } finally {
    loading.value = false
  }
}

onMounted(loadLogs)
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
