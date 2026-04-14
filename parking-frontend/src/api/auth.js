import request from '@/utils/request'

export function login(data) {
  return request.post('/auth/login', data)
}

export function register(data) {
  return request.post('/auth/register', data)
}

export function checkAuth() {
  return request.get('/auth/check')
}

export function getToken() {
  return localStorage.getItem('token')
}

export function setToken(token) {
  localStorage.setItem('token', token)
}

export function removeToken() {
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
}

export function getUserInfo() {
  const info = localStorage.getItem('userInfo')
  if (!info || info === 'undefined' || info === 'null') {
    return null
  }
  try {
    return JSON.parse(info)
  } catch {
    return null
  }
}

export function setUserInfo(info) {
  localStorage.setItem('userInfo', JSON.stringify(info))
}

export function isAuthenticated() {
  return !!getToken()
}
