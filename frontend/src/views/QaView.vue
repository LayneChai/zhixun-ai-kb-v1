<template>
  <el-container style="height:100vh;">
    <el-aside v-if="showSidebar" width="260px" class="qa-sidebar">
      <div class="qa-sidebar-top">
        <el-button type="primary" style="width:100%" @click="newSession" :disabled="!selectedDatasetId">
          新建会话
        </el-button>
      </div>
      <div class="session-list">
        <div
          v-for="session in sessions"
          :key="session.id"
          class="session-item"
          :class="{ active: current?.id === session.id }"
          @click="pick(session)"
        >
          <span class="session-title">{{ session.title || (`会话 #${session.id}`) }}</span>
          <div class="session-actions">
            <el-button link size="small" @click.stop="renameSession(session)">重命名</el-button>
            <el-button link type="danger" size="small" @click.stop="deleteSession(session)">删除</el-button>
          </div>
        </div>
      </div>
      <div class="qa-sidebar-bottom" :style="{ height: bottomBarHeight > 0 ? bottomBarHeight + 'px' : 'auto' }">
        <el-dropdown trigger="click" @command="handleUserCommand" placement="top-start" style="width: 100%;">
          <div class="avatar-wrapper sidebar-avatar">
            <el-avatar :size="36" class="user-avatar">{{ username.charAt(0).toUpperCase() }}</el-avatar>
            <span class="username-display">{{ username }}</span>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="info">修改信息</el-dropdown-item>
              <el-dropdown-item command="password">修改密码</el-dropdown-item>
              <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-aside>

    <el-main class="qa-main">
      <div class="msg-list" ref="msgListRef">
        <div v-for="message in messages" :key="message.id" class="msg-block">
          <div class="msg-row user-row">
            <div class="msg-bubble user-bubble">{{ message.question }}</div>
          </div>
          <div class="msg-row ai-row">
            <div class="msg-avatar">AI</div>
            <div class="msg-content">
              <div class="msg-bubble ai-bubble" v-html="renderMarkdown(message.answer || '...')"></div>
              <div v-if="parseRefs(message.referencesJson).length" class="refs-area">
                <div class="refs-title">知识来源（BM25 检索）</div>
                <el-collapse>
                  <el-collapse-item
                    v-for="(refItem, idx) in parseRefs(message.referencesJson)"
                    :key="idx"
                    :title="`${refItem.docName || '未知文档'}  相关度：${((refItem.score || 0) * 100).toFixed(1)}%`"
                  >
                    <pre class="ref-snippet">{{ refItem.snippet }}</pre>
                  </el-collapse-item>
                </el-collapse>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="input-bar-container" ref="inputBarRef">
        <div class="input-bar">
          <el-input
            v-model="question"
            type="textarea"
            :autosize="{ minRows: 1, maxRows: 6 }"
            resize="none"
            placeholder="输入问题，按 Enter 发送，Shift + Enter 换行"
            @keydown.enter.exact.prevent="ask"
            :disabled="!current || pending"
            class="chat-input"
          />
          <div class="input-actions">
            <el-checkbox v-model="useStream" style="margin:0 10px;white-space:nowrap">流式</el-checkbox>
            <el-button type="primary" @click="ask" :disabled="!current || !question.trim() || pending">
              发送
            </el-button>
          </div>
        </div>
      </div>
    </el-main>
  </el-container>

  <!-- 修改信息弹窗 -->
  <el-dialog v-model="infoDialogVisible" title="修改个人信息" width="400px">
    <el-form label-width="80px">
      <el-form-item label="用户名">
        <el-input v-model="infoForm.username" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="infoDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="saveInfo">保存</el-button>
    </template>
  </el-dialog>

  <!-- 修改密码弹窗 -->
  <el-dialog v-model="pwdDialogVisible" title="修改密码" width="400px">
    <el-form label-width="90px">
      <el-form-item label="原密码">
        <el-input v-model="pwdForm.oldPassword" type="password" show-password />
      </el-form-item>
      <el-form-item label="新密码">
        <el-input v-model="pwdForm.newPassword" type="password" show-password />
      </el-form-item>
      <el-form-item label="确认新密码">
        <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="pwdDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="savePassword">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, nextTick, onMounted, onBeforeUnmount, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import MarkdownIt from 'markdown-it'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../api/http'

