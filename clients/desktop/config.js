'use strict';

/**
 * Dual-entry Web base URLs for the single desktop installable.
 * Profile: BGSSAI_CLIENT_ENV = local | dev | prod (default: dev)
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

function resolveProfile() {
  const raw = (process.env.BGSSAI_CLIENT_ENV || 'dev').toLowerCase();
  if (Object.prototype.hasOwnProperty.call(PROFILES, raw)) {
    return raw;
  }
  return 'dev';
}

function getConfig() {
  const profile = resolveProfile();
  return {
    profile,
    entries: {
      user: { id: 'user', label: '用户入口', url: PROFILES[profile].user },
      admin: { id: 'admin', label: '管理入口', url: PROFILES[profile].admin },
    },
  };
}

module.exports = { getConfig, PROFILES };
