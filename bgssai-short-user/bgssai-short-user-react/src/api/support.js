import http from '../utils/http'

const GUEST_KEY = 'bgssai_short_cs_guest_token'

export function getGuestToken() {
  try {
    return localStorage.getItem(GUEST_KEY) || ''
  } catch {
    return ''
  }
}

export function setGuestToken(token) {
  if (!token) return
  try {
    localStorage.setItem(GUEST_KEY, token)
  } catch {
    /* ignore */
  }
}

const createSession = (body) => http.post('/user/support/sessions/create', body)
const listMine = (body) => http.post('/user/support/sessions/mine', body)
const detail = (body) => http.post('/user/support/sessions/detail', body)
const send = (body) => http.post('/user/support/messages/send', body)

export default { createSession, listMine, detail, send }
