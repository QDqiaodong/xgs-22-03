<template>
  <div class="transfer-container">
    <el-card>
      <div class="card-header">
        <el-button type="primary" @click="handleTransfer" :disabled="selectedIds.size === 0">
          执行批量划转
        </el-button>
        <el-button @click="handleClearSelection">清空选中</el-button>
      </div>
      
      <div class="search-bar">
        <el-form :model="filterForm" inline>
          <el-form-item label="分区">
            <el-select v-model="filterForm.zoneId" placeholder="请选择分区" clearable>
              <el-option 
                v-for="zone in zoneList" 
                :key="zone.id" 
                :value="zone.id" 
                :label="zone.zoneName"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="灯组编号">
            <el-input v-model="filterForm.groupCode" placeholder="请输入灯组编号" clearable />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
      
      <div class="selection-info">
        <span>已选中 <strong>{{ selectedIds.size }}</strong> 个灯组</span>
        <span v-if="selectedIds.size > 0" class="hint">（跨页选中会累积）</span>
      </div>
      
      <el-table
        ref="tableRef"
        :data="tableData"
        :row-key="(row) => row.id"
        :default-sort="{ prop: 'groupCode', order: 'ascending' }"
        stripe
      >
        <el-table-column type="selection" width="55" :reserve-selection="true" :selectable="canTransfer" @selection-change="handleSelectionChange" />
        <el-table-column prop="groupCode" label="灯组编号" />
        <el-table-column prop="power" label="功率(W)" />
        <el-table-column prop="zoneName" label="当前分区" />
        <el-table-column prop="installationLocation" label="安装位置" />
        <el-table-column prop="status" label="启用状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="归档状态" width="100">
          <template #default="{ row }">
            <el-tag :type="archiveTagType(row.archiveStatus)" size="small" effect="plain">
              {{ archiveStatusText(row.archiveStatus) }}
            </el-tag>
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
    
    <el-dialog v-model="transferDialogVisible" title="批量划转确认" width="500px">
      <div class="transfer-info">
        <div class="info-item">
          <span class="label">待划转灯组数量：</span>
          <span class="value">{{ selectedIds.size }}</span>
        </div>
        <div class="info-item">
          <span class="label">目标分区：</span>
          <el-select v-model="transferForm.targetZoneId" placeholder="请选择目标分区" style="width: 200px">
            <el-option 
              v-for="zone in zoneList" 
              :key="zone.id" 
              :value="zone.id" 
              :label="zone.zoneName"
            />
          </el-select>
        </div>
        <div class="info-item">
          <span class="label">操作人：</span>
          <el-input v-model="transferForm.operator" placeholder="请输入操作人" />
        </div>
        <div class="info-item">
          <span class="label">操作描述：</span>
          <el-input v-model="transferForm.description" type="textarea" :rows="3" placeholder="请输入操作描述（可选）" />
        </div>
      </div>
      <template #footer>
        <el-button @click="transferDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmTransfer" :loading="transferLoading">确定划转</el-button>
      </template>
    </el-dialog>
    
    <el-dialog v-model="resultDialogVisible" title="划转结果" width="400px">
      <div class="result-info">
        <el-icon :size="48" color="#67c23a"><component :is="Icons.Check" /></el-icon>
        <div class="result-text">划转成功！</div>
        <div class="result-detail">
          <p>批次号：{{ transferResult?.batchNo }}</p>
          <p>划转数量：{{ transferResult?.lightCount }}</p>
          <p>操作时间：{{ transferResult?.transferTime }}</p>
        </div>
      </div>
      <template #footer>
        <el-button type="primary" @click="resultDialogVisible = false">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Check } from '@element-plus/icons-vue'
import { lightGroupApi, zoneApi, transferApi } from '@/api'

const tableRef = ref(null)
const tableData = ref([])
const zoneList = ref([])
const selectedIds = ref(new Map())
const transferDialogVisible = ref(false)
const resultDialogVisible = ref(false)
const transferLoading = ref(false)
const transferResult = ref(null)

const Icons = { Check }

