<template>
  <div class="dashboard">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #409eff;">
              <el-icon :size="32"><Location /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalSpaces }}</div>
              <div class="stat-label">总车位数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #67c23a;">
              <el-icon :size="32"><CircleCheck /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.availableSpaces }}</div>
              <div class="stat-label">可用车位</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #e6a23c;">
              <el-icon :size="32"><Van /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.occupiedSpaces }}</div>
              <div class="stat-label">在停车辆</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #f56c6c;">
              <el-icon :size="32"><Wallet /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">¥{{ stats.todayRevenue || 0 }}</div>
              <div class="stat-label">今日收入</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>车位使用情况</span>
            </div>
          </template>
          <div ref="pieChartRef" style="height: 300px;"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>今日入场车辆</span>
              <span class="today-count">{{ stats.todayEntries || 0 }} 辆</span>
            </div>
          </template>
          <div ref="barChartRef" style="height: 300px;"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import * as echarts from 'echarts'
import { getDashboardStats, getHourlyStats } from '@/api/statistics'

const stats = reactive({
  totalSpaces: 0,
  availableSpaces: 0,
  occupiedSpaces: 0,
  occupancyRate: 0,
  todayEntries: 0,
  todayRevenue: 0
})

const pieChartRef = ref(null)
const barChartRef = ref(null)

const loadStats = async () => {
  try {
    const data = await getDashboardStats()
    Object.assign(stats, data)
    updateCharts()
  } catch (error) {
    console.error('Failed to load stats:', error)
  }
}

const loadHourlyStats = async () => {
  try {
    const data = await getHourlyStats()
    updateBarChart(data)
  } catch (error) {
    console.error('Failed to load hourly stats:', error)
    updateBarChart({})
  }
}

const updateCharts = () => {
  if (pieChartRef.value) {
    const pieChart = echarts.init(pieChartRef.value)
    pieChart.setOption({
      tooltip: {
        trigger: 'item',
        formatter: '{b}: {c} ({d}%)'
      },
      legend: {
        orient: 'vertical',
        left: 'left'
      },
      series: [
        {
          type: 'pie',
          radius: ['40%', '70%'],
          avoidLabelOverlap: false,
          itemStyle: {
            borderRadius: 10,
            borderColor: '#fff',
            borderWidth: 2
          },
          label: {
            show: true,
            formatter: '{b}: {c}'
          },
          data: [
            { value: stats.occupiedSpaces || 0, name: '已占用', itemStyle: { color: '#e6a23c' } },
            { value: stats.availableSpaces || 0, name: '可用', itemStyle: { color: '#67c23a' } }
          ]
        }
      ]
    })
  }
}

const updateBarChart = (hourlyData) => {
  if (barChartRef.value) {
    const barChart = echarts.init(barChartRef.value)
    const hours = Array.from({ length: 24 }, (_, i) => `${i}:00`)
    const data = Array.from({ length: 24 }, (_, i) => hourlyData[`hour_${i}`] || 0)
    
    barChart.setOption({
      tooltip: {
        trigger: 'axis'
      },
      xAxis: {
        type: 'category',
        data: hours,
        axisLabel: {
          rotate: 45
        }
      },
      yAxis: {
        type: 'value',
        name: '入场车辆'
      },
      series: [
        {
          data: data,
          type: 'bar',
          itemStyle: {
            color: '#409eff'
          },
          barWidth: '60%'
        }
      ]
    })
  }
}

onMounted(() => {
  loadStats()
  loadHourlyStats()
  window.addEventListener('resize', () => {
    echarts.getInstanceByDom(pieChartRef.value)?.resize()
    echarts.getInstanceByDom(barChartRef.value)?.resize()
  })
})
</script>

<style lang="scss" scoped>
.dashboard {
  .stat-card {
    .stat-content {
      display: flex;
      align-items: center;
      gap: 20px;
      
      .stat-icon {
        width: 60px;
        height: 60px;
        border-radius: 12px;
        display: flex;
        align-items: center;
        justify-content: center;
        color: #fff;
      }
      
      .stat-info {
        .stat-value {
          font-size: 28px;
          font-weight: bold;
          color: #333;
        }
        
        .stat-label {
          font-size: 14px;
          color: #999;
          margin-top: 4px;
        }
      }
    }
  }
  
  .chart-card {
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      
      .today-count {
        color: #409eff;
        font-weight: bold;
      }
    }
  }
}
</style>
