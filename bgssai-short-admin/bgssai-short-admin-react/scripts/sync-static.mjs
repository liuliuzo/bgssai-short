#!/usr/bin/env node
/**
 * Build artifact sync script:
 * Copies the built SPA, root icons, and brand assets into the bgssai-short-admin
 * backend static resource directory, served by Spring Boot as the admin console SPA.
 *
 * Safety constraints:
 * - Only overwrites managed SPA and brand entries; never wipes the entire static directory.
 * - Preserves any non-build files the admin backend owns under static/.
 *
 * Cross-project prerequisite: the admin backend and this project must be checked out as sibling
 * directories (../bgssai-short-admin), or set BGSS_STATIC_DIR to override.
 */
import { cpSync, existsSync, rmSync } from 'node:fs'
import { join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const here = fileURLToPath(new URL('.', import.meta.url))
const distDir = resolve(here, '../dist')
const staticDir = process.env.BGSS_STATIC_DIR
  ? resolve(process.env.BGSS_STATIC_DIR)
  : resolve(here, '../../bgssai-short-admin/src/main/resources/static')
const ROOT_FILES = ['favicon.png', 'favicon.ico', 'apple-touch-icon.png']

const fail = (msg) => {
  console.error(`[sync-static] ${msg}`)
  process.exit(1)
}

if (!existsSync(distDir)) fail(`dist does not exist: ${distDir} — run vite build first`)
if (!existsSync(join(distDir, 'index.html'))) fail('dist/index.html not found, build output is incomplete')
if (!existsSync(join(distDir, 'assets'))) fail('dist/assets not found, build output is incomplete')
if (!existsSync(join(distDir, 'brand'))) fail('dist/brand not found, brand assets are missing')
for (const file of ROOT_FILES) {
  if (!existsSync(join(distDir, file))) fail(`dist/${file} not found, icon assets are missing`)
}
if (!existsSync(staticDir)) fail(`Admin backend static directory not found: ${staticDir} (check sibling checkout of bgssai-short-admin, or set BGSS_STATIC_DIR)`)

// Remove only managed build artifacts, including the retired SVG favicon.
for (const entry of ['index.html', 'assets', 'brand', 'favicon.svg', ...ROOT_FILES]) {
  rmSync(join(staticDir, entry), { recursive: true, force: true })
}

cpSync(join(distDir, 'index.html'), join(staticDir, 'index.html'))
cpSync(join(distDir, 'assets'), join(staticDir, 'assets'), { recursive: true })
cpSync(join(distDir, 'brand'), join(staticDir, 'brand'), { recursive: true })
for (const file of ROOT_FILES) {
  cpSync(join(distDir, file), join(staticDir, file))
}

console.log(`[sync-static] Synced SPA, root icons, and brand assets -> ${staticDir}`)
