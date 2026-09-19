# bgssai-short mobile (Expo)

One iOS / Android app with **two login entrances** (创作者 / 运营), Boss直聘 style. Not separate admin vs user installs.

## Setup

```bash
cd clients/mobile
cp .env.example .env
# edit EXPO_PUBLIC_USER_PRODUCT_URL and EXPO_PUBLIC_ADMIN_PRODUCT_URL
npm install
npx expo start
```

Or:

```bash
EXPO_PUBLIC_USER_PRODUCT_URL=https://dev.user.bgssai-short.com \
EXPO_PUBLIC_ADMIN_PRODUCT_URL=https://dev.admin.bgssai-short.com \
npx expo start
```

## Native projects

```bash
npx expo prebuild
```

App Store / Play upload and signing are out of scope for this scaffold.
