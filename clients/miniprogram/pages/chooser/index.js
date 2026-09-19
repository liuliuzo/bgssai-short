Page({
  data: {
    env: 'dev',
  },
  onShow() {
    const app = getApp();
    this.setData({ env: app.globalData.env || 'dev' });
  },
  openUser() {
    wx.navigateTo({
      url: '/pages/webview/index?entry=user',
    });
  },
  openAdmin() {
    wx.navigateTo({
      url: '/pages/webview/index?entry=admin',
    });
  },
});
