<template>
  <div class="light-group-container">
    <el-card>
      <div class="card-header">
        <el-button type="primary" @click="handleAdd">新增灯组</el-button>
        <el-button type="success" @click="handleExport" :disabled="selectedIds.length === 0">
          导出选中
        </el-button>
        <el-button type="info" @click="handleExportZone" :disabled="!filterForm.zoneId">
          导出分区资产
        </el-button>
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
          <el-form-item label="启用状态">
            <el-select v-model="filterForm.status" placeholder="请选择" clearable>
              <el-option :value="1" label="启用" />
              <el-option :value="0" label="停用" />
            </el-select>
          </el-form-item>
          <el-form-item label="归档状态">
            <el-select v-model="filterForm.archiveStatus" placeholder="全部" clearable style="width: 130px">
              <el-option v-for="a in archiveStatusOptions" :key="a.value" :value="a.value" :label="a.label" />
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
        @selection-change="handleSelectionChange"
        stripe
      >
        <el-table-column type="selection" width="55" :selectable="canSelect" />
        <el-table-column prop="groupCode" label="灯组编号" />
        <el-table-column prop="power" label="功率(W)" />
        <el-table-column prop="zoneName" label="所属分区" />
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
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button type="primary" link @click="openArchive(row)">
              {{ row.archiveStatus === 'ACTIVE' ? '报废归档' : '归档详情' }}
            </el-button>
            <el-button v-if="row.archiveStatus !== 'ARCHIVED'" type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button v-else type="info" link disabled title="已归档，记录只读">编辑</el-button>
            <el-button v-if="row.archiveStatus !== 'ARCHIVED'" type="danger" link @click="handleDelete(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="灯组编号" required>
          <el-input v-model="form.groupCode" placeholder="请输入灯组编号" />
        </el-form-item>
        <el-form-item label="功率(W)" required>
          <el-input-number v-model="form.power" :min="1" :max="1000" />
        </el-form-item>
        <el-form-item label="所属分区" required>
          <el-select v-model="form.zoneId" placeholder="请选择分区">
            <el-option
              v-for="zone in zoneList"
              :key="zone.id"
              :value="zone.id"
              :label="zone.zoneName"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="安装位置">
          <el-input v-model="form.installationLocation" placeholder="请输入安装位置" />
        </el-form-item>
        <el-form-item label="启用状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <ArchiveDrawer v-model="archiveVisible" :light-group-id="archiveLightId" @archived="loadTable" />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { lightGroupApi, zoneApi } from '@/api'
import ArchiveDrawer from '@/components/ArchiveDrawer.vue'

const tableData = ref([])
const zoneList = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('新增灯组')
const selectedIds = ref([])

const archiveVisible = ref(false)
const archiveLightId = ref(null)

const archiveStatusOptions = [
  { value: 'ACTIVE', label: '正常' },
  { value: 'PENDING_REVIEW', label: '待复核' },
  { value: 'RETURNED', label: '已退回' },
  { value: 'ARCHIVED', label: '已归档' }
]
const archiveStatusText = (s) => ({
  ACTIVE: '正常', PENDING_REVIEW: '待复核', RETURNED: '已退回', ARCHIVED: '已归档'
}[s || 'ACTIVE'])
const archiveTagType = (s) => ({
  ACTIVE: 'info', PENDING_REVIEW: 'warning', RETURNED: 'danger', ARCHIVED: 'success'
}[s || 'ACTIVE'])

const filterForm = reactive({
  zoneId: null,
  groupCode: '',
  status: null,
  archiveStatus: ''
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const form = reactive({
  id: null,
  groupCode: '',
  power: 100,
  zoneId: null,
  installationLocation: '',
  status: 1
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
    groupCode: '',
    status: null,
    archiveStatus: ''
  })
  pagination.pageNum = 1
  loadTable()
}

const handlePageChange = () => {
  loadTable()
}

// 已归档灯组不可被勾选用于批量划转/导出操作（保留只读查看）
const canSelect = (row) => row.archiveStatus !== 'ARCHIVED'

const handleSelectionChange = (val) => {
  selectedIds.value = val.map(item => item.id)
}

const openArchive = (row) => {
  archiveLightId.value = row.id
  archiveVisible.value = true
}

const handleAdd = () => {
  dialogTitle.value = '新增灯组'
  Object.assign(form, {
    id: null,
    groupCode: '',
    power: 100,
    zoneId: null,
    installationLocation: '',
    status: 1
  })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  if (row.archiveStatus === 'ARCHIVED') {
    ElMessage.warning('该灯组已归档，记录只读，不能编辑')
    return
  }
  dialogTitle.value = '编辑灯组'
  Object.assign(form, {
    id: row.id,
    groupCode: row.groupCode,
    power: row.power,
    zoneId: row.zoneId,
    installationLocation: row.installationLocation,
    status: row.status
  })
  dialogVisible.value = true
}

const handleDelete = async (row) => {
  if (row.archiveStatus === 'ARCHIVED') {
    ElMessage.warning('该灯组已归档，归档记录不可删除')
    return
  }
  try {
    await ElMessageBox.confirm('确定删除该灯组吗？', '提示', {
      type: 'warning'
    })
    await lightGroupApi.delete(row.id)
    ElMessage.success('删除成功')
    loadTable()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

const handleSubmit = async () => {
  if (!form.groupCode || !form.power || !form.zoneId) {
    ElMessage.warning('请填写必填字段')
    return
  }

  try {
    if (form.id) {
      await lightGroupApi.update(form)
      ElMessage.success('更新成功')
    } else {
      await lightGroupApi.create(form)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadTable()
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  }
}

const handleExport = async () => {
  try {
    const res = await lightGroupApi.exportSelected(selectedIds.value)
    downloadFile(res, '灯组资产.xlsx')
  } catch (error) {
    ElMessage.error('导出失败')
  }
}

const handleExportZone = async () => {
  try {
    const res = await lightGroupApi.exportByZoneId(filterForm.zoneId)
    downloadFile(res, '分区灯组资产.xlsx')
  } catch (error) {
    ElMessage.error('导出失败')
  }
}

const downloadFile = (blob, filename) => {
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  window.URL.revokeObjectURL(url)
  document.body.removeChild(a)
}

onMounted(() => {
  loadZones()
  loadTable()
})
</script>

<style lang="scss" scoped>
.light-group-container {
  height: 100%;

  .card-header {
    display: flex;
    gap: 10px;
    margin-bottom: 20px;
  }

  .search-bar {
    margin-bottom: 20px;
  }

  .pagination {
    margin-top: 20px;
    text-align: right;
  }
}
</style>
