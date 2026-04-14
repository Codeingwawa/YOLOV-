<template>
  <div class="parking-space">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>车位管理</span>
          <div class="stats">
            <el-tag type="success">可用: {{ availableCount }}</el-tag>
            <el-tag type="warning">占用: {{ occupiedCount }}</el-tag>
          </div>
        </div>
      </template>
      
      <el-empty v-if="parkingSpaces.length === 0" description="暂无车位数据，请在数据库中初始化车位" />
      <div v-else class="space-grid">
        <div
          v-for="space in parkingSpaces"
          :key="space.id"
          :class="['space-item', getStatusClass(space.status)]"
          @click="showSpaceDetail(space)"
        >
          <div class="space-number">{{ space.spaceNumber }}</div>
          <div class="space-status">
            <el-icon v-if="space.status === 'AVAILABLE'"><CircleCheck /></el-icon>
            <el-icon v-else><Close /></el-icon>
          </div>
          <div v-if="space.status === 'OCCUPIED' && space.currentPlate" class="plate-info">
            {{ space.currentPlate }}
          </div>
        </div>
      </div>
    </el-card>
    
    <el-dialog v-model="dialogVisible" title="车位详情" width="400px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="车位编号">{{ currentSpace.spaceNumber }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(currentSpace.status)">
            {{ getStatusText(currentSpace.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="类型">{{ getTypeText(currentSpace.type) }}</el-descriptions-item>
        <el-descriptions-item v-if="currentSpace.currentPlate" label="车牌号">
          {{ currentSpace.currentPlate }}
        </el-descriptions-item>
        <el-descriptions-item v-if="currentSpace.occupiedTime" label="占用时间">
          {{ currentSpace.occupiedTime }}
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button 
          v-if="currentSpace.status === 'OCCUPIED'" 
          type="danger" 
          @click="releaseSpace"
        >
          释放车位
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { CircleCheck, Close } from '@element-plus/icons-vue'
import request from '@/utils/request'

const dialogVisible = ref(false)
const currentSpace = ref({})
const parkingSpaces = ref([])
const loading = ref(false)

const availableCount = computed(() => 
  parkingSpaces.value.filter(s => s.status === 'AVAILABLE').length
)

const occupiedCount = computed(() => 
  parkingSpaces.value.filter(s => s.status === 'OCCUPIED').length
)

const getStatusClass = (status) => {
  return status ? status.toLowerCase() : 'available'
}

const getStatusType = (status) => {
  const types = {
    AVAILABLE: 'success',
    OCCUPIED: 'warning',
    RESERVED: 'info',
    MAINTENANCE: 'danger'
  }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = {
    AVAILABLE: '可用',
    OCCUPIED: '占用',
    RESERVED: '预约',
    MAINTENANCE: '维护中'
  }
  return texts[status] || status
}

const getTypeText = (type) => {
  const texts = {
    STANDARD: '标准车位',
    LARGE: '大型车位',
    DISABLED: '无障碍车位',
    VIP: 'VIP车位'
  }
  return texts[type] || type
}

import { ElMessage } from 'element-plus'

const showSpaceDetail = (space) => {
  currentSpace.value = space
  dialogVisible.value = true
}

const releaseSpace = async () => {
  if (!currentSpace.value.id) return
  
  try {
    await request.put(`/parking-space/${currentSpace.value.id}/release`)
    ElMessage.success('车位已释放')
    dialogVisible.value = false
    loadParkingSpaces()
  } catch (error) {
    ElMessage.error('释放失败：' + error.message)
  }
}

const loadParkingSpaces = async () => {
  loading.value = true
  try {
    const data = await request.get('/parking-space/list')
    if (Array.isArray(data)) {
      parkingSpaces.value = data
    } else if (data && data.content) {
      parkingSpaces.value = data.content
    } else {
      parkingSpaces.value = []
    }
  } catch (error) {
    console.error('Failed to load parking spaces:', error)
    parkingSpaces.value = []
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadParkingSpaces()
})
</script>

<style lang="scss" scoped>
.parking-space {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    .stats {
      display: flex;
      gap: 10px;
    }
  }
  
  .space-grid {
    display: grid;
    grid-template-columns: repeat(10, 1fr);
    gap: 10px;
    
    .space-item {
      aspect-ratio: 1;
      border-radius: 8px;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      transition: all 0.3s;
      
      &.available {
        background: #f0f9eb;
        border: 2px solid #67c23a;
        
        &:hover {
          background: #e1f3d8;
        }
      }
      
      &.occupied {
        background: #fdf6ec;
        border: 2px solid #e6a23c;
        
        &:hover {
          background: #faecd8;
        }
      }
      
      &.reserved {
        background: #ecf5ff;
        border: 2px solid #409eff;
        
        &:hover {
          background: #d9ecff;
        }
      }
      
      &.maintenance {
        background: #fef0f0;
        border: 2px solid #f56c6c;
        
        &:hover {
          background: #fde2e2;
        }
      }
      
      .space-number {
        font-size: 14px;
        font-weight: bold;
        color: #333;
      }
      
      .space-status {
        margin-top: 4px;
      }
      
      .plate-info {
        font-size: 10px;
        color: #666;
        margin-top: 4px;
      }
    }
  }
}

@media (max-width: 1200px) {
  .parking-space .space-grid {
    grid-template-columns: repeat(8, 1fr);
  }
}

@media (max-width: 992px) {
  .parking-space .space-grid {
    grid-template-columns: repeat(5, 1fr);
  }
}

@media (max-width: 768px) {
  .parking-space .space-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}
</style>
