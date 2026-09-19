'use strict';

const { app, BrowserWindow, shell, ipcMain } = require('electron');
const path = require('path');
const fs = require('fs');

/**
 * Resolve env from process or optional .env (no secrets committed).
 */
function loadDotEnv() {
  const envPath = path.join(__dirname, '.env');
  if (!fs.existsSync(envPath)) {
    return;
  }
  const text = fs.readFileSync(envPath, 'utf8');
  for (const line of text.split(/\r?\n/)) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith('#')) {
      continue;
    }
    const eq = trimmed.indexOf('=');
    if (eq <= 0) {
      continue;
    }
    const key = trimmed.slice(0, eq).trim();
    let value = trimmed.slice(eq + 1).trim();
    if (
      (value.startsWith('"') && value.endsWith('"')) ||
      (value.startsWith("'") && value.endsWith("'"))
    ) {
      value = value.slice(1, -1);
    }
    if (process.env[key] === undefined) {
      process.env[key] = value;
    }
  }
}

/**
 * Single desktop install, two login entrances (Boss-style).
 * USER_PRODUCT_URL / ADMIN_PRODUCT_URL are required.
 */
function resolveEntranceUrls() {
  loadDotEnv();
  const userUrl = String(
    process.env.USER_PRODUCT_URL ||
      process.env.EXPO_PUBLIC_USER_PRODUCT_URL ||
      process.env.PRODUCT_URL_USER ||
      ''
  ).trim();
  const adminUrl = String(
    process.env.ADMIN_PRODUCT_URL ||
      process.env.EXPO_PUBLIC_ADMIN_PRODUCT_URL ||
      process.env.PRODUCT_URL_ADMIN ||
      ''
  ).trim();
  if (!userUrl || !adminUrl) {
    console.error(
      '[bgssai-short-desktop] Set USER_PRODUCT_URL and ADMIN_PRODUCT_URL (see .env.example).'
    );
    return null;
  }
  return { userUrl, adminUrl };
}

let mainWindow = null;
let entranceUrls = null;

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

  mainWindow.loadFile(path.join(__dirname, 'chooser.html'));

  mainWindow.webContents.setWindowOpenHandler(({ url }) => {
    shell.openExternal(url);
    return { action: 'deny' };
  });

  mainWindow.on('closed', () => {
    mainWindow = null;
  });
}

ipcMain.handle('entrances:get', () => entranceUrls);

ipcMain.handle('entrances:open', (_evt, role) => {
  if (!mainWindow || !entranceUrls) {
    return { ok: false };
  }
  const url = role === 'admin' ? entranceUrls.adminUrl : entranceUrls.userUrl;
  if (!url) {
    return { ok: false };
  }
  mainWindow.loadURL(url);
  return { ok: true, role };
});

ipcMain.handle('entrances:back', () => {
  if (!mainWindow) {
    return { ok: false };
  }
  mainWindow.loadFile(path.join(__dirname, 'chooser.html'));
  return { ok: true };
});

app.whenReady().then(() => {
  entranceUrls = resolveEntranceUrls();
  if (!entranceUrls) {
    app.quit();
    return;
  }
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
