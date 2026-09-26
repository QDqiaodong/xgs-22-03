<template>
  <div class="zone-container">
    <el-card>
      <div class="card-header">
        <el-button type="primary" @click="handleAdd">新增分区</el-button>
        <el-button @click="loadTree">刷新树</el-button>
      </div>

      <div class="tree-container">
        <el-tree
          :data="treeData"
          :props="treeProps"
          :default-expand-all="true"
          node-key="id"
          @node-click="handleNodeClick"
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
              <span class="node-actions">
                <el-button link type="primary" size="small" @click.stop="openEdit(data)">编辑</el-button>
                <el-button link type="danger" size="small" @click.stop="handleDelete(data)">删除</el-button>
              </span>
            </span>
          </template>
        </el-tree>
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="420px" @closed="handleDialogClosed">
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
              :disabled="isParentOptionDisabled(zone.id)"
            >
              <span :class="{ 'option-disabled': isParentOptionDisabled(zone.id) }">
                {{ zone.zoneName }}（{{ zone.zoneCode }}）{{ optionSuffix(zone.id) }}
              </span>
            </el-option>
          </el-select>
          <div v-if="form.id && form.parentId === form.id" class="form-tip error-tip">
            上级分区不能选择自身
          </div>
          <div v-else-if="form.id && descendantIds.has(form.parentId)" class="form-tip error-tip">
            不能挂到自己的下级分区，否则会形成循环层级
          </div>
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
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 删除阻断原因：子分区 / 灯组引用 / 未核销划转流水逐条展示 -->
    <el-dialog v-model="blockDialogVisible" title="删除被阻断" width="600px">
      <el-alert
        :title="blockMessage"
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom: 12px"
      />
      <el-table :data="blockList" size="small" max-height="360" border>
        <el-table-column label="阻断类型" prop="typeText" width="130" />
        <el-table-column label="所在分区" prop="zoneName" width="150" />
        <el-table-column label="关联编号" prop="refNo" width="150" />
        <el-table-column label="说明" prop="detail" />
      </el-table>
      <template #footer>
        <el-button type="primary" @click="blockDialogVisible = false">我知道了</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { zoneApi } from '@/api'

const treeData = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('新增分区')
const selectedNode = ref(null)
const submitting = ref(false)

// 删除阻断明细
const blockDialogVisible = ref(false)
const blockMessage = ref('')
const blockList = ref([])

