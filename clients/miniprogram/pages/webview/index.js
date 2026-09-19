Page({
  data: {
    entry: 'user',
    url: '',
    title: '用户入口',
  },
  onLoad(query) {
    const entry = query.entry === 'admin' ? 'admin' : 'user';
    const app = getApp();
    const url = app.getEntryUrl(entry);
    this.setData({
      entry,
      url,
      title: entry === 'admin' ? '管理入口' : '用户入口',
    });
    wx.setNavigationBarTitle({
      title: entry === 'admin' ? '管理入口' : '用户入口',
    });
  },
});
