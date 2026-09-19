/**
 * Dual-entry Web base URLs for the single WeChat miniprogram.
 * Switch profile in app.globalData.clientEnv or here.
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

function resolveProfile(clientEnv) {
  const raw = String(clientEnv || 'dev').toLowerCase();
  if (Object.prototype.hasOwnProperty.call(PROFILES, raw)) {
    return raw;
  }
  return 'dev';
}

function getEntries(clientEnv) {
  const profile = resolveProfile(clientEnv);
  const urls = PROFILES[profile];
  return {
    profile,
    entries: [
      { id: 'user', label: '用户入口', url: urls.user },
      { id: 'admin', label: '管理入口', url: urls.admin },
    ],
  };
}

module.exports = {
  PROFILES,
  getEntries,
};
