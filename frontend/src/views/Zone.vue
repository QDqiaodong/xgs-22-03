<template>
  <div class="zone-container">
    <el-card>
      <div class="card-header">
        <el-button type="primary" @click="handleAdd">新增分区</el-button>
        <el-button :icon="Refresh" circle @click="loadTree" title="重新加载最新分区树" />
      </div>

      <div class="tree-container">
        <el-tree
          :data="treeData"
          :props="treeProps"
          :default-expand-all="true"
          node-key="id"
          @node-click="handleNodeClick"
          @node-contextmenu="handleContextMenu"
        >
          <template #default="{ data }">
            <span class="tree-node">
              <span>{{ data.zoneName }}</span>
              <span class="node-code">({{ data.zoneCode }})</span>
              <span class="node-level">L{{ data.level }}</span>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="420px" @closed="onDialogClosed">
      <el-form :model="form" label-width="80px">
        <el-form-item label="分区编码" required>
          <el-input v-model="form.zoneCode" placeholder="请输入分区编码" />
        </el-form-item>
        <el-form-item label="分区名称" required>
          <el-input v-model="form.zoneName" placeholder="请输入分区名称" />
        </el-form-item>
        <el-form-item label="上级分区">
          <el-tree-select
            v-model="form.parentId"
            :data="parentOptions"
            :props="treeProps"
            :render-after-expand="false"
            check-strictly
            node-key="id"
            default-expand-all
            placeholder="请选择上级分区"
            clearable
            style="width: 100%"
          />
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
        <el-alert
          v-if="form.id"
          type="info"
          :closable="false"
          title="换父级会整体移动该分区的全部子树，灯组仍归属各自原分区，不会随目录划转"
        />
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
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

    <!-- 删除阻断：逐条列出具体冲突分区 / 灯组 / 未核销流水 -->
    <el-dialog v-model="blockDialogVisible" title="无法删除该分区" width="640px">
      <el-alert
        :title="blockMessage"
        type="error"
        :closable="false"
        show-icon
        style="margin-bottom: 12px"
      />
      <el-table :data="deleteBlocks" border size="small" max-height="360">
        <el-table-column label="阻断类型" width="120">
          <template #default="{ row }">
            <el-tag :type="blockTagType(row.type)" size="small">{{ row.typeText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="冲突分区" width="150">
          <template #default="{ row }">{{ row.zoneName || '-' }}</template>
        </el-table-column>
        <el-table-column label="编号" width="150">
          <template #default="{ row }">{{ row.refNo || '-' }}</template>
        </el-table-column>
        <el-table-column label="阻断原因" prop="detail" />
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
import { Refresh } from '@element-plus/icons-vue'
import { zoneApi } from '@/api'

const treeData = ref([])
const dialogVisible = ref(false)
const blockDialogVisible = ref(false)
const blockMessage = ref('')
const deleteBlocks = ref([])
const dialogTitle = ref('新增分区')
const popoverVisible = ref(false)
const popoverRef = ref(null)
const selectedNode = ref(null)
const submitting = ref(false)

const form = reactive({
  id: null,
  zoneCode: '',
  zoneName: '',
  parentId: 0,
  sortOrder: 0,
  status: 1,
  version: 0
})

const treeProps = {
  children: 'children',
  label: 'zoneName'
}

/** 递归收集节点自身与全部后代 id（环防御） */
const collectSubtreeIds = (node, acc = new Set()) => {
  if (!node || acc.has(node.id)) return acc
  acc.add(node.id)
  if (Array.isArray(node.children)) {
    node.children.forEach(child => collectSubtreeIds(child, acc))
  }
  return acc
}

const findNodeById = (id) => {
  let found = null
  const walk = (nodes) => {
    if (found) return
    for (const node of nodes) {
      if (node.id === id) {
        found = node
        return
      }
      if (Array.isArray(node.children)) walk(node.children)
    }
  }
  walk(treeData.value)
  return found
}

/**
 * 上级候选树：编辑时剪掉“被编辑节点自身及其全部后代”，
 * 让自挂 / 挂到任意后代的成环操作在界面上也无法选择（后端仍做权威校验）。
 */
const parentOptions = computed(() => {
  if (!form.id) return treeData.value
  const blockedIds = (() => {
    const self = findNodeById(form.id)
    return self ? collectSubtreeIds(self) : new Set([form.id])
  })()
  const prune = (nodes) => nodes
    .filter(node => !blockedIds.has(node.id))
    .map(node => {
      const clone = { ...node }
      if (Array.isArray(node.children) && node.children.length) {
        clone.children = prune(node.children)
      } else {
        clone.children = null
      }
      return clone
    })
  return prune(treeData.value)
})

const loadTree = async () => {
  try {
    const res = await zoneApi.getTree()
    treeData.value = res.data || []
  } catch (error) {
    ElMessage.error('加载分区树失败')
  }
}

const resetForm = () => {
  Object.assign(form, {
    id: null,
    zoneCode: '',
    zoneName: '',
    parentId: 0,
    sortOrder: 0,
    status: 1,
    version: 0
  })
}

const handleAdd = () => {
  dialogTitle.value = '新增分区'
  resetForm()
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
    sortOrder: selectedNode.value.sortOrder ?? 0,
    status: selectedNode.value.status ?? 1,
    version: selectedNode.value.version ?? 0
  })
  dialogVisible.value = true
}

