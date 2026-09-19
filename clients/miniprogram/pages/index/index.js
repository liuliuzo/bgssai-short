const config = require('../../config.js');

Page({
  data: {
    userProductUrl: config.userProductUrl,
    adminProductUrl: config.adminProductUrl,
  },

  openUser() {
    wx.navigateTo({
      url: '/pages/webview/webview?role=user',
    });
  },

  openAdmin() {
    wx.navigateTo({
      url: '/pages/webview/webview?role=admin',
    });
  },
});
