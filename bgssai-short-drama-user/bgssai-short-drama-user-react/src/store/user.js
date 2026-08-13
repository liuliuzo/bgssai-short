import { create } from 'zustand'

// Login response fields (snake_case contract):
// user_id / user_name / jwt_token / role_code / role_name / real_name
// role_code: 'USER' for the user app; other roles are rejected by RequireAuth.
export const ALLOWED_ROLE = 'USER'

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
