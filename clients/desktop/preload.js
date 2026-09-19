'use strict';

const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('bgssaiClients', {
  getBootstrap: () => ipcRenderer.invoke('clients:get-bootstrap'),
  openEntry: (entryId) => ipcRenderer.invoke('clients:open-entry', entryId),
  backToGate: () => ipcRenderer.invoke('clients:back-to-gate'),
});
