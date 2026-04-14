<template>
  <div class="login-container">
    <div class="login-background">
      <div class="car-animation">
        <div class="car"></div>
      </div>
      <div class="parking-lines">
        <div v-for="i in 8" :key="i" class="line"></div>
      </div>
    </div>
    
    <div class="login-box">
      <div class="login-header">
        <div class="logo-container">
          <div class="logo-icon">
            <el-icon :size="42"><MapLocation /></el-icon>
          </div>
        </div>
        <h1>智慧停车场管理系统</h1>
        <p class="subtitle">Smart Parking Management System</p>
      </div>
      
      <el-form ref="formRef" :model="form" :rules="rules" class="login-form">
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            :prefix-icon="User"
            size="large"
          />
        </el-form-item>
        
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            size="large"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            @click="handleLogin"
            class="login-btn"
          >
            <el-icon v-if="!loading"><Unlock /></el-icon>
            <span>{{ loading ? '登录中...' : '登 录' }}</span>
          </el-button>
        </el-form-item>
      </el-form>
      
      <div class="login-footer">
        <p>管理员账户: admin / admin123</p>
      </div>
    </div>
    
    <div class="floating-cars">
      <div class="floating-car car-1">🚗</div>
      <div class="floating-car car-2">🚙</div>
      <div class="floating-car car-3">🚕</div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, MapLocation, Unlock } from '@element-plus/icons-vue'
import { login, setToken, setUserInfo } from '@/api/auth'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: 'admin',
  password: 'admin123'
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  await formRef.value.validate()
  
  loading.value = true
  try {
    const response = await login({
      username: form.username,
      password: form.password
    })
    
    setToken(response.token)
    setUserInfo({
      username: response.username,
      realName: response.realName,
      role: response.role
    })
    
    ElMessage.success('登录成功，欢迎回来！')
    router.push('/dashboard')
  } catch (error) {
    console.error('Login error:', error)
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.login-container {
  width: 100%;
  height: 100vh;
  background: linear-gradient(135deg, #0f0c29 0%, #302b63 50%, #24243e 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  
  .login-background {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    overflow: hidden;
    
    .parking-lines {
      position: absolute;
      bottom: 0;
      left: 0;
      right: 0;
      height: 200px;
      display: flex;
      justify-content: space-around;
      padding: 0 50px;
      
      .line {
        width: 80px;
        height: 100%;
        background: repeating-linear-gradient(
          to top,
          transparent,
          transparent 30px,
          rgba(255, 255, 255, 0.1) 30px,
          rgba(255, 255, 255, 0.1) 32px
        );
        border-left: 2px dashed rgba(255, 255, 255, 0.2);
        border-right: 2px dashed rgba(255, 255, 255, 0.2);
      }
    }
  }
  
  .floating-cars {
    position: absolute;
    width: 100%;
    height: 100%;
    pointer-events: none;
    
    .floating-car {
      position: absolute;
      font-size: 40px;
      animation: float 20s infinite linear;
      opacity: 0.3;
      
      &.car-1 {
        top: 20%;
        animation-delay: 0s;
      }
      
      &.car-2 {
        top: 50%;
        animation-delay: -7s;
      }
      
      &.car-3 {
        top: 75%;
        animation-delay: -14s;
      }
    }
  }
  
  @keyframes float {
    0% {
      left: -50px;
      transform: rotate(0deg);
    }
    100% {
      left: calc(100% + 50px);
      transform: rotate(0deg);
    }
  }
  
  .login-box {
    width: 420px;
    padding: 50px 40px;
    background: rgba(255, 255, 255, 0.08);
    backdrop-filter: blur(20px);
    border-radius: 24px;
    border: 1px solid rgba(255, 255, 255, 0.1);
    box-shadow: 
      0 25px 50px rgba(0, 0, 0, 0.3),
      inset 0 0 80px rgba(255, 255, 255, 0.05);
    position: relative;
    z-index: 10;
    
    .login-header {
      text-align: center;
      margin-bottom: 40px;
      
      .logo-container {
        display: inline-block;
        margin-bottom: 20px;
        
        .logo-icon {
          width: 80px;
          height: 80px;
          background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
          border-radius: 20px;
          display: flex;
          align-items: center;
          justify-content: center;
          box-shadow: 0 10px 30px rgba(102, 126, 234, 0.4);
          animation: pulse 2s infinite;
          
          .el-icon {
            color: white;
          }
        }
      }
      
      h1 {
        margin: 0;
        font-size: 28px;
        color: white;
        font-weight: 600;
        letter-spacing: 2px;
      }
      
      .subtitle {
        margin-top: 8px;
        font-size: 12px;
        color: rgba(255, 255, 255, 0.5);
        letter-spacing: 3px;
      }
    }
    
    @keyframes pulse {
      0%, 100% {
        transform: scale(1);
        box-shadow: 0 10px 30px rgba(102, 126, 234, 0.4);
      }
      50% {
        transform: scale(1.05);
        box-shadow: 0 15px 40px rgba(102, 126, 234, 0.6);
      }
    }
    
    .login-form {
      :deep(.el-input__wrapper) {
        background: rgba(255, 255, 255, 0.1);
        border: 1px solid rgba(255, 255, 255, 0.2);
        box-shadow: none;
        border-radius: 12px;
        
        &:hover, &:focus {
          border-color: rgba(102, 126, 234, 0.5);
        }
        
        .el-input__inner {
          color: white;
          
          &::placeholder {
            color: rgba(255, 255, 255, 0.4);
          }
        }
        
        .el-input__prefix {
          color: rgba(255, 255, 255, 0.6);
        }
      }
      
      .login-btn {
        width: 100%;
        height: 48px;
        font-size: 16px;
        border-radius: 12px;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        border: none;
        letter-spacing: 4px;
        font-weight: 500;
        transition: all 0.3s ease;
        
        &:hover {
          transform: translateY(-2px);
          box-shadow: 0 10px 30px rgba(102, 126, 234, 0.5);
        }
        
        &:active {
          transform: translateY(0);
        }
      }
    }
    
    .login-footer {
      margin-top: 30px;
      text-align: center;
      
      p {
        color: rgba(255, 255, 255, 0.4);
        font-size: 12px;
      }
    }
  }
}
</style>
