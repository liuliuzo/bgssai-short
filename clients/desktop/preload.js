'use strict';

const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('bgssaiClients', {
  getConfig: () => ipcRenderer.invoke('clients:get-config'),
  openEntry: (entry) => ipcRenderer.invoke('clients:open-entry', entry),
  backToChooser: () => ipcRenderer.invoke('clients:back-to-chooser'),
});