const filterForm = reactive({
  zoneId: null,
  groupCode: ''
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const transferForm = reactive({
  targetZoneId: null,
  operator: '',
  description: ''
})

const loadTable = async () => {
  try {
    const res = await lightGroupApi.getPage({
      ...filterForm,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })
    tableData.value = res.data
    pagination.total = res.total
    
    await nextTick()
    if (tableRef.value) {
      tableData.value.forEach(row => {
        if (selectedIds.value.has(row.id)) {
          tableRef.value.toggleRowSelection(row, true)
        }
      })
    }
  } catch (error) {
    ElMessage.error('加载数据失败')
  }
}

const loadZones = async () => {
  try {
    const res = await zoneApi.getAll()
    zoneList.value = res.data
  } catch (error) {
    ElMessage.error('加载分区失败')
  }
}

const handleSearch = () => {
  pagination.pageNum = 1
  loadTable()
}

const handleReset = () => {
  Object.assign(filterForm, {
    zoneId: null,
    groupCode: ''
  })
  pagination.pageNum = 1
  loadTable()
}

const handlePageChange = () => {
  loadTable()
}

const handleSelectionChange = (val) => {
  tableData.value.forEach(row => {
    const isSelected = val.some(item => item.id === row.id)
    if (isSelected) {
      selectedIds.value.set(row.id, row)
    } else {
      selectedIds.value.delete(row.id)
    }
  })
}

// 已归档灯组只读，不能参与划转
const canTransfer = (row) => row.archiveStatus !== 'ARCHIVED'

const archiveStatusText = (s) => ({
  ACTIVE: '正常', PENDING_REVIEW: '待复核', RETURNED: '已退回', ARCHIVED: '已归档'
}[s || 'ACTIVE'])
const archiveTagType = (s) => ({
  ACTIVE: 'info', PENDING_REVIEW: 'warning', RETURNED: 'danger', ARCHIVED: 'success'
}[s || 'ACTIVE'])

const handleClearSelection = () => {
  selectedIds.value.clear()
  if (tableRef.value) {
    tableRef.value.clearSelection()
  }
}

const handleTransfer = () => {
  if (selectedIds.value.size === 0) {
    ElMessage.warning('请先选择要划转的灯组')
    return
  }
  transferDialogVisible.value = true
}

const confirmTransfer = async () => {
  if (!transferForm.targetZoneId) {
    ElMessage.warning('请选择目标分区')
    return
  }
  if (!transferForm.operator) {
    ElMessage.warning('请输入操作人')
    return
  }
  
  transferLoading.value = true
  
  try {
    const res = await transferApi.batchTransfer({
      lightGroupIds: Array.from(selectedIds.value.keys()),
      targetZoneId: transferForm.targetZoneId,
      operator: transferForm.operator,
      description: transferForm.description
    })
    
    transferResult.value = res.data
    transferDialogVisible.value = false
    resultDialogVisible.value = true
    
    handleClearSelection()
    loadTable()
    
    Object.assign(transferForm, {
      targetZoneId: null,
      operator: '',
      description: ''
    })
  } catch (error) {
    ElMessage.error(error.message || '划转失败：已归档灯组不能划转')
    loadTable()
  } finally {
    transferLoading.value = false
  }
}

onMounted(() => {
  loadZones()
  loadTable()
})
</script>

<style lang="scss" scoped>
.transfer-container {
  height: 100%;
  
  .card-header {
    display: flex;
    gap: 10px;
    margin-bottom: 20px;
  }
  
  .search-bar {
    margin-bottom: 20px;
  }
  
  .selection-info {
    margin-bottom: 15px;
    padding: 10px 15px;
    background-color: #f0f9ff;
    border-radius: 4px;
    font-size: 14px;
    
    .hint {
      color: #909399;
    }
  }
  
  .pagination {
    margin-top: 20px;
    text-align: right;
  }
  
  .transfer-info {
    .info-item {
      display: flex;
      align-items: flex-start;
      margin-bottom: 15px;
      
      .label {
        width: 100px;
        flex-shrink: 0;
        font-weight: 500;
      }
      
      .value {
        font-weight: bold;
        color: #409eff;
      }
    }
  }
  
  .result-info {
    text-align: center;
    padding: 20px;
    
    .result-text {
      font-size: 18px;
      font-weight: bold;
      margin: 15px 0;
      color: #67c23a;
    }
    
    .result-detail {
      text-align: left;
      margin-top: 20px;
      padding: 15px;
      background-color: #f5f5f5;
      border-radius: 4px;
      
      p {
        margin: 5px 0;
        font-size: 14px;
      }
    }
  }
}
</style>