<template>
  <div class="anomaly-container">
    <el-card>
      <div class="search-bar">
        <el-form :model="filterForm" inline>
          <el-form-item label="批次号">
            <el-input v-model="filterForm.batchNo" placeholder="批次号" clearable style="width: 160px" />
          </el-form-item>
          <el-form-item label="灯组编号">
            <el-input v-model="filterForm.groupCode" placeholder="灯组编号" clearable style="width: 150px" />
          </el-form-item>
          <el-form-item label="异常类型">
            <el-select v-model="filterForm.anomalyType" placeholder="全部" clearable style="width: 120px">
              <el-option v-for="t in anomalyTypes" :key="t.value" :label="t.label" :value="t.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="filterForm.status" placeholder="全部" clearable style="width: 120px">
              <el-option v-for="s in statusList" :key="s.value" :label="s.label" :value="s.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="当前分区">
            <el-tree-select
              v-model="filterForm.zoneId"
              :data="zoneTreeData"
              :props="{ label: 'zoneName', value: 'id', children: 'children' }"
              check-strictly
              clearable
              placeholder="全部"
              style="width: 170px"
            />
          </el-form-item>
          <el-form-item>
            <el-checkbox v-model="zoneChangedOnly">仅看分区已变化</el-checkbox>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table :data="tableData" stripe>
        <el-table-column prop="groupCode" label="灯组编号" width="140" />
        <el-table-column label="所属分区" min-width="220">
          <template #default="{ row }">
            <div class="zone-cell">
              <el-tag size="small" type="info" effect="plain">巡检时: {{ row.snapshotZoneName }}</el-tag>
              <el-tag
                v-if="row.zoneChanged"
                size="small"
                type="danger"
                effect="dark"
              >
                当前已变为: {{ row.currentZoneName || '灯组已不存在' }}
              </el-tag>
              <el-tag v-else size="small" type="success" effect="plain">当前: {{ row.currentZoneName }}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="异常类型" width="110">
          <template #default="{ row }">
            <el-tag :type="anomalyTagType(row.anomalyType)">{{ anomalyTypeText(row.anomalyType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="originalDescription" label="现场说明" min-width="180" show-overflow-tooltip />
        <el-table-column prop="reporter" label="上报人" width="90" />
        <el-table-column prop="reportedAt" label="上报时间" width="170" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDetail(row)">处置</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadList"
          @current-change="loadList"
        />
      </div>
    </el-card>

    <!-- 异常处置详情抽屉 -->
    <el-drawer
      v-model="detailVisible"
      :title="`异常处置详情 - ${detail?.groupCode || ''}`"
      size="640px"
      destroy-on-close
    >
      <div v-if="detail" class="detail-wrap">
        <el-alert
          v-if="detail.zoneChanged"
          type="warning"
          :closable="false"
          show-icon
          class="zone-alert"
          :title="`该灯组当前分区已由巡检时「${detail.snapshotZoneName}」变为「${detail.currentZoneName || '灯组已不存在'}」，历史仍按巡检现场快照保留`"
        />

        <el-descriptions :column="2" border size="small" class="desc">
          <el-descriptions-item label="批次号">{{ detail.batchNo }}</el-descriptions-item>
          <el-descriptions-item label="灯组编号">{{ detail.groupCode }}</el-descriptions-item>
          <el-descriptions-item label="异常类型">
            <el-tag :type="anomalyTagType(detail.anomalyType)" size="small">{{ anomalyTypeText(detail.anomalyType) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="当前状态">
            <el-tag :type="statusTagType(detail.status)" size="small">{{ statusText(detail.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="巡检时分区(快照)">{{ detail.snapshotZoneName }}</el-descriptions-item>
          <el-descriptions-item label="当前分区">{{ detail.currentZoneName || '灯组已不存在' }}</el-descriptions-item>
          <el-descriptions-item label="安装位置(快照)" :span="2">{{ detail.snapshotLocation || '-' }}</el-descriptions-item>
          <el-descriptions-item label="原始现场说明" :span="2">
            <span class="orig-desc">{{ detail.originalDescription }}</span>
          </el-descriptions-item>
          <el-descriptions-item v-if="detail.supplementDescription" label="补充说明" :span="2">
            {{ detail.supplementDescription }}
          </el-descriptions-item>
          <el-descriptions-item label="上报人">{{ detail.reporter }}</el-descriptions-item>
          <el-descriptions-item label="上报时间">{{ detail.reportedAt }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.reviewer" label="复核确认人">{{ detail.reviewer }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.confirmedAt" label="确认时间">{{ detail.confirmedAt }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.reviewOpinion" label="处理意见" :span="2">{{ detail.reviewOpinion }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.handlingDeadline" label="处理时限">{{ detail.handlingDeadline }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.handler" label="处理登记人">{{ detail.handler }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.handlingResult" label="处理结果" :span="2">{{ detail.handlingResult }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.handledAt" label="处理登记时间">{{ detail.handledAt }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.closer" label="关闭人">{{ detail.closer }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.closedAt" label="关闭时间">{{ detail.closedAt }}</el-descriptions-item>
        </el-descriptions>

        <!-- 操作区：按当前状态受控显示 -->
        <div class="action-panel">
          <template v-if="detail.status === 'PENDING_REVIEW'">
            <el-divider content-position="left">复核操作</el-divider>
            <div class="field-label">确认异常（填写处理意见和处理时限）</div>
            <el-input v-model="actionForm.reviewOpinion" type="textarea" :rows="2" placeholder="处理意见（确认必填）" class="mt8" />
            <el-date-picker
              v-model="actionForm.deadline"
              type="datetime"
              placeholder="处理时限（确认必填）"
              format="YYYY-MM-DD HH:mm:ss"
              value-format="YYYY-MM-DD HH:mm:ss"
              class="mt8"
              style="width: 100%"
            />
            <el-divider content-position="left">或退回巡检人补充</el-divider>
            <el-input v-model="actionForm.reason" type="textarea" :rows="2" placeholder="退回原因（退回必填）" />
            <div class="action-btns mt8">
              <el-input v-model="actionForm.operator" placeholder="复核人姓名" class="operator-input" />
              <el-button @click="doAction('return')" :loading="loadingMap.return">退回补充</el-button>
              <el-button type="primary" @click="doAction('confirm')" :loading="loadingMap.confirm">确认异常</el-button>
            </div>
          </template>

          <template v-else-if="detail.status === 'RETURNED'">
            <el-alert type="warning" :closable="false" show-icon title="该异常已被退回，请补充现场说明后重新提交复核" />
            <el-input
              v-model="actionForm.supplementDescription"
              type="textarea"
              :rows="3"
              placeholder="补充现场说明（必填）"
              class="mt8"
            />
            <div class="action-btns mt8">
              <el-input v-model="actionForm.operator" placeholder="巡检人姓名" class="operator-input" />
              <el-button type="primary" @click="doAction('supplement')" :loading="loadingMap.supplement">
                补充并重新提交
              </el-button>
            </div>
          </template>

          <template v-else-if="detail.status === 'CONFIRMED'">
            <el-divider content-position="left">处置执行</el-divider>
            <el-input v-model="actionForm.handlingResult" type="textarea" :rows="3" placeholder="登记处理结果（必填）" />
            <div class="action-btns mt8">
              <el-input v-model="actionForm.operator" placeholder="处理人姓名" class="operator-input" />
              <el-button type="primary" @click="doAction('resolve')" :loading="loadingMap.resolve">登记处理结果</el-button>
            </div>
          </template>

          <template v-else-if="detail.status === 'PROCESSING'">
            <el-divider content-position="left">收口关闭</el-divider>
            <el-alert type="info" :closable="false" title="处理结果已登记，确认无误后关闭；关闭后记录只读，不能改回处理中" />
            <div class="action-btns mt8">
              <el-input v-model="actionForm.operator" placeholder="关闭操作人" class="operator-input" />
              <el-button type="success" @click="doAction('close')" :loading="loadingMap.close">关闭异常</el-button>
            </div>
          </template>

          <template v-else>
            <el-alert type="success" :closable="false" show-icon title="该异常已关闭，记录只读" />
          </template>
        </div>

        <!-- 连续处置记录 -->
        <el-divider content-position="left">处置记录（{{ detail.logs?.length || 0 }}）</el-divider>
        <el-timeline class="timeline">
          <el-timeline-item
            v-for="log in detail.logs"
            :key="log.id"
            :type="actionColor(log.actionType)"
            :timestamp="`${log.operatedAt} · ${log.operator}`"
            placement="top"
          >
            <div class="log-item">
              <el-tag size="small" :type="actionColor(log.actionType)">{{ actionText(log.actionType) }}</el-tag>
              <span class="log-content">{{ log.content }}</span>
            </div>
          </el-timeline-item>
        </el-timeline>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { anomalyApi, zoneApi } from '@/api'

const route = useRoute()

const tableData = ref([])
const zoneTreeData = ref([])
const detailVisible = ref(false)
const detail = ref(null)
const zoneChangedOnly = ref(false)

const filterForm = reactive({ batchNo: '', groupCode: '', anomalyType: '', status: '', zoneId: null })
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const actionForm = reactive({
  operator: localStorage.getItem('handler') || '',
  reason: '',
  supplementDescription: '',
  reviewOpinion: '',
  deadline: '',
  handlingResult: ''
})
const loadingMap = reactive({ return: false, confirm: false, supplement: false, resolve: false, close: false })

const anomalyTypes = [
  { value: 'EXTINGUISHED', label: '熄灭' },
  { value: 'FLICKER', label: '频闪' },
  { value: 'DIM', label: '亮度不足' }
]
const statusList = [
  { value: 'PENDING_REVIEW', label: '待复核' },
  { value: 'RETURNED', label: '已退回' },
  { value: 'CONFIRMED', label: '已确认' },
  { value: 'PROCESSING', label: '处理中' },
  { value: 'CLOSED', label: '已关闭' }
]

const anomalyTypeText = (t) => ({ EXTINGUISHED: '熄灭', FLICKER: '频闪', DIM: '亮度不足' }[t] || t)
const anomalyTagType = (t) => ({ EXTINGUISHED: 'danger', FLICKER: 'warning', DIM: 'info' }[t] || '')
const statusText = (s) => ({
  PENDING_REVIEW: '待复核', RETURNED: '已退回', CONFIRMED: '已确认',
  PROCESSING: '处理中', CLOSED: '已关闭'
}[s] || s)
const statusTagType = (s) => ({
  PENDING_REVIEW: 'warning', RETURNED: 'danger', CONFIRMED: 'primary',
  PROCESSING: 'info', CLOSED: 'success'
}[s] || '')
const actionText = (a) => ({
  SUBMIT: '上报', RETURN: '退回', SUPPLEMENT: '补充',
  CONFIRM: '确认', RESOLVE: '登记处理结果', CLOSE: '关闭'
}[a] || a)
const actionColor = (a) => ({
  SUBMIT: 'primary', RETURN: 'danger', SUPPLEMENT: 'warning',
  CONFIRM: 'primary', RESOLVE: 'info', CLOSE: 'success'
}[a] || '')

const loadList = async () => {
  try {
    const params = {
      ...filterForm,
      zoneChanged: zoneChangedOnly.value ? true : undefined,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    }
    const res = await anomalyApi.getPage(params)
    tableData.value = res.data
    pagination.total = res.total
  } catch (e) {
    ElMessage.error('加载异常清单失败')
  }
}

const loadZones = async () => {
  try {
    const res = await zoneApi.getTree()
    zoneTreeData.value = res.data
  } catch (e) {
    ElMessage.error('加载分区失败')
  }
}

const handleSearch = () => { pagination.pageNum = 1; loadList() }
const handleReset = () => {
  Object.assign(filterForm, { batchNo: '', groupCode: '', anomalyType: '', status: '', zoneId: null })
  zoneChangedOnly.value = false
  pagination.pageNum = 1
  loadList()
}

const resetActionForm = () => {
  Object.assign(actionForm, {
    operator: localStorage.getItem('handler') || '',
    reason: '', supplementDescription: '', reviewOpinion: '', deadline: '', handlingResult: ''
  })
}

const openDetail = async (row) => {
  detailVisible.value = true
  await loadDetail(row.id)
}

const loadDetail = async (id) => {
  try {
    const res = await anomalyApi.getDetail(id)
    detail.value = res.data
    resetActionForm()
  } catch (e) {
    ElMessage.error('加载异常详情失败')
  }
}

const buildPayload = (action) => {
  const base = { operator: actionForm.operator.trim(), version: detail.value.version }
  if (action === 'return') return { ...base, reason: actionForm.reason }
  if (action === 'supplement') return { ...base, supplementDescription: actionForm.supplementDescription }
  if (action === 'confirm') return { ...base, reviewOpinion: actionForm.reviewOpinion, deadline: actionForm.deadline }
  if (action === 'resolve') return { ...base, handlingResult: actionForm.handlingResult }
  return base
}

const doAction = async (action) => {
  if (!actionForm.operator.trim()) {
    ElMessage.warning('请填写操作人姓名')
    return
  }
  if (action === 'return' && !actionForm.reason.trim()) {
    ElMessage.warning('退回时必须填写退回原因')
    return
  }
  if (action === 'supplement' && !actionForm.supplementDescription.trim()) {
    ElMessage.warning('补充时必须填写补充说明')
    return
  }
  if (action === 'confirm') {
    if (!actionForm.reviewOpinion.trim()) {
      ElMessage.warning('确认异常时必须填写处理意见')
      return
    }
    if (!actionForm.deadline) {
      ElMessage.warning('确认异常时必须填写处理时限')
      return
    }
  }
  if (action === 'resolve' && !actionForm.handlingResult.trim()) {
    ElMessage.warning('请填写处理结果')
    return
  }
  try {
    await ElMessageBox.confirm(`确认执行「${actionLabel(action)}」操作？`, '操作确认', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
    })
  } catch {
    return
  }

  localStorage.setItem('handler', actionForm.operator.trim())
  loadingMap[action] = true
  try {
    // return 是保留字，API 模块中命名为 doReturn
    const apiMethod = {
      return: anomalyApi.doReturn,
      supplement: anomalyApi.supplement,
      confirm: anomalyApi.confirm,
      resolve: anomalyApi.resolve,
      close: anomalyApi.close
    }[action]
    const res = await apiMethod(detail.value.id, buildPayload(action))
    detail.value = res.data
    ElMessage.success('操作成功')
    loadList()
  } catch (e) {
    if (e.code === 409) {
      // 并发冲突：后提交者看到记录已变化及最新内容，且不产生重复处理记录
      await ElMessageBox.alert(
        (e.message || '该记录已被其他人先行处理') +
        (e.latestData ? `\n最新状态：${statusText(e.latestData.status)}` : ''),
        '操作冲突：记录已被更新', { confirmButtonText: '查看最新内容', type: 'warning' })
      if (e.latestData) {
        detail.value = e.latestData
        resetActionForm()
      } else {
        await loadDetail(detail.value.id)
      }
      loadList()
    } else {
      ElMessage.error(e.message || '操作失败')
    }
  } finally {
    loadingMap[action] = false
  }
}

const actionLabel = (a) => ({
  return: '退回补充', supplement: '补充并重新提交', confirm: '确认异常',
  resolve: '登记处理结果', close: '关闭异常'
}[a] || a)

onMounted(async () => {
  await loadZones()
  if (route.query.batchNo) filterForm.batchNo = String(route.query.batchNo)
  loadList()
})
</script>

<style lang="scss" scoped>
.anomaly-container {
  .search-bar { margin-bottom: 12px; }
  .pagination { margin-top: 20px; text-align: right; }

  .zone-cell {
    display: flex;
    flex-direction: column;
    gap: 4px;
    align-items: flex-start;
  }

  .detail-wrap {
    padding: 0 20px 20px;

    .zone-alert { margin-bottom: 14px; }
    .desc { margin-bottom: 8px; }

    .orig-desc {
      color: #303133;
      font-weight: 500;
    }

    .action-panel {
      margin-top: 8px;

      .action-btns {
        display: flex;
        gap: 10px;
        align-items: center;

        .operator-input {
          width: 160px;
          flex-shrink: 0;
        }
      }
    }

    .mt8 { margin-top: 8px; }

    .field-label {
      font-size: 13px;
      font-weight: 600;
      color: #606266;
    }

    .timeline {
      padding-left: 4px;

      .log-item {
        display: flex;
        align-items: flex-start;
        gap: 8px;

        .log-content {
          font-size: 13px;
          color: #303133;
          line-height: 1.5;
          white-space: pre-wrap;
          word-break: break-all;
        }
      }
    }
  }
}
</style>
