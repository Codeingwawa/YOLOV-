import request from '@/utils/request'

export function getDashboardStats() {
  return request.get('/statistics/dashboard')
}

export function getRevenueStats(start, end) {
  return request.get('/statistics/revenue', {
    params: { start, end }
  })
}

export function getRevenueTrend(start, end) {
  return request.get('/statistics/revenue-trend', {
    params: { start, end }
  })
}

export function getTopFrequentVehicles(limit = 10) {
  return request.get('/statistics/top-vehicles', {
    params: { limit }
  })
}

export function getHourlyStats(date) {
  const params = date ? { date } : {}
  return request.get('/statistics/hourly', { params })
}
