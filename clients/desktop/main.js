'use strict';

const path = require('path');
const { app, BrowserWindow, ipcMain, shell } = require('electron');
const { getConfig } = require('./config');

let mainWindow = null;
let currentEntryId = null;

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

  mainWindow.webContents.setWindowOpenHandler(({ url }) => {
    shell.openExternal(url);
    return { action: 'deny' };
  });

  showEntryGate();
}

function showEntryGate() {
  currentEntryId = null;
  mainWindow.loadFile(path.join(__dirname, 'entry', 'index.html'));
}

function openEntry(entryId) {
  const config = getConfig();
  const entry = config.entries[entryId];
  if (!entry) {
    return { ok: false, message: 'unknown entry' };
  }
  currentEntryId = entryId;
  mainWindow.loadURL(entry.url);
  return { ok: true, entry };
}

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

ipcMain.handle('clients:get-bootstrap', () => {
  const config = getConfig();
  return {
    product: 'bgssai-short',
    installable: 'single-app dual-entry',
    profile: config.profile,
    currentEntryId,
    entries: Object.values(config.entries).map((e) => ({
      id: e.id,
      label: e.label,
      url: e.url,
    })),
  };
});

ipcMain.handle('clients:open-entry', (_event, entryId) => openEntry(entryId));

ipcMain.handle('clients:back-to-gate', () => {
  showEntryGate();
  return { ok: true };
});
