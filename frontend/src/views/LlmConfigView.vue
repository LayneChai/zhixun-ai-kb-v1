<template>
  <el-main class="page">
    <el-card class="table-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="page-title">大模型配置中心</span>
          <el-button type="primary" @click="openCreate">
            <el-icon><Plus /></el-icon> 新增配置
          </el-button>
        </div>
      </template>

      <el-table :data="rows" border stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" align="center"/>
        <el-table-column prop="provider" label="供应商" width="150">
          <template #default="{ row }">
            <span class="provider-name">{{ row.provider }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="displayName" label="显示名称" width="180"/>
        <el-table-column prop="baseUrl" label="BaseURL" show-overflow-tooltip/>
        <el-table-column prop="model" label="模型" width="180">
          <template #default="{ row }">
            <el-tag type="info" effect="light">{{ row.model }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="enabled" label="启用" width="80" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.enabled === 1 ? 'success' : 'danger'" effect="dark">
              {{ scope.row.enabled === 1 ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="isDefault" label="默认" width="80" align="center">
          <template #default="scope">
            <el-tag v-if="scope.row.isDefault === 1" type="warning" effect="dark">是</el-tag>
            <span v-else style="color:#999">否</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="scope">
            <el-button size="small" plain @click="openEdit(scope.row)">编辑</el-button>
            <el-button size="small" type="success" plain @click="setDefault(scope.row)" :disabled="scope.row.isDefault === 1">设为默认</el-button>
            <el-button size="small" type="danger" plain @click="remove(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="visible" :title="form.id ? '编辑模型配置' : '新增模型配置'" width="680px">
      <el-form label-width="90px">
        <el-form-item label="供应商"><el-input v-model="form.provider" placeholder="如 OpenAI / Qwen / DeepSeek / Gemini"/></el-form-item>
        <el-form-item label="显示名称"><el-input v-model="form.displayName"/></el-form-item>
        <el-form-item label="BaseURL"><el-input v-model="form.baseUrl" placeholder="如 https://api.openai.com/v1"/></el-form-item>
        <el-form-item label="API Key"><el-input v-model="form.apiKey" show-password/></el-form-item>
        <el-form-item label="模型"><el-input v-model="form.model" placeholder="如 gpt-5.3-codex / deepseek-chat / qwen-max"/></el-form-item>
        <el-form-item label="启用"><el-switch v-model="enabledBool"/></el-form-item>
        <el-form-item label="扩展JSON"><el-input v-model="form.extraJson" type="textarea" :rows="4"/></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible=false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </el-main>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import http from '../api/http'

const rows = ref([])
const visible = ref(false)
const form = reactive({ id: null, provider: '', displayName: '', baseUrl: '', apiKey: '', model: '', enabled: 1, isDefault: 0, extraJson: '{}' })

const enabledBool = computed({
  get: () => form.enabled === 1,
  set: (v) => { form.enabled = v ? 1 : 0 }
})

const load = async () => {
  rows.value = (await http.get('/llm-config')).data.data || []
}

const reset = () => {
  Object.assign(form, { id: null, provider: '', displayName: '', baseUrl: '', apiKey: '', model: '', enabled: 1, isDefault: 0, extraJson: '{}' })
}

const openCreate = () => { reset(); visible.value = true }
const openEdit = (r) => { Object.assign(form, r); visible.value = true }

const save = async () => {
  if (form.id) await http.put(`/llm-config/${form.id}`, form)
  else await http.post('/llm-config', form)
  ElMessage.success('保存成功')
  visible.value = false
  await load()
}

const setDefault = async (row) => {
  await http.post(`/llm-config/${row.id}/default`)
  ElMessage.success('已设为默认模型')
  await load()
}

const remove = async (id) => {
  await http.delete(`/llm-config/${id}`)
  ElMessage.success('删除成功')
  await load()
}

onMounted(load)
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

.provider-name {
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
