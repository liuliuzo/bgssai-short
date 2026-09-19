const config = require('../../config.js');

Page({
  data: {
    productUrl: '',
    roleLabel: '',
  },

  onLoad(query) {
    const role = query && query.role === 'admin' ? 'admin' : 'user';
    const productUrl =
      role === 'admin' ? config.adminProductUrl : config.userProductUrl;
    this.setData({
      productUrl,
      roleLabel: role === 'admin' ? '运营' : '创作者',
    });
    wx.setNavigationBarTitle({
      title: role === 'admin' ? '运营登录' : '创作者登录',
    });
  },
});
