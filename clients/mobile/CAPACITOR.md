# Capacitor optional bridge

本目录主栈为 **Expo**（iOS / Android 单一 App + 双入口）。

若需原生壳打包，可在本工程上叠加 Capacitor（仍保持 **一个** App 工程，禁止拆成 admin/user 两包）：

```bash
npx cap init "BGSSAI Short" com.bgssai.short
npx cap add ios
npx cap add android
```

入口选择与 Web 基址切换逻辑仍以 `App.js` / `config.js` 为准。
