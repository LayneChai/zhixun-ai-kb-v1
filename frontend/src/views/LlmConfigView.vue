<template>
  <el-main>
    <h3>大模型配置中心</h3>
    <el-button type="primary" @click="openCreate">新增配置</el-button>
    <el-table :data="rows" style="margin-top:12px">
      <el-table-column prop="id" label="ID" width="70"/>
      <el-table-column prop="provider" label="供应商" width="150"/>
      <el-table-column prop="displayName" label="显示名称" width="180"/>
      <el-table-column prop="baseUrl" label="BaseURL"/>
      <el-table-column prop="model" label="模型" width="180"/>
      <el-table-column prop="enabled" label="启用" width="80">
        <template #default="scope">{{ scope.row.enabled === 1 ? '是' : '否' }}</template>
      </el-table-column>
      <el-table-column prop="isDefault" label="默认" width="80">
        <template #default="scope">{{ scope.row.isDefault === 1 ? '是' : '否' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="320">
        <template #default="scope">
          <el-button size="small" @click="openEdit(scope.row)">编辑</el-button>
          <el-button size="small" type="success" @click="setDefault(scope.row)">设为默认</el-button>
          <el-button size="small" type="danger" @click="remove(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

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
