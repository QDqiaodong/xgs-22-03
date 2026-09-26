<template>
  <div class="ledger-container">
    <el-card>
      <div class="search-bar">
        <el-form :model="filterForm" inline>
          <el-form-item label="批次号">
            <el-input v-model="filterForm.batchNo" placeholder="请输入批次号" clearable />
          </el-form-item>
          <el-form-item label="操作人">
            <el-input v-model="filterForm.operator" placeholder="请输入操作人" clearable />
          </el-form-item>
          <el-form-item label="时间范围">
            <el-date-picker
              v-model="dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
            />
          </el-form-item>
          <el-form-item label="核销状态">
            <el-select v-model="filterForm.settleStatus" placeholder="全部" clearable style="width: 120px">
              <el-option value="OPEN" label="未核销" />
              <el-option value="CLOSED" label="已核销" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table
        :data="tableData"
        :row-key="(row) => row.id"
        @row-click="handleRowClick"
        stripe
      >
        <el-table-column prop="batchNo" label="批次号" width="180" />
        <el-table-column prop="operator" label="操作人" width="100" />
        <el-table-column prop="transferTime" label="划转时间" width="170" />
        <el-table-column prop="sourceZoneIds" label="源分区ID" />
        <el-table-column prop="targetZoneId" label="目标分区ID" />
        <el-table-column prop="lightCount" label="划转数量" width="90" />
        <el-table-column label="核销状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.settleStatus === 'CLOSED' ? 'success' : 'warning'" size="small">
              {{ row.settleStatus === 'CLOSED' ? '已核销' : '未核销' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="操作描述" show-overflow-tooltip />
        <el-table-column label="操作" width="130">
          <template #default="{ row }">
            <el-button type="primary" link @click.stop="handleViewDetails(row)">详情</el-button>
            <el-button
              v-if="row.settleStatus !== 'CLOSED'"
              type="success"
              link
              @click.stop="handleSettle(row)"
            >核销</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handlePageChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="detailDialogVisible" :title="`批次 ${currentLedger?.batchNo} 详情`" width="820px">
      <div class="ledger-summary">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="操作人">{{ currentLedger?.operator }}</el-descriptions-item>
          <el-descriptions-item label="划转时间">{{ currentLedger?.transferTime }}</el-descriptions-item>
          <el-descriptions-item label="源分区ID">{{ currentLedger?.sourceZoneIds }}</el-descriptions-item>
          <el-descriptions-item label="目标分区ID">{{ currentLedger?.targetZoneId }}</el-descriptions-item>
          <el-descriptions-item label="划转数量">{{ currentLedger?.lightCount }}</el-descriptions-item>
          <el-descriptions-item label="核销状态">
            <el-tag :type="currentLedger?.settleStatus === 'CLOSED' ? 'success' : 'warning'" size="small">
              {{ currentLedger?.settleStatus === 'CLOSED' ? `已核销（${currentLedger?.settledBy || ''} ${currentLedger?.settledAt || ''}）` : '未核销' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="操作描述" :span="2">{{ currentLedger?.description || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <div class="detail-table">
        <h4>划转明细（分区为划转当时快照；归档状态取当前台账）</h4>
        <el-table :data="detailList" stripe size="small">
          <el-table-column prop="groupCode" label="灯组编号" width="150" />
          <el-table-column prop="lightGroupId" label="灯组ID" width="90" />
          <el-table-column label="原分区" min-width="150">
            <template #default="{ row }">{{ row.sourceZoneName || row.sourceZoneId }}</template>
          </el-table-column>
          <el-table-column label="新分区" min-width="150">
            <template #default="{ row }">{{ row.targetZoneName || row.targetZoneId }}</template>
          </el-table-column>
          <el-table-column label="当前归档状态" width="110">
            <template #default="{ row }">
              <el-tag v-if="row.archiveStatus === 'ARCHIVED'" type="success" size="small">已归档</el-tag>
              <el-tag v-else type="info" size="small" effect="plain">正常</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <template #footer>
        <el-button
          v-if="currentLedger && currentLedger.settleStatus !== 'CLOSED'"
          type="success"
          @click="handleSettle(currentLedger)"
        >核销该流水（解除归档引用）</el-button>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { transferApi } from '@/api'

const tableData = ref([])
const detailList = ref([])
const detailDialogVisible = ref(false)
const currentLedger = ref(null)
const dateRange = ref([])
const settleOperator = ref(localStorage.getItem('handler') || '')

const filterForm = reactive({
  batchNo: '',
  operator: '',
  settleStatus: ''
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const loadTable = async () => {
  try {
    const params = {
      ...filterForm,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    }

    if (dateRange.value.length === 2) {
      params.startTime = dateRange.value[0] + ' 00:00:00'
      params.endTime = dateRange.value[1] + ' 23:59:59'
    }

    const res = await transferApi.getLedgerPage(params)
    tableData.value = res.data
    pagination.total = res.total
  } catch (error) {
    ElMessage.error('加载数据失败')
  }
}

const handleSearch = () => {
  pagination.pageNum = 1
  loadTable()
}

const handleReset = () => {
  Object.assign(filterForm, {
    batchNo: '',
    operator: '',
    settleStatus: ''
  })
  dateRange.value = []
  pagination.pageNum = 1
  loadTable()
}

const handlePageChange = () => {
  loadTable()
}

const handleRowClick = (row) => {
  handleViewDetails(row)
}

const handleViewDetails = async (row) => {
  currentLedger.value = row
  detailDialogVisible.value = true

  try {
    const res = await transferApi.getLedgerDetails(row.id)
    detailList.value = res.data
  } catch (error) {
    ElMessage.error('加载明细失败')
  }
}

const handleSettle = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入核销操作人', '核销划转流水', {
      confirmButtonText: '确认核销',
      cancelButtonText: '取消',
      inputValue: settleOperator.value,
      inputValidator: (v) => (v && v.trim() ? true : '操作人不能为空')
    })
    const res = await transferApi.settleLedger(row.id, { operator: value.trim() })
    settleOperator.value = value.trim()
    localStorage.setItem('handler', value.trim())
    ElMessage.success('流水已核销，相关灯组归档时不再被该流水阻断')
    if (currentLedger.value && currentLedger.value.id === row.id) {
      currentLedger.value = res.data
    }
    loadTable()
  } catch (error) {
    if (error !== 'cancel' && error?.message) {
      ElMessage.error(error.message)
    }
  }
}

onMounted(() => {
  loadTable()
})
</script>

<style lang="scss" scoped>
.ledger-container {
  height: 100%;

  .search-bar {
    margin-bottom: 20px;
  }

  .pagination {
    margin-top: 20px;
    text-align: right;
  }

  .ledger-summary {
    margin-bottom: 20px;
  }

  .detail-table {
    h4 {
      margin-bottom: 15px;
      font-size: 14px;
      font-weight: bold;
    }
  }
}
</style>
