import request from '@/utils/request'

export function detectAndSave(image, category) {
  const formData = new FormData()
  formData.append('image', image)
  if (category) {
    formData.append('category', category)
  }
  return request.post('/vehicle-info/detect', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function getVehicleList(params) {
  return request.get('/vehicle-info/list', { params })
}

export function searchVehicles(params) {
  return request.get('/vehicle-info/search', { params })
}

export function getVehicleById(id) {
  return request.get(`/vehicle-info/${id}`)
}

export function getVehicleByPlate(plateNumber) {
  return request.get(`/vehicle-info/plate/${plateNumber}`)
}

export function updateVehicle(id, data) {
  return request.put(`/vehicle-info/${id}`, data)
}

export function deleteVehicle(id) {
  return request.delete(`/vehicle-info/${id}`)
}

export function deleteVehicleByPlate(plateNumber) {
  return request.delete(`/vehicle-info/plate/${plateNumber}`)
}

export function getVehicleTypeStats() {
  return request.get('/vehicle-info/stats/type')
}

export function getCategoryStats() {
  return request.get('/vehicle-info/stats/category')
}
