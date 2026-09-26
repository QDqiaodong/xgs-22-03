<template>
  <div class="zone-container">
    <el-card>
      <div class="card-header">
        <el-button type="primary" @click="handleAdd">新增分区</el-button>
      </div>
      
      <div class="tree-container">
        <el-tree
          :data="treeData"
          :props="treeProps"
          :default-expand-all="true"
          @node-click="handleNodeClick"
          @node-contextmenu="handleContextMenu"
        >
          <template #default="{ node, data }">
            <span class="tree-node">
              <span>{{ data.zoneName }}</span>
              <span class="node-code">({{ data.zoneCode }})</span>
              <span class="node-counts">
                灯组 {{ data.lightCount ?? 0 }}
                <el-tag v-if="data.archivedLightCount > 0" type="success" size="small" effect="plain" class="archived-tag">
                  已归档 {{ data.archivedLightCount }}
                </el-tag>
              </span>
            </span>
          </template>
        </el-tree>
      </div>
    </el-card>
    
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="400px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="分区编码" required>
          <el-input v-model="form.zoneCode" placeholder="请输入分区编码" />
        </el-form-item>
        <el-form-item label="分区名称" required>
          <el-input v-model="form.zoneName" placeholder="请输入分区名称" />
        </el-form-item>
        <el-form-item label="上级分区">
          <el-select v-model="form.parentId" placeholder="请选择上级分区">
            <el-option :value="0" label="无（顶级分区）" />
            <el-option 
              v-for="zone in flatZones" 
              :key="zone.id" 
              :value="zone.id" 
              :label="zone.zoneName"
              :disabled="form.id === zone.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
    
    <el-popover
      v-model="popoverVisible"
      :ref="popoverRef"
      trigger="manual"
      placement="bottom-start"
    >
      <el-button type="text" @click="handleEdit">编辑</el-button>
      <el-button type="text" @click="handleDelete">删除</el-button>
    </el-popover>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { zoneApi } from '@/api'

const treeData = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('新增分区')
const popoverVisible = ref(false)
const popoverRef = ref(null)
const selectedNode = ref(null)

const form = reactive({
  id: null,
  zoneCode: '',
  zoneName: '',
  parentId: 0,
  sortOrder: 0,
  status: 1
})

const treeProps = {
  children: 'children',
  label: 'zoneName'
}

const flatZones = computed(() => {
  const result = []
  const flatten = (nodes) => {
    nodes.forEach(node => {
      result.push(node)
      if (node.children) {
        flatten(node.children)
      }
    })
  }
  flatten(treeData.value)
  return result
})

const loadTree = async () => {
  try {
    const res = await zoneApi.getTree()
    treeData.value = res.data
  } catch (error) {
    ElMessage.error('加载分区树失败')
  }
}

const handleAdd = () => {
  dialogTitle.value = '新增分区'
  Object.assign(form, {
    id: null,
    zoneCode: '',
    zoneName: '',
    parentId: 0,
    sortOrder: 0,
    status: 1
  })
  dialogVisible.value = true
}

const handleNodeClick = (data) => {
  selectedNode.value = data
}

const handleContextMenu = (event, data) => {
  event.preventDefault()
  selectedNode.value = data
  popoverVisible.value = true
  popoverRef.value?.show(event)
}

const handleEdit = () => {
  popoverVisible.value = false
  dialogTitle.value = '编辑分区'
  Object.assign(form, {
    id: selectedNode.value.id,
    zoneCode: selectedNode.value.zoneCode,
    zoneName: selectedNode.value.zoneName,
    parentId: selectedNode.value.parentId || 0,
    sortOrder: selectedNode.value.sortOrder || 0,
    status: selectedNode.value.status || 1
  })
  dialogVisible.value = true
}

const handleDelete = async () => {
  popoverVisible.value = false
  try {
    await ElMessageBox.confirm('确定删除该分区吗？', '提示', {
      type: 'warning'
    })
    await zoneApi.delete(selectedNode.value.id)
    ElMessage.success('删除成功')
    loadTree()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const handleSubmit = async () => {
  if (!form.zoneCode || !form.zoneName) {
    ElMessage.warning('请填写必填字段')
    return
  }
  
  try {
    if (form.id) {
      await zoneApi.update(form)
      ElMessage.success('更新成功')
    } else {
      await zoneApi.create(form)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadTree()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

onMounted(() => {
  loadTree()
})
</script>

<style lang="scss" scoped>
.zone-container {
  height: 100%;
  
  .card-header {
    margin-bottom: 20px;
  }
  
  .tree-container {
    height: calc(100% - 60px);
    overflow-y: auto;
  }
  
  .tree-node {
    display: flex;
    align-items: center;

    .node-code {
      margin-left: 8px;
      font-size: 12px;
      color: #999;
    }

    .node-counts {
      margin-left: 12px;
      font-size: 12px;
      color: #606266;

      .archived-tag {
        margin-left: 6px;
      }
    }
  }
}
</style>