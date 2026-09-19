'use strict';

const { app, BrowserWindow, ipcMain, shell } = require('electron');
const path = require('path');
const { getUrls } = require('./config');

let mainWindow = null;
const urls = getUrls();

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1280,
    height: 800,
    minWidth: 960,
    minHeight: 640,
    title: 'BGSSAI Short',
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: true,
    },
  });

  mainWindow.loadFile(path.join(__dirname, 'renderer', 'index.html'));

  mainWindow.webContents.setWindowOpenHandler(({ url }) => {
    shell.openExternal(url);
    return { action: 'deny' };
  });

  mainWindow.on('closed', () => {
    mainWindow = null;
  });
}

ipcMain.handle('clients:get-config', () => ({
  env: urls.env,
  userUrl: urls.user,
  adminUrl: urls.admin,
}));

ipcMain.handle('clients:open-entry', (_event, entry) => {
  if (!mainWindow) {
    return { ok: false, error: 'no-window' };
  }
  const target = entry === 'admin' ? urls.admin : urls.user;
  if (!target) {
    return { ok: false, error: 'missing-url' };
  }
  mainWindow.loadURL(target);
  return { ok: true, entry, url: target };
});

ipcMain.handle('clients:back-to-chooser', () => {
  if (!mainWindow) {
    return { ok: false };
  }
  mainWindow.loadFile(path.join(__dirname, 'renderer', 'index.html'));
  return { ok: true };
});

app.whenReady().then(() => {
  createWindow();
  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createWindow();
    }
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});