const blockTagType = (type) => {
  if (type === 'CHILD_ZONE') return 'warning'
  if (type === 'LIGHT_GROUP') return 'danger'
  return 'info'
}

const handleDelete = async () => {
  popoverVisible.value = false
  const target = selectedNode.value
  try {
    await ElMessageBox.confirm(
      `确定删除分区「${target.zoneName}」吗？仅空的叶子分区可以删除。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  try {
    await zoneApi.delete(target.id)
    ElMessage.success('删除成功')
    await loadTree()
  } catch (error) {
    if (error.code === 412 && Array.isArray(error.latestData) && error.latestData.length) {
      // 引用阻断：列出全部阻断原因（子分区 / 灯组 / 未核销流水），不做静默删除
      blockMessage.value = error.message || '该分区存在阻断引用，不能删除'
      deleteBlocks.value = error.latestData
      blockDialogVisible.value = true
    } else {
      ElMessage.error(error.message || '删除失败')
    }
    await loadTree()
  }
}

const submitOnce = async (payload) => {
  if (payload.id) {
    return await zoneApi.update(payload)
  }
  return await zoneApi.create(payload)
}

const handleSubmit = async () => {
  if (!form.zoneCode || !form.zoneName) {
    ElMessage.warning('请填写必填字段')
    return
  }

  // 前端先按当前树做一层环拦截，给出即时反馈；后端始终基于最新层级做权威校验
  if (form.id) {
    const self = findNodeById(form.id)
    if (form.parentId === form.id) {
      ElMessage.error('上级分区不能选择分区自身，否则会形成循环层级')
      return
    }
    if (self) {
      const descendantIds = collectSubtreeIds(self)
      descendantIds.delete(form.id)
      if (form.parentId && descendantIds.has(form.parentId)) {
        const target = findNodeById(form.parentId)
        ElMessage.error(`不能挂到下级分区「${target?.zoneName || ''}」下，否则会形成循环层级`)
        return
      }
    }
  }

  const payload = {
    id: form.id,
    zoneCode: form.zoneCode,
    zoneName: form.zoneName,
    parentId: form.parentId || 0,
    sortOrder: form.sortOrder,
    status: form.status
  }
  if (form.id) payload.version = form.version

  submitting.value = true
  try {
    await submitOnce(payload)
    ElMessage.success(form.id ? '更新成功' : '创建成功')
    dialogVisible.value = false
    await loadTree()
  } catch (error) {
    if (error.code === 409) {
      // 版本冲突：后确认者不覆盖先确认结果，刷新为最新父子关系后可选择重试
      const payloadData = error.latestData
      await loadTree()
      if (payloadData?.latestZone) {
        const latest = payloadData.latestZone
        ElMessageBox.alert(
          `${error.message || '该分区刚被其他管理员调整过，您的确认未生效。'}\n\n`
          + `最新上级：${latest.parentId ? (findNodeById(latest.parentId)?.zoneName || latest.parentId) : '无（顶级分区）'}\n`
          + `最新层级：L${latest.level}，最新版本：v${latest.version}`,
          '版本冲突，请核对最新层级',
          { type: 'warning', confirmButtonText: '我已了解，按最新树重试' }
        ).then(() => {
          const fresh = findNodeById(latest.id)
          if (fresh) {
            selectedNode.value = fresh
            dialogTitle.value = '编辑分区'
            Object.assign(form, {
              id: fresh.id,
              zoneCode: fresh.zoneCode,
              zoneName: fresh.zoneName,
              parentId: fresh.parentId || 0,
              sortOrder: fresh.sortOrder ?? 0,
              status: fresh.status ?? 1,
              version: fresh.version ?? 0
            })
            dialogVisible.value = true
          }
        }).catch(() => {})
      } else {
        ElMessage.error(error.message || '分区已被其他管理员调整，请刷新后重试')
      }
    } else if (error.code === 400) {
      // 自挂 / 挂到后代等层级冲突，消息中含具体冲突分区
      ElMessage.error(error.message || '层级调整不合法')
      await loadTree()
    } else {
      ElMessage.error(error.message || '操作失败')
    }
  } finally {
    submitting.value = false
  }
}

const onDialogClosed = () => {
  resetForm()
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
    display: flex;
    gap: 12px;
    align-items: center;
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

    .node-level {
      margin-left: 8px;
      font-size: 12px;
      color: #b0914a;
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