const router = useRouter()
const sessions = ref([])
const messages = ref([])
const current = ref(null)
const question = ref('')
const useStream = ref(true)
const pending = ref(false)
const selectedDatasetId = ref(null)
const msgListRef = ref(null)
const username = ref(localStorage.getItem('username') || '')
const role = ref(localStorage.getItem('role') || 'user')
const inputBarRef = ref(null)
const bottomBarHeight = ref(0)
let resizeObserver = null

// 对于问答页面，无论任何角色，都默认显示左侧对话管理边栏
const showSidebar = computed(() => true)

const infoDialogVisible = ref(false)
const pwdDialogVisible = ref(false)
const infoForm = reactive({ username: '' })
const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const handleUserCommand = (cmd) => {
  if (cmd === 'info') {
    infoForm.username = username.value
    infoDialogVisible.value = true
  } else if (cmd === 'password') {
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
    pwdDialogVisible.value = true
  } else if (cmd === 'logout') {
    ElMessageBox.confirm('确认退出系统吗？', '提示', { type: 'warning' }).then(() => {
      localStorage.removeItem('token')
      localStorage.removeItem('userId')
      localStorage.removeItem('username')
      localStorage.removeItem('perms')
      localStorage.removeItem('perms_loaded')
      router.push('/login')
    }).catch(() => {})
  }
}

const saveInfo = async () => {
  if (!infoForm.username.trim()) {
    return ElMessage.warning('用户名不能为空')
  }
  try {
    const userId = localStorage.getItem('userId')
    const res = await http.put(`/auth/profile`, { username: infoForm.username })
    // If backend doesn't have this, try `system/users/${userId}` as fallback, handled dynamically in a full scenario
    if (res.data.code === 0) {
      ElMessage.success('修改成功')
      localStorage.setItem('username', infoForm.username)
      username.value = infoForm.username
      infoDialogVisible.value = false
    } else {
      ElMessage.error(res.data.message || '修改失败')
    }
  } catch (e) {
    ElMessage.error('该接口未实现或调用失败')
  }
}

const savePassword = async () => {
  if (!pwdForm.oldPassword || !pwdForm.newPassword) {
    return ElMessage.warning('密码不能为空')
  }
  if (pwdForm.newPassword !== pwdForm.confirmPassword) {
    return ElMessage.warning('两次输入的新密码不一致')
  }
  try {
    const res = await http.put(`/auth/password`, {
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword
    })
    if (res.data.code === 0) {
      ElMessage.success('密码修改成功，请重新登录')
      localStorage.removeItem('token')
      router.push('/login')
    } else {
      ElMessage.error(res.data.message || '修改失败')
    }
  } catch (e) {
    ElMessage.error('该接口未实现或调用失败')
  }
}

const md = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true
})

const renderMarkdown = (text) => md.render(text || '')

const parseRefs = (refs) => {
  if (!refs) return []
  if (Array.isArray(refs)) return refs
  try {
    return JSON.parse(refs) || []
  } catch {
    return []
  }
}

const scrollBottom = () => nextTick(() => {
  if (msgListRef.value) {
    msgListRef.value.scrollTop = msgListRef.value.scrollHeight
  }
})

const defaultDataset = ref(null)

const loadDefaultDataset = async () => {
  try {
    const res = await http.get('/datasets/getDefault')
    defaultDataset.value = res.data.data || null
    selectedDatasetId.value = defaultDataset.value?.id || null
    current.value = null
    messages.value = []
    await loadSessions()
  } catch {
    ElMessage.error(decodeURIComponent('%E5%8A%A0%E8%BD%BD%E9%BB%98%E8%AE%A4%E7%9F%A5%E8%AF%86%E5%BA%93%E5%A4%B1%E8%B4%A5'))
  }
}

const loadSessions = async () => {
  if (!selectedDatasetId.value) {
    sessions.value = []
    return
  }

  const res = await http.get('/qa/sessions', {
    params: { datasetId: selectedDatasetId.value }
  })
  sessions.value = res.data.data || []
  if (!current.value && sessions.value.length) {
    await pick(sessions.value[0])
    return
  }
  if (!sessions.value.length && !showSidebar.value) {
    await newSession(true)
  }
}

