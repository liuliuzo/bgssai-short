#!/usr/bin/env node
/**
 * Build artifact sync script:
 * Copies dist/index.html + dist/assets/ into the bgssai-short-user backend static resource
 * directory, served by Spring Boot as the user-app SPA.
 *
 * Safety constraints:
 * - Only overwrites index.html and assets/; never wipes the entire static directory.
 * - Preserves any non-build files the admin backend owns under static/.
 *
 * Cross-project prerequisite: the admin backend and this project must be checked out as sibling
 * directories (../bgssai-short-user), or set BGSS_STATIC_DIR to override.
 */
import { cpSync, existsSync, rmSync, statSync } from 'node:fs'
import { join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const here = fileURLToPath(new URL('.', import.meta.url))
const distDir = resolve(here, '../dist')
const staticDir = process.env.BGSS_STATIC_DIR
  ? resolve(process.env.BGSS_STATIC_DIR)
  : resolve(here, '../../bgssai-short-user/src/main/resources/static')

const fail = (msg) => {
  console.error(`[sync-static] ${msg}`)
  process.exit(1)
}

if (!existsSync(distDir)) fail(`dist does not exist: ${distDir} — run vite build first`)
if (!existsSync(join(distDir, 'index.html'))) fail('dist/index.html not found, build output is incomplete')
if (!existsSync(staticDir)) fail(`User backend static directory not found: ${staticDir} (check sibling checkout of bgssai-short-user, or set BGSS_STATIC_DIR)`)

// Remove only the build artifacts that will be replaced (index.html + assets/), leave everything else.
const oldIndex = join(staticDir, 'index.html')
const oldAssets = join(staticDir, 'assets')
if (existsSync(oldIndex)) rmSync(oldIndex)
if (existsSync(oldAssets) && statSync(oldAssets).isDirectory()) rmSync(oldAssets, { recursive: true })

cpSync(join(distDir, 'index.html'), join(staticDir, 'index.html'))
cpSync(join(distDir, 'assets'), join(staticDir, 'assets'), { recursive: true })

console.log(`[sync-static] Synced dist/index.html + dist/assets -> ${staticDir}`)
