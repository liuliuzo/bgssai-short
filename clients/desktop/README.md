# bgssai-short desktop (Electron)

One install for Windows / macOS / Linux. **Two login entrances** (创作者 / 运营), same pattern as Boss直聘 — not separate admin vs user apps.

## Setup

```bash
cd clients/desktop
cp .env.example .env
# edit USER_PRODUCT_URL and ADMIN_PRODUCT_URL if needed
npm install
npm start
```

Or:

```bash
USER_PRODUCT_URL=https://dev.user.bgssai-short.com \
ADMIN_PRODUCT_URL=https://dev.admin.bgssai-short.com \
npm start
```

Chooser loads first; each entrance opens the matching Web UI.

## Pack (local only)

```bash
npm run pack:win
npm run pack:mac
npm run pack:linux
```

Signing / store upload / DNS are out of scope for this scaffold.
