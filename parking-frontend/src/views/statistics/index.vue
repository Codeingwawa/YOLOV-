<template>
  <div class="statistics">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>统计报表</span>
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            @change="loadAllData"
          />
        </div>
      </template>
      
      <el-row :gutter="20">
        <el-col :span="8">
          <el-statistic title="总收入" :value="revenueStats.totalRevenue || 0" prefix="¥" />
        </el-col>
        <el-col :span="8">
          <el-statistic title="总车辆数" :value="revenueStats.totalVehicles || 0" suffix="辆" />
        </el-col>
        <el-col :span="8">
          <el-statistic title="平均费用" :value="revenueStats.averageFee || 0" prefix="¥" :precision="2" />
        </el-col>
      </el-row>
    </el-card>
    
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>收入趋势</span>
          </template>
          <div ref="lineChartRef" style="height: 350px;"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>高频车辆TOP10</span>
          </template>
          <el-empty v-if="topVehicles.length === 0" description="暂无数据" />
          <el-table v-else :data="topVehicles" stripe>
            <el-table-column prop="0" label="车牌号">
              <template #default="{ row }">
                <el-tag type="primary">{{ row[0] }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="1" label="进出次数">
              <template #default="{ row }">
                {{ row[1] }} 次
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import * as echarts from 'echarts'
import dayjs from 'dayjs'
import { getRevenueStats, getRevenueTrend, getTopFrequentVehicles } from '@/api/statistics'

const dateRange = ref([
  dayjs().subtract(7, 'day').toDate(),
  dayjs().toDate()
])

const revenueStats = reactive({
  totalRevenue: 0,
  totalVehicles: 0,
  averageFee: 0
})

const topVehicles = ref([])
const lineChartRef = ref(null)
let lineChart = null

const loadRevenueStats = async () => {
  try {
    const [start, end] = dateRange.value || []
    if (!start || !end) return
    
    const data = await getRevenueStats(
      dayjs(start).format('YYYY-MM-DDTHH:mm:ss'),
      dayjs(end).format('YYYY-MM-DDTHH:mm:ss')
    )
    Object.assign(revenueStats, data)
  } catch (error) {
    console.error('Failed to load revenue stats:', error)
  }
}

const loadRevenueTrend = async () => {
  try {
    const [start, end] = dateRange.value || []
    if (!start || !end) return

    const data = await getRevenueTrend(
      dayjs(start).format('YYYY-MM-DDTHH:mm:ss'),
      dayjs(end).format('YYYY-MM-DDTHH:mm:ss')
    )

    const dates = data.map(item => item.date)
    const revenues = data.map(item => item.revenue)
    const counts = data.map(item => item.count)

    if (lineChart) {
      lineChart.setOption({
        xAxis: { data: dates },
        series: [
          { data: revenues },
          { data: counts }
        ]
      })
    }
  } catch (error) {
    console.error('Failed to load revenue trend:', error)
  }
}

const loadTopVehicles = async () => {
  try {
    const data = await getTopFrequentVehicles(10)
    topVehicles.value = Array.isArray(data) ? data : []
  } catch (error) {
    console.error('Failed to load top vehicles:', error)
    topVehicles.value = []
  }
}

const initLineChart = () => {
  if (lineChartRef.value) {
    lineChart = echarts.init(lineChartRef.value)

    lineChart.setOption({
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'cross' }
      },
      legend: {
        data: ['收入(元)', '车辆数']
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: [],
        axisLabel: { rotate: 30 }
      },
      yAxis: [
        {
          type: 'value',
          name: '收入(元)',
          position: 'left',
          axisLabel: { formatter: '¥{value}' }
        },
        {
          type: 'value',
          name: '车辆数',
          position: 'right',
          axisLabel: { formatter: '{value}辆' }
        }
      ],
      series: [
        {
          name: '收入(元)',
          data: [],
          type: 'line',
          smooth: true,
          yAxisIndex: 0,
          itemStyle: { color: '#409eff' },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
              { offset: 1, color: 'rgba(64, 158, 255, 0.05)' }
            ])
          }
        },
        {
          name: '车辆数',
          data: [],
          type: 'bar',
          yAxisIndex: 1,
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(103, 194, 58, 0.8)' },
              { offset: 1, color: 'rgba(103, 194, 58, 0.3)' }
            ]),
            borderRadius: [4, 4, 0, 0]
          },
          barWidth: '40%'
        }
      ]
    })
  }
}

const loadAllData = () => {
  loadRevenueStats()
  loadRevenueTrend()
  loadTopVehicles()
}

onMounted(() => {
  initLineChart()
  loadAllData()

  window.addEventListener('resize', () => {
    lineChart?.resize()
  })
})
</script>

<style lang="scss" scoped>
.statistics {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}
</style>
