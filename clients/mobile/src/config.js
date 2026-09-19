/**
 * Dual-entry Web base URLs for the single mobile installable (iOS + Android).
 * Override profile with EXPO_PUBLIC_BGSSAI_CLIENT_ENV = local | dev | prod
 */
const PROFILES = {
  local: {
    user: 'http://localhost:3002',
    admin: 'http://localhost:3001',
  },
  dev: {
    user: 'https://dev.user.bgssai-short.com',
    admin: 'https://dev.admin.bgssai-short.com',
  },
  prod: {
    user: 'https://user.bgssai-short.com',
    admin: 'https://admin.bgssai-short.com',
  },
};

export function resolveProfile() {
  const raw = String(
    process.env.EXPO_PUBLIC_BGSSAI_CLIENT_ENV || 'dev',
  ).toLowerCase();
  if (Object.prototype.hasOwnProperty.call(PROFILES, raw)) {
    return raw;
  }
  return 'dev';
}

export function getEntries() {
  const profile = resolveProfile();
  const urls = PROFILES[profile];
  return {
    profile,
    entries: [
      { id: 'user', label: '用户入口', url: urls.user },
      { id: 'admin', label: '管理入口', url: urls.admin },
    ],
  };
}
