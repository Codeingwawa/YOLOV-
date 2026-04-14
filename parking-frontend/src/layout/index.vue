<template>
  <el-container class="layout-container">
    <el-aside :width="isCollapse ? '64px' : '220px'" class="sidebar">
      <div class="logo">
        <div class="logo-icon">
          <el-icon :size="24"><MapLocation /></el-icon>
        </div>
        <span v-show="!isCollapse" class="logo-text">智慧停车</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :collapse-transition="false"
        router
        class="sidebar-menu"
      >
        <el-menu-item index="/dashboard">
          <el-icon><DataLine /></el-icon>
          <template #title>仪表盘</template>
        </el-menu-item>
        <el-menu-item index="/vehicle">
          <el-icon><Van /></el-icon>
          <template #title>车辆管理</template>
        </el-menu-item>
        <el-menu-item index="/parking-space">
          <el-icon><Location /></el-icon>
          <template #title>车位管理</template>
        </el-menu-item>
        <el-menu-item index="/monitor">
          <el-icon><VideoCamera /></el-icon>
          <template #title>实时监控</template>
        </el-menu-item>
        <el-menu-item index="/statistics">
          <el-icon><TrendCharts /></el-icon>
          <template #title>统计报表</template>
        </el-menu-item>
      </el-menu>
      
      <div class="sidebar-footer" v-show="!isCollapse">
        <div class="parking-status">
          <div class="status-item">
            <span class="label">空闲车位</span>
            <span class="value available">{{ parkingStats.available }}</span>
          </div>
          <div class="status-item">
            <span class="label">已占用</span>
            <span class="value occupied">{{ parkingStats.occupied }}</span>
          </div>
        </div>
      </div>
    </el-aside>
    
    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-icon 
            class="collapse-btn" 
            @click="isCollapse = !isCollapse"
          >
            <component :is="isCollapse ? 'Expand' : 'Fold'" />
          </el-icon>
          <div class="breadcrumb">
            <el-breadcrumb separator="/">
              <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
              <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
            </el-breadcrumb>
          </div>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="32" class="user-avatar">
                {{ userInfo?.realName?.charAt(0) || 'A' }}
              </el-avatar>
              <span class="username">{{ userInfo?.realName || '管理员' }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>
                  个人设置
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      
      <el-main class="main-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
    
    <el-dialog v-model="profileDialogVisible" title="个人设置" width="450px">
      <el-form :model="profileForm" label-width="100px">
        <el-form-item label="用户名">
          <el-input :value="userInfo?.username" disabled />
        </el-form-item>
        <el-form-item label="角色">
          <el-tag type="primary">{{ userInfo?.role === 'ADMIN' ? '管理员' : '用户' }}</el-tag>
        </el-form-item>
        <el-form-item label="真实姓名">
          <el-input v-model="profileForm.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-divider>修改密码</el-divider>
        <el-form-item label="当前密码">
          <el-input v-model="profileForm.oldPassword" type="password" show-password placeholder="不修改请留空" />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="profileForm.newPassword" type="password" show-password placeholder="不修改请留空" />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="profileForm.confirmPassword" type="password" show-password placeholder="不修改请留空" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="profileDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveProfile" :loading="profileSaving">保存</el-button>
      </template>
    </el-dialog>
  </el-container>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown, User, SwitchButton, MapLocation } from '@element-plus/icons-vue'
import { removeToken, getUserInfo, setUserInfo } from '@/api/auth'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()

const isCollapse = ref(false)
const activeMenu = computed(() => route.path)
const currentTitle = computed(() => route.meta?.title || '')
const userInfo = ref(getUserInfo())

