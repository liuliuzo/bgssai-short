# 视频生成结果下载修复：修复设计

需求版本：[2026-09-07](../feature/launch-audit-20260907.md)。原型：[交互验收](../demo-static/web/user/launch-audit-20260907.html)。

扩展二进制下载客户端的 headers 参数，Veo 对同来源文件使用 x-goog-api-key。JDK 客户端手工处理最多五次跳转，HTTPS 不允许降级，跨主机或端口删除认证头。最终响应要求 2xx 且内容非空。参考 Google Veo 官方文件下载示例：https://ai.google.dev/gemini-api/docs/video 。

验证应使用隔离存储、模拟 HTTP 上游或单元测试，不以静态目录存在代替供应商实际调用成功。真实供应商密钥、额度、平台审核和部署环境的可用性须在上线前联调验收。
