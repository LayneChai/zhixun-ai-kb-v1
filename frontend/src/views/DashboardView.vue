<template>
  <div class="dashboard">
    <div class="dash-header">
      <div class="header-left">
        <h2 class="dash-title">工作台</h2>
        <p class="dash-sub">欢迎回来，<span class="highlight-user">{{ username }}</span> 👋，开始今天的工作吧！</p>
      </div>
      <div class="header-right">
        <!-- 可以在这里放一些时间或者天气组件 -->
      </div>
    </div>

    <!-- 顶栏统计卡片 -->
    <el-row :gutter="20" class="stat-row">
      <el-col :xs="24" :sm="8">
        <el-card class="stat-card gradient-blue" shadow="hover">
          <div class="stat-inner">
            <div class="stat-content">
              <div class="stat-label">知识库数量</div>
              <div class="stat-num">{{ stats.datasetCount }}</div>
            </div>
            <div class="stat-icon-wrapper"><el-icon><Collection /></el-icon></div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card class="stat-card gradient-green" shadow="hover">
          <div class="stat-inner">
            <div class="stat-content">
              <div class="stat-label">文档总数</div>
              <div class="stat-num">{{ stats.docCount }}</div>
            </div>
            <div class="stat-icon-wrapper"><el-icon><Document /></el-icon></div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card class="stat-card gradient-purple" shadow="hover">
          <div class="stat-inner">
            <div class="stat-content">
              <div class="stat-label">问答交互总计</div>
              <div class="stat-num">{{ stats.qaCount }}</div>
            </div>
            <div class="stat-icon-wrapper"><el-icon><ChatDotRound /></el-icon></div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 中间图表区域 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :xs="24" :lg="16">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="card-title">系统问答活跃度 (近7日)</span>
              <el-tag type="success" size="small" effect="light">实时</el-tag>
            </div>
          </template>
          <div class="chart-container">
            <v-chart class="chart" :option="lineChartOption" autoresize />
          </div>
        </el-card>
      </el-col>
      
      <el-col :xs="24" :lg="8">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="card-title">系统存储资产占比</span>
            </div>
          </template>
          <div class="chart-container">
            <v-chart class="chart" :option="pieChartOption" autoresize />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快捷功能与系统说明 -->
    <el-row :gutter="20" class="bottom-row">
      <el-col :xs="24" :sm="12">
        <el-card class="action-card" shadow="hover">
          <template #header><div class="card-header"><span class="card-title">常用操作</span></div></template>
          <div class="quick-grid">
            <div class="quick-item" @click="$router.push('/dataset')">
              <div class="q-icon bg-light-blue"><el-icon><Collection /></el-icon></div>
              <div class="q-text">知识库配置</div>
            </div>
            <div class="quick-item" @click="$router.push('/document')">
              <div class="q-icon bg-light-green"><el-icon><Document /></el-icon></div>
              <div class="q-text">上传文档</div>
            </div>
            <div class="quick-item" @click="$router.push('/qa')">
              <div class="q-icon bg-light-orange"><el-icon><ChatLineSquare /></el-icon></div>
              <div class="q-text">发起问答</div>
            </div>
            <div class="quick-item" @click="$router.push('/llm-config')">
              <div class="q-icon bg-light-purple"><el-icon><Setting /></el-icon></div>
              <div class="q-text">模型设置</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :xs="24" :sm="12">
        <el-card class="info-card" shadow="hover">
           <template #header><div class="card-header"><span class="card-title">检索引擎技术栈</span></div></template>
           <div class="info-content">
              <div class="info-row"><span class="info-label">核心算法</span><span class="info-val">BM25 (Okapi)</span></div>
              <div class="info-row"><span class="info-label">中文分词器</span><span class="info-val">HanLP Portable V1.8.4</span></div>
              <div class="info-row"><span class="info-label">调优参数</span><span class="info-val">k₁=1.5 (词频饱和) / b=0.75 (长度归一)</span></div>
              <div class="info-row"><span class="info-label">索引机制</span><span class="info-val">内存倒排索引，按需懒加载，实时失效缓存</span></div>
           </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onMounted, reactive, computed, ref } from 'vue'
import http from '../api/http'
import { Collection, Document, ChatDotRound, ChatLineSquare, Setting } from '@element-plus/icons-vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, GridComponent, LegendComponent } from 'echarts/components'

use([CanvasRenderer, LineChart, PieChart, TitleComponent, TooltipComponent, GridComponent, LegendComponent])

const stats = reactive({ datasetCount: 0, docCount: 0, qaCount: 0 })
const username = computed(() => localStorage.getItem('username') || 'admin')

// 图表假数据或预设配置，实际中可结合后端返回值动态改变
const lineChartOption = ref({
  tooltip: { trigger: 'axis' },
  grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
    axisLine: { lineStyle: { color: '#e0e6ed' } },
    axisLabel: { color: '#606266' }
  },
  yAxis: {
    type: 'value',
    splitLine: { lineStyle: { type: 'dashed', color: '#ebeef5' } },
    axisLabel: { color: '#606266' }
  },
  series: [
    {
      name: '问答请求量',
      type: 'line',
      smooth: true,
      lineStyle: { width: 3, color: '#409eff', shadowColor: 'rgba(64,158,255,0.4)', shadowBlur: 10, shadowOffsetY: 5 },
      itemStyle: { color: '#409eff' },
      areaStyle: {
        color: {
          type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [{ offset: 0, color: 'rgba(64,158,255,0.3)' }, { offset: 1, color: 'rgba(64,158,255,0.01)' }]
        }
      },
      data: [12, 23, 18, 45, 30, 60, 48]
    }
  ]
})

const pieChartOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
  legend: { bottom: '0%', left: 'center', icon: 'circle', itemWidth: 10, itemHeight: 10 },
  series: [
    {
      name: '资源占比',
      type: 'pie',
      radius: ['45%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 8,
        borderColor: '#fff',
        borderWidth: 2
      },
      label: { show: false, position: 'center' },
      emphasis: {
        label: { show: true, fontSize: 18, fontWeight: 'bold' }
      },
      labelLine: { show: false },
      data: [
        { value: stats.datasetCount || 1, name: '知识库集', itemStyle: { color: '#409eff' } },
        { value: stats.docCount || 3, name: '解析文档', itemStyle: { color: '#67c23a' } },
        { value: stats.qaCount || 5, name: '会话记录', itemStyle: { color: '#e6a23c' } }
      ]
    }
  ]
}))


onMounted(async () => {
  try {
    const res = await http.get('/system/stats')
    Object.assign(stats, res.data.data || {})
    
    // 如果后端能返回每天的数据这里可以直接更新 lineChartOption.value.series[0].data
  } catch (_) {}
})
</script>

<style scoped>
.dashboard { width: 100%; max-width: 1400px; margin: 0 auto; padding-bottom: 40px; }

/* 头部样式 */
.dash-header { margin-bottom: 24px; padding: 10px 0; }
.dash-title { font-size: 26px; font-weight: 800; color: #1a1c2e; letter-spacing: 0.5px; margin-bottom: 8px;}
.dash-sub { color: #606266; font-size: 15px; }
.highlight-user { color: #409eff; font-weight: bold; }

/* 卡片公用样式 */
:deep(.el-card) {
  border: none;
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04) !important;
  transition: all 0.3s ease;
}
:deep(.el-card:hover) {
  transform: translateY(-4px);
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.08) !important;
}
.card-header { display: flex; justify-content: space-between; align-items: center; }
.card-title { font-size: 16px; font-weight: 700; color: #1f2235; }

/* 顶栏统计卡片渐变设计 */
.stat-row { margin-bottom: 24px; }
.stat-card {
  color: #fff;
  position: relative;
  overflow: hidden;
}
.stat-card::after {
  content: ""; position: absolute; top: -50px; right: -50px; width: 150px; height: 150px;
  background: rgba(255,255,255,0.1); border-radius: 50%;
}
.gradient-blue { background: linear-gradient(135deg, #409eff, #53a8ff); }
.gradient-green { background: linear-gradient(135deg, #67c23a, #85ce61); }
.gradient-purple { background: linear-gradient(135deg, #9b59b6, #b373ce); }

.stat-inner { display: flex; justify-content: space-between; align-items: center; padding: 10px; }
.stat-content { display: flex; flex-direction: column; gap: 8px;}
.stat-label { font-size: 15px; opacity: 0.9; font-weight: 500;}
.stat-num { font-size: 36px; font-weight: 900; line-height: 1; text-shadow: 0 2px 4px rgba(0,0,0,0.1); }
.stat-icon-wrapper {
  width: 60px; height: 60px; border-radius: 50%; background: rgba(255,255,255,0.2);
  display: flex; align-items: center; justify-content: center; font-size: 28px;
}

/* 图表区域 */
.chart-row { margin-bottom: 24px; }
.chart-card { height: 380px; display: flex; flex-direction: column;}
:deep(.chart-card .el-card__body) { flex: 1; padding: 12px 20px; box-sizing: border-box; }
.chart-container { width: 100%; height: 100%; min-height: 280px;}
.chart { width: 100%; height: 100%; }

/* 快捷入口网格布局 */
.action-card { height: 260px; }
.quick-grid {
  display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; padding: 10px;
}
.quick-item {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  padding: 16px; border-radius: 12px; background: #f8f9fc; cursor: pointer; transition: all 0.2s;
}
.quick-item:hover { background: #fff; box-shadow: 0 6px 16px rgba(0,0,0,0.06); transform: scale(1.02); }
.q-icon {
  width: 48px; height: 48px; border-radius: 12px; display: flex; align-items: center; justify-content: center;
  font-size: 24px; color: #fff; margin-bottom: 12px;
}
.q-text { font-size: 14px; font-weight: 600; color: #303133; }
.bg-light-blue { background: #409eff; }
.bg-light-green { background: #67c23a; }
.bg-light-orange { background: #e6a23c; }
.bg-light-purple { background: #9b59b6; }

/* 算法说明列表 */
.info-card { height: 260px; }
.info-content { display: flex; flex-direction: column; gap: 14px; padding-top: 5px; }
.info-row { display: flex; justify-content: space-between; align-items: center; border-bottom: 1px dashed #ebeef5; padding-bottom: 12px;}
.info-row:last-child { border-bottom: none; }
.info-label { font-size: 14px; color: #606266; font-weight: 500; }
.info-val { font-size: 13px; color: #303133; font-weight: 600; background: #f4f4f5; padding: 4px 8px; border-radius: 6px;}

</style>