import request from '@/utils/request'

export function detectAndSaveRecord(image, source = '图片检测') {
  const formData = new FormData()
  formData.append('image', image)
  formData.append('source', source)
  return request.post('/detection-record/detect', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function addDetectionRecord(data) {
  const formData = new FormData()
  formData.append('plateNumber', data.plateNumber)
  if (data.plateColor) formData.append('plateColor', data.plateColor)
  if (data.plateType) formData.append('plateType', data.plateType)
  if (data.vehicleType) formData.append('vehicleType', data.vehicleType)
  if (data.confidence) formData.append('confidence', data.confidence)
  if (data.source) formData.append('source', data.source)
  if (data.image) formData.append('image', data.image)
  return request.post('/detection-record/add', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function getDetectionRecords(params) {
  return request.get('/detection-record/list', { params })
}

export function getUnsavedRecords(params) {
  return request.get('/detection-record/unsaved', { params })
}

export function getRecordById(id) {
  return request.get(`/detection-record/${id}`)
}

export function saveToVehicleInfo(id, category) {
  return request.post(`/detection-record/${id}/save-to-vehicle`, null, {
    params: { category }
  })
}

export function deleteDetectionRecord(id) {
  return request.delete(`/detection-record/${id}`)
}

export function getRecordsByPlate(plateNumber) {
  return request.get(`/detection-record/plate/${plateNumber}`)
}