const profileDialogVisible = ref(false)
const profileSaving = ref(false)
const profileForm = reactive({
  realName: '',
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const parkingStats = ref({
  available: 0,
  occupied: 0
})

const loadParkingStats = async () => {
  try {
    const data = await request.get('/parking-space/list')
    const spaces = data || []
    parkingStats.value.available = spaces.filter(s => s.status === 'AVAILABLE').length
    parkingStats.value.occupied = spaces.filter(s => s.status === 'OCCUPIED').length
  } catch (error) {
    console.error('Failed to load parking stats:', error)
  }
}

const handleCommand = (command) => {
  if (command === 'logout') {
    handleLogout()
  } else if (command === 'profile') {
    profileForm.realName = userInfo.value?.realName || ''
    profileForm.oldPassword = ''
    profileForm.newPassword = ''
    profileForm.confirmPassword = ''
    profileDialogVisible.value = true
  }
}

const saveProfile = async () => {
  if (profileForm.newPassword && profileForm.newPassword !== profileForm.confirmPassword) {
    ElMessage.error('两次输入的密码不一致')
    return
  }
  
  profileSaving.value = true
  try {
    const data = { realName: profileForm.realName }
    if (profileForm.newPassword) {
      data.oldPassword = profileForm.oldPassword
      data.newPassword = profileForm.newPassword
    }
    
    await request.put('/auth/profile', data)
    
    userInfo.value = { ...userInfo.value, realName: profileForm.realName }
    setUserInfo(userInfo.value)
    
    ElMessage.success('保存成功')
    profileDialogVisible.value = false
  } catch (error) {
    ElMessage.error(error.response?.data?.error || '保存失败')
  } finally {
    profileSaving.value = false
  }
}

const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    removeToken()
    ElMessage.success('已退出登录')
    router.push('/login')
  }).catch(() => {})
}

onMounted(() => {
  loadParkingStats()
  setInterval(loadParkingStats, 30000)
})
</script>

<style lang="scss" scoped>
.layout-container {
  height: 100vh;
}

.sidebar {
  background: linear-gradient(180deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  transition: width 0.3s;
  display: flex;
  flex-direction: column;
  
  .logo {
    height: 64px;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.08);
    
    .logo-icon {
      width: 40px;
      height: 40px;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      border-radius: 10px;
      display: flex;
      align-items: center;
      justify-content: center;
      
      .el-icon {
        color: white;
      }
    }
    
    .logo-text {
      font-size: 18px;
      font-weight: 600;
      color: white;
      letter-spacing: 2px;
    }
  }
  
  .sidebar-menu {
    border: none;
    background: transparent;
    flex: 1;
    
    :deep(.el-menu-item) {
      color: rgba(255, 255, 255, 0.7);
      margin: 4px 8px;
      border-radius: 8px;
      
      &:hover {
        background: rgba(102, 126, 234, 0.2);
        color: white;
      }
      
      &.is-active {
        background: linear-gradient(135deg, rgba(102, 126, 234, 0.3) 0%, rgba(118, 75, 162, 0.3) 100%);
        color: white;
        box-shadow: 0 4px 15px rgba(102, 126, 234, 0.3);
      }
      
      .el-icon {
        font-size: 18px;
      }
    }
  }
  
  .sidebar-footer {
    padding: 16px;
    border-top: 1px solid rgba(255, 255, 255, 0.08);
    
    .parking-status {
      background: rgba(255, 255, 255, 0.05);
      border-radius: 12px;
      padding: 12px;
      
      .status-item {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 8px 0;
        
        &:first-child {
          border-bottom: 1px solid rgba(255, 255, 255, 0.05);
        }
        
        .label {
          color: rgba(255, 255, 255, 0.6);
          font-size: 13px;
        }
        
        .value {
          font-size: 18px;
          font-weight: 600;
          
          &.available {
            color: #67c23a;
          }
          
          &.occupied {
            color: #f56c6c;
          }
        }
      }
    }
  }
}

.header {
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 64px;
  
  .header-left {
    display: flex;
    align-items: center;
    gap: 20px;
    
    .collapse-btn {
      font-size: 20px;
      cursor: pointer;
      color: #666;
      padding: 8px;
      border-radius: 8px;
      transition: all 0.3s;
      
      &:hover {
        background: #f5f7fa;
        color: #667eea;
      }
    }
    
    .breadcrumb {
      :deep(.el-breadcrumb__item) {
        .el-breadcrumb__inner {
          color: #666;
          
          &.is-link:hover {
            color: #667eea;
          }
        }
        
        &:last-child .el-breadcrumb__inner {
          color: #333;
          font-weight: 500;
        }
      }
    }
  }
  
  .header-right {
    .user-info {
      display: flex;
      align-items: center;
      gap: 10px;
      cursor: pointer;
      padding: 6px 12px;
      border-radius: 8px;
      transition: all 0.3s;
      
      &:hover {
        background: #f5f7fa;
      }
      
      .user-avatar {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: white;
        font-weight: 500;
      }
      
      .username {
        color: #333;
        font-weight: 500;
      }
      
      .el-icon {
        color: #999;
        font-size: 12px;
      }
    }
  }
}

.main-content {
  background: #f0f2f5;
  padding: 24px;
  min-height: calc(100vh - 64px);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
