<template>
  <div class="monitor">
    <el-row :gutter="20">
      <el-col :span="16">
        <el-card class="video-card">
          <template #header>
            <div class="card-header">
              <span>实时监控</span>
              <div class="actions">
                <el-button type="primary" @click="startCamera" :disabled="isCameraActive">
                  开启摄像头
                </el-button>
                <el-button type="danger" @click="stopCamera" :disabled="!isCameraActive">
                  关闭摄像头
                </el-button>
              </div>
            </div>
          </template>
          <div class="video-container">
            <video ref="videoRef" autoplay playsinline></video>
            <canvas ref="canvasRef" style="display: none;"></canvas>
            <div v-if="!isCameraActive" class="placeholder">
              <el-icon :size="64"><VideoCamera /></el-icon>
              <p>点击"开启摄像头"开始监控</p>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="8">
        <el-card class="result-card">
          <template #header>
            <span>识别结果</span>
          </template>
          <div class="result-list">
            <div v-for="(item, index) in recentResults" :key="index" class="result-item">
              <div class="plate-number">{{ item.plateNumber }}</div>
              <div class="info">
                <span class="time">{{ item.time }}</span>
                <el-tag :type="item.type === 'entry' ? 'success' : 'warning'" size="small">
                  {{ item.type === 'entry' ? '入场' : '出场' }}
                </el-tag>
              </div>
            </div>
            <el-empty v-if="recentResults.length === 0" description="暂无识别记录" />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onUnmounted } from 'vue'
import { VideoCamera } from '@element-plus/icons-vue'

const videoRef = ref(null)
const canvasRef = ref(null)
const isCameraActive = ref(false)
const recentResults = ref([])

let mediaStream = null
let captureInterval = null

const startCamera = async () => {
  try {
    mediaStream = await navigator.mediaDevices.getUserMedia({
      video: { facingMode: 'environment' }
    })
    videoRef.value.srcObject = mediaStream
    isCameraActive.value = true
  } catch (error) {
    console.error('Failed to start camera:', error)
  }
}

const stopCamera = () => {
  if (mediaStream) {
    mediaStream.getTracks().forEach(track => track.stop())
    mediaStream = null
  }
  if (captureInterval) {
    clearInterval(captureInterval)
    captureInterval = null
  }
  isCameraActive.value = false
}

onUnmounted(() => {
  stopCamera()
})
</script>

<style lang="scss" scoped>
.monitor {
  .video-card {
    .video-container {
      position: relative;
      width: 100%;
      height: 480px;
      background: #000;
      border-radius: 8px;
      overflow: hidden;
      
      video {
        width: 100%;
        height: 100%;
        object-fit: cover;
      }
      
      .placeholder {
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        color: #666;
        background: #f5f7fa;
      }
    }
  }
  
  .result-card {
    .result-list {
      max-height: 500px;
      overflow-y: auto;
      
      .result-item {
        padding: 12px;
        border-bottom: 1px solid #eee;
        
        .plate-number {
          font-size: 18px;
          font-weight: bold;
          color: #333;
        }
        
        .info {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-top: 8px;
          
          .time {
            font-size: 12px;
            color: #999;
          }
        }
      }
    }
  }
}
</style>
