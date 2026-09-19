'use strict';

const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('bgssaiDesktop', {
  platform: process.platform,
  shell: 'electron',
  getEntrances: () => ipcRenderer.invoke('entrances:get'),
  openEntrance: (role) => ipcRenderer.invoke('entrances:open', role),
  backToChooser: () => ipcRenderer.invoke('entrances:back'),
});
