import { createRouter, createWebHistory } from 'vue-router'
import Layout from '@/layout/index.vue'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', public: true }
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '仪表盘', icon: 'DataLine' }
      },
      {
        path: 'vehicle',
        name: 'Vehicle',
        component: () => import('@/views/vehicle/index.vue'),
        meta: { title: '车辆管理', icon: 'Van' }
      },
      {
        path: 'monitor',
        name: 'Monitor',
        component: () => import('@/views/monitor/index.vue'),
        meta: { title: '实时监控', icon: 'VideoCamera' }
      },
      {
        path: 'statistics',
        name: 'Statistics',
        component: () => import('@/views/statistics/index.vue'),
        meta: { title: '统计报表', icon: 'TrendCharts' }
      },
      {
        path: 'parking-space',
        name: 'ParkingSpace',
        component: () => import('@/views/parking-space/index.vue'),
        meta: { title: '车位管理', icon: 'Location' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - 智慧停车场管理系统` : '智慧停车场管理系统'
  
  const token = localStorage.getItem('token')
  const isPublic = to.meta.public
  
  if (!isPublic && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/dashboard')
  } else {
    next()
  }
})

export default router
