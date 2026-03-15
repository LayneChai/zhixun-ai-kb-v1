<template>
  <div class="dashboard">
    <div class="dash-header">
      <h2 class="dash-title">控制台</h2>
      <p class="dash-sub">欢迎回来，{{ username }} 👋</p>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stat-row">
      <el-col :xs="24" :sm="8">
        <el-card class="stat-card stat-blue" shadow="hover">
          <div class="stat-inner">
            <div class="stat-icon">📚</div>
            <div class="stat-info">
              <div class="stat-num">{{ stats.datasetCount }}</div>
              <div class="stat-label">知识库数量</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card class="stat-card stat-green" shadow="hover">
          <div class="stat-inner">
            <div class="stat-icon">📄</div>
            <div class="stat-info">
              <div class="stat-num">{{ stats.docCount }}</div>
              <div class="stat-label">文档总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card class="stat-card stat-purple" shadow="hover">
          <div class="stat-inner">
            <div class="stat-icon">💬</div>
            <div class="stat-info">
              <div class="stat-num">{{ stats.qaCount }}</div>
              <div class="stat-label">问答次数</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快捷入口 -->
    <el-card class="quick-card" shadow="never">
      <template #header><b>快捷入口</b></template>
      <div class="quick-btns">
        <el-button type="primary"  @click="$router.push('/dataset')">📚 知识库管理</el-button>
        <el-button type="success"  @click="$router.push('/document')">📄 文档管理</el-button>
        <el-button type="warning"  @click="$router.push('/qa')">💬 智能问答</el-button>
        <el-button type="info"     @click="$router.push('/llm-config')">⚙️ 大模型配置</el-button>
      </div>
    </el-card>

    <!-- BM25 算法说明 -->
    <el-card class="algo-card" shadow="never">
      <template #header><b>🔬 检索算法说明</b></template>
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="检索引擎">BM25（Okapi BM25）</el-descriptions-item>
        <el-descriptions-item label="中文分词">HanLP Portable（hanlp-portable-1.8.4）</el-descriptions-item>
        <el-descriptions-item label="算法参数">k₁=1.5（词频饱和）丨b=0.75（长度归一化）</el-descriptions-item>
        <el-descriptions-item label="索引策略">内存倒排索引，按知识库懒加载，文档解析后自动失效</el-descriptions-item>
        <el-descriptions-item label="返回结果">Top-5 相关片段，附归一化相似度分数</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, computed } from 'vue'
import http from '../api/http'

const stats    = reactive({ datasetCount: 0, docCount: 0, qaCount: 0 })
const username = computed(() => localStorage.getItem('username') || 'admin')

onMounted(async () => {
  try {
    const res = await http.get('/system/stats')
    Object.assign(stats, res.data.data || {})
  } catch (_) {}
})
</script>

<style scoped>
.dashboard { width: 100%; max-width: 1600px; margin: 0 auto; }
.dash-header { margin-bottom: 24px; }
.dash-title { font-size: 24px; font-weight: 700; color: #1a1c2e; }
.dash-sub   { color: #888; margin-top: 4px; }
.stat-row   { margin-bottom: 20px; }
.stat-card  { border-radius: 12px; cursor: default; }
.stat-inner { display: flex; align-items: center; gap: 16px; padding: 8px 0; }
.stat-icon  { font-size: 40px; }
.stat-num   { font-size: 32px; font-weight: 800; line-height: 1; }
.stat-label { font-size: 13px; color: #888; margin-top: 4px; }
.stat-blue  { border-left: 4px solid #409eff; }
.stat-green { border-left: 4px solid #67c23a; }
.stat-purple{ border-left: 4px solid #9b59b6; }
.quick-card { margin-bottom: 20px; border-radius: 12px; }
.quick-btns { display: flex; gap: 12px; flex-wrap: wrap; }
.algo-card  { border-radius: 12px; }
</style>