'use strict';

const path = require('path');
const { app, BrowserWindow, ipcMain, shell, session } = require('electron');
const { getConfig } = require('./config');

let mainWindow = null;
let currentEntryId = null;
const PARTITION = 'persist:bgssai-short';

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
      partition: PARTITION,
      spellcheck: false,
    },
  });

  mainWindow.webContents.setWindowOpenHandler(({ url }) => {
    shell.openExternal(url);
    return { action: 'deny' };
  });

  mainWindow.webContents.on('did-fail-load', (_e, code, desc, url, isMainFrame) => {
    if (!isMainFrame || !mainWindow) return;
    // Network / session errors recover to gate without duplicate side effects.
    mainWindow.loadFile(path.join(__dirname, 'entry', 'error.html'), {
      query: { code: String(code), desc: String(desc || ''), url: String(url || '') },
    });
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
  return { ok: true, entry, persistPartition: PARTITION };
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
    productName: 'BGSSAI Short',
    installable: 'electron-builder nsis/dmg/AppImage (unsigned in CI)',
    profile: config.profile,
    currentEntryId,
    persistPartition: PARTITION,
    platforms: ['win32', 'darwin', 'linux'],
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

ipcMain.handle('clients:clear-session', async () => {
  const ses = session.fromPartition(PARTITION);
  await ses.clearStorageData();
  return { ok: true };
});
