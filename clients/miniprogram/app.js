App({
  globalData: {
    env: 'dev',
    urls: {
      dev: {
        user: 'https://dev.user.bgssai-short.com',
        admin: 'https://dev.admin.bgssai-short.com',
      },
      prod: {
        user: 'https://www.bgssai-short.com',
        admin: 'https://admin.bgssai-short.com',
      },
    },
  },
  onLaunch() {},
  getEntryUrl(entry) {
    const env = this.globalData.env === 'prod' ? 'prod' : 'dev';
    const table = this.globalData.urls[env];
    return entry === 'admin' ? table.admin : table.user;
  },
});
