<template>
  <el-container class="app-container">
    <el-aside width="200px" class="aside">
      <div class="logo">
        <span>灯组管理系统</span>
      </div>
      <el-menu :default-active="activeMenu" mode="vertical" @select="handleMenuSelect">
        <el-menu-item index="/light-group">
          <el-icon><component :is="Icons.Lightbulb" /></el-icon>
          <span>灯组管理</span>
        </el-menu-item>
        <el-menu-item index="/zone">
          <el-icon><component :is="Icons.Location" /></el-icon>
          <span>分区管理</span>
        </el-menu-item>
        <el-menu-item index="/transfer">
          <el-icon><component :is="Icons.Swap" /></el-icon>
          <span>批量划转</span>
        </el-menu-item>
        <el-menu-item index="/ledger">
          <el-icon><component :is="Icons.Document" /></el-icon>
          <span>操作台账</span>
        </el-menu-item>
        <el-menu-item index="/inspection">
          <el-icon><component :is="Icons.Inspections" /></el-icon>
          <span>灯组巡检</span>
        </el-menu-item>
        <el-menu-item index="/inspection-anomaly">
          <el-icon><component :is="Icons.Warning" /></el-icon>
          <span>异常处置</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="header-title">
          {{ pageTitle }}
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  ReadingLamp as IconLightbulb,
  Location as IconLocation,
  RefreshLeft as IconSwap,
  Document as IconDocument,
  Finished as IconInspections,
  Warning as IconWarning
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

const Icons = {
  Lightbulb: IconLightbulb,
  Location: IconLocation,
  Swap: IconSwap,
  Document: IconDocument,
  Inspections: IconInspections,
  Warning: IconWarning
}

const activeMenu = computed(() => route.path)

const pageTitleMap = {
  '/light-group': '灯组管理',
  '/zone': '车库分区管理',
  '/transfer': '批量划转',
  '/ledger': '操作台账',
  '/inspection': '灯组巡检',
  '/inspection-anomaly': '巡检异常处置'
}

const pageTitle = computed(() => pageTitleMap[route.path] || '灯组管理系统')

const handleMenuSelect = (index) => {
  router.push(index)
}

onMounted(() => {})
</script>

<style lang="scss">
.app-container {
  height: 100vh;
}

.aside {
  background-color: #2a3f5f;
  
  .logo {
    padding: 20px;
    text-align: center;
    color: #fff;
    font-size: 16px;
    font-weight: bold;
    border-bottom: 1px solid #3a4f6f;
  }
  
  :deep(.el-menu) {
    border-right: none;
    background-color: #2a3f5f;
    
    :deep(.el-menu-item) {
      color: #b0c4de;
      
      &:hover, &.is-active {
        background-color: #1a2f4f;
        color: #fff;
      }
    }
  }
}

.header {
  background-color: #fff;
  border-bottom: 1px solid #e0e0e0;
  padding: 0 20px;
  
  .header-title {
    font-size: 18px;
    font-weight: bold;
    color: #333;
    line-height: 60px;
  }
}

.main {
  background-color: #f5f5f5;
  padding: 20px;
}
</style>