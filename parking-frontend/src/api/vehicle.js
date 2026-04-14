import request from '@/utils/request'

export function vehicleEntry(image) {
  const formData = new FormData()
  formData.append('image', image)
  return request.post('/vehicle/entry', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function vehicleExit(image) {
  const formData = new FormData()
  formData.append('image', image)
  return request.post('/vehicle/exit', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function recognizePlate(image) {
  const formData = new FormData()
  formData.append('image', image)
  return request.post('/vehicle/recognize', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function getVehicleRecords(params) {
  return request.get('/vehicle/records', { params })
}

export function getParkedVehicles(params) {
  return request.get('/vehicle/parked', { params })
}

export function getRecordById(id) {
  return request.get(`/vehicle/records/${id}`)
}

export function getRecordsByTimeRange(start, end) {
  return request.get('/vehicle/records/time-range', {
    params: { start, end }
  })
}
