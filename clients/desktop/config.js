'use strict';

const DEFAULTS = {
  dev: {
    user: 'https://dev.user.bgssai-short.com',
    admin: 'https://dev.admin.bgssai-short.com',
  },
  prod: {
    user: 'https://www.bgssai-short.com',
    admin: 'https://admin.bgssai-short.com',
  },
};

function resolveEnv() {
  const raw = (process.env.BGSSAI_ENV || 'dev').toLowerCase();
  return raw === 'prod' || raw === 'production' ? 'prod' : 'dev';
}

function getUrls() {
  const env = resolveEnv();
  const base = DEFAULTS[env];
  return {
    env,
    user: process.env.BGSSAI_USER_URL || base.user,
    admin: process.env.BGSSAI_ADMIN_URL || base.admin,
  };
}

module.exports = { getUrls, DEFAULTS };
