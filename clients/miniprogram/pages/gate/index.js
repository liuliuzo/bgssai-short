const { getEntries } = require('../../utils/config');

Page({
  data: {
    profile: 'dev',
    entries: [],
  },

  onShow() {
    const app = getApp();
    const bootstrap = getEntries(app.globalData.clientEnv);
    this.setData({
      profile: bootstrap.profile,
      entries: bootstrap.entries,
    });
  },

  onSelect(event) {
    const { id, url, label } = event.currentTarget.dataset;
    wx.navigateTo({
      url:
        '/pages/webview/index?entryId=' +
        encodeURIComponent(id) +
        '&url=' +
        encodeURIComponent(url) +
        '&label=' +
        encodeURIComponent(label),
    });
  },
});