const form = reactive({
  id: null,
  zoneCode: '',
  zoneName: '',
  parentId: 0,
  sortOrder: 0,
  status: 1,
  version: null
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

/** 当前编辑节点自身及其全部后代 id：这些节点禁止选为上级（自挂/挂后代会成环） */
const descendantIds = computed(() => {
  const set = new Set()
  if (!form.id) {
    return set
  }
  const find = (nodes) => {
    for (const node of nodes) {
      if (node.id === form.id) {
        collect(node)
        return true
      }
      if (node.children && find(node.children)) {
        return true
      }
    }
    return false
  }
  const collect = (node) => {
    if (!node.children) {
      return
    }
    node.children.forEach(child => {
      set.add(child.id)
      collect(child)
    })
  }
  find(treeData.value)
  return set
})

const isParentOptionDisabled = (zoneId) => {
  if (!form.id) {
    return false
  }
  return zoneId === form.id || descendantIds.value.has(zoneId)
}

const optionSuffix = (zoneId) => {
  if (form.id && zoneId === form.id) {
    return '（自身，不可选）'
  }
  if (descendantIds.value.has(zoneId)) {
    return '（下级分区，不可选）'
  }
  return ''
}

const loadTree = async () => {
  try {
    const res = await zoneApi.getTree()
    treeData.value = res.data
  } catch (error) {
    ElMessage.error('加载分区树失败')
  }
}

const findZoneById = (id) => flatZones.value.find(z => z.id === id) || null

const handleAdd = () => {
  dialogTitle.value = '新增分区'
  Object.assign(form, {
    id: null,
    zoneCode: '',
    zoneName: '',
    parentId: 0,
    sortOrder: 0,
    status: 1,
    version: null
  })
  dialogVisible.value = true
}

const handleNodeClick = (data) => {
  selectedNode.value = data
}

const openEdit = (data) => {
  selectedNode.value = data
  dialogTitle.value = '编辑分区'
  Object.assign(form, {
    id: data.id,
    zoneCode: data.zoneCode,
    zoneName: data.zoneName,
    parentId: data.parentId || 0,
    sortOrder: data.sortOrder || 0,
    status: data.status ?? 1,
    version: data.version ?? 0
  })
  dialogVisible.value = true
}

const handleDialogClosed = () => {
  submitting.value = false
}

const handleDelete = async (data) => {
  selectedNode.value = data
  try {
    await ElMessageBox.confirm(
      `确定删除分区「${data.zoneName}」吗？仅有不含子分区、灯组与未核销流水的空叶子分区可以删除。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  try {
    await zoneApi.delete(data.id)
    ElMessage.success('删除成功')
    loadTree()
  } catch (error) {
    if (error.code === 412 && Array.isArray(error.latestData)) {
      // 引用删除保护：逐条列出阻断原因
      blockMessage.value = error.message || '该分区不是空叶子分区，不能删除'
      blockList.value = error.latestData
      blockDialogVisible.value = true
    } else {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

const clientSideCycleCheck = () => {
  if (!form.id) {
    return null
  }
  if (form.parentId === form.id) {
    return '上级分区不能选择自身，自挂会形成循环层级'
  }
  if (descendantIds.value.has(form.parentId)) {
    const target = findZoneById(form.parentId)
    return `不能把该分区挂到它自己的下级分区「${target ? target.zoneName : '#' + form.parentId}」下面，该调整会形成循环层级`
  }
  return null
}

const handleSubmit = async () => {
  if (!form.zoneCode || !form.zoneName) {
    ElMessage.warning('请填写必填字段')
    return
  }

  // 前端先按当前树拦一道自挂/挂后代，后端仍会基于最新层级强校验
  const cycleError = clientSideCycleCheck()
  if (cycleError) {
    ElMessage.error(cycleError)
    return
  }

  submitting.value = true
  try {
    if (form.id) {
      await zoneApi.update({ ...form })
      ElMessage.success('更新成功')
    } else {
      await zoneApi.create({ ...form })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadTree()
  } catch (error) {
    if (error.code === 409 && error.latestData) {
      // 版本冲突：其他人先确认过，本次提交未生效。
      // 先重读最新树展示最新父子关系，再由管理员基于新版本决定是否重试。
      const latest = error.latestData
      await loadTree()
      let retry = false
      if (form.id && latest && latest.id === form.id) {
        const latestParentName = !latest.parentId
          ? '无（顶级分区）'
          : (findZoneById(latest.parentId)?.zoneName || `#${latest.parentId}`)
        try {
          await ElMessageBox.confirm(
            `${error.message || '该分区刚被其他管理员调整过，您的修改未生效。'}\n\n` +
            `最新父子关系：${latest.zoneName} 的上级为「${latestParentName}」。\n\n` +
            '点击「仍按我的选择重试」将基于最新层级再次提交；点击「取消」放弃本次修改。',
            '版本冲突，请刷新确认',
            { confirmButtonText: '仍按我的选择重试', cancelButtonText: '取消', type: 'warning' }
          )
          retry = true
        } catch (e) {
          retry = false
        }
        if (retry) {
          // 保留管理员刚才的父级选择，仅同步最新版本号与最新基础字段后重试一次
          Object.assign(form, {
            zoneCode: latest.zoneCode,
            zoneName: latest.zoneName,
            parentId: form.parentId,
            sortOrder: latest.sortOrder ?? form.sortOrder,
            status: latest.status ?? form.status,
            version: latest.version
          })
          dialogVisible.value = true
        } else {
          dialogVisible.value = false
        }
      } else {
        ElMessage.error(error.message || '分区已被他人调整，请基于最新层级重试')
        dialogVisible.value = false
      }
    } else {
      ElMessage.error(error.message || '操作失败')
    }
  } finally {
    submitting.value = false
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

    .node-actions {
      margin-left: 12px;
    }
  }

  .form-tip {
    font-size: 12px;
    line-height: 1.4;
    margin-top: 4px;
  }

  .error-tip {
    color: #f56c6c;
  }

  .option-disabled {
    color: #c0c4cc;
  }
}
</style>
