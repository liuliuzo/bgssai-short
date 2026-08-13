import { create } from 'zustand'

// Login response fields (snake_case contract):
// user_id / user_name / jwt_token / role_code / role_name / real_name
// role_code: 'PLATFORM_ADMIN' for the admin console; other roles are rejected by RequireAuth.
export const ALLOWED_ROLE = 'PLATFORM_ADMIN'

const readCached = () => {
  try {
    const raw = localStorage.getItem('userInfo')
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

export const useUserStore = create((set) => ({
  userInfo: readCached(),
  setUserInfo: (userInfo) => {
    localStorage.setItem('userInfo', JSON.stringify(userInfo))
    set({ userInfo })
  },
  clearUserInfo: () => {
    localStorage.removeItem('userInfo')
    set({ userInfo: null })
  },
}))

export const isAllowedRole = (userInfo) =>
  !!userInfo && userInfo.role_code === ALLOWED_ROLE
