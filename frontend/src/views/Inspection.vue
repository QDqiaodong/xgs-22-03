<template>
  <div class="inspection-container">
    <el-card>
      <div class="card-header">
        <el-button type="primary" @click="openCreateDialog">创建巡检批次</el-button>
        <span class="header-hint">选择一个或多个分区创建批次，可暂存后逐个补录巡检结果</span>
      </div>

      <div class="search-bar">
        <el-form :model="filterForm" inline>
          <el-form-item label="批次号">
            <el-input v-model="filterForm.batchNo" placeholder="请输入批次号" clearable />
          </el-form-item>
          <el-form-item label="巡检人">
            <el-input v-model="filterForm.inspector" placeholder="请输入巡检人" clearable />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="filterForm.status" placeholder="全部" clearable style="width: 130px">
              <el-option label="暂存中" value="DRAFT" />
              <el-option label="已提交" value="SUBMITTED" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table :data="tableData" stripe>
        <el-table-column prop="batchNo" label="批次号" width="190" />
        <el-table-column prop="inspector" label="巡检人" width="110" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'DRAFT' ? 'warning' : 'success'">
              {{ row.status === 'DRAFT' ? '暂存中' : '已提交' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalCount" label="灯组数" width="90" />
        <el-table-column prop="abnormalCount" label="异常数" width="90">
          <template #default="{ row }">
            <span :class="{ 'abnormal-num': row.abnormalCount > 0 }">{{ row.abnormalCount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="startedAt" label="巡检开始时间" width="180" />
        <el-table-column prop="submittedAt" label="提交时间" width="180">
          <template #default="{ row }">{{ row.submittedAt || '-' }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" show-overflow-tooltip />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openRegisterDialog(row)">
              {{ row.status === 'DRAFT' ? '登记/补录' : '查看结果' }}
            </el-button>
            <el-button
              v-if="row.status === 'SUBMITTED' && row.abnormalCount > 0"
              type="danger" link
              @click="goAnomaly(row)"
            >异常清单</el-button>
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

    <!-- 创建批次 -->
    <el-dialog v-model="createDialogVisible" title="创建巡检批次" width="520px">
      <el-form :model="createForm" label-width="90px">
        <el-form-item label="巡检分区" required>
          <el-tree-select
            v-model="createForm.zoneIds"
            :data="zoneTreeData"
            :props="{ label: 'zoneName', value: 'id', children: 'children' }"
            multiple
            collapse-tags
            collapse-tags-tooltip
            check-strictly
            placeholder="请选择一个或多个分区（含子分区灯组）"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="巡检人" required>
          <el-input v-model="createForm.inspector" placeholder="请输入巡检人" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="createForm.remark" type="textarea" :rows="2" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="confirmCreate">创建并开始登记</el-button>
      </template>
    </el-dialog>

    <!-- 登记 / 补录 / 查看 -->
    <el-dialog
      v-model="registerDialogVisible"
      :title="`巡检登记 - ${currentBatch?.batchNo || ''}`"
      width="900px"
      top="6vh"
      @closed="handleRegisterClosed"
    >
      <div class="register-summary">
        <el-tag :type="currentBatch?.status === 'DRAFT' ? 'warning' : 'success'" size="small">
          {{ currentBatch?.status === 'DRAFT' ? '暂存中（可补录）' : '已提交（结果已冻结，只读）' }}
        </el-tag>
        <span>灯组总数：<strong>{{ currentBatch?.totalCount }}</strong></span>
        <span>已登记：<strong>{{ registeredCount }}</strong></span>
        <span>异常：<strong :class="{ 'abnormal-num': abnormalCount > 0 }">{{ abnormalCount }}</strong></span>
        <span class="snapshot-hint">分区以巡检开始时的现场快照为准</span>
      </div>

      <el-table :data="registerRows" max-height="440" border stripe>
        <el-table-column prop="groupCode" label="灯组编号" width="140" />
        <el-table-column label="巡检时分区(快照)" width="160">
          <template #default="{ row }">
            <span>{{ row.snapshotZoneName }}</span>
            <el-tag v-if="row.zoneChangedNow" type="danger" size="small" effect="plain" class="ml4">
              现已划转: {{ row.currentZoneName || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="snapshotLocation" label="安装位置" show-overflow-tooltip />
        <el-table-column label="巡检结果" width="260">
          <template #default="{ row }">
            <el-radio-group
              v-model="row.resultType"
              :disabled="currentBatch?.status !== 'DRAFT'"
              size="small"
            >
              <el-radio-button value="NORMAL">正常</el-radio-button>
              <el-radio-button value="EXTINGUISHED">熄灭</el-radio-button>
              <el-radio-button value="FLICKER">频闪</el-radio-button>
              <el-radio-button value="DIM">亮度不足</el-radio-button>
            </el-radio-group>
          </template>
        </el-table-column>
        <el-table-column label="现场说明" min-width="200">
          <template #default="{ row }">
            <el-input
              v-model="row.description"
              :disabled="currentBatch?.status !== 'DRAFT'"
              type="textarea"
              :rows="1"
              :placeholder="isAbnormal(row.resultType) ? '异常必须填写现场说明' : '正常无需填写'"
            />
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <template v-if="currentBatch?.status === 'DRAFT'">
          <el-button @click="registerDialogVisible = false">关闭</el-button>
          <el-button :loading="saving" @click="handleSave(false)">暂存</el-button>
          <el-button type="primary" :loading="submitting" @click="handleSave(true)">提交批次</el-button>
        </template>
        <el-button v-else type="primary" @click="registerDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { inspectionApi, zoneApi, lightGroupApi } from '@/api'

const router = useRouter()
const route = useRoute()

const tableData = ref([])
const zoneTreeData = ref([])
const allZones = ref([])
const createDialogVisible = ref(false)
const registerDialogVisible = ref(false)
const creating = ref(false)
const saving = ref(false)
const submitting = ref(false)
const currentBatch = ref(null)
const registerRows = ref([])

const filterForm = reactive({ batchNo: '', inspector: '', status: '' })
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const savedInspector = localStorage.getItem('inspector') || ''
const createForm = reactive({ zoneIds: [], inspector: savedInspector, remark: '' })

const registeredCount = computed(() => registerRows.value.filter(r => r.resultType).length)
const abnormalCount = computed(() => registerRows.value.filter(r => isAbnormal(r.resultType)).length)

function isAbnormal(type) {
  return type && type !== 'NORMAL'
}

const loadList = async () => {
  try {
    const res = await inspectionApi.getBatchPage({
      ...filterForm,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })
    tableData.value = res.data
    pagination.total = res.total
  } catch (e) {
    ElMessage.error('加载巡检批次失败')
  }
}

const loadZones = async () => {
  try {
    const [tree, list] = await Promise.all([zoneApi.getTree(), zoneApi.getAll()])
    zoneTreeData.value = tree.data
    allZones.value = list.data
  } catch (e) {
    ElMessage.error('加载分区失败')
  }
}

const handleSearch = () => { pagination.pageNum = 1; loadList() }
const handleReset = () => {
  Object.assign(filterForm, { batchNo: '', inspector: '', status: '' })
  pagination.pageNum = 1
  loadList()
}

const openCreateDialog = () => {
  Object.assign(createForm, { zoneIds: [], inspector: localStorage.getItem('inspector') || '', remark: '' })
  createDialogVisible.value = true
}

const confirmCreate = async () => {
  if (!createForm.zoneIds.length) {
    ElMessage.warning('请至少选择一个巡检分区')
    return
  }
  if (!createForm.inspector.trim()) {
    ElMessage.warning('请输入巡检人')
    return
  }
  creating.value = true
  try {
    localStorage.setItem('inspector', createForm.inspector.trim())
    const res = await inspectionApi.createBatch({
      zoneIds: createForm.zoneIds,
      inspector: createForm.inspector.trim(),
      remark: createForm.remark
    })
    ElMessage.success('批次已创建，可逐个登记巡检结果')
    createDialogVisible.value = false
    pagination.pageNum = 1
    loadList()
    await openRegister(res.data.id)
  } catch (e) {
    ElMessage.error(e.message || '创建批次失败')
  } finally {
    creating.value = false
  }
}

const openRegisterDialog = async (row) => {
  await openRegister(row.id)
}

const openRegister = async (id) => {
  try {
    const res = await inspectionApi.getBatchDetail(id)
    currentBatch.value = res.data
    registerRows.value = res.data.items.map(it => ({ ...it }))
    registerDialogVisible.value = true
    // 已提交批次：标注哪些灯组当前分区已相对快照发生变化
    if (res.data.status === 'SUBMITTED') {
      await markZoneChanged()
    } else {
      registerRows.value.forEach(r => { r.zoneChangedNow = false })
    }
  } catch (e) {
    ElMessage.error('加载巡检条目失败')
  }
}

const markZoneChanged = async () => {
  if (!registerRows.value.length) return
  // 拉取灯组当前分区，与批次现场快照比对（数量级可控，一次性加载缓存）
  await ensureAllLightGroups()
  const lgMap = new Map(allLightGroups.value.map(g => [g.id, g]))
  registerRows.value.forEach(r => {
    const g = lgMap.get(r.lightGroupId)
    r.currentZoneName = g ? g.zoneName : null
    r.zoneChangedNow = !g || g.zoneId !== r.snapshotZoneId
  })
}

const allLightGroups = ref([])
const ensureAllLightGroups = async () => {
  if (allLightGroups.value.length) return
  const res = await lightGroupApi.getPage({ pageNum: 1, pageSize: 1000 })
  allLightGroups.value = res.data
}

const collectChangedItems = () => registerRows.value
  .filter(r => r.resultType)
  .map(r => ({ itemId: r.id, resultType: r.resultType, description: r.description || '' }))

const handleSave = async (submit) => {
  const filled = registerRows.value.filter(r => r.resultType)
  if (!filled.length) {
    ElMessage.warning('请至少登记一个灯组的巡检结果')
    return
  }
  // 前端先做一次校验，最终以后端为准
  for (const r of filled) {
    if (isAbnormal(r.resultType) && !(r.description || '').trim()) {
      ElMessage.warning(`灯组${r.groupCode}登记为异常，必须填写现场说明`)
      return
    }
  }
  if (submit) {
    if (filled.length < registerRows.value.length) {
      try {
        await ElMessageBox.confirm(
          `还有 ${registerRows.value.length - filled.length} 个灯组未登记结果，提交前必须全部登记。是否继续补录？`,
          '尚未登记完成', { confirmButtonText: '去补录', cancelButtonText: '取消', type: 'warning' })
      } catch { return }
      return
    }
    submitting.value = true
  } else {
    saving.value = true
  }

  try {
    await inspectionApi.saveItems(currentBatch.value.id, {
      items: collectChangedItems(),
      operator: createForm.inspector || currentBatch.value.inspector
    })
    if (submit) {
      const res = await inspectionApi.submitBatch(currentBatch.value.id, {})
      ElMessage.success(`批次已提交，形成 ${res.data.abnormalCount} 条异常记录`)
      registerDialogVisible.value = false
      loadList()
    } else {
      ElMessage.success('已暂存，可继续补录')
      await openRegister(currentBatch.value.id)
      loadList()
    }
  } catch (e) {
    ElMessage.error(e.message || (submit ? '提交失败' : '暂存失败'))
  } finally {
    saving.value = false
    submitting.value = false
  }
}

const handleRegisterClosed = () => {
  registerRows.value = []
  currentBatch.value = null
}

const goAnomaly = (row) => {
  router.push({ path: '/inspection-anomaly', query: { batchNo: row.batchNo } })
}

onMounted(async () => {
  await loadZones()
  if (route.query.batchNo) {
    filterForm.batchNo = String(route.query.batchNo)
  }
  loadList()
})
</script>

<style lang="scss" scoped>
.inspection-container {
  .card-header {
    display: flex;
    align-items: center;
    gap: 14px;
    margin-bottom: 20px;

    .header-hint {
      color: #909399;
      font-size: 13px;
    }
  }

  .search-bar { margin-bottom: 12px; }

  .abnormal-num {
    color: #f56c6c;
    font-weight: bold;
  }

  .pagination {
    margin-top: 20px;
    text-align: right;
  }

  .register-summary {
    display: flex;
    align-items: center;
    gap: 18px;
    margin-bottom: 12px;
    font-size: 13px;

    .snapshot-hint {
      color: #909399;
      margin-left: auto;
    }
  }

  .ml4 { margin-left: 4px; }
}
</style>
