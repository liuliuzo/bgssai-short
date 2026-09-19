Page({
  data: {
    entryId: '',
    label: '',
    url: '',
  },

  onLoad(query) {
    const entryId = query.entryId ? decodeURIComponent(query.entryId) : '';
    const label = query.label ? decodeURIComponent(query.label) : '入口';
    const url = query.url ? decodeURIComponent(query.url) : '';
    this.setData({ entryId, label, url });
    if (label) {
      wx.setNavigationBarTitle({ title: label });
    }
  },

  onBack() {
    wx.navigateBack({ delta: 1 });
  },
});
