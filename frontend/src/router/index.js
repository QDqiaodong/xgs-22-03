import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/light-group'
  },
  {
    path: '/light-group',
    name: 'LightGroup',
    component: () => import('../views/LightGroup.vue')
  },
  {
    path: '/zone',
    name: 'Zone',
    component: () => import('../views/Zone.vue')
  },
  {
    path: '/transfer',
    name: 'Transfer',
    component: () => import('../views/Transfer.vue')
  },
  {
    path: '/ledger',
    name: 'Ledger',
    component: () => import('../views/Ledger.vue')
  },
  {
    path: '/inspection',
    name: 'Inspection',
    component: () => import('../views/Inspection.vue')
  },
  {
    path: '/inspection-anomaly',
    name: 'InspectionAnomaly',
    component: () => import('../views/InspectionAnomaly.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router