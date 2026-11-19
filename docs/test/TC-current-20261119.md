# 账户密钥与分镜制作

被测版本为 bgssai-short 的 develop 提交 `0f1d97337c5da416590f27ee2211d81b1f8ebccb`。使用隔离测试数据，执行后记录环境、执行时间、构建 SHA 和脱敏证据。

版本依据为 [user-custom-ai-keys.md](../../docs/feature/user-custom-ai-keys.md)、[storyboard-workbench.md](../../docs/feature/storyboard-workbench.md)、[modelKeySelection.test.js](../../bgssai-short-user/bgssai-short-user-react/src/pages/account/modelKeySelection.test.js)。

| 用例ID | 关联需求 | 优先级 | 端 | 前置条件与数据 | 测试步骤 | 期望结果 | 结果 | 备注 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| TC-SHORT-20261119-01-P | REQ-SHORT-20261119-01 | P0 | user | 已登录，在 /account 为同一供应商准备两把不同名称的测试凭据 | 选中第一把并为文本能力启用模型，切换到第二把，刷新账号页 | 当前密钥切为第二把，模型和能力选择不变，列表只显示掩码 | 待测 | 密钥切换 |
| TC-SHORT-20261119-01-N | REQ-SHORT-20261119-01 | P0 | user | 文本能力有自有模型，自建模型可用 | 在模型选择中停用自有文本模型，查看文本任务的可选模型；再停用自建文本模型 | 第一次可选 BGSSAI 默认文本模型；两者均停用后文本能力不可用，其他能力不变 | 待测 | 模型停用与回退 |
| TC-SHORT-20261119-02-P | REQ-SHORT-20261119-02 | P1 | user | 测试集有可编辑镜头，记录其运镜与台词 | 先选二维动画，再选水墨动画；再点一次水墨动画 | 同镜头只选中最后一种风格；再次点击取消，运镜与台词保留 | 待测 | 镜头画面风格 |
| TC-SHORT-20261119-02-N | REQ-SHORT-20261119-02 | P1 | user | 准备一个已锁定镜头和一个可编辑镜头 | 在列表和画布查看两镜头的运镜；修改可编辑镜头后刷新 | 锁定镜头只读；可编辑镜头显示新运镜，已有素材保留 | 待测 | 锁定镜头 |

## 本地回归

在已安装仓库开发依赖的环境中，从仓库根目录执行以下命令。记录实际运行的测试数和失败项；界面、真机和外部集成按上表另行验收。

```text
node --test bgssai-short-user/bgssai-short-user-react/src/pages/account/modelKeySelection.test.js
```
