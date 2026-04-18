<template>
  <div class="vehicle-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>车辆管理</span>
          <div class="actions">
            <el-upload
              :show-file-list="false"
              :before-upload="handleImageDetect"
              accept="image/*"
            >
              <el-button type="primary" :icon="Camera" :loading="imageLoading">
                图片检测
              </el-button>
            </el-upload>
            <el-upload
              :show-file-list="false"
              :before-upload="handleVideoDetect"
              accept="video/*"
            >
              <el-button type="success" :icon="VideoCamera" :loading="videoLoading">
                视频检测
              </el-button>
            </el-upload>
            <el-button type="warning" :icon="VideoCameraFilled" @click="openCameraDialog">
              摄像头检测
            </el-button>
          </div>
        </div>
      </template>
      
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="车辆列表" name="list">
          <el-form :inline="true" :model="searchForm" class="search-form">
            <el-form-item label="车牌号">
              <el-input v-model="searchForm.plateNumber" placeholder="请输入车牌号" clearable style="width: 180px" />
            </el-form-item>
            <el-form-item label="车型">
              <el-select v-model="searchForm.vehicleType" placeholder="请选择车型" clearable style="width: 150px">
                <el-option label="新能源" value="新能源" />
                <el-option label="普通" value="普通" />
              </el-select>
            </el-form-item>
            <el-form-item label="分类">
              <el-select v-model="searchForm.category" placeholder="请选择分类" clearable style="width: 150px">
                <el-option label="新能源" value="NEW_ENERGY" />
                <el-option label="油车" value="FUEL" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSearch">搜索</el-button>
              <el-button @click="resetSearch">重置</el-button>
            </el-form-item>
          </el-form>
          
          <el-table :data="vehicleList" v-loading="loading" stripe>
            <el-table-column prop="plateNumber" label="车牌号" width="150">
              <template #default="{ row }">
                <el-tag type="primary">{{ row.plateNumber }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="plateColor" label="车牌颜色" width="100" />
            <el-table-column prop="vehicleType" label="车型" width="100" />
            <el-table-column prop="category" label="分类" width="100">
              <template #default="{ row }">
                {{ getCategoryLabel(row.category) }}
              </template>
            </el-table-column>
            <el-table-column prop="parkingSpaceNumber" label="车位号" width="100">
              <template #default="{ row }">
                <el-tag v-if="row.parkingSpaceNumber" type="success">
                  {{ row.parkingSpaceNumber }}
                </el-tag>
                <span v-else class="text-gray">-</span>
              </template>
            </el-table-column>
            <el-table-column prop="parkingStartTime" label="入场时间" width="180">
              <template #default="{ row }">
                {{ formatTime(row.parkingStartTime) || '-' }}
              </template>
            </el-table-column>
            <el-table-column prop="parkingEndTime" label="预计离场" width="180">
              <template #default="{ row }">
                <template v-if="row.parkingEndTime">
                  <span :class="{ 'text-warning': isExpiringSoon(row.parkingEndTime) }">
                    {{ formatTime(row.parkingEndTime) }}
                  </span>
                </template>
                <span v-else class="text-gray">-</span>
              </template>
            </el-table-column>
            <el-table-column prop="imageUrl" label="车辆图片" width="120">
              <template #default="{ row }">
                <el-image
                  v-if="row.imageUrl"
                  :src="getImageUrl(row.imageUrl)"
                  :preview-src-list="[getImageUrl(row.imageUrl)]"
                  :z-index="9999"
                  preview-teleported
                  style="width: 80px; height: 60px;"
                  fit="cover"
                />
                <span v-else class="text-gray">无图片</span>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="录入时间" width="180">
              <template #default="{ row }">
                {{ formatTime(row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" size="small" @click="editVehicle(row)">编辑</el-button>
                <el-popconfirm title="确定删除该车辆?" @confirm="handleDelete(row.id)">
                  <template #reference>
                    <el-button type="danger" size="small">删除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
          
          <el-pagination
            v-model:current-page="pagination.page"
            v-model:page-size="pagination.size"
            :total="pagination.total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @current-change="loadVehicleList"
            @size-change="loadVehicleList"
            style="margin-top: 20px; justify-content: flex-end;"
          />
        </el-tab-pane>
        
        <el-tab-pane label="检测记录" name="records">
          <el-empty v-if="detectionRecords.length === 0 && !recordsLoading" description="暂无检测记录，请进行图片、视频或摄像头检测" />
          <el-table v-else :data="detectionRecords" v-loading="recordsLoading" stripe>
            <el-table-column prop="plateNumber" label="车牌号" width="150">
              <template #default="{ row }">
                <el-tag type="primary">{{ row.plateNumber }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="plateColor" label="车牌颜色" width="100" />
            <el-table-column prop="plateType" label="车牌类型" width="100" />
            <el-table-column prop="vehicleType" label="车型" width="100">
              <template #default="{ row }">
                <el-tag v-if="row.vehicleType" :type="row.vehicleType !== '未知' ? 'success' : 'info'" size="small">
                  {{ row.vehicleType || '未知' }}
                </el-tag>
                <span v-else class="text-gray">-</span>
              </template>
            </el-table-column>
            <el-table-column prop="confidence" label="置信度" width="100">
              <template #default="{ row }">
                {{ row.confidence ? row.confidence + '%' : '-' }}
              </template>
            </el-table-column>
            <el-table-column prop="source" label="来源" width="100" />
            <el-table-column prop="detectionTime" label="检测时间" width="180">
              <template #default="{ row }">
                {{ formatTime(row.detectionTime) }}
              </template>
            </el-table-column>
            <el-table-column prop="saved" label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.saved ? 'success' : 'warning'">
                  {{ row.saved ? '已保存' : '未保存' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200">
              <template #default="{ row }">
                <el-button 
                  v-if="!row.saved" 
                  type="primary" 
                  size="small" 
                  @click="saveRecordToDatabase(row)"
                  :loading="savingRecordId === row.id"
                >
                  保存到车辆库
                </el-button>
                <el-popconfirm title="确定删除该记录?" @confirm="handleDeleteRecord(row.id)">
                  <template #reference>
                    <el-button type="danger" size="small">删除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
          
          <el-pagination
            v-model:current-page="recordsPagination.page"
            v-model:page-size="recordsPagination.size"
            :total="recordsPagination.total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @current-change="loadDetectionRecords"
            @size-change="loadDetectionRecords"
            style="margin-top: 20px; justify-content: flex-end;"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>
    
    <el-dialog v-model="resultDialogVisible" title="检测结果" width="500px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="车牌号">
          <el-tag size="large" type="primary">{{ recognitionResult.plateNumber }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="置信度">{{ recognitionResult.confidence }}%</el-descriptions-item>
        <el-descriptions-item label="车牌颜色">{{ recognitionResult.plateColor }}</el-descriptions-item>
        <el-descriptions-item label="车牌类型">{{ recognitionResult.plateType }}</el-descriptions-item>
        <el-descriptions-item label="车型识别">
          <el-tag :type="recognitionResult.vehicleType !== '未知' ? 'success' : 'info'">
            {{ recognitionResult.vehicleType }}
          </el-tag>
          <span v-if="recognitionResult.vehicleConfidence > 0" style="margin-left: 8px; color: #909399;">
            (置信度: {{ recognitionResult.vehicleConfidence }}%)
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="分类">
          <el-select v-model="selectedCategory" placeholder="选择分类">
            <el-option label="新能源" value="NEW_ENERGY" />
            <el-option label="油车" value="FUEL" />
          </el-select>
        </el-descriptions-item>
      </el-descriptions>
      <div v-if="recognitionResult.resultImage" style="margin-top: 16px;">
        <img :src="'data:image/jpeg;base64,' + recognitionResult.resultImage" style="max-width: 100%;" />
      </div>
      <template #footer>
        <el-button @click="resultDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="saveDetectionResult" :loading="saving">保存到车辆库</el-button>
      </template>
    </el-dialog>
    
    <el-dialog v-model="editDialogVisible" title="编辑车辆信息" width="500px">
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="车牌号">
          <el-input v-model="editForm.plateNumber" disabled />
        </el-form-item>
        <el-form-item label="车牌颜色">
          <el-input v-model="editForm.plateColor" />
        </el-form-item>
        <el-form-item label="车型">
          <el-input v-model="editForm.vehicleType" />
        </el-form-item>
        <el-form-item label="车辆品牌">
          <el-input v-model="editForm.vehicleBrand" />
        </el-form-item>
        <el-form-item label="车辆颜色">
          <el-input v-model="editForm.vehicleColor" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="editForm.category" placeholder="选择分类">
            <el-option label="新能源" value="NEW_ENERGY" />
            <el-option label="油车" value="FUEL" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="editForm.remarks" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>
    
    <el-dialog v-model="cameraDialogVisible" title="摄像头实时检测" width="800px">
      <div class="camera-container">
        <video ref="cameraVideo" autoplay playsinline style="width: 100%; max-height: 400px;"></video>
        <canvas ref="cameraCanvas" style="display: none;"></canvas>
      </div>
      <div class="camera-controls">
        <el-button type="primary" @click="startCamera" :disabled="cameraActive">开启摄像头</el-button>
        <el-button type="danger" @click="stopCamera" :disabled="!cameraActive">关闭摄像头</el-button>
        <el-button type="success" @click="captureAndDetect" :disabled="!cameraActive">截取检测</el-button>
      </div>
      <div class="detection-results">
        <h4>实时检测结果</h4>
        <el-empty v-if="cameraResults.length === 0" description="暂无检测结果" :image-size="60" />
        <div v-else>
          <el-tag v-for="(result, index) in cameraResults" :key="index" style="margin: 5px;" type="success">
            {{ result.plateNumber }} ({{ result.confidence }}%)
          </el-tag>
        </div>
      </div>
    </el-dialog>
    
    <el-dialog v-model="videoDialogVisible" title="视频检测结果" width="800px">
      <div v-if="videoProcessing" class="video-processing">
        <el-progress :percentage="videoProgress" :format="formatProgress" />
        <p>正在处理视频，请稍候...</p>
      </div>
      <div v-else>
        <el-descriptions :column="3" border>
          <el-descriptions-item label="总帧数">{{ videoResult.totalFrames }}</el-descriptions-item>
          <el-descriptions-item label="处理帧数">{{ videoResult.processedFrames }}</el-descriptions-item>
          <el-descriptions-item label="检测次数">{{ videoResult.detectionCount }}</el-descriptions-item>
          <el-descriptions-item label="唯一车牌数">{{ videoResult.uniquePlates }}</el-descriptions-item>
        </el-descriptions>
        <el-empty v-if="videoResult.results.length === 0" description="未检测到车牌" style="margin-top: 16px;" />
        <el-table v-else :data="videoResult.results" style="margin-top: 16px;" max-height="300">
          <el-table-column prop="plateNumber" label="车牌号" width="130">
            <template #default="{ row }">
              <el-tag type="primary">{{ row.plateNumber }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="plateColor" label="颜色" width="80" />
          <el-table-column prop="vehicleType" label="车型" width="80">
            <template #default="{ row }">
              <el-tag v-if="row.vehicleType" :type="row.vehicleType !== '未知' ? 'success' : 'info'" size="small">
                {{ row.vehicleType || '未知' }}
              </el-tag>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column prop="confidence" label="置信度" width="80">
            <template #default="{ row }">{{ row.confidence }}%</template>
          </el-table-column>
          <el-table-column prop="timestamp" label="时间点" width="80">
            <template #default="{ row }">{{ row.timestamp }}s</template>
          </el-table-column>
          <el-table-column label="操作">
            <template #default="{ row }">
              <el-button type="primary" size="small" @click="addVideoResultToRecords(row)">添加到记录</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Camera, VideoCamera, VideoCameraFilled } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { 
  detectAndSave, 
  searchVehicles, 
  updateVehicle, 
  deleteVehicle
} from '@/api/vehicleInfo'
import {
  detectAndSaveRecord,
  getDetectionRecords,
  saveToVehicleInfo,
  deleteDetectionRecord,
  addDetectionRecord
} from '@/api/detectionRecord'

const activeTab = ref('list')
const loading = ref(false)
const imageLoading = ref(false)
const videoLoading = ref(false)
const saving = ref(false)
const recordsLoading = ref(false)
const savingRecordId = ref(null)

const vehicleList = ref([])
const detectionRecords = ref([])

const searchForm = reactive({
  plateNumber: '',
  vehicleType: '',
  category: ''
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const recordsPagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const resultDialogVisible = ref(false)
const editDialogVisible = ref(false)
const cameraDialogVisible = ref(false)
const videoDialogVisible = ref(false)

const recognitionResult = reactive({
  plateNumber: '',
  confidence: 0,
  plateColor: '',
  plateType: '',
  vehicleType: '',
  vehicleCategory: '',
  vehicleConfidence: 0,
  resultImage: null
})

const selectedCategory = ref('SEDAN')
const currentRecordId = ref(null)

const editForm = reactive({
  id: null,
  plateNumber: '',
  plateColor: '',
  vehicleType: '',
  vehicleBrand: '',
  vehicleColor: '',
  category: '',
  remarks: ''
})

const cameraVideo = ref(null)
const cameraCanvas = ref(null)
const cameraActive = ref(false)
const cameraResults = ref([])
let mediaStream = null
let detectionInterval = null

const videoProcessing = ref(false)
const videoProgress = ref(0)
const videoResult = reactive({
  totalFrames: 0,
  processedFrames: 0,
  detectionCount: 0,
  uniquePlates: 0,
  results: []
})

const formatTime = (time) => {
  return time ? dayjs(time).format('YYYY-MM-DD HH:mm:ss') : '-'
}

const isExpiringSoon = (endTime) => {
  if (!endTime) return false
  const now = dayjs()
  const end = dayjs(endTime)
  const hoursLeft = end.diff(now, 'hour')
  return hoursLeft <= 2 && hoursLeft > 0
}

const getCategoryLabel = (category) => {
  const labels = {
    NEW_ENERGY: '新能源',
    FUEL: '油车'
  }
  return labels[category] || category || '-'
}

const determineCategory = (plateColor, vehicleType) => {
  if (plateColor && plateColor.includes('绿')) {
    return 'NEW_ENERGY'
  }
  return 'FUEL'
}

const getImageUrl = (path) => {
  if (!path) return ''
  if (path.startsWith('http')) return path
  return `/api/files?path=${encodeURIComponent(path)}`
}

const handleTabChange = (tab) => {
  if (tab === 'records') {
    loadDetectionRecords()
  } else if (tab === 'list') {
    loadVehicleList()
  }
}

const handleImageDetect = async (file) => {
  imageLoading.value = true
  
  try {
    const formData = new FormData()
    formData.append('image', file)
    
    const response = await fetch('http://localhost:5000/api/recognize-with-image', {
      method: 'POST',
      body: formData
    })
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }
    
    const data = await response.json()
    
    if (data.error) {
      throw new Error(data.error)
    }
    
    if (data.plateNumber) {
      Object.assign(recognitionResult, {
        plateNumber: data.plateNumber,
        confidence: data.confidence || 0,
        plateColor: data.plateColor || '',
        plateType: data.plateType || '',
        vehicleType: data.vehicleType || '未知',
        vehicleCategory: data.vehicleCategory || 'OTHER',
        vehicleConfidence: data.vehicleConfidence || 0,
        resultImage: data.resultImage || null
      })
      selectedCategory.value = determineCategory(data.plateColor, data.vehicleType)
      
      const recordFormData = new FormData()
      recordFormData.append('image', file)
      recordFormData.append('plateNumber', data.plateNumber)
      recordFormData.append('plateColor', data.plateColor || '')
      recordFormData.append('plateType', data.plateType || '')
      recordFormData.append('confidence', data.confidence || 0)
      recordFormData.append('source', '图片检测')
      
      const recordResponse = await fetch('/api/detection-record/add', {
        method: 'POST',
        body: recordFormData
      }).then(res => res.json())
      
      currentRecordId.value = recordResponse.id
      
      resultDialogVisible.value = true
      ElMessage.success('检测成功！请选择分类后保存到车辆库')
    } else {
      ElMessage.warning('未检测到车牌')
    }
  } catch (error) {
    console.error('Detection error:', error)
    if (error.message.includes('Failed to fetch') || error.message.includes('NetworkError')) {
      ElMessage.error('无法连接到识别服务，请确保Python服务已启动 (http://localhost:5000)')
    } else {
      ElMessage.error('检测失败：' + error.message)
    }
  } finally {
    imageLoading.value = false
  }
  return false
}

const handleVideoDetect = async (file) => {
  videoLoading.value = true
  videoProcessing.value = true
  videoDialogVisible.value = true
  videoProgress.value = 0
  
  try {
    const formData = new FormData()
    formData.append('video', file)
    
    const response = await fetch('http://localhost:5000/api/video/detect', {
      method: 'POST',
      body: formData
    })
    
    const data = await response.json()
    
    Object.assign(videoResult, {
      totalFrames: data.totalFrames || 0,
      processedFrames: data.processedFrames || 0,
      detectionCount: data.detectionCount || 0,
      uniquePlates: data.uniquePlates || 0,
      results: data.results || []
    })
    videoProcessing.value = false
    videoProgress.value = 100
    
    if (data.uniquePlates > 0) {
      ElMessage.success(`视频处理完成，检测到 ${data.uniquePlates} 个车牌`)
    } else {
      ElMessage.info('视频处理完成，未检测到车牌')
    }
  } catch (error) {
    ElMessage.error('视频检测失败：' + error.message)
    videoProcessing.value = false
  } finally {
    videoLoading.value = false
  }
  return false
}

const formatProgress = (percentage) => {
  return percentage === 100 ? '完成' : `${percentage}%`
}

const openCameraDialog = () => {
  cameraDialogVisible.value = true
}

const startCamera = async () => {
  try {
    mediaStream = await navigator.mediaDevices.getUserMedia({
      video: { facingMode: 'environment', width: 1280, height: 720 }
    })
    cameraVideo.value.srcObject = mediaStream
    cameraActive.value = true
    
    detectionInterval = setInterval(captureAndDetect, 2000)
  } catch (error) {
    ElMessage.error('无法访问摄像头：' + error.message)
  }
}

const stopCamera = () => {
  if (mediaStream) {
    mediaStream.getTracks().forEach(track => track.stop())
    mediaStream = null
  }
  if (detectionInterval) {
    clearInterval(detectionInterval)
    detectionInterval = null
  }
  cameraActive.value = false
  cameraResults.value = []
}

const captureAndDetect = async () => {
  if (!cameraVideo.value || !cameraCanvas.value) return
  
  const video = cameraVideo.value
  const canvas = cameraCanvas.value
  canvas.width = video.videoWidth
  canvas.height = video.videoHeight
  
  const ctx = canvas.getContext('2d')
  ctx.drawImage(video, 0, 0)
  
  canvas.toBlob(async (blob) => {
    const formData = new FormData()
    formData.append('frame', blob, 'frame.jpg')
    
    try {
      const response = await fetch('http://localhost:5000/api/video/detect-frame', {
        method: 'POST',
        body: formData
      })
      
      const data = await response.json()
      
      if (data.results && data.results.length > 0) {
        cameraResults.value = data.results.slice(0, 5)
        
        let imageFile = null
        if (data.resultImage) {
          const byteCharacters = atob(data.resultImage)
          const byteNumbers = new Array(byteCharacters.length)
          for (let i = 0; i < byteCharacters.length; i++) {
            byteNumbers[i] = byteCharacters.charCodeAt(i)
          }
          const byteArray = new Uint8Array(byteNumbers)
          imageFile = new Blob([byteArray], { type: 'image/jpeg' })
        }
        
        for (const result of data.results) {
          if (result.plateNumber && result.plateNumber !== '无法识别') {
            try {
              await addDetectionRecord({
                plateNumber: result.plateNumber,
                plateColor: result.plateColor,
                plateType: result.plateType,
                confidence: result.confidence,
                source: '摄像头检测',
                image: imageFile
              })
            } catch (e) {
              console.log('Record may already exist')
            }
          }
        }
      }
    } catch (error) {
      console.error('Frame detection error:', error)
    }
  }, 'image/jpeg')
}

const saveDetectionResult = async () => {
  if (!currentRecordId.value) {
    ElMessage.warning('检测记录不存在')
    return
  }
  
  saving.value = true
  try {
    await saveToVehicleInfo(currentRecordId.value, selectedCategory.value)
    ElMessage.success('保存成功，已切换到车辆列表')
    resultDialogVisible.value = false
    activeTab.value = 'list'
    await loadVehicleList()
    loadDetectionRecords()
  } catch (error) {
    ElMessage.error('保存失败：' + error.message)
  } finally {
    saving.value = false
  }
}

const addVideoResultToRecords = async (result) => {
  try {
    let imageFile = null
    if (result.resultImage) {
      const byteCharacters = atob(result.resultImage)
      const byteNumbers = new Array(byteCharacters.length)
      for (let i = 0; i < byteCharacters.length; i++) {
        byteNumbers[i] = byteCharacters.charCodeAt(i)
      }
      const byteArray = new Uint8Array(byteNumbers)
      imageFile = new Blob([byteArray], { type: 'image/jpeg' })
    }
    
    await addDetectionRecord({
      plateNumber: result.plateNumber,
      plateColor: result.plateColor,
      plateType: result.plateType,
      vehicleType: result.vehicleType,
      confidence: result.confidence,
      source: '视频检测',
      image: imageFile
    })
    ElMessage.success('已添加到检测记录')
  } catch (error) {
    ElMessage.error('添加失败：' + error.message)
  }
}

const saveRecordToDatabase = async (record) => {
  savingRecordId.value = record.id
  try {
    await saveToVehicleInfo(record.id, record.plateColor?.includes('绿') ? 'NEW_ENERGY' : 'SEDAN')
    ElMessage.success('保存成功，已切换到车辆列表')
    activeTab.value = 'list'
    await loadVehicleList()
    loadDetectionRecords()
  } catch (error) {
    ElMessage.error('保存失败：' + error.message)
  } finally {
    savingRecordId.value = null
  }
}

const handleDeleteRecord = async (id) => {
  try {
    await deleteDetectionRecord(id)
    ElMessage.success('删除成功')
    loadDetectionRecords()
  } catch (error) {
    ElMessage.error('删除失败：' + error.message)
  }
}

const handleSearch = () => {
  pagination.page = 1
  loadVehicleList()
}

const resetSearch = () => {
  searchForm.plateNumber = ''
  searchForm.vehicleType = ''
  searchForm.category = ''
  pagination.page = 1
  loadVehicleList()
}

const loadVehicleList = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page - 1,
      size: pagination.size
    }
    if (searchForm.plateNumber) params.plateNumber = searchForm.plateNumber
    if (searchForm.vehicleType) params.vehicleType = searchForm.vehicleType
    if (searchForm.category) params.category = searchForm.category
    
    const data = await searchVehicles(params)
    
    if (data && data.content) {
      vehicleList.value = data.content
      pagination.total = data.totalElements || 0
    } else if (Array.isArray(data)) {
      vehicleList.value = data
      pagination.total = data.length
    } else {
      vehicleList.value = []
      pagination.total = 0
    }
  } catch (error) {
    console.error('Failed to load vehicles:', error)
    vehicleList.value = []
  } finally {
    loading.value = false
  }
}

const loadDetectionRecords = async () => {
  recordsLoading.value = true
  try {
    const data = await getDetectionRecords({
      page: recordsPagination.page - 1,
      size: recordsPagination.size
    })
    
    if (data && data.content) {
      detectionRecords.value = data.content
      recordsPagination.total = data.totalElements || 0
    } else if (Array.isArray(data)) {
      detectionRecords.value = data
      recordsPagination.total = data.length
    } else {
      detectionRecords.value = []
      recordsPagination.total = 0
    }
  } catch (error) {
    console.error('Failed to load detection records:', error)
    detectionRecords.value = []
  } finally {
    recordsLoading.value = false
  }
}

const editVehicle = (vehicle) => {
  Object.assign(editForm, {
    id: vehicle.id,
    plateNumber: vehicle.plateNumber,
    plateColor: vehicle.plateColor,
    vehicleType: vehicle.vehicleType,
    vehicleBrand: vehicle.vehicleBrand || '',
    vehicleColor: vehicle.vehicleColor || '',
    category: vehicle.category,
    remarks: vehicle.remarks || ''
  })
  editDialogVisible.value = true
}

const saveEdit = async () => {
  try {
    await updateVehicle(editForm.id, editForm)
    ElMessage.success('更新成功')
    editDialogVisible.value = false
    loadVehicleList()
  } catch (error) {
    ElMessage.error('更新失败：' + error.message)
  }
}

const handleDelete = async (id) => {
  try {
    await deleteVehicle(id)
    ElMessage.success('删除成功')
    loadVehicleList()
  } catch (error) {
    ElMessage.error('删除失败：' + error.message)
  }
}

onMounted(() => {
  loadVehicleList()
})

onUnmounted(() => {
  stopCamera()
})
</script>

<style lang="scss" scoped>
.vehicle-management {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    .actions {
      display: flex;
      gap: 10px;
    }
  }
  
  .search-form {
    margin-bottom: 20px;
  }
  
  .camera-container {
    background: #000;
    border-radius: 8px;
    overflow: hidden;
    margin-bottom: 16px;
    
    video {
      width: 100%;
      display: block;
    }
  }
  
  .camera-controls {
    display: flex;
    gap: 10px;
    justify-content: center;
    margin-bottom: 16px;
  }
  
  .detection-results {
    background: #f5f7fa;
    padding: 16px;
    border-radius: 8px;
    
    h4 {
      margin-bottom: 10px;
    }
  }
  
  .video-processing {
    text-align: center;
    padding: 40px;
  }
  
  .text-warning {
    color: #e6a23c;
    font-weight: 500;
  }
  
  .text-gray {
    color: #909399;
  }
}
</style>