const newSession = async (silent = false) => {
  if (!selectedDatasetId.value) {
    if (!silent) {
      ElMessage.warning(decodeURIComponent('%E8%AF%B7%E5%85%88%E7%94%B1%E7%AE%A1%E7%90%86%E5%91%98%E5%90%AF%E7%94%A8%E7%9F%A5%E8%AF%86%E5%BA%93'))
    }
    return
  }

  const res = await http.post('/qa/session', {
    datasetId: selectedDatasetId.value,
    title: decodeURIComponent('%E6%96%B0%E4%BC%9A%E8%AF%9D')
  })
  sessions.value.unshift(res.data.data)
  current.value = res.data.data
  messages.value = []
}

const pick = async (session) => {
  current.value = session
  const res = await http.get('/qa/history', { params: { sessionId: session.id } })
  messages.value = res.data.data || []
  scrollBottom()
}

const renameSession = async (session) => {
  const { value: title } = await ElMessageBox.prompt('输入新的会话名称', '重命名', {
    inputValue: session.title || '',
    inputPattern: /\S+/,
    inputErrorMessage: '名称不能为空'
  })
  await http.post('/qa/session/rename', { sessionId: session.id, title })
  await loadSessions()
  ElMessage.success('已重命名')
}

const deleteSession = async (session) => {
  try {
    await ElMessageBox.confirm(`确认删除会话 "${session.title || '会话 #' + session.id}" 吗？该操作不可恢复。`, '警告', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    // 确保调用路径正确
    const res = await http.post(`/qa/session/${session.id}/delete`)
    if (res.data.code === 0) {
      ElMessage.success('删除成功')
      if (current.value?.id === session.id) {
        current.value = null
        messages.value = []
      }
      await loadSessions()
    } else {
      ElMessage.error(res.data.message || '删除失败')
    }
  } catch (e) {
    // cancelled or error
  }
}

const handleSseEvent = (rawEvent, tempMessage) => {
  const lines = rawEvent.split('\n')
  let eventName = 'message'
  const dataLines = []

  for (const line of lines) {
    if (line.startsWith('event:')) {
      eventName = line.slice(6).trim()
    } else if (line.startsWith('data:')) {
      dataLines.push(line.slice(5).trimStart())
    }
  }

  const data = dataLines.join('\n')
  if (eventName === 'chunk') {
    tempMessage.answer += data
    scrollBottom()
    return false
  }
  if (eventName === 'refs') {
    tempMessage.referencesJson = data || '[]'
    return false
  }
  if (eventName === 'error') {
    throw new Error(data || 'stream error')
  }
  return eventName === 'done'
}

const askWithStream = async (q, tempMessage) => {
  const token = localStorage.getItem('token') || ''
  const response = await fetch(`/api/qa/ask/stream?sessionId=${current.value.id}&question=${encodeURIComponent(q)}`, {
    headers: {
      Authorization: `Bearer ${token}`
    }
  })

  if (!response.ok || !response.body) {
    throw new Error(`stream request failed: ${response.status}`)
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  let doneReceived = false

  while (true) {
    const { value, done } = await reader.read()
    if (done) break

    buffer += decoder.decode(value, { stream: true })
    const events = buffer.split('\n\n')
    buffer = events.pop() || ''

    for (const rawEvent of events) {
      if (!rawEvent.trim()) continue
      if (handleSseEvent(rawEvent, tempMessage)) {
        doneReceived = true
      }
    }
  }

  if (buffer.trim() && handleSseEvent(buffer, tempMessage)) {
    doneReceived = true
  }

  if (!doneReceived) {
    throw new Error('stream ended unexpectedly')
  }
}

const ask = async () => {
  if (!current.value || pending.value) return

  const q = question.value.trim()
  if (!q) return

  question.value = ''
  pending.value = true

  try {
    if (!useStream.value) {
      await http.post('/qa/ask', { sessionId: current.value.id, question: q })
      await pick(current.value)
      return
    }

    const tempMessage = reactive({
      id: `tmp-${Date.now()}`,
      question: q,
      answer: '',
      referencesJson: '[]'
    })
    messages.value.push(tempMessage)
    scrollBottom()

    await askWithStream(q, tempMessage)
    await pick(current.value)
  } catch {
    ElMessage.warning('流式响应中断，已刷新历史记录')
    await pick(current.value)
  } finally {
    pending.value = false
  }
}

onMounted(() => {
  loadDefaultDataset()
  if (inputBarRef.value) {
    resizeObserver = new ResizeObserver((entries) => {
      for (const entry of entries) {
        bottomBarHeight.value = entry.target.offsetHeight
      }
    })
    resizeObserver.observe(inputBarRef.value)
  }
})

onBeforeUnmount(() => {
  if (resizeObserver) {
    resizeObserver.disconnect()
  }
})
</script>

<style scoped>
.qa-sidebar {
  background: #fff;
  border-right: 1px solid #eee;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.qa-sidebar-top {
  padding: 14px 12px 10px;
  border-bottom: 1px solid #f0f0f0;
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}

.session-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 10px 14px;
  cursor: pointer;
  border-radius: 6px;
  margin: 2px 6px;
  transition: background 0.15s;
}

.session-item:hover {
  background: #f5f7fa;
}

.session-item.active {
  background: #ecf5ff;
  color: #409eff;
}

.session-actions {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}

.session-title {
  font-size: 13px;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.qa-main {
  display: flex;
  flex-direction: column;
  padding: 0;
  background: #f7f8fa;
}

.msg-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.msg-row {
  display: flex;
}

.user-row {
  justify-content: flex-end;
}

.ai-row {
  align-items: flex-start;
  gap: 10px;
}

.msg-avatar {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  background: #1a1c2e;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.msg-content {
  flex: 1;
  min-width: 0;
}

.msg-bubble {
  display: inline-block;
  max-width: 680px;
  padding: 12px 16px;
  border-radius: 12px;
  line-height: 1.6;
  font-size: 14px;
  word-break: break-word;
}

.user-bubble {
  background: #409eff;
  color: #fff;
  border-bottom-right-radius: 4px;
  white-space: pre-wrap;
}

.ai-bubble {
  background: #fff;
  color: #333;
  border: 1px solid #eee;
  border-bottom-left-radius: 4px;
}

:deep(.ai-bubble p) {
  margin: 0 0 10px 0;
}
:deep(.ai-bubble p:last-child) {
  margin-bottom: 0;
}
:deep(.ai-bubble pre) {
  background: #f5f7fa;
  padding: 10px;
  border-radius: 6px;
  overflow-x: auto;
}

.refs-area {
  margin-top: 10px;
  max-width: 680px;
  background: #fff;
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 12px 16px;
}

.refs-title {
  font-size: 13px;
  color: #666;
  margin-bottom: 8px;
  font-weight: 700;
}

.ref-snippet {
  font-size: 12px;
  color: #555;
  white-space: pre-wrap;
  word-break: break-word;
  margin: 0;
  line-height: 1.6;
  background: #f9fafc;
  padding: 10px;
  border-radius: 6px;
}

.input-bar-container {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  background: #fff;
  border-top: 1px solid #eee;
  box-sizing: border-box;
  min-height: 64px;
}

.input-bar {
  display: flex;
  align-items: flex-end;
  flex: 1;
  width: 100%;
  gap: 12px;
}

.chat-input {
  flex: 1;
}

:deep(.chat-input .el-textarea__inner) {
  font-family: inherit;
  font-size: 14px;
  padding: 6px 12px;
  line-height: 1.5;
}

.input-actions {
  display: flex;
  align-items: center;
  flex-shrink: 0;
  padding-bottom: 2px;
}

.qa-sidebar-bottom {
  padding: 12px 16px;
  border-top: 1px solid #eee;
  display: flex;
  align-items: center;
  background: #fff;
  flex-shrink: 0;
  box-sizing: border-box;
  min-height: 64px;
}

.sidebar-avatar {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 12px;
  padding: 4px 8px;
  border-radius: 8px;
  width: 100%;
  box-sizing: border-box;
}

.sidebar-avatar:hover {
  background: #f5f7fa;
  border-color: transparent;
}

.username-display {
  font-size: 14px;
  color: #333;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.avatar-wrapper {
  cursor: pointer;
  border: 2px solid transparent;
  transition: all 0.2s;
}

.user-avatar {
  background: #409eff;
  color: #fff;
  font-weight: 600;
  font-size: 16px;
  flex-shrink: 0;
}
</style>
