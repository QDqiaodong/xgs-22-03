<template>
  <el-drawer
    v-model="visible"
    :title="`灯组报废归档 - ${detail?.groupCode || ''}`"
    size="720px"
    destroy-on-close
    @close="handleClose"
  >
    <div v-if="detail" class="archive-wrap">
      <!-- 当前状态 -->
      <el-alert
        :type="statusAlertType(detail.archiveStatus)"
        :closable="false"
        show-icon
        class="status-alert"
      >
        <template #title>
          当前状态：
          <el-tag :type="statusTagType(detail.archiveStatus)" size="small">
            {{ archiveStatusText(detail.archiveStatus) }}
          </el-tag>
          <span class="status-zone">所属分区：{{ detail.zoneName || '-' }}（历史分区以各条记录快照为准）</span>
        </template>
      </el-alert>

      <el-descriptions :column="2" border size="small" class="desc">
        <el-descriptions-item label="灯组编号">{{ detail.groupCode }}</el-descriptions-item>
        <el-descriptions-item label="所属分区">{{ detail.zoneName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="安装位置">{{ detail.installationLocation || '-' }}</el-descriptions-item>
        <el-descriptions-item label="启用状态">
          <el-tag :type="detail.status === 1 ? 'success' : 'danger'" size="small">
            {{ detail.status === 1 ? '启用' : '停用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="报废生效日期">{{ detail.effectiveDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="最近送审人">{{ detail.archiveApplicant || '-' }}</el-descriptions-item>
        <el-descriptions-item label="最近送审时间">{{ detail.archiveAppliedAt || '-' }}</el-descriptions-item>
        <el-descriptions-item label="最近复核人">{{ detail.archiveReviewer || '-' }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.archiveArchivedAt" label="归档生效时间" :span="2">
          {{ detail.archiveArchivedAt }}
        </el-descriptions-item>
        <el-descriptions-item label="原始报废理由" :span="2">
          <span class="orig-reason">{{ detail.scrapReason || '-' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="现场照片说明" :span="2">{{ detail.photoNote || '-' }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.supplementNote" label="最近补充内容" :span="2">
          {{ detail.supplementNote }}
        </el-descriptions-item>
        <el-descriptions-item v-if="detail.archiveReviewOpinion" label="最近复核意见" :span="2">
          {{ detail.archiveReviewOpinion }}
        </el-descriptions-item>
      </el-descriptions>

      <!-- 操作区：按当前生命周期状态受控显示 -->
      <div class="action-panel">
        <!-- 正常：管理员发起送审 -->
        <template v-if="detail.archiveStatus === 'ACTIVE'">
          <el-divider content-position="left">发起报废送审</el-divider>
          <div class="field-label">报废理由（必填）</div>
          <el-input v-model="form.scrapReason" type="textarea" :rows="2" placeholder="请填写报废理由" class="mt8" />
          <div class="field-label mt8">现场照片说明（必填）</div>
          <el-input v-model="form.photoNote" type="textarea" :rows="2" placeholder="如：照片3张，显示灯壳开裂、镇流器烧毁" class="mt8" />
          <div class="field-label mt8">报废生效日期（必填）</div>
          <el-date-picker
            v-model="form.effectiveDate"
            type="date"
            placeholder="选择生效日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            class="mt8"
            style="width: 100%"
          />
          <div class="action-btns mt8">
            <el-input v-model="form.operator" placeholder="管理员姓名" class="operator-input" />
            <el-button type="primary" :loading="loading" @click="doSubmit">送交复核</el-button>
          </div>
        </template>

        <!-- 待复核：复核人退回或确认归档 -->
        <template v-else-if="detail.archiveStatus === 'PENDING_REVIEW'">
          <el-divider content-position="left">复核操作（第 {{ currentRound }} 轮送审）</el-divider>

          <!-- 引用检查结果直接呈现 -->
          <div class="ref-panel">
            <div class="ref-head">
              <span class="field-label">归档前引用检查</span>
              <el-button link type="primary" :loading="refLoading" @click="loadReferences">重新检查</el-button>
            </div>
            <el-alert
              v-if="refLoaded && references.length === 0"
              type="success"
              :closable="false"
              show-icon
              title="未发现未关闭的巡检批次、异常处置记录或未核销划转流水，可以归档"
              class="mt8"
            />
            <el-table v-if="references.length > 0" :data="references" size="small" border class="mt8">
              <el-table-column prop="typeText" label="类型" width="120" />
              <el-table-column label="记录" min-width="180">
                <template #default="{ row }">
                  <div>{{ row.refNo }}</div>
                  <div class="ref-sub">灯组：{{ row.groupCode }}</div>
                </template>
              </el-table-column>
              <el-table-column prop="statusText" label="当前状态" width="100" />
              <el-table-column prop="detail" label="阻断原因" min-width="220" show-overflow-tooltip />
            </el-table>
            <div v-if="references.length > 0" class="ref-tip mt8">
              共 {{ references.length }} 条阻断记录：未关闭巡检批次请先提交/处理，异常记录请先处置关闭，划转流水请在操作台账核销；全部解除后重新检查并确认归档。
            </div>
          </div>

          <el-divider content-position="left">确认归档</el-divider>
          <el-input v-model="form.opinion" type="textarea" :rows="2" placeholder="归档确认意见（确认必填）" />
          <div class="action-btns mt8">
            <el-input v-model="form.operator" placeholder="复核人姓名" class="operator-input" />
            <el-button type="primary" :loading="loading" @click="doConfirm">确认归档</el-button>
          </div>

          <el-divider content-position="left">或退回补充</el-divider>
          <el-input v-model="form.reason" type="textarea" :rows="2" placeholder="退回原因（退回必填）" />
          <div class="action-btns mt8">
            <el-button :loading="loading" @click="doReturn">退回补充</el-button>
          </div>
        </template>

        <!-- 已退回：管理员补充后再次送审 -->
        <template v-else-if="detail.archiveStatus === 'RETURNED'">
          <el-alert type="warning" :closable="false" show-icon class="mt8"
            :title="`已被复核人退回（第 ${currentRound} 轮意见）：${detail.archiveReviewOpinion || ''}`" />
          <el-divider content-position="left">补充后再次送审</el-divider>
          <div class="field-label">补充内容（必填，原始报废理由保留不变）</div>
          <el-input v-model="form.supplementNote" type="textarea" :rows="3" class="mt8"
            placeholder="针对退回原因补充现场说明" />
          <div class="action-btns mt8">
            <el-input v-model="form.operator" placeholder="管理员姓名" class="operator-input" />
            <el-button type="primary" :loading="loading" @click="doResubmit">再次送审</el-button>
          </div>
        </template>

        <!-- 已归档：只读 -->
        <template v-else>
          <el-alert type="success" :closable="false" show-icon
            title="该灯组已归档，台账记录只读：编号、分区和启用状态不可再被编辑或划转" />
        </template>
      </div>

      <!-- 完整历史轨迹：同一条链路 -->
      <el-divider content-position="left">归档历史轨迹（{{ detail.archiveRecords?.length || 0 }} 条）</el-divider>
      <el-timeline class="timeline">
        <el-timeline-item
          v-for="rec in detail.archiveRecords"
          :key="rec.id"
          :type="actionColor(rec.actionType)"
          :timestamp="`${rec.operatedAt} · ${rec.operator}`"
          placement="top"
        >
          <div class="log-item">
            <el-tag size="small" :type="actionColor(rec.actionType)">
              {{ actionText(rec.actionType) }}{{ rec.round ? `·第${rec.round}轮` : '' }}
            </el-tag>
            <div class="log-body">
              <div v-if="rec.scrapReason" class="log-line"><b>报废理由：</b>{{ rec.scrapReason }}</div>
              <div v-if="rec.supplementNote" class="log-line"><b>补充内容：</b>{{ rec.supplementNote }}</div>
              <div v-if="rec.photoNote" class="log-line"><b>照片说明：</b>{{ rec.photoNote }}</div>
              <div v-if="rec.effectiveDate" class="log-line"><b>生效日期：</b>{{ rec.effectiveDate }}</div>
              <div v-if="rec.opinion" class="log-line"><b>意见：</b>{{ rec.opinion }}</div>
              <div class="log-line snapshot">
                操作时编号：{{ rec.snapshotGroupCode }} ｜ 操作时分区：{{ rec.snapshotZoneName }}
                ｜ 版本：v{{ rec.fromVersion }} → v{{ rec.toVersion }}
              </div>
            </div>
          </div>
        </el-timeline-item>
      </el-timeline>
    </div>
  </el-drawer>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { archiveApi } from '@/api'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  lightGroupId: { type: [Number, String], default: null }
})
const emit = defineEmits(['update:modelValue', 'archived'])

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const detail = ref(null)
const loading = ref(false)
const refLoading = ref(false)
const refLoaded = ref(false)
const references = ref([])

const form = reactive({
  operator: localStorage.getItem('handler') || '',
  scrapReason: '',
  photoNote: '',
  effectiveDate: '',
  supplementNote: '',
  reason: '',
  opinion: ''
})

const currentRound = computed(() => {
  const records = detail.value?.archiveRecords || []
  if (!records.length) return 1
  return records.reduce((m, r) => Math.max(m, r.round || 1), 1)
})

const archiveStatusText = (s) => ({
  ACTIVE: '正常', PENDING_REVIEW: '待复核', RETURNED: '已退回', ARCHIVED: '已归档'
}[s] || s)
const statusTagType = (s) => ({
  ACTIVE: 'info', PENDING_REVIEW: 'warning', RETURNED: 'danger', ARCHIVED: 'success'
}[s] || '')
const statusAlertType = (s) => ({
  ACTIVE: 'info', PENDING_REVIEW: 'warning', RETURNED: 'error', ARCHIVED: 'success'
}[s] || 'info')
const actionText = (a) => ({
  SUBMIT: '发起送审', RESUBMIT: '再次送审', RETURN: '复核退回', ARCHIVE: '确认归档'
}[a] || a)
const actionColor = (a) => ({
  SUBMIT: 'primary', RESUBMIT: 'primary', RETURN: 'danger', ARCHIVE: 'success'
}[a] || '')

const resetForm = () => {
  Object.assign(form, {
    operator: localStorage.getItem('handler') || '',
    scrapReason: '', photoNote: '', effectiveDate: '',
    supplementNote: '', reason: '', opinion: ''
  })
}

const loadDetail = async () => {
  if (!props.lightGroupId) return
  try {
    const res = await archiveApi.getDetail(props.lightGroupId)
    detail.value = res.data
    resetForm()
    // 进入待复核页自动展示引用检查结果
    if (detail.value.archiveStatus === 'PENDING_REVIEW') {
      loadReferences()
    } else {
      refLoaded.value = false
      references.value = []
    }
  } catch (e) {
    ElMessage.error(e.message || '加载归档详情失败')
  }
}

const loadReferences = async () => {
  refLoading.value = true
  try {
    const res = await archiveApi.checkReferences(props.lightGroupId)
    references.value = res.data || []
    refLoaded.value = true
  } catch (e) {
    ElMessage.error(e.message || '引用检查失败')
  } finally {
    refLoading.value = false
  }
}

watch(() => [props.modelValue, props.lightGroupId], ([open]) => {
  if (open) loadDetail()
})

const requireOperator = () => {
  if (!form.operator.trim()) {
    ElMessage.warning('请填写操作人姓名')
    return false
  }
  localStorage.setItem('handler', form.operator.trim())
  return true
}

const callArchive = async (apiFn, payload, successText, options = {}) => {
  loading.value = true
  try {
    const res = await apiFn(props.lightGroupId, payload)
    detail.value = res.data
    resetForm()
    if (detail.value.archiveStatus === 'PENDING_REVIEW') loadReferences()
    else { references.value = []; refLoaded.value = false }
    ElMessage.success(successText)
    if (options.emitArchived) emit('archived', res.data)
  } catch (e) {
    if (e.code === 409) {
      // 并发冲突：只提示并刷新到最新意见，不额外增加历史
      await ElMessageBox.alert(
        (e.message || '该送审版本已被其他人处理') +
        (e.latestData ? `\n最新状态：${archiveStatusText(e.latestData.archiveStatus)}` : ''),
        '操作冲突：送审版本已更新',
        { confirmButtonText: '查看最新意见', type: 'warning' }
      )
      if (e.latestData) {
        detail.value = e.latestData
        resetForm()
        if (detail.value.archiveStatus === 'PENDING_REVIEW') loadReferences()
        else { references.value = []; refLoaded.value = false }
        if (e.latestData.archiveStatus === 'ARCHIVED') emit('archived', e.latestData)
      } else {
        await loadDetail()
      }
    } else if (e.code === 412) {
      // 引用阻断：把具体阻断记录直接呈现在页面上
      references.value = e.latestData || []
      refLoaded.value = true
      await ElMessageBox.alert(
        (e.message || '存在未解除引用') +
        (e.latestData?.length ? `\n阻断记录 ${e.latestData.length} 条，已在页面列出。` : ''),
        '归档被阻断', { confirmButtonText: '查看阻断记录', type: 'error' }
      )
    } else {
      ElMessage.error(e.message || '操作失败')
    }
  } finally {
    loading.value = false
  }
}

const doSubmit = async () => {
  if (!requireOperator()) return
  if (!form.scrapReason.trim()) return ElMessage.warning('请填写报废理由')
  if (!form.photoNote.trim()) return ElMessage.warning('请填写现场照片说明')
  if (!form.effectiveDate) return ElMessage.warning('请选择报废生效日期')
  try {
    await ElMessageBox.confirm('确认将该灯组送交报废复核？', '操作确认', { type: 'warning' })
  } catch { return }
  await callArchive(archiveApi.submit, {
    operator: form.operator.trim(),
    version: detail.value.archiveVersion,
    scrapReason: form.scrapReason,
    photoNote: form.photoNote,
    effectiveDate: form.effectiveDate
  }, '已送交复核')
}

const doResubmit = async () => {
  if (!requireOperator()) return
  if (!form.supplementNote.trim()) return ElMessage.warning('请填写补充内容')
  try {
    await ElMessageBox.confirm('确认补充后再次送审？原始报废理由将保留。', '操作确认', { type: 'warning' })
  } catch { return }
  await callArchive(archiveApi.resubmit, {
    operator: form.operator.trim(),
    version: detail.value.archiveVersion,
    supplementNote: form.supplementNote
  }, '已再次送审')
}

const doReturn = async () => {
  if (!requireOperator()) return
  if (!form.reason.trim()) return ElMessage.warning('退回时必须填写退回原因')
  try {
    await ElMessageBox.confirm('确认退回给管理员补充？', '操作确认', { type: 'warning' })
  } catch { return }
  await callArchive(archiveApi.reviewReturn, {
    operator: form.operator.trim(),
    version: detail.value.archiveVersion,
    reason: form.reason
  }, '已退回补充')
}

const doConfirm = async () => {
  if (!requireOperator()) return
  if (!form.opinion.trim()) return ElMessage.warning('确认归档必须填写确认意见')
  if (refLoaded && references.value.length > 0) {
    return ElMessage.error('仍有未解除的阻断引用，请先解除后再确认归档')
  }
  try {
    await ElMessageBox.confirm('确认归档后灯组台账只读，编号、分区和启用状态不可再编辑或划转。确认归档？',
      '归档最终确认', { type: 'warning', confirmButtonText: '确认归档' })
  } catch { return }
  await callArchive(archiveApi.confirm, {
    operator: form.operator.trim(),
    version: detail.value.archiveVersion,
    opinion: form.opinion
  }, '已完成归档', { emitArchived: true })
}

const handleClose = () => {
  emit('archived', detail.value)
}
</script>

<style lang="scss" scoped>
.archive-wrap {
  padding: 0 20px 20px;

  .status-alert { margin-bottom: 12px; }
  .status-zone { margin-left: 10px; font-size: 12px; color: #606266; }
  .desc { margin-bottom: 8px; }
  .orig-reason { font-weight: 600; color: #303133; }

  .field-label { font-size: 13px; font-weight: 600; color: #606266; }
  .mt8 { margin-top: 8px; }

  .action-btns {
    display: flex;
    gap: 10px;
    align-items: center;

    .operator-input { width: 170px; flex-shrink: 0; }
  }

  .ref-panel {
    border: 1px solid #ebeef5;
    border-radius: 6px;
    padding: 10px 12px;
    background: #fafafa;

    .ref-head {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    .ref-sub { font-size: 12px; color: #909399; }
    .ref-tip { font-size: 12px; color: #e6a23c; }
  }

  .timeline {
    padding-left: 4px;

    .log-item {
      display: flex;
      align-items: flex-start;
      gap: 8px;

      .log-body {
        font-size: 13px;
        color: #303133;
        line-height: 1.6;

        .log-line { white-space: pre-wrap; word-break: break-all; }
        .snapshot { color: #909399; font-size: 12px; margin-top: 2px; }
      }
    }
  }
}
</style>
