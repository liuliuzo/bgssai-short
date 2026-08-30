# 功能名称（feature-id）

> 复制本模板新建功能文档。删除本引用块与所有 `<...>` 占位说明。

## 一、背景与目标

- 背景：`<为什么要做，解决什么问题>`
- 目标：`<本功能达成的可度量结果>`
- 关联角色：`<PLATFORM_ADMIN / USER / ...>`

## 二、需求澄清（WHAT）

- 用户故事：作为 `<角色>`，我希望 `<能力>`，以便 `<收益>`。
- 业务规则：`<规则 1 / 规则 2 ...>`

## 三、验收标准

1. `<可验证的验收条件 1>`
2. `<可验证的验收条件 2>`

## 四、接口契约（snake_case）

> 详细契约同步登记到 `docs/api/`；此处给出关键字段以便评审。

- 方法与路径：`POST /bgssai/<side>/<resource>`（POST 业务参数走 JSON Body）
- 请求体：
  ```json
  { "field_a": "string", "page_num": 1, "page_size": 20 }
  ```
- 响应体（统一封装）：
  ```json
  { "code": "0", "message": "OK", "success": true, "result": {},
    "page_num": 1, "page_size": 20, "total_size": 0 }
  ```
- 鉴权：`<公开 / 需 @NeedAop（请求头 Jwttoken）/ 需角色 X>`
- 错误码：`<4001 凭证错误 / 4003 无权限 / 2002 token 失效 / 429 限流 / ...>`

## 五、流程 / 状态机（须配图）

`<在此嵌入或链接自包含 HTML 图：流程图 / 时序图 / 状态泳道图。可复制 ../architecture/_template.html>`

## 六、数据模型影响

- 表：`<表名>`，新增 / 变更列：`<列名 类型 说明>`（同步 `sql/DDL.sql` 与 MyBatis 持久层三件套）。

## 七、原型

- 涉及端：`<web 管理端 / web 用户端 / ios / android>`（按实际涉及端勾选，逐端落原型）
- 页面：`../demo-static/web/admin/<page>.html`、`../demo-static/web/user/<page>.html`
  （移动端为 `../demo-static/ios/<page>.html`、`../demo-static/android/<page>.html`）
- 跳转关系：登记到**涉及端各自**的 `PAGE-FLOW.html`（如 `../demo-static/web/admin/PAGE-FLOW.html`）
- 页面 → API 映射：同步 `../api/page-api-mapping.md` 对应行

## 八、四层同步检查清单

- [ ] 需求（本文件 / `PRODUCT_VISION.md`）
- [ ] 原型（`docs/demo-static/<端>/**`，涉及的每个端目录及其 `PAGE-FLOW.html`）
- [ ] 设计与契约（`docs/architecture` / `docs/api`，含 `docs/api/page-api-mapping.md`；涉安全含 `docs/security`）
- [ ] 代码与数据库（`src/**`、前端工程、`sql/**`）
