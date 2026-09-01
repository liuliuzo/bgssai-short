# BGSSAI 全局开发规范（强制执行指令）

## 0. 核心基准与执行指令

* **【最高准则】全产出版本一致（强制，违者代码不予合并；凌驾于便利性 / 进度之上）**：本项目的**一切产出必须以需求为单一版本基准、全部保持版本一致**——需求（`docs/feature/**`）是**版本基准与单一事实来源**；设计与技术方案、原型、应用代码、数据库脚本，以及配置 / 脚本 / 文档 / 测试等一切下游产出，必须始终对齐到**需求的当前版本**、保持同一版本一致。这是本规范的**最高准则**，**凌驾于开发便利性、交付进度之上**；当它与本规范任何其它条款冲突时，**一致性优先**。任何会话**严禁**以「赶进度」「先跑通回头补」「只改一层省事」等任何理由偏离本准则。
    * **严禁任一层版本滞后、抢跑或偏离需求当前版本**：需求版本一旦变更，必须把所有下游产出同步推进到该版本后方可合入；不允许某层停留在旧版本需求（滞后），也不允许某层超前实现尚未定稿的需求（抢跑 / 版本漂移）。
  * **覆盖产出（穷举本仓库实际分层与路径）**：
    1. **功能需求**：`docs/feature/**`（需求 WHAT + 详细功能设计）；
    2. **设计与技术方案**：`docs/architecture`·`docs/api`（HOW，含数据模型、状态机、接口契约）；
    3. **原型**：`docs/demo-static/<端>/*.html`（可运行 HTML 原型 + 页面跳转描述，均为 HTML 形式；端目录为 `web/admin`、`web/user`、`ios`、`android`，各端概览入口为该目录 `index.html`、跳转真值为该目录 `PAGE-FLOW.html`）；
    4. **应用代码**：`src/main/**`（Java 业务代码、Mapper XML、静态资源）；
    5. **数据库脚本**：schema / DDL / DML——统一收敛到仓库根 `sql/**`；
    6. **配置 / 脚本 / 文档 / 测试**：`application*.properties`、`scripts/**`、`docs/**`、`src/test/**` 等其它一切产出。
  * **必须逐字对齐的口径（同一概念全量对齐到需求当前版本）**：字段命名（**一律 snake_case**）、响应结构、枚举 / 状态值 / 状态机、分页 / 错误码契约、页面跳转关系、数据模型——**同一概念在各层产出中必须逐字对齐到需求的当前版本，任一层不得擅自偏离或停留在旧版本**。
  * **落地机制（引用并强化既有规约，服务于「需求为版本锚点、下游逐层对齐到同一版本」）**：
    1. **单向依赖链即版本传播方向**：严格遵循「PRD → 原型 → 架构 → 代码」单向依赖链推进（见 CLAUDE.md「新功能必经链路」），版本沿此链自需求向下游单向传播，下游一律以上游为准，并最终对齐到需求当前版本；
    2. **原型即字段契约权威**：接口 JSON 字段命名 / 响应结构 / 状态机以原型（`docs/demo-static/**`）为权威参照，后端实现逐字对齐（见 §1 snake_case、§9 / §10）；
    3. **决策传播即版本同步**：删除 / 重命名任何枚举 · 字段 · 状态 · 接口时，必须按 §11「决策传播规约」对被删 / 被改标识符做**全仓 grep 一致同步**（覆盖 PRD / 原型 / 架构 / 数据库 / 代码各层），使各层一并推进到需求新版本，**严禁只改一层就合入**；
    4. **与既有强制约定并行不悖**：本准则与仓库 POST→body、Conventional Commits 等强制约定（见 CLAUDE.md「仓库强制约定」、§3）并行不悖，不得相互架空。
  * **CR 校验（Blocker 红线）**：CR 阶段发现设计 / 原型 / 代码 / 数据库等**任一产出与需求当前版本不一致**（版本滞后 / 抢跑 / 偏离），一律按 **Blocker 打回、PR 不予合并**，无论改动规模大小、无论是否「只是一处字段 / 只是文档」。本条与「严禁使用 Lombok」「JSON 强制 snake_case」「全局禁用 emoji」**同档次**，是**跨需求 / 设计 / 原型 / 代码 / 数据库**的全局硬红线。
* **【最高准则】需求与缺陷变更四层同步完成、缺一不可（强制，违者代码不予合并）**：任何一次**需求变更（新增 / 修改 / 删除功能）或缺陷修复（bug fix）**，都必须在**同一次变更（同一 PR / 提交范围）内一次性同步完成以下四层产出的对应修改，缺一不可**，以从根本上杜绝代码与文档漂移：
  * **四层同步范围（缺一不可）**：
    1. **需求文档**：`docs/feature/**`（PRD + 详细功能设计）及受影响的 `PRODUCT_VISION.md`；
    2. **原型**：`docs/demo-static/**`（可运行 HTML 原型；按端分目录 `web/admin`、`web/user`、`ios`、`android`，各端概览入口为该目录 `index.html`、跳转真值为该目录 `PAGE-FLOW.html`）；
    3. **设计文档**：`docs/architecture` · `docs/api`（涉安全另含 `docs/security`）——含数据模型、状态机、接口契约；
    4. **代码与数据库**：应用代码（`src/main/**`、前端工程）与数据库脚本（`sql/**`）。
  * **完整性判定**：一次变更只要触及上述任一层的语义（字段 / 枚举 / 状态 / 接口 / 页面跳转 / 业务规则），即视为四层全部相关，必须逐层核对并同步到位；某层确无需改动的，须在提交说明中显式说明「该层不涉及」的理由，不得默认省略、不得留待「以后补」。
  * **禁止（反例，违者打回）**：只改代码而不回改需求 / 原型 / 设计文档；只补文档而不落代码；bug 修复只改代码却不回溯修订对应的需求 / 原型 / 设计（造成文档与实现长期漂移）；把四层中任意一层拆到后续 PR「回头补」。
  * **与既有准则的关系**：本条是本节「全产出版本一致」最高准则在**单次变更粒度**上的强制落地——前者要求最终各层必须同版本，本条要求每一次需求 / bug 变更都必须四层同步完成、当次交清；并与「设计必须干净彻底」反例「只改代码不同步改文档 / SQL / 原型 / 测试」相互印证。
  * **CR 校验（Blocker 红线）**：CR 阶段发现某次需求 / bug 变更只落了部分层（如只改代码未同步需求 / 原型 / 设计文档），一律按 **Blocker** 打回、PR 不予合并，**无论改动规模大小**。本条与「全产出版本一致」「严禁使用 Lombok」「JSON 强制 snake_case」「全局禁用 emoji」**同档次**，是全局硬红线。
* **【最高准则】功能研发必经顺序：需求 → 原型 → 详设 → 代码（强制，违者代码不予合并）**：任何**功能需求（新增 / 修改 / 删除对外功能）**的研发，必须严格按 **需求文档 → 原型设计 HTML → 详细设计文档 → 代码编写** 的先后顺序推进；上游产物未定稿并落盘前，不得启动下游，顺序**不可颠倒、不可跳跃**。本条是 CLAUDE.md「新功能必经链路」单向依赖在**研发时序**上的强制落地，与「需求与缺陷变更四层同步完成、缺一不可」互为表里——前者管**顺序**（上游先行、逐层向下），后者管**完整**（同一次变更四层齐备、当次交清）。
  * **四阶段与产出物（顺序即依赖，前序未定稿不得进入后序）**：
    1. **需求文档（WHAT，版本基准）**：`docs/feature/**`（PRD + 详细功能设计）及受影响的 `PRODUCT_VISION.md`——先定稿需求，作为版本基准与单一事实来源；
    2. **原型设计 HTML**：`docs/demo-static/**`（可运行 HTML 原型；按端分目录 `web/admin`、`web/user`、`ios`、`android`，各端概览入口为该目录 `index.html`、跳转真值为该目录 `PAGE-FLOW.html`）——依据已定稿需求产出 HTML 原型，作为 API 字段命名 / 响应结构 / 枚举 / 状态值 / 页面跳转的权威参照（§9 / §10）；
    3. **详细设计文档（HOW）**：`docs/architecture` · `docs/api`（涉安全另含 `docs/security`）——含数据模型、状态机、API 契约、页面 → API 映射，逐字对齐已定稿原型；
    4. **代码编写**：应用代码（`src/main/**`、前端工程）与数据库脚本（`sql/**`）——按已定稿详设落地实现。
  * **非功能需求除外（豁免范围）**：**不改变对外功能契约**（字段 / 枚举 / 状态 / 接口语义 / 页面跳转 / 业务规则）的**非功能需求**，不强制走「原型设计 HTML」与「详细设计文档」两步，含但不限于：性能 / 并发调优、内部重构与代码整洁、依赖 / 框架升级、配置 / 构建 / CI-CD / 部署脚本调整、日志与可观测性、纯安全加固（不改契约者）、技术债清理、文档 / 注释订正、测试补强等。此类变更仍须遵守本规范其余全部硬红线（Lombok / snake_case / emoji / 每次 PR 可运行等）；一旦触及对外功能语义，即回落为「功能需求」、必须补齐上述四阶段顺序。
  * **禁止（反例，违者打回）**：需求未定稿即先写原型 / 直接编码；跳过原型或详设直接实现功能；先写代码事后补需求 / 原型 / 详设（倒序补文档，造成文档与实现漂移）；把「原型设计 HTML」「详细设计文档」当作对功能需求可选、可略过的步骤。
  * **CR 校验（Blocker 红线）**：CR 阶段发现某功能需求缺失上游产物或时序倒置（如无原型 / 无详设即提交功能代码、或事后补文档），一律按 **Blocker** 打回、PR 不予合并，**无论改动规模大小**。本条与「需求与缺陷变更四层同步完成」「设计必须干净彻底」「严禁使用 Lombok」「JSON 强制 snake_case」「全局禁用 emoji」**同档次**，是全局硬红线。
* **设计必须干净彻底，永不因改动量妥协（最高准则，强制，违者代码不予合并）**：永远不要因为"改动多 / 影响面大 / 涉及文件多 / 赶进度"而在设计上妥协——设计与重构必须做到**干净、彻底、不留半吊子**。一旦判定某个设计 / 数据模型 / 接口 / 命名应当变更，就必须把它在**所有受影响的层一次性彻底改干净**，宁可大范围改动，也绝不打补丁、不留兼容旁路、不保留死代码。
  * **禁止（反例，违者打回）**：
    1. 为少改文件 / 少动代码，保留旧的错误设计，在旁边加 `if-else` / adapter / 兼容层 / 双轨实现绕过；
    2. 改一半就合入——只改 Controller 不改 Service、只改一处引用不改其余、只改代码不同步改文档 / SQL / 原型 / 测试；
    3. 重命名 / 删除字段 · 枚举 · 状态 · 接口时只改部分引用，留下全仓不一致（违反 §11「决策传播规约」）；
    4. 保留死代码 / 注释掉的旧实现 / "暂时留着回头删"的兼容分支 / 仅 `@Deprecated` 占位却不迁移调用方；
    5. 以"改动太多""影响面太大""赶进度""先跑通回头补"为由，把本应彻底的重构降级为打补丁。
  * **要求（正例，强制）**：
    1. 判定该改，就把改动**贯穿所有受影响层**（产品需求 / 原型 / 架构与 API 契约 / 数据模型 SQL / 应用代码 / 配置 / 测试）一次性改到位、改干净；
    2. 删除即彻底删除——含全仓 `grep` 清理一切引用与残留（呼应 §11「决策传播规约」、§0「【最高准则】全产出版本一致」）；
    3. **改动量大不是降低标准的理由，恰恰是更要把传播做完整的理由**。
  * **与"可运行版本"门槛并行不悖**：本条要求"彻底"，但**不等于**一个巨型不可运行 PR——大改可按 §0「每次 PR 提交必须是一个可运行版本」拆成多个**各自可运行的里程碑** PR；但每个里程碑**内部同样必须干净彻底**，严禁把"不彻底 / 半吊子兼容"伪装成"分步落地"。
  * **优先级**：与"便利性 / 进度"冲突时，**干净彻底优先**——与「【最高准则】全产出版本一致」「严禁使用 Lombok」「JSON 强制 snake_case」「全局禁用 emoji」**同档次**，是全局硬红线。
  * **CR 校验（Blocker 红线）**：CR 阶段发现"为少改而保留的兼容层 / 旁路 / 死代码 / 半吊子重构 / 只改一层就合入"，一律按 **Blocker** 打回、PR 不予合并，**无论改动规模大小**。
* **前置验证**：在生成或修改任何代码前，必须自我校验是否完全符合本规范。
* **适用范围**：本规范适用于所有 bgssai.* 命名空间下的微服务及业务系统（包括但不限于 geo, saas 等）。
* **冲突解决原则**：当通用行业规范（如《阿里巴巴 Java 开发手册》）与本项目自定义核心规约冲突时，**绝对优先服从项目自定义规约**。
* **编译验证时机（强制）**：开发过程中**严禁频繁执行 `mvn compile`** 进行边写边编译式的校验。正确流程是：所有功能/重构改动**全部落地完成后**，在 develop 分支上**统一执行一次** `mvn compile`（或 `mvn -q -DskipTests compile`）进行最终编译校验即可。避免单次修改后立即跑编译造成的反复等待，节省时间与 token。若一次编译报错，**集中**修复后再跑一次，禁止"改一行 → 编译 → 改一行 → 编译"的低效循环。
* **每次 PR 提交必须是一个可运行版本（强制，违者代码不予合并）**：每一次 PR / 合并请求（Merge Request）所提交的最终状态，都必须是一个**可独立运行的完整版本**——即该分支必须能通过编译（`mvn compile` / `mvn -q -DskipTests compile` 零错误）并成功启动，核心业务链路功能可用。**严禁**提交处于"半成品"、"编译不过"、"启动即崩"、"功能断裂"等任何不可运行中间态的代码。
  * **拆分提交也须各自可运行**：若一个较大需求需拆成多次 PR 逐步落地，则每一次 PR 都必须自成一个**可运行的里程碑**——未完成的部分通过功能开关（Feature Flag）/ 灰度隐藏，或保持旧逻辑可用，绝不允许把不可运行的半成品中间态直接合入目标分支。
  * **提交前自检清单**：发起 PR 前，开发者须在本地确认（1）编译零错误；（2）应用可正常启动；（3）**单元测试 `mvn test` 全部通过**；（4）本次改动涉及的核心链路经手工验证可用（呼应 §8「测试规约」：单元测试 + 本地手工验证 + Code Review + 灰度回归共同保障质量）。四项任一不满足，禁止提交 PR。
* **产出物表达约束**：本仓库所有产出物（代码 / 文档 / 原型 / 配置 / 脚本 / 提交信息 / 评审记录 / AI 助手回信等）一律**禁用 emoji 及任何装饰性图形符号**，详见 §1「全局禁用 emoji」条款。

## 1. 项目自定义核心规约 (最高优先级)

* **严禁使用 Lombok**
* **JSON 序列化策略与特殊字段命名**：API 接口的 JSON 响应及请求字段统一强制使用 snake_case（下划线命名法），严禁在接口层暴露 camelCase 驼峰命名（例如必须使用 stack_trace，严禁使用 stackTrace）。（注：Java 类内部属性仍遵循驼峰命名，通过底层 Jackson 注解或全局配置处理转换）。
* **极致平铺的代码风格 (Flat Code)**：业务主干逻辑必须在单个方法内自顶向下平铺直叙。**严格限制抽取子方法**，除非是跨类复用的纯工具型方法。允许方法较长，以换取阅读时无需进行上下文跳转的连贯性。
* **代码尽可能解耦 / 单接口单 Controller（强制）**：代码必须最大化解耦，**一个 HTTP 接口对应一个独立的 `XxxController.java` 文件**，严禁在同一个 Controller 类中聚合多个端点。每个 Controller 类内**仅含一个**处理方法（`@GetMapping` / `@PostMapping` / ...），且 Controller 内代码**越少越好**（理想 < 50 行，含 import）—— 仅负责参数绑定、权限注解、调用 Service、组装 Response，所有业务分支、数据库查询、跨实体组装、循环计算等逻辑一律下沉到 Service 层。详细命名、路由、DTO、helper 去 Controller 化等规约见 §3"单接口单 Controller 契约"。
* **全局禁用 emoji（强制，违者代码不予合并）**：本仓库及本项目体系下的**一切产出物**严禁出现 emoji 字符及任何装饰性图形符号；违者一律 CR 阶段打回，含相关符号的 PR / MR 不予合并。
  * **覆盖范围（穷举，不留模糊空间）**：
    1. Java 源码（`src/**`）：标识符、字符串字面量、行内 / 块注释、Javadoc、常量值；
    2. 资源与配置文件：`application*.properties` / `application*.yml` / `*.json` / `*.xml`（含 Mapper XML） / `*.sql`（DDL / DML / DQL / DCL / 种子 / 备份脚本） / `*.toml` / `.env*`；
    3. 文档：`docs/**` 下所有 Markdown / HTML，含但不限于 `docs/architecture/**`、`docs/api/**`、`docs/feature/**`、`docs/project/**`、`docs/review/**`、`docs/security/**`、`docs/demo-static/**`、根级 `README.md` 等；
    4. 原型：`docs/demo-static/**` 下所有 HTML 原型的可见文本、HTML 注释、内联 mock 数据字段值与示例字符串；
    5. 构建与自动化：`pom.xml`、`Dockerfile*`、`docker-compose*`、`.github/**`（Issue / PR 模板与 Actions 工作流）、`deploy/**`、`Jenkinsfile`、`.gitlab-ci.yml`；
    6. Git 与协作工件：分支名、tag 名、commit message（标题 + 正文 + footer）、PR / MR 标题与描述、Issue 标题与描述、Code Review 评论；
    7. AI 助手产出物：AI 编码助手在本仓库语境下生成的一切文字（含回报正文与回信正文）。
  * **禁止对象（穷举）**：
    1. Unicode emoji（含 emoji 表情、emoji 序列、emoji 修饰符、肤色 / 性别 / 旗帜 ZWJ 组合等所有 emoji 平面字符）；
    2. emoji 风格的图形符号与装饰符号（例如 ✅ ❌ ⚠ ⚡ 🔥 💡 📋 👀 🎯 ✨ ⭐ ✔ ✖ ➜ ➤ ► ◆ ● ■ ★ ☆ 等任何作"标注 / 强调 / 装饰"用途的图形符号）；
    3. kaomoji 颜文字（如 `(｡◕‿◕｡)` / `¯\_(ツ)_/¯` / `(╯°□°）╯︵ ┻━┻` 等）；
    4. 装饰性 ASCII art / box-drawing 图案（仅用于"视觉装饰"的边框、横幅、招牌字符）；用于结构性表达的代码块 / 表格边框 / 树形目录拓扑（如 `├──` / `└──`）不在禁用之列。
  * **唯一例外**：当且仅当**用户在某次会话中明确要求使用 emoji**时，仅当次允许且仅限用户指定的范围；任何会话**严禁**以"提升可读性"、"增强分辨度"、"标注优先级"、"庆祝里程碑"等任何理由自作主张引入 emoji。本条不接受白名单，不接受"仅状态标识可用"之类的让步。
  * **替代表达手段（强制使用以下方式表达分级 / 强调 / 分类）**：
    1. Markdown 标题层级（`##` / `###` / `####`）做结构划分；
    2. 加粗（`**...**`）/ 倾斜（`*...*`）做强调；
    3. 文字标签做状态 / 优先级标注：「P0」「P1」「P2」「Blocker」「Major」「Minor」「强制」「建议」「示例」「反例」「正例」「TODO」「FIXME」「已完成」「进行中」「阻塞」「已废弃」等；
    4. 表格与列表（有序 / 无序）做并列结构；
    5. 代码块（``` 围栏）承载示例代码与命令；
    6. 链接与引用块（`>`）做引述与跳转。
  * **历史遗留与整改**：发现已合入历史中残留的 emoji，开整改条目并及时清理；清理 PR 同样适用本条约束（清理时不得引入新的 emoji 替代符号）。
  * **执行口径**：本条与「严禁使用 Lombok」、「JSON 强制 snake_case」同档次，是**跨代码 / 文档 / 沟通**的全局硬红线；CR 阶段任何含被禁符号的提交一律打回，无论改动规模大小、无论是否"只是注释 / 只是文档"。
* **全局禁用 properties 占位符 `${}`（强制，违者代码不予合并）**：本项目所有 properties 配置文件**严禁出现以 `${` 开头、`}` 闭合的占位符表达式**，环境差异统一通过 Spring Boot profile 文件（`application-{profile}.properties`）分环境硬编码真实值实现，启动时以 `spring.profiles.active=<profile>` 选定本环境配置。
  * **覆盖文件（穷举）**：
    1. `src/main/resources/application.properties`（主配置）；
    2. `src/main/resources/application-*.properties`（所有 profile 分环境配置，如 `application-dev.properties` / `application-test.properties` / `application-prod.properties`）；
    3. 任何被 Spring Boot `@PropertySource` / `@ConfigurationProperties` / `spring.config.import` 加载的额外 `*.properties` 文件，无论位于 `src/main/resources/` 还是 `src/main/resources/config/` 等子目录。
  * **禁止形式（穷举，不留模糊空间）**：
    1. `${ENV_VAR}`——纯环境变量注入；
    2. `${ENV_VAR:default_value}`——环境变量带默认值回退；
    3. `${spring.application.name}` / `${server.port}` / `${spring.datasource.url}` 等 Spring 内部属性引用（含跨 key 互引）；
    4. `${random.uuid}` / `${random.int}` / `${random.long}` / `${random.value}` 等 Spring Boot `RandomValuePropertySource`；
    5. 任何形如 `${...}` 的 SpEL / PropertyPlaceholder / `${a:${b}}` 嵌套占位符表达式；
    6. **键、值、注释**三处均禁——即便是 `# 示例：${VAR:default}` 这样的注释也禁止出现，避免误导后续维护者复制粘贴。
  * **正确做法（强制采用）**：
    1. **三环境共享、永不变化**的值（如 `spring.application.name=blog`、`mybatis.configuration.map-underscore-to-camel-case=true`）才允许写在 `application.properties` 主配置中，直接给最终字面量；
    2. **任何会因环境变化的中间件 / 基础设施项**（数据库连接串 / Redis 主机 / ES / OBS·对象存储的 endpoint·AK/SK·bucket / MQTT broker 地址 / OAuth 回调地址 / CORS 白名单 / 服务端口等）**一律下沉到对应的 profile 文件**（`application-dev.properties` / `application-test.properties` / `application-prod.properties`），在每份 profile 文件中直接写死本环境的真实值；**业务第三方凭证（短信 / 支付 / OAuth AppSecret / LLM·业务 API Key 等）不在此列**——见本节「配置分层：中间件 vs 业务第三方凭证」条；
    3. 启动时通过 `--spring.profiles.active=prod` 或环境变量 `SPRING_PROFILES_ACTIVE=prod` 选定 profile，由 Spring Boot 加载对应的 `application-prod.properties` 覆盖主配置；
    4. 同一 key 在多个 profile 文件中重复出现是 profile 机制的**正常用法**，不算「重复键违规」，CR 不得据此打回。
  * **同步禁止的等价绕路**：
    1. 严禁通过自定义 Java 逻辑（如 `Environment.getProperty(...)` + `String.format(...)` / 字符串拼接 / 自写解析）在运行时把 `${}` 风格占位符或跨 key 引用「还原」进属性值——属性值在各 profile 文件里一律写死为最终字面量。**注意**：Java 端用 `@Value("${some.key}")` / `@Value("${some.key:default}")` 注入读取配置值**不在此列、属允许用法**（详见本节末「`@Value` 占位符允许使用」澄清），本项禁止的是「绕过 profile 字面量、在运行时用自写代码重新引入占位符 / 动态引用解析」；
    2. 严禁把 `${}` 转写到 `.yml` / `.yaml` 文件以规避本规则——本规则**等价适用于 `application*.yml` / `application*.yaml`**，YAML 形式同样禁用 `${}` 占位符；
    3. 严禁通过自定义 `PropertySource` / `EnvironmentPostProcessor` 在加载 properties 后注入 `${}` 风格表达式让 Spring 二次解析。
  * **历史遗留整改**：本条立规后，已存在的 `${...}` 表达在单独的存量整改 PR 中统一清理（不在本规范条款提交内同步动 properties 文件）；整改 PR 须确保每份 profile 文件给出本环境的完整最终值，且整改后 `grep -r '\${' src/main/resources/*.properties` 应零匹配。
  * **CR 校验**：任何 properties / YAML 配置文件的提交，若 `grep` 命中 `${` 字符（无论出现在键、值还是注释），一律打回，**无论改动规模大小、无论是否"只是一个默认值"**。
  * **执行口径**：本条与「严禁使用 Lombok」、「JSON 强制 snake_case」、「全局禁用 emoji」同档次，是**配置层的全局硬红线**；任何「为了开发便利保留默认值占位符」的妥协请求一律不予接受——若开发期需要本地值，写在 `application-dev.properties` 即可，无需任何 `${}`。
  * **`@Value` 占位符允许使用（澄清，强制口径）**：本条只禁止 **properties / YAML 文件内容**（键 / 值 / 注释）出现 `${}` 占位符；Java 端用 `@Value("${some.key}")` 注入读取配置值是 Spring 消费端的**正常、必需用法，允许使用**，含 `@Value("${some.key:default}")` 带默认值形式。一句话——**禁的是「在 properties / yml 文件里写 `${}`」，不是「在 Java 里用 `@Value` 读取配置 key」**。CR **不得**以「用了 `@Value("${...}")`」为由打回；`@Value` 读取的 key，其值仍须在各 profile 文件里写死为最终字面量（不得在 properties 文件里再用 `${}` 串联）。

* **配置分层：中间件 vs 业务第三方凭证（强制，违者代码不予合并）**：
  1. **中间件 / 基础设施**（数据库、Redis、ES、OBS / 对象存储、MQTT、服务端口 / SSL、日志、MyBatis 等纯技术连接参数）——**只写** `application-dev.properties` / `application-test.properties` / `application-prod.properties`；**严禁**进 Admin 管理端，**严禁**写入 `sys_secret` / `system_config` / `platform_*_config` 等库表或对应 DML 种子。
  2. **业务第三方凭证**（短信、业务邮件 SMTP、微信支付 / 支付宝支付、OAuth 授权登录密钥、LLM / 业务 API Key 等随业务变更的配置）——**由 Admin 管理端维护并落库**，运行时读库；**严禁**以 `application-*.properties` 为唯一或主配置来源。
  3. **DML 种子强制（全量清库部署）**：部署链路为 `bgssai-all clean database` → `init database` → `deploy`（dev / prod），每次 clean 清空库数据；因此业务第三方凭证必须在对应产品 `sql/DML.sql`（及 `sql/<env>/DML.sql`）维护一份可用种子——Admin 是运行期改配入口，DML 是清库后的恢复来源。写法：一行数据一条 INSERT；`ON DUPLICATE KEY UPDATE` 仅空值回填，不覆盖 Admin 已写入的非空值。跨环境共享凭据放主 DML；环境差异（域名、回调 URL 等非密钥项）可放 `sql/dev` / `sql/prod` 或 profile。
  4. **边界澄清**：OAuth **回调 URL**、CORS 白名单等环境相关非密钥项可留在 profile；OBS 的 endpoint / AK/SK / bucket 属中间件，走 profile 不进 Admin。
  5. **CR 校验**：新增中间件配置若进 Admin / 库表一律打回；新增业务第三方凭证若只写 properties、或有 Admin 落库却无 DML 种子，一律打回。

* **配置文件一律 `.properties`，全局禁用 `.yml` / `.yaml`（强制，违者代码不予合并）**：Spring Boot 配置只允许写在 `application.properties` 与 `application-{profile}.properties` 中；**严禁**新增或保留任何 `application*.yml` / `application*.yaml`，也严禁把配置拆一部分到 YAML。
  * **理由不只是「统一风格」，还有一处会真出事的编码差异**：Spring Boot 按 `java.util.Properties` 规范以 **ISO-8859-1** 解码 `.properties`，而 `.yml` 一律按 **UTF-8** 解码。两种文件混用时，同一段中文在一处正常、换到另一处变乱码，且**编译与启动都不报错**——只有等到它作为配置值出现在页面 / 日志 / 接口响应里才被发现。统一到 `.properties` 一种文件，就只剩一条编码规则要记。
  * **非 ASCII 的配置值必须写成 `\uXXXX` 转义**（承上：`.properties` 按 ISO-8859-1 解码）。直接写中文字面量会被读成乱码。转换命令：`python3 -c "import sys;print(sys.argv[1].encode('unicode_escape').decode())" 用户中心`。
    * **只约束「值」**：注释行不参与解析，**注释里的中文照常直接写**，且应在被转义的值旁用注释给出可读原文，避免文件变成天书。
    * ASCII 值（URL / 类名 / 布尔 / 时长 / 枚举名等）照常直接写，不必转义。
    * 若某项配置的中文值多到写转义已影响可维护，正确做法是**把该文案移出配置**（放进代码常量、`messages.properties` 国际化资源或数据库），而不是为它把文件改回 YAML。
  * **禁止的等价绕路**：不得通过 `spring.config.import` / `@PropertySource` 引入 YAML；不得自定义 `PropertySourceLoader` / `EnvironmentPostProcessor` 让 YAML 重新生效。
  * **例外（不在本条约束内）**：非 Spring 配置的 YAML 照常使用——`.github/workflows/*.yml`（GitHub Actions）、`docker-compose*.yml`、`pnpm-lock.yaml` / `pnpm-workspace.yaml` 等工具链自有格式，它们不是本项目的应用配置。
  * **CR 校验**：`find src -name "application*.yml" -o -name "application*.yaml"` 应零匹配；`.properties` 的**值**里出现非 ASCII 字符（`grep -nP "^[^#].*[^\x00-\x7F]"` 命中）一律打回。
  * **执行口径**：本条与「严禁使用 Lombok」、「JSON 强制 snake_case」、「全局禁用 emoji」、「全局禁用 `${}` 占位符」同档次，是**配置层的全局硬红线**。

## 2. 数据库与持久层限制 (严格 Example 模式)

* **禁用自定义 SQL**：严禁在 Mapper XML 中手写复杂的连表查询、自定义 SQL 标签或使用 SELECT *（一律不要使用 * 作为查询字段列表，必须明确写明所需字段）。
* **方法白名单**：MyBatis 的交互被严格限制为只允许使用 Example 通用方法，即且仅即：countByExample, deleteByExample, deleteByPrimaryKey, insert, insertSelective, selectByExample, selectOneByExample, selectByPrimaryKey, updateByExampleSelective, updateByExample, updateByPrimaryKeySelective, updateByPrimaryKey。
  * Example 对象自身的辅助方法（`createCriteria()` / `andXxxEqualTo()` / `andXxxIn()` / `andXxxIsNull()` / `setOrderByClause("col ASC")` 等 Example API）允许使用——这些只是构造查询条件 / 排序的对象操作，不是新增 SQL 语句。
  * **严禁**通过 `setOrderByClause` 拼接 `LIMIT/OFFSET` 等 SQL 片段绕过白名单。
* **分页强制使用 PageHelper 插件（强制，违者代码不予合并）**：所有列表 / 分页查询必须使用 MyBatis 物理分页插件 `pagehelper-spring-boot-starter`。使用范式：
  ```java
  PageHelper.startPage(pageNum, pageSize);          // 1-based 页码
  List<XxxRow> rows = xxxMapper.selectByExample(ex); // 已被插件改写为物理分页 SQL
  long total = ((com.github.pagehelper.Page<XxxRow>) rows).getTotal();
  ```
  * **严禁**自行用 `List.subList()` 做内存分页，也严禁通过 `setOrderByClause` 拼 `LIMIT`/`OFFSET` 绕过白名单。
  * `PageHelper.startPage` 必须紧贴在对应 `selectByExample` / `selectOneByExample` 调用前一行；中间不得插入任何其它 SQL 调用，否则插件会把分页强加给错误的 select。
  * 分页参数命名建议：HTTP 入参用 `page`（0-based）+ `size`，传入 PageHelper 前自行 `+1` 转 1-based。`size` 必须经过 `EntityConverters.clampPageSize(...)` 截断到 ≤100，防止 DoS。
  * 仅用 `selectByPrimaryKey` 的精准查询不需要分页插件。
* **按需列查询（强制，违者代码不予合并）**：所有 `XxxExample` 类必须包含 `selectColumns(String... cols)` 扩展能力（由 Example 基类统一提供；XML 中的 `selectByExample` / `selectOneByExample` 在该字段非空时用 `<foreach>` 替代 `<include refid="Base_Column_List"/>`）。Service 在以下两种场景**必须**显式指定返回列：
  * "ID 列表 + 内存 join" 场景（如取 article_tag 表的 article_id 集合）：`example.selectColumns("id", "article_id")`
  * "存在性 / 计数" 路径中需要少量字段判定（如取 owner_id 校验权限）：仅查 owner_id
  默认（不调用 `selectColumns`）走 `Base_Column_List`，行为不变；调用后仅查指定列，减少 IO + 网络 + 反序列化开销，对大宽表（含 TEXT/MEDIUMTEXT 内容列）效果尤其显著。**严禁**为绕过本规约把 `selectColumns` 作为"模糊列名拼接"接口使用——参数必须是字段名常量字符串，不接受任何外部输入。
* **表必备基础字段（精确列定义，强制，违者代码不予合并）**：每张数据表必备 `id`、`del_flag`、`creator`、`modifier`、`gmt_create`、`gmt_modified` 六个公共字段，**所有表逐字使用以下 canonical 列定义**——类型 / 是否为空（NULL）/ 默认值 / `ON UPDATE` 行为必须完全一致，不得逐表各写一套：

  ```sql
  id           BIGINT       NOT NULL AUTO_INCREMENT                                COMMENT '主键',
  -- ... 业务字段（自顶向下，审计字段统一收口在末尾、PRIMARY KEY 之前）...
  del_flag     TINYINT(1)   NOT NULL DEFAULT 0                                     COMMENT '逻辑删除字段',
  creator      VARCHAR(64)  DEFAULT NULL                                           COMMENT '创建者标识',
  modifier     VARCHAR(64)  DEFAULT NULL                                           COMMENT '最后修改者标识',
  gmt_create   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                     COMMENT '创建时间',
  gmt_modified DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (id),
  ```

  * **id**：主键，`BIGINT NOT NULL AUTO_INCREMENT`，单表自增；所有表统一 `PRIMARY KEY (id)`。
  * **del_flag**：逻辑删除标记，`TINYINT(1) NOT NULL DEFAULT 0`（0=正常 / 1=已删）。查询默认带 `del_flag = 0` 过滤，删除走逻辑删除（置 1）而非物理删行。**本列是 §4「数据库布尔列用 is_xxx 命名」的唯一例外**：`is_` 前缀在 ORM 与序列化两侧会被反复剥离与还原（`is_delete` 被映射成属性 `delete`），而 `delete` 既贴近 SQL 保留字、又与集合删除语义冲突；故逻辑删除列统一定名 `del_flag`，Java 属性 `delFlag`。其余布尔列仍按 §4 用 `is_xxx`。
  * **creator / modifier**：操作者标识，`VARCHAR(64) DEFAULT NULL`，记录创建者 / 最后修改者（用户名或系统标识，可空）。
  * **gmt_create**：创建时间，`DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP`——插入时若未显式赋值，由 DB 默认值兜底为当前时间。
  * **gmt_modified**：修改时间，`DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP`——插入时兜底为当前时间，且**每次行更新时由 DB 自动刷新**为当前时间（无需业务代码手动维护）。
  * **与持久层的衔接**：Java 端统一用 `insertSelective`（见本节「方法白名单」），未显式 set 的 `gmt_create` / `gmt_modified` 会被省略、由 DB 默认值兜底；服务如需指定时间可显式 `setGmtCreate` / `setGmtModified` 覆盖。**严禁**对 `gmt_modified` 显式插入 `NULL`（违反 NOT NULL 约束，连 DML 种子脚本也不例外）。
  * **注释文案**：上表注释为默认文案；个别表的 `gmt_create` / `del_flag` 若带领域语义（如 `gmt_create` 实为「快照时间 / 打赏时间 / 发送时间」、`del_flag` 标注「取消关注=1」等），允许保留领域注释，但**列定义本身必须逐字一致**。
  * **新增 / 改表强制项**：新建任何表必须逐字带齐上述六字段的 canonical 定义；结构性改列须按本节「持久层三件套同步更新契约」先改 `sql/DDL.sql`，再同步手工更新 Entity / Example / Mapper XML。**CR 校验**：发现缺字段、`gmt_create` 缺 `DEFAULT CURRENT_TIMESTAMP`、或 `gmt_modified` 非 `NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP` 的提交一律打回，无论改动规模大小。
* **数据库与持久层命名一律单数（强制，违者代码不予合并）**：表名 / 列名 / 索引名 / 外键名一律用**单数 + snake_case**；对应的 Java Entity / Example / Mapper 接口 / Mapper XML 文件名也**必须使用单数 UpperCamelCase**。例：`article` 不是 `articles`、`payment_order` 不是 `payment_orders`、`article_tag` 不是 `article_tags`、`audit_log` 不是 `audit_logs`、`appeal` 不是 `appeals`、`report` 不是 `reports`、`comment_like` 不是 `comment_likes`、`system_config` 不是 `system_configs`。
  * **唯一例外**：表名与 SQL 保留字（`column` / `order` / `user` / `group` 等）冲突时，必须在表名前加领域前缀消歧义。本项目落地的示例：`users` → `app_user`、`columns` → `blog_column`。严禁用反引号兜底（影响所有 SQL 可读性 + 跨方言迁移风险）。
  * **持久层三件套命名链（强制对齐）**：单数表名 → 单数 Entity → 单数 Example → 单数 Mapper → 单数 XML，**全链路单数无复数**。一旦中间任何一环出现复数（如 `Articles.java` / `ArticlesExample.java` / `ArticlesMapper.java` / `ArticlesMapper.xml`），即视为违规，CR 阶段必须打回。命名映射示例：
    | 表名 | Entity | Example | Mapper 接口 | Mapper XML |
    | --- | --- | --- | --- | --- |
    | `app_user` | `AppUser` | `AppUserExample` | `AppUserMapper` | `AppUserMapper.xml` |
    | `article` | `Article` | `ArticleExample` | `ArticleMapper` | `ArticleMapper.xml` |
    | `audit_log` | `AuditLog` | `AuditLogExample` | `AuditLogMapper` | `AuditLogMapper.xml` |
    | `article_tag` | `ArticleTag` | `ArticleTagExample` | `ArticleTagMapper` | `ArticleTagMapper.xml` |
    | `payment_order` | `PaymentOrder` | `PaymentOrderExample` | `PaymentOrderMapper` | `PaymentOrderMapper.xml` |
  * **禁止"单复数双 Entity"并存**：每张表只允许存在**一个** Entity 类（单数版本）；严禁同时保留旧 JPA 时代的"富领域单数"和"复数"两套实体，必须二选一并彻底删除另一套。Service / Controller / DTO 一律 import 单数 Entity。
  * 本规约**仅约束数据库层 / 持久层**；REST API 路径的资源集合名按 §3 路由命名规范保留复数（如 `GET /api/articles`、`POST /api/articles/take-down`），与表名 `article` 不冲突 —— 一个是"集合资源"语义，一个是"实体类型"语义。
* **关联查询降级 (In-memory Join)**：遇到复杂的业务关联，严禁使用数据库 Join（超过三个表禁止 join）。必须在 Service 层通过多次调用上述单表通用方法，在 Java 内存中完成数据组装。
* **持久层三件套同步更新契约**：Mapper XML / Example / Entity 三件套是仓库内的普通源码，由开发者手工维护（本仓已下线 MyBatis Generator，不引入任何代码生成工具）。当表结构发生变更（加列 / 改列 / 改索引）时，正确流程是：
  1. 改 `sql/DDL.sql`
  2. 手工同步 Entity 字段、Example 条件方法、Mapper XML 的 resultMap / Base_Column_List / insert / update 片段
  3. 提交 entity / example / mapper / xml 一次性更新
  禁止只改 Java 端不改 DDL，或只改 DDL 不同步三件套。三件套与 `sql/DDL.sql` 必须逐列一致，CR 阶段按列比对。
* **事务范围最小化**：@Transactional 注解仅用于包含多表写操作的核心 Service 方法 。严禁将耗时的网络 I/O（如调用外部 HTTP 接口或 RPC）包裹在数据库事务中 。
* **DML 种子 / 数据维护脚本写法：一行数据一条 INSERT（强制，违者代码不予合并）**：所有 DML 脚本（基础种子数据 Seed Data、日常数据维护 / 修订脚本）中，**每一行数据必须是一条独立的 `INSERT` 语句**，**严禁**使用「单条 `INSERT` 携带多组 `VALUES`」的批量多值写法。
  * **正例（强制：一行数据一条 INSERT，显式列清单）**：
    ```sql
    INSERT INTO app_user (id, username, del_flag, gmt_create, gmt_modified) VALUES (1, 'alice', 0, '2026-01-01 00:00:00', '2026-01-01 00:00:00');
    INSERT INTO app_user (id, username, del_flag, gmt_create, gmt_modified) VALUES (2, 'bob', 0, '2026-01-01 00:00:00', '2026-01-01 00:00:00');
    INSERT INTO app_user (id, username, del_flag, gmt_create, gmt_modified) VALUES (3, 'carol', 0, '2026-01-01 00:00:00', '2026-01-01 00:00:00');
    ```
  * **反例（禁止：单条 INSERT 多组 VALUES 批量拼接）**：
    ```sql
    INSERT INTO app_user (id, username, del_flag, gmt_create, gmt_modified) VALUES
      (1, 'alice', 0, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
      (2, 'bob', 0, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
      (3, 'carol', 0, '2026-01-01 00:00:00', '2026-01-01 00:00:00');
    ```
  * **理由**：逐行独立 INSERT 便于 Code Review 逐条审查、便于精确定位执行失败 / 报错的具体行、便于单行增删改与幂等重跑，并显著降低多分支并行编辑同一 DML 文件时的合并冲突与坏合并风险（呼应 §11「同区域并行改动规约」）。
  * **覆盖范围**：本条适用于 `sql/DML.sql`（§9 数据库脚本收敛规约，DML / 种子数据唯一落点）；结构性 DDL 不在本条约束内（DDL 按本节「持久层三件套同步更新契约」与 §9 目录规约管理）。
  * **CR 校验**：DML / 种子脚本中出现「单条 INSERT 多组 VALUES」的批量写法一律打回，无论涉及行数多少。

* **现阶段数据库一律明文存储（强制）**：当前处于开发 / 联调 / 内部演示阶段，**所有入库数据一律明文落库、明文比对，一律不做任何哈希 / 加密**——此为用户拍板的临时开发期口径，目的是在 MVP / 联调阶段降低实现复杂度、令数据在库中可读可调试。
  * **明文范围（IN-SCOPE，强制）**：账号 / 管理员密码、短信 / 邮箱验证码、会话 / 访问 / 刷新 token 的服务端副本、各类 API-KEY / access key / 设备与租约凭证（如有），以及第三方渠道（支付 / LLM / OAuth / 短信 / 对象存储等）的 access key / token / secret / 商户私钥等——凡属「静态存放在数据库或配置里的凭证与敏感字段」，一律**明文存库或写配置**。
  * **一律不引入的加密 / 哈希机制（强制）**：慢哈希（Argon2id 等）、HMAC + pepper、字段级加密（AES-256-GCM）、信封加密（KEK / MEK 包裹 per-record DEK）、加密入库主密钥 MEK（`bgssai.encryption.key` / `APP_SECRET_MEK` 一类数据加密密钥）、KMS / Vault / HSM、`secret_config` / `secret_audit_log` 等加密入库表与加解密 / 轮换组件——开发期**一律不新增**；且**已实现的入库哈希 / 加密（如管理员密码哈希、API-KEY 的 HMAC + pepper、设备 / 租约凭证哈希，如有）一并拆为明文存储与明文比对**。
  * **范围护栏（OUT-OF-SCOPE，强制，优先级高于「改明文」）**：本口径**只把「静态存放在数据库 / 配置里的凭证与数据」改为明文**，**严禁**据此关闭 / 降级 / 注释 / 顺手简化任何**签名 / 验签 / 鉴权 / 审计**的运行逻辑——支付协议签名与回调验签、JWT / MQTT 令牌签名密钥（按 §1 写入各环境 profile 字面量；缺失时启动 fail-fast）、OAuth state / CSRF 一次性消费、HTTPS 强制、零信任输入校验与 deny-by-default、审计只写不可篡改，**一律原样保留、继续生效，不因明文存储而放松**。拿不准某项属「入库存储」还是「运行逻辑」时，按更保守一档处理——**视为运行逻辑、不拆**。
  * **骨架同样套用本口径（本仓库现状，强制）**：本骨架仓库**口令明文存储**（`PasswordService` + `sql/DML.sql` 中的 `$2a$` 种子），**不**按本条拆为明文——骨架不处于任何产品的联调阶段，且是新产品的起点，起点必须是安全默认值。本条是**给基于骨架新建的产品**在其自身开发期可选套用的口径：套用须由该产品在自己的 `docs/security/` 下落一份细则文档（穷举 IN-SCOPE / OUT-OF-SCOPE、风险登记、回滚动作清单与发布前必核项）并经拍板，**不得**仅凭本条就在代码里拆哈希。
  * **权威细则**：本条属 §0「决策即版本锚点、全产出版本一致」下的开发期版本锚点；套用本口径的产品，其完整明文范围（IN-SCOPE）与范围护栏（OUT-OF-SCOPE）穷举、风险登记、回滚动作清单与发布前必核项以该产品 `docs/security/` 下的细则文档为准，下游需求 / 架构 / API 契约 / 代码 / 数据库脚本 / 测试各层一律对齐该文档。

## 3. 架构与服务边界 (遵循零信任原则)

* **严格服务隔离 (No Shared State)**：严禁跨业务域（如 订单域 与 账户域）直接读写对方的数据库表。跨域交互必须通过对外暴露的 Service 接口进行 。
* **深度防线与零信任 (Zero Trust)**：绝不将信任边界、输入验证或 DoS 防御外包给通用的反向代理或网关机制 。任何微服务/App 外部调用的接口（包括内部服务调用的 API）均视为信任边界，微服务之间不得盲目信任其客户端 。
* **边界防御性校验**：在 Controller层及 Service 核心逻辑中，必须对外部传入的所有资金、状态变量、任务参数进行严格的边界验证和业务一致性校验 。必须在边界执行正向白名单验证（"Accept known good" 或 "Allow list"） ，对未认证或不合规的输入默认行为必须是拒绝（Deny by default） 。
* **接口优先 (API over SDK)**：内部服务间的依赖原则上通过明确的 API 契约集成，避免构建和维护强耦合的 Java SDK 。
* **单接口单 Controller 契约（强制，违者代码不予合并）**：
  * **一个 HTTP 端点 = 一个 `XxxController.java` 文件**。严禁在同一个 Controller 类中聚合多个端点（即不允许出现"模块大杂烩"式的 `AdminController` / `ArticleController`）。每个 Controller 类内**仅含一个** `@GetMapping` / `@PostMapping` / `@PutMapping` / `@DeleteMapping` 等处理方法。
  * **包结构（按业务模块分包）**：Controller 按业务模块归位到 `com.bgssai.blog.<module>.controller` 子包下，**严禁**继续把所有 Controller 扁平堆在 `com.bgssai.blog.controller/` 根目录。`<module>` 与已存在的业务包对齐（如 `payment`、`marketing`、`promotion`、`moderation`、`notification`、`location`、`storage`、`oauth`、`article`、`comment`、`user` 等）；如果业务模块尚不存在，先在 `com.bgssai.blog/` 下新建模块包再创建 `controller` 子包。
  * **命名规范**：类名采用 `动词 + 资源 + Controller` 的 UpperCamelCase 形式。示例：`com.bgssai.blog.article.controller.ListArticlesController`、`com.bgssai.blog.article.controller.CreateArticleController`、`com.bgssai.blog.payment.controller.CreatePaymentController`、`com.bgssai.blog.admin.user.controller.BanUserController`、`com.bgssai.blog.moderation.controller.ApproveArticleController`。由于包名已携带业务域信息，**优先**通过包路径区分同名动作（如公开端 `article.controller.ListArticlesController` vs 管理端 `admin.article.controller.ListArticlesController`），不再强制 `...AdminController`/`...PublicController` 后缀；仅当同一模块内确实存在公开端与管理端同名歧义时，才允许加领域后缀消歧。严禁简单数字编号。
  * **路由风格**：类上 `@RequestMapping("/api/资源前缀")` 仅声明基础路径，方法上写 `@GetMapping("/子路径")` / `@PostMapping(...)` 等指明 HTTP 动词和子路径。
  * **方法名**：处理方法统一命名为 `handle(...)`。由于类名已经表达"动词+资源"，方法名再写一遍是噪声。
  * **Controller 内代码越少越好**：Controller 仅负责参数绑定、权限注解 / 调用 `@PreAuthorize`、调用 Service、组装 Response。**严禁**在 Controller 内写业务分支、数据库查询、跨实体组装、循环计算等逻辑——一律下沉到 Service 层。如果发现 Controller 超过 ~50 行（含 import），几乎可以确定有逻辑该拆到 Service。
  * **共享辅助方法去 Controller 化**：旧 Controller 里那些被多个端点复用的私有 helper（如 `recordAudit`、`findUserByUsername`、Entity↔DTO 转换、`clampPageSize`），必须升级为独立的 `@Component` 或放进 `util/` / `service/` 包，供各 Controller 注入或静态调用。**严禁**为了让 Controller "看起来薄" 而把 helper 复制到每个新 Controller。
  * **DTO 归位（按模块归属，强制）**：旧 Controller 内的 `record` / `static class` 形式的请求/响应 DTO，必须抽出为独立的 `.java` 文件，按业务模块就近收敛到 `com.bgssai.blog.<module>.dto` 包下（如 `com.bgssai.blog.article.dto.ArticleRequest`、`com.bgssai.blog.user.dto.ChangeEmailRequest`、`com.bgssai.blog.oauth.dto.OAuthLoginRequest`）。Controller 文件只剩一个类定义。详细约束见 §5"层级隔离"。
* **传输参数契约（强制，违者代码不予合并）**：
  * **POST 请求的所有业务参数必须放在 JSON Body 中**——包括资源 ID、操作目标 ID、动作参数、原因 / 备注等。严禁使用 `@PathVariable`、`@RequestParam` 在 POST 端点上承载业务参数。
  * **每个 POST 端点必须有专属的 Request DTO 类**（位于所属模块的 `com.bgssai.blog.<module>.dto` 包下，命名规则见 §5"层级隔离"），不允许直接用 `Map<String, Object>` / `JSONObject` / `String` 单字段裸接。单 ID 场景可复用框架级的 `com.bgssai.blog.dto.IdRequest`；其它情况一律新建命名清晰的 `XxxRequest`，并标注 Bean Validation 注解（`@NotNull` / `@NotBlank` / `@Size` 等）。
  * **多参数查询接口统一用 @RequestBody + 专属 Query DTO（强制，违者代码不予合并）**：当一个查询 / 列表接口的**业务查询参数（过滤条件 + 分页参数，不含从 JWT 取的身份参数）达到 3 个或以上**时，该接口**必须**改为 `POST` + `@RequestBody` + 一个专属的 `XxxQueryRequest` POJO 承载全部查询参数，**严禁**用一长串 `@RequestParam` 平铺在方法签名上。
    * **理由**：多参数 `@RequestParam` 方法签名冗长、参数无法整体校验（Bean Validation 无处标注）、新增 / 删减过滤条件都要改方法签名、调用方拼 query string 易错且无契约；收敛为一个 Query DTO 后类型安全、可标注校验、可契约化、易演进。
    * **Query DTO 归位**：`XxxQueryRequest` 放所属模块 `com.bgssai.blog.<module>.dto` 包下，字段 camelCase（靠全局 snake_case 策略自动转），分页用 `page`（0-based）+ `size`，`size` 由 Service 截断到 ≤100 防 DoS，过滤字段可空（不传即不过滤）。
    * **路由命名**：用 `POST /api/.../query` 等动词短语（如 `POST /api/<resources>/query`）；身份参数（userId 等）仍只从 JWT 取，不进 Query DTO。
    * **CR 校验**：方法签名上业务查询 / 过滤 `@RequestParam` 达到 3 个或以上即打回，须改为 POST + @RequestBody + 专属 Query DTO；`multipart/form-data` 文件上传按上一条「协议层例外」豁免。
  * **公共参数（用户身份、租户标识）走 JWT Token**：Controller 通过 `@AuthenticationPrincipal UserDetails` 取当前用户，绝不把 username / userId / tenantId 放进请求体或路径让客户端自行声明。
  * **路由命名**：动作类路由用 kebab-case 动词短语，例如 `POST /api/articles/take-down`、`POST /api/admin/users/ban`；不要让 `{id}` 出现在 POST 路径段中。
  * **协议层例外（仅此一项）**：`multipart/form-data` 文件上传由于 HTTP 协议本身限制，允许在 POST 上使用 `@RequestParam MultipartFile` 与必要的同表单字段；这类端点必须在方法注释中显式标注「multipart 例外」。
  * **GET 端点的参数阈值（与上一条联动）**：GET 仍适用于单资源标识 / 简单分页 / 业务过滤参数 ≤2 个的查询，参数通过 `@PathVariable` 与 `@RequestParam` 表达即可；一旦业务查询 / 过滤参数达到 3 个或以上，必须按上一条改为 POST + `@RequestBody` + 专属 Query DTO。
  * **DELETE 端点资源标识沿用 path variable（全局现行风格）**：资源删除用 `DELETE /api/<resources>/{id}`、关系取消用 `DELETE /api/<resources>/{id}/<relation>`（例：`DELETE /api/articles/{id}/favorite`、`DELETE /api/users/{id}/follow`），与「正向动作 POST + JSON body」成对出现——正向 POST 走 body、反向 DELETE 走 path id。
    * 本条与「POST 业务参数必须走 JSON Body」**并行不悖**：DELETE 走 path id 是该谓词下的既定风格，**严禁**以「动作参数应走 body」为由给既有 DELETE 端点另立一个 POST 重复端点（**一个动作只允许一个端点**，避免端点重复与契约分裂）。
    * **例外须成文锁定**：确有 `DELETE` + JSON body 的必要时（如批量删除、复合标识；身份仍只取 JWT），该例外必须由本产品 `docs/security/` 或 `docs/api/` 下对应文档**显式锁定并写明理由**，此后实现与全部文档（接口契约、`docs/api/page-api-mapping.md`、架构设计）一律以该契约为准，**禁止**再改回 path id / query id，也**禁止**未成文就自行开例外。骨架自身不含此类例外。

## 4. 编码规约与防御性编程

* **命名规范**：类名必须使用 UpperCamelCase 风格 ，方法名、参数名、成员变量和局部变量统一使用 lowerCamelCase 风格 。
* **布尔值命名契约**：POJO 类中的任何布尔类型变量都**严禁加 is 前缀**，以规避部分框架解析引起的序列化错误 ；但在数据库建表时，表达是与否概念的字段必须使用 is_xxx 方式命名 。**唯一例外**：逻辑删除列定名 `del_flag`（不写 `is_delete`），Java 属性 `delFlag`，理由见 §2 六公共字段。
* **空指针与包装类防御 (NPE Prevention)**：
  * 所有整型包装类对象之间值的比较，**全部且必须使用 equals() 方法**，严禁使用 == 运算符。
  * 判断所有集合内部的元素是否为空，**必须使用 isEmpty() 方法**，而不是 size() == 0 的方式。
  * Service 层返回的集合类型，如果没有数据，必须返回空集合（如 `Collections.emptyList()`），**严禁返回 null**。
* **并发与线程池**：严禁在应用中自行显式创建线程或使用 Executors 创建线程池（规避 OOM 风险） 。必须通过 ThreadPoolExecutor 的方式显式声明 ，且必须指定有意义的线程名称，方便出错时回溯 。所有线程池必须在项目的全局异步配置类中进行统筹管理。
* **配置文件数组型参数必须索引展开（强制，违者代码不予合并）**：Spring Boot 配置（`application*.properties`）中所有数组 / `List` / `Set` 型配置项，**严禁**写成单行逗号分隔字符串，必须逐项展开为带下标 `[n]` 的形式。本条与 §1「全局禁用 properties 占位符 `${}`」**联合生效**——数组项的值一律直接写最终字面量（域名 / 主机 / URL 等），**严禁**在数组项内出现任何 `${...}` 占位符；环境差异通过 `application-{profile}.properties` 在每份 profile 文件中**复制整份数组**并逐项给出本环境的最终值实现。
  * **反例 A（禁止：单行逗号分隔字符串）**：
    ```properties
    app.cors.allowed-origins=http://localhost:5173,http://localhost:3000,http://127.0.0.1:5173
    ```
  * **反例 B（禁止：在数组项中使用 `${}` 占位符做环境变量回退）**：
    ```properties
    app.cors.allowed-origins[0]=${CORS_ALLOWED_ORIGINS_0:http://localhost:5173}
    app.cors.allowed-origins[1]=${CORS_ALLOWED_ORIGINS_1:http://localhost:3000}
    app.cors.allowed-origins[2]=${CORS_ALLOWED_ORIGINS_2:http://127.0.0.1:5173}
    ```
  * **正例（强制：按 profile 分环境各自写死真实值）**：
    ```properties
    # application-dev.properties
    app.cors.allowed-origins[0]=http://localhost:5173
    app.cors.allowed-origins[1]=http://localhost:3000
    app.cors.allowed-origins[2]=http://127.0.0.1:5173
    ```
    ```properties
    # application-prod.properties
    app.cors.allowed-origins[0]=https://blog.bgssai.com
    app.cors.allowed-origins[1]=https://www.bgssai.com
    ```
  * **profile 间槽位数量按需**：各 profile 文件中数组的下标槽位数量按本环境实际条目数给出即可，**不需要**保持与其它 profile 槽位数一致——`application-prod.properties` 只需写到本环境真实拥有的下标，不必为对齐 dev 的槽位数硬填占位。
  * **消费端绑定契约**：数组型配置一律通过 `@ConfigurationProperties` 绑定到 `List<String>`（参照 `StorageProperties` / `MqttProperties` 的写法，并在 `BlogApplication` 的 `@EnableConfigurationProperties` 中注册），**严禁**用 `@Value` 接收单字符串后在 Java 里 `split(",")` 手工切分——后者既绕过类型安全，也与索引形式不兼容（`@Value` 解析不到带下标的键会导致启动失败）。注意：此处不用 `@Value` **仅因它无法绑定索引式数组键**，并非因 `@Value` 占位符被禁——`@Value("${...}")` 占位符本身**允许使用**（见 §1「`@Value` 占位符允许使用」澄清），只是不适用于数组 / `List` 型配置（数组型一律走 `@ConfigurationProperties`）。
  * **生产部署校验**：每次发布前须确认 `application-prod.properties` 的本数组下标项**全部**为生产域名 / 真实主机字面量，无任何 dev 痕迹（如 `localhost` / `127.0.0.1`）；CR 阶段对涉及数组型配置改动的 PR 须逐项核对各 profile 文件内的值是否为本环境的最终字面量。

## 5. 类型与数据传输控制

* **强类型契约：严禁 `Map<String, Object>` / `JSONObject` 作业务传输载体（强制，违者代码不予合并）**：Java 方法签名必须有明确的入参与出参类型；业务数据的传输（接口请求 / 响应体、Service 层入参 / 出参、跨层 / 跨方法的数据传递）一律使用**强类型对象**（专属 Request / Response DTO、VO、`record`、Entity，或带明确元素类型的集合 / 泛型）。**绝对禁止**使用 `Map<String, Object>`、`JSONObject`（含 fastjson 的 `com.alibaba.fastjson*.JSONObject` 与 Jackson 的 `ObjectNode` / `JsonNode`），或任何等价的「字符串键 → 任意值（`Object`）」松散结构来承载业务字段。本条与「严禁使用 Lombok」、「JSON 强制 snake_case」、「全局禁用 emoji」**同档次**，是**类型层的全局硬红线**。
  * **理由**：松散 `Map` / `JSON` 作载体会丢失编译期类型检查、字段无法被 IDE / 静态分析追踪与重构、字段命名（snake_case 契约）无处约束、Bean Validation（`@NotNull` / `@NotBlank` / `@Size`）无从标注、出参结构对前端不可契约化——直接架空 §1「强类型 / snake_case」、§3「单接口单 Controller 的专属 Request DTO 与边界防御」、§5「层级隔离与统一响应」。
  * **禁止形式（穷举，不留模糊空间）**：
    1. **Controller 入参**：`@RequestBody Map<String, Object>` / `@RequestBody JSONObject` / `@RequestParam Map<String, Object>` 裸接请求体（呼应 §3「每个 POST 端点必须有专属 Request DTO 类」）；
    2. **Controller / Service 出参**：方法返回 `Map<String, Object>` / `JSONObject`，或 `ApiResponse<Map<String, Object>>` / `ApiResponse<JSONObject>` 作为业务响应体（应返回专属 `XxxResponse` / `XxxResult` / `XxxItem`）；
    3. **Service 方法签名**：用 `Map<String, Object>` / `JSONObject` 作业务入参或出参在层间传递业务数据；
    4. **临时「数据袋」**：用 `Map<String, Object>` 现场打包多个业务字段传给下游方法 / 类，而非定义一个承载语义的 `record` / DTO；
    5. **现场拼装响应**：用 `new HashMap<>()` / `Map.of(...)` 拼出业务响应结构再交给序列化层返回。
  * **正例（强制：强类型对象承载业务字段）**：
    ```java
    // 入参：专属 Request DTO（带 Bean Validation）
    public record CreateTaskRequest(@NotBlank String title, @NotNull Long categoryId) {}
    // 出参：专属 Response，字段 camelCase，靠全局 snake_case 策略自动转
    public record TaskDetailResponse(Long id, String title, String status) {}

    @PostMapping("/create")
    public ApiResponse<TaskDetailResponse> handle(@RequestBody @Valid CreateTaskRequest req) {
        return ApiResponse.ok(taskService.create(currentUserId(), req));
    }
    ```
  * **反例（禁止：松散 Map / JSONObject 承载业务字段）**：
    ```java
    // 反例：裸接 Map 入参 + 返回 Map 出参——丢类型 / 丢校验 / 丢 snake_case 约束 / 出参不可契约化
    @PostMapping("/create")
    public Map<String, Object> handle(@RequestBody Map<String, Object> body) {
        Long categoryId = (Long) body.get("category_id");      // 反例：手工取值 + 强转
        Map<String, Object> resp = new HashMap<>();
        resp.put("id", id);
        return resp;                                            // 反例：现场拼装业务响应
    }
    ```
  * **允许的例外边界（明确不禁止，须在方法注释显式标注用途）**：
    1. **第三方原始报文透传 / 存档**：与外部系统对接时无法预知字段、仅做「原样转发 / 落库存档」而不消费其业务语义的场景，可用 `Map` / `JsonNode` 暂存，但**不得**借此承载本系统自身的业务语义字段；
    2. **业务语义本就是任意 KV 的字段**：如用户自定义 `extra` / `metadata` / `tags` 等，应作为**强类型 DTO 里一个有名字的字段**出现（值类型尽量收窄，如 `Map<String, String>`，而非 `Map<String, Object>`），而非让整个传输载体退化为 `Map`；
    3. **持久层三件套 / 框架回调签名**：Entity / Example / Mapper XML 三件套、框架强制的回调方法签名等非业务自定义代码，不在本条约束内。
  * **CR 校验**：Controller / Service 方法签名出现 `Map<String, Object>` / `JSONObject` / `ObjectNode` / `JsonNode` 作业务入参 / 出参，或用 `HashMap` / `Map.of` 现场拼装业务响应的提交，一律打回，**无论改动规模大小**；属上述「允许例外」者须在注释中标注用途，否则按违规处理。
* **层级隔离 (DTO/Entity/VO)**：
  * 接口请求参数必须使用专属的 Request DTO 对象。
  * 严禁将数据库 Entity 实体类直接作为 API 返回值暴露给前端，必须转换为 Response/VO 显示层对象。
* **DTO 包结构契约（强制，违者代码不予合并）**：
  * **按模块归属**：所有业务 Request / Response DTO 一律放在所属业务模块包下的 `dto` 子包，即 `com.bgssai.blog.<module>.dto`。`<module>` 取业务领域名（单数，lowerCamelCase / 全小写），例如：`article`、`comment`、`column`、`category`、`tag`、`user`、`auth`、`question`、`answer`、`favorite`、`follow`、`notification`、`oauth`、`promotion`、`payment`、`moderation`、`appeal`、`credit`、`audit`、`analytics`、`announcement`、`recharge`、`report`、`search`、`security`、`storage` 等。
  * **禁用中心化 DTO 目录**：严禁继续向 `com.bgssai.blog.dto.request` 或 `com.bgssai.blog.dto.response` 包内新增任何业务 DTO；这两个旧包应保持空置或被完全删除。**新增 DTO 必须直接落在模块包下**，不允许"先放中心包，回头再搬"。
  * **module/dto 内部扁平**：模块 dto 包内不再继续分 `request/` `response/`、`admin/` 等子包，所有 DTO 文件以 `XxxRequest` / `XxxResponse` / `XxxResult` / `XxxItem` 等明确的命名后缀直接平铺在 `com.bgssai.blog.<module>.dto` 下；Admin 视角的响应通过 `Admin` 前缀（如 `AdminUserDetail`）消歧义，不再通过子包隔离。
  * **跨模块复用谁拥有谁负责**：当一个 Response 跨多个模块使用时（例如 `UserResponse` 被 article / comment / follow 等模块引用），DTO 归属于其主语义所在的模块（`UserResponse` 归 `user`），其他模块通过 `import` 引用，**严禁复制副本**。
  * **唯一保留在 `com.bgssai.blog.dto` 根包下的是纯框架级类型**：当前仅限 `ApiResponse`（全局响应包装）、`PageQuery` / `PageResult` / `PageResponse`（分页通用契约）、`IdRequest`（通用单 ID 请求体）、`CountResponse`（通用单计数响应体）。其它一切业务相关 DTO 必须下沉到模块包。
* **统一响应**：所有 Controller API 必须使用当前 project 基础包下的全局统一响应对象 `com.bgssai.blog.dto.ApiResponse` 进行包装。

## 6. 异常处理与日志流

* **禁用捕获式控制流**：遇到业务校验失败（如余额不足、权限越界），严禁返回 null 或特定的 ErrorCode 字符串来进行业务分支流转，更不准通过捕获 RuntimeException 来做条件控制 。
* **精准抛出异常**：必须直接抛出项目中定义的特定业务异常（如 BizException, UnauthorizedException 等） 。
* **统一接管**：所有异常由项目的 @ControllerAdvice 全局异常处理器统一拦截、打印日志并转换为标准的错误 JSON 格式 。
* **禁止吞噬异常**：catch 块中必须处理异常，生产环境禁止使用 e.printStackTrace() 。如果必须重抛，必须保留原始异常的堆栈信息 。
* **核心审计日志**：涉及资金账本变动、核心状态机流转或权限变更的操作，必须在日志中打印上下文标识（如 userId, orderNo, traceId）以备追溯 。日志输出时，字符串变量之间的拼接必须使用占位符的方式 。

### 6.1 接口日志规范（强制，违者代码不予合并）

本节是 §6「异常处理与日志流」的强化细则，将「所有接口都补充日志」落为项目统一、可执行、可评审的实现路径。本节与 §1（严禁 Lombok / 全局禁用 emoji）、§3（单接口单 Controller / 零信任与边界防御）、§6（异常与日志流既有条款）、§7（外部调用解耦）联合生效，冲突时以本节给出的更具体口径为准。

> **实现方式总纲（最高优先级硬规则，见 §6.1.2）**：接口日志一律由开发者在业务代码里**显式手写**，落点在该接口对应的 **Service 方法入口**与 Service 关键节点；**严禁用 Spring AOP / `@Aspect` / 任何切面实现接口日志，也不采用集中式 HandlerInterceptor / Filter 来统一打接口日志**。Controller 保持轻薄、原则上不写日志，以守住 §3「单接口单 Controller / Controller < 50 行」。

#### 6.1.1 覆盖维度（强制）

* **每个对外 HTTP 接口都必须有日志**，且日志必须覆盖以下三个维度，缺一不可：
  1. **接口名称**：能唯一定位到具体端点（业务动作名称，或 HTTP 方法 + 路由 / Service 类全名 + 方法）。
  2. **请求参数**：仅记录非敏感的关键字段（白名单显式字段，见 §6.1.6）。
  3. **关键处理流程**：业务主干的关键节点（状态流转、账本变动、权限变更、外部调用、关键分支、完成点）。
* **「接口名称 + 请求参数」由该接口对应的 Service 方法入口的一行显式 INFO 日志覆盖**（§6.1.2），其中请求参数一层**仅记录非敏感的关键字段（白名单）**，见 §6.1.6；**「关键处理流程」由 Service 关键节点的显式日志覆盖**（§6.1.3）。二者合起来才算满足本条三维度要求，**单覆盖入口或单覆盖流程任一项都不算达标**。全部日志均为开发者显式手写，不存在任何切面 / 拦截器自动兜底——若某接口对应的 Service 方法入口缺少入参日志，即视为该接口无日志，CR 阶段必须打回。

#### 6.1.2 实现方式：显式手写日志，禁止 AOP / 切面（最高优先级硬规则，强制）

> 本条与 §1 / §3「单接口单 Controller、Controller 内代码越少越好（理想 < 50 行，含 import）」强制协调一致，是本节的最高优先级硬规则，与 §6.1 其余各条冲突时以本条为准。

* **接口日志一律由开发者在业务代码里显式手写**——「接口名称 + 请求参数」这一层，必须在**该接口对应的 Service 方法入口**显式记录一行 INFO（业务动作 / 接口名称 + 仅记录非敏感的关键字段（白名单），见 §6.1.6）。这是本仓库接口日志唯一允许的实现方式。
* **严禁用 Spring AOP / `@Aspect` / 任何切面（`@Around` / `@Before` / `@After` / `@AfterReturning` / `@AfterThrowing`）实现接口日志**，**严禁为此引入 `spring-boot-starter-aop` 依赖**（用户明确决定："打印日志不要使用 aop"）。
* **同样不采用集中式 `HandlerInterceptor` / `Filter` 来统一打接口日志**——不得为"做接口日志"这一目的新建任何集中式拦截器 / 过滤器组件，出入口日志必须由各 Service 方法自己显式写出。
  * **例外（既有基础设施，保留）**：链路 `traceId` 的 `RequestTraceFilter` 是用于把 traceId 注入 MDC 的既有基础设施（见 §6.1.7），**保留、不在本禁止之列**；本条禁止的是"为打接口出入口日志而新建集中式拦截器 / 过滤器 / 切面"。
* **落点：出入口日志下沉到 Service 方法入口，Controller 保持轻薄、原则上不写日志**。本仓库当前有 **226 个单接口 Controller**，若把日志硬塞进每个薄 Controller，会：
  1. 撑爆 §3「Controller < 50 行」红线（出入口日志 + 计时会让薄 Controller 迅速膨胀）；
  2. 与「Controller 仅负责参数绑定、调用 Service、组装 Response」的职责边界相悖。
  
  因此把「接口名 + 非敏感关键字段（白名单）」的显式日志统一写在**对应 Service 方法入口**，既满足 §6.1.1 维度要求，又守住 Controller 的薄边界。
* **Service 方法入口日志（建议内容）**：进入方法时记录一行 INFO，包含——业务动作 / 接口名称、当前用户标识（如 `userId`，来自上层传入而非客户端自述，呼应 §3 零信任）、仅记录业务排查所需的非敏感关键字段（白名单显式字段，见 §6.1.6）；**严禁打印整个请求 DTO / 整对象 dump**，需要更细的调试明细时只针对个别非敏感字段走 DEBUG。返回结果摘要 / 耗时按需在 Service 关键节点或完成点（§6.1.3）记录，不强制每方法计时。
* **正例（在 Service 方法入口显式手写白名单关键字段日志）**：

  ```java
  // 正例：出入口日志写在 Service 方法入口，Controller 保持轻薄不写日志
  @Service
  public class ArticleTakeDownService {

      private static final Logger log = LoggerFactory.getLogger(ArticleTakeDownService.class);

      @Transactional
      public void takeDown(Long operatorId, TakeDownArticleRequest req) {
          // 接口名 + 白名单显式字段的入口日志（占位符传参，只取非敏感关键字段，不打印整个请求对象）
          log.info("下架文章 operatorId={} articleId={}", operatorId, req.getArticleId());
          // ... 业务主干自顶向下平铺，关键节点按 §6.1.3 补流程日志 ...
          log.info("下架文章完成 operatorId={} articleId={}", operatorId, req.getArticleId());
      }
  }
  ```

* **反例 A（禁止：用 AOP 切面 / 引入 spring-boot-starter-aop 实现接口日志）**：

  ```java
  // 反例：本仓库严禁用 AOP / @Aspect 打接口日志，亦禁止为此引入 spring-boot-starter-aop
  @Aspect
  @Component
  public class ControllerLogAspect {
      @Around("within(com.bgssai.blog..controller..*)")
      public Object around(ProceedingJoinPoint pjp) throws Throwable {
          // 反例：切面集中兜底接口日志，违反 §6.1.2 显式手写硬规则
          return pjp.proceed();
      }
  }
  ```

* **反例 B（禁止：把大量出入口日志堆进薄 Controller）**：

  ```java
  // 反例：撑爆 Controller < 50 行红线，把本该在 Service 入口的日志硬塞进薄 Controller
  @PostMapping("/take-down")
  public ApiResponse<Void> handle(@RequestBody @Valid TakeDownArticleRequest req) {
      log.info("进入下架文章接口, 参数=" + req);          // 反例：日志撑厚 Controller + 字符串拼接
      long start = System.currentTimeMillis();
      articleTakeDownService.takeDown(currentUserId(), req);
      log.info("下架文章完成, 耗时=" + (System.currentTimeMillis() - start));
      return ApiResponse.ok();
  }
  ```

* **反例 C（禁止：为打接口日志新建集中式拦截器 / 过滤器）**：

  ```java
  // 反例：为统一打接口出入口日志而新建集中式 HandlerInterceptor，等价于切面兜底，违反 §6.1.2
  @Component
  public class AccessLogInterceptor implements HandlerInterceptor {
      @Override
      public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) {
          // 反例：集中式拦截器统一打接口日志，本仓库不允许（traceId 的 RequestTraceFilter 属既有基础设施例外）
          return true;
      }
  }
  ```

#### 6.1.3 Service 层关键处理流程日志（强制）

* **「关键处理流程」这一层同样由开发者在 Service 层关键节点显式手写日志**，至少覆盖以下场景：
  1. **状态机流转**：实体状态变更（如文章 DRAFT → PUBLISHED → TAKEN_DOWN、订单 / 工单状态切换）；
  2. **资金 / 积分账本变动**：余额、积分、钱包流水的增减（呼应 §6「核心审计日志」）；
  3. **权限 / 角色变更**：授权、封禁、角色调整等；
  4. **外部第三方调用前后**：调用 OAuth / 短信 / 支付 / 内容审核 / 对象存储 / 邮件 / MQTT 等之前与之后（含成功 / 失败结果，呼应 §7「外部调用解耦」）；
  5. **重要业务分支**：校验拒绝、命中风控、限额触发、幂等命中等关键判定点；
  6. **方法完成点**：核心写操作 / 状态机方法成功落库后的里程碑日志。
* **纯查询 / 列表 / get 类读路径只需在 Service 方法入口保留一行接口名 + 非敏感关键字段（白名单）的 INFO（§6.1.2）即可**——**不要**为了凑数在简单读路径里堆砌关键流程日志，避免噪声淹没真正重要的审计信息。
* **示例（Service 关键节点显式补点）**：

  ```java
  log.info("发布文章 userId={} articleId={}", userId, articleId);
  log.info("积分扣减 userId={} before={} delta={} after={}", userId, before, -cost, after);
  log.info("调用支付网关前 orderNo={} channel={} amount={}", orderNo, channel, amount);
  log.info("调用支付网关返回 orderNo={} success={} gateway_code={}", orderNo, success, gatewayCode);
  log.warn("发布被风控拒绝 userId={} reason={}", userId, reason);
  ```

#### 6.1.4 Logger 声明规范（强制）

> 本条呼应 §1「严禁使用 Lombok」。

* 统一使用 slf4j 门面声明 Logger，固定写法：

  ```java
  private static final Logger log = LoggerFactory.getLogger(Xxx.class);
  ```

  其中 `Logger` / `LoggerFactory` 为 `org.slf4j.Logger` / `org.slf4j.LoggerFactory`，`Xxx.class` 为当前类。
* **严禁**使用 Lombok 的 `@Slf4j` / `@Log4j2` / `@CommonsLog` 等任何注解生成 logger（与 §1 全局禁用 Lombok 一致）。
* **反例（禁止）**：

  ```java
  @Slf4j                       // 反例：Lombok 注解，违反 §1
  @Service
  public class ArticleService { }
  ```

#### 6.1.5 占位符拼接与日志级别策略（强制）

* **占位符拼接（强制，与 §6「日志输出时字符串变量之间的拼接必须使用占位符」一致）**：日志消息中的变量一律使用 `{}` 占位符传参，**严禁**字符串 `+` 拼接。
  * **正例**：`log.info("发布文章 userId={} articleId={}", userId, articleId);`
  * **反例**：`log.info("发布文章 userId=" + userId + " articleId=" + articleId);`
* **日志级别策略（强制）**：

  | 级别 | 用途 | 示例场景 |
  | --- | --- | --- |
  | **INFO** | 接口出入口 + 关键业务里程碑 | Service 方法入口/出口显式日志行、状态机流转成功、账本变动落库、权限变更完成、外部调用成功返回 |
  | **DEBUG** | 详细调试 / 大对象明细 | 完整请求 / 响应体明细、中间计算过程、循环内逐条明细（默认生产不开） |
  | **WARN** | 可恢复异常、被拒绝的请求、风控命中 | 参数校验拒绝、权限越界被拒、风控 / 限额命中、幂等重复请求、外部调用可重试失败 |
  | **ERROR** | 未预期失败（带异常栈） | 系统级未捕获异常、不可恢复的外部依赖故障、数据一致性破坏 |

* **异常栈打印归属（强制，避免重复栈）**：
  * 异常堆栈统一由 `@RestControllerAdvice` 全局异常处理器（`GlobalExceptionHandler`）打印（呼应 §6「统一接管」「禁止吞噬异常」）；
  * **业务代码（Controller / Service）catch 后重抛时不得重复打印异常堆栈**——若需在 Service 的 catch 中记录上下文，仅记录一行 WARN：业务动作 + 关键标识 + 异常类型 + 简短 message（**无栈**），并保留原始异常原样重抛（呼应 §6「保留原始异常的堆栈信息」）交由全局异常处理器统一打栈；
  * **严禁在多个层级重复打印同一份堆栈**，最终栈以全局异常处理器 `GlobalExceptionHandler` 输出为准。

#### 6.1.6 敏感数据：白名单显式字段（强制，违者代码不予合并）

> 本条呼应 §3「零信任与边界防御」「DoS 防御不外包」。

* **本仓库不设专门的日志脱敏工具类**（不需要 `SensitiveLogUtil` / `LogSanitizer` 之类的脱敏 / 掩码工具方法）。敏感数据的安全性**通过"压根不记录敏感字段"保证**，而非"先取全量再掩码"。
* **参数日志一律采用白名单显式字段方式**：**只显式记录业务排查所需的非敏感关键字段**（如资源 id / 标题 / 状态 / 数量 / 分页参数等），逐字段以 `{}` 占位符传入；**严禁打印整个请求 DTO / 整对象 dump**（即严禁 `log.info("... req={}", req)` 这类把整个请求对象交给日志的写法）。
* **下列敏感字段一律不打印**——从源头不取、不记（不是"取出来再掩码"）。保留以下分类清单，但语义为**命中即不打印**：

  | 类别 | 关键字（命中即不打印，大小写不敏感，snake_case 与 camelCase 同时覆盖） |
  | --- | --- |
  | 口令类 | `password` / `pwd` / `passwd` / `old_password` / `new_password` |
  | 密钥类 | `secret` / `client_secret` / `private_key` / `api_key` |
  | 令牌类 | `token` / `access_token` / `refresh_token` |
  | 验证码类 | `code` / `sms_code` / `email_code` / `captcha` |
  | 签名类 | `sign` / `signature` |
  | 证件 / 卡号类 | `id_card` / `bank_card` / `card_no` / `cvv` |

* **手机号 / 邮箱**：如确需在日志中出现，直接在写日志前对该字段做内联部分掩码（如手机号 `138****8000`、邮箱 `a***@example.com`）即可，**无需任何专门工具类**——内联拼出掩码后的字符串作为占位符参数传入。能不打印则不打印，避免在日志中沉淀可直接复用的完整 PII。
* **截断 / 防 DoS（强制）**：通过"只记必要字段、不打印整 body"自然达成——既然参数日志只输出少量白名单字段，就不会出现超大 body 刷屏或被构造为日志 DoS 的情形（呼应 §3 零信任与边界防御、§2 `clampPageSize` 截断思路）。**严禁打印超大 body / 大对象明细**（如完整富文本正文、整批列表数据）；确需调试个别长字段时按 §6.1.5 走 DEBUG，且默认生产不开。本条不再依赖任何工具方法做截断。
* **执行口径**：敏感字段从源头不进日志，**严禁**先打印明文再事后处理，也**严禁**为统一掩码而新建脱敏工具类。CR 阶段发现参数日志含明文敏感字段、或出现整请求对象 dump 一律打回，无论改动规模大小。

#### 6.1.7 traceId 链路追踪（强制约束业务代码不得手工拼接）

* 链路追踪 `traceId` 已由 `RequestTraceFilter` 在请求入口注入 MDC，并通过 logback 的日志 pattern `%X{traceId}` 自动输出到**每一行**日志，无需业务代码介入。
* 因此业务代码（Controller / Service）**不得**再手工把 `traceId` 拼进日志文案——既冗余又易与 MDC 输出重复。需要按 traceId 串联排障时，直接依赖每行日志已自动携带的 `%X{traceId}` 字段即可（呼应 §6「打印上下文标识 …… traceId …… 以备追溯」）。`RequestTraceFilter` 仅用于注入 traceId 到 MDC，属既有基础设施，不与 §6.1.2「不为接口日志新建集中式拦截器 / 过滤器」冲突。

#### 6.1.8 日志文案禁用 emoji（强制）

* 日志文案（即代码中的字符串字面量）同样适用 §1「全局禁用 emoji（强制，违者代码不予合并）」：**严禁**在日志消息、占位符模板、字段标签中出现任何 emoji 或装饰性图形符号（如 ✅ ❌ ⚠ 等）。分级 / 强调一律用纯文字（如 `WARN` / `已拒绝` / `命中风控`）表达。

#### 6.1.9 Code Review 检查清单（接口日志专项）

涉及新增 / 修改对外 HTTP 接口或日志的 PR，CR 阶段须逐条核对，任一不满足即打回：

1. **入口显式日志 + 禁 AOP**：该接口对应的 **Service 方法入口**是否有「接口名 + 非敏感关键字段（白名单）」的显式 INFO 日志（§6.1.2），且确认**未使用 Spring AOP / `@Aspect` / 任何切面、未引入 `spring-boot-starter-aop` 依赖、未新建为打接口日志而设的集中式 `HandlerInterceptor` / `Filter`**；同时确认 Controller **未因日志变厚**——出入口日志下沉到 Service 入口、Controller 保持轻薄不写日志（守住 §3「单接口单 Controller / Controller < 50 行」）；
2. **关键流程日志**：关键写 / 状态机 / 资金 / 积分 / 权限 / 外部第三方调用路径是否在 Service 层补了关键流程日志（§6.1.3）；纯读路径未堆砌噪声日志；
3. **敏感字段（白名单显式字段）**：参数 / 流程日志是否**只记录非敏感字段**、有无整个请求对象 dump（`req={}` 之类）、有无敏感字段明文（对照 §6.1.6 分类清单，命中即一律不打印），手机号 / 邮箱是否按需做内联部分掩码（确认**未**为此引入任何脱敏工具类，§6.1.6）；
4. **占位符**：日志是否一律使用 `{}` 占位符，无字符串 `+` 拼接（§6.1.5）；
5. **Logger 声明**：是否为 `private static final Logger log = LoggerFactory.getLogger(Xxx.class);`，**未**误用 Lombok `@Slf4j` 等注解生成 logger（§6.1.4 / §1）；
6. **异常栈不重复**：业务代码（Controller / Service）catch 后重抛时是否**不重复打印异常堆栈**（仅按需记一行无栈 WARN：业务动作 + 关键标识 + 异常类型 + 简短 message），异常栈是否仅由全局异常处理器 `GlobalExceptionHandler` 统一打印，无多层重复栈（§6.1.5）；
7. **traceId**：日志文案中是否**未**手工拼接 `traceId`（依赖 MDC + `%X{traceId}` 自动输出，§6.1.7）；
8. **禁 emoji**：日志文案是否未引入任何 emoji / 装饰性符号（§6.1.8 / §1）。

## 7. 扩展性设计约束

* **策略隔离**：新增同类外部渠道（如支付通道、第三方模型、短信供应商）必须实现对应的基础接口（Strategy/Provider），并通过工厂模式路由，**严禁在核心业务逻辑中写 if-else 分发** 。
* **外部调用解耦**：所有对外部第三方系统的耗时调用，必须评估是否需要接入异步任务队列补偿与重试机制，确保主业务链路的高可用 。
* **第三方 API 能力前置核对（强制，违者代码不予合并）**：本系统大量依赖第三方 API（已在用：OAuth 第三方登录 WeChat / Alipay / Douyin / Xiaohongshu / Weibo / Baidu、短信 Tencent 云短信、支付 Alipay、内容审核 DeepSeek、对象存储 S3 兼容、邮件 SMTP、消息通知 MQTT broker）。**在设计任何依赖第三方 API 的功能之前，必须先核对该第三方 API 的真实能力是否支撑本功能**，严禁凭记忆或假设"第三方接口能做"就直接落设计——想当然假定到开发阶段才发现第三方不支持，会造成返工甚至功能断裂。
  * **核对维度（至少覆盖）**：① 所需**接口 / 端点是否存在**；② **请求 / 响应字段**是否覆盖业务所需；③ **回调 / Webhook 机制**（是否提供、推送内容、验签方式）；④ **配额与限流**（QPS / 日调用上限）；⑤ **鉴权方式**（OAuth scope / 签名算法 / 密钥形态）；⑥ **接口版本与弃用状态**；⑦ **地域 / 资质 / 合规前置**（如短信签名与模板报备、支付资质、内容审核合规要求）。
  * **核对手段**：必须查阅第三方**官方接口文档**（可用 `WebFetch` 拉取官方文档核验），不接受仅凭记忆或假设下结论。
  * **不支持即降级**：**严禁在第三方实际不支持的能力之上做设计**；若第三方不支持或能力受限，必须在设计中**显式标注该限制并给出降级 / 替代方案**。
  * **结论必须落盘**：核对结论必须沉淀到对应设计文档（`docs/architecture` / `docs/api`），使"该功能依赖哪个第三方接口、是否已确认支持、官方文档出处"可追溯。
  * **CR 校验**：凡涉及第三方 API 的设计 / 实现 PR，评审阶段须核对是否已完成并记录上述能力核对；缺失或未落盘者一律打回。

## 8. 测试规约（强制）

* **必须编写单元测试（强制，违者代码不予合并）**：本项目要求对新增 / 修改的业务逻辑补充单元测试。测试框架统一用 JUnit 5（`spring-boot-starter-test`，已内置于 `pom.xml`）；按需可用 Spring Boot Test / `spring-security-test`。测试源码统一放 `src/test/java/**`，包结构与被测类对齐（例：`com.bgssai.blog.util.EntityConverters` 对应 `com.bgssai.blog.util.EntityConvertersTest`）。
* **测试通过是提交 PR 的前置门槛（强制，违者代码不予合并）**：发起 PR 前必须在本地执行 `mvn test`（或 `mvn -B test`）且**全部通过**；CI 也在合入前运行单元测试（实际以 `mvn -B package` 一次性跑通编译 + surefire 单元测试），测试失败的分支不予合并。这是对 §0「每次 PR 必须是可运行版本」的强化——「可编译 + 单元测试通过 + 可启动 + 核心链路手工可用」齐备才算可合入。
* **测试归属与维护**：`src/test/**` 随业务代码一并维护——谁实现 `src/main/**` 业务代码，谁补对应单元测试（开发自测的一部分）。评审时核对「改动是否带了对应单测、`mvn test` 是否通过」；数据层结构（`sql/**`）变更连同相关持久层逻辑的单测一并同步。
* **测试内容要求**：优先覆盖纯函数 / Service 层业务分支 / 边界条件 / 异常路径（如参数校验、状态机流转、权限判定）；对依赖外部第三方（OAuth / 短信 / 支付 / 内容审核 / 对象存储 / 邮件 / MQTT）的路径，用 mock / stub 隔离，**严禁**让单测依赖真实外部服务而在 CI 不稳定 / 失败。测试代码同样遵守 §1「全局禁用 emoji」与项目命名规约。
* **AI 工具生成测试**：使用 AI 编码助手开发功能 / 修复 bug 时，应**一并补充或更新**对应单元测试，使本次改动可由 `mvn test` 验证；不再以「节省 token」为由跳过测试。

## 9. 项目文件与目录结构规范

* **数据库脚本收敛规约**：项目中所有 SQL 脚本统一强制收敛到仓库根 `sql/` 目录下进行唯一管理。**目录扁平化**——不使用 `migration` 子目录与 `V*__xxx.sql` 版本化命名约定；上线 schema 通过手动执行 `DDL.sql` 建立，应用启动期不允许任何 DDL。具体目录拓扑及行为定义如下：
```text
sql/
├── DDL.sql                    ← 最终态 DDL（合并所有历史增量；手动执行）
├── DML.sql                    ← 基础种子数据（Seed Data）及日常数据维护脚本
└── README.md                  ← 目录总览及启动行为说明
```

* **`docs/` 目录拓扑（强制，违者代码不予合并）**：`docs/` 顶层只设 7 类目录，职责单一、互不混装；目录组织的**单一权威**是 [`docs/DIRECTORY-LAYOUT.md`](./DIRECTORY-LAYOUT.md)，与本节表述不一致处以该文件为准。

```text
docs/
├── api/                      ← 接口文档（含 page-api-mapping.md：页面 → 后端 API 映射主表）
├── architecture/             ← 架构文档（系统分解 / ADR / 数据模型 / 状态机 / 时序图 / 架构图）
├── demo-static/              ← 原型页面（见下条「原型与页面跳转描述」）
├── feature/                  ← 功能需求（需求澄清 + 验收标准 + 详细设计）
├── project/                  ← 项目进度（里程碑 / 排期 / 迭代计划 / 待办与风险 / 发布记录）
├── review/                   ← 项目检查记录（CR 结论 / 合规巡检 / 四层一致性核对 / 坏合并事后审计）
└── security/                 ← 安全相关文档（安全设计 / 威胁建模 / 安全规范）
```

  * **数据库脚本不入 `docs/`**：统一收敛到仓库根 `sql/`（见本节上条）。
  * **只有 `demo-static` 按端拆分（强制）**：管理端与用户端共用同一套数据结构与接口契约，因此 `docs/api`、`docs/architecture`、`docs/feature`、`docs/project`、`docs/review`、`docs/security` **一律统一维护、不按端拆目录**，端差异在同一篇文档内分小节并列说明；**仅** `docs/demo-static` 按端拆分（原型须直观呈现各端功能界面）。**禁止**在上述目录下新建 `admin/` · `user/` 子目录或 `*-admin.md` · `*-user.md` 双份副本——同一概念留两份必然漂移（见本节「全产出版本一致」、§11 型六）。
  * **进度与检查记录不混入需求 / 设计**：`docs/project` 只记「做到哪一步」，`docs/review` 只记「查了什么、发现什么、怎么处置」，需求与设计正文仍以 `docs/feature` / `docs/architecture` 为单一事实来源，二者只做引用，**严禁**留第二份需求副本。

* **原型与页面跳转描述（按端 + 按角色端拓扑，强制，违者代码不予合并）**：所有可运行 HTML 原型与页面跳转描述统一存放在 `docs/demo-static/` 下的**端目录**中，各端独立维护。web 端再按角色端拆 `admin/`（管理端）与 `user/`（用户端）；`ios/` 与 `android/` **只有用户端**，直接平铺、不再分 `admin/` / `user/`。目录拓扑如下：

```text
docs/demo-static/
├── web/                      ← web 端
│   ├── index.html            ← web 端分流概览（导向 admin/ 与 user/）
│   ├── assets/               ← 两个角色端共享 CSS（各页以 ../assets/*.css 引用）
│   ├── admin/                ← 管理端
│   │   ├── index.html        ← 本端概览启动台（页面清单 + 入口，浏览器双击即开）
│   │   ├── PAGE-FLOW.html    ← 本端跳转唯一真值（自包含 + 路由矩阵 + 核心用户旅程 + 导航不变量）
│   │   └── *.html            ← 业务页原型（仅 HTML + 内联 mock 数据，字段 snake_case）
│   └── user/                 ← 用户端（结构同 admin/）
├── ios/                      ← iOS 端（仅用户端）
│   ├── index.html            ← 本端原型概览入口
│   ├── PAGE-FLOW.html        ← 本端跳转唯一真值
│   └── *.html                ← 本端业务页原型
└── android/                  ← Android 端（仅用户端，结构同 ios/）

docs/api/page-api-mapping.md  ← 页面 → 后端 API 映射主表
docs/architecture/            ← 设计规范 / PRD-vs-原型差异清单等非原型设计产出
```

  * **端目录口径**：下文「`<端>`」指 `web/admin`、`web/user`、`ios`、`android` 四个**端目录**——每个端目录内必须各有一份 `index.html`（概览入口）与 `PAGE-FLOW.html`（跳转唯一真值）。`web/index.html` 只做两角色端分流，不承载页面清单真值。
  * **领地约束（强制）**：`docs/demo-static/<端>/` **只放可运行 HTML 原型与页面跳转描述（HTML 形式）**，**严禁放任何 `.md` 文档**。设计规范 / PRD-vs-原型差异清单归 `docs/architecture/`，功能需求归 `docs/feature/`。
  * **跳转真值唯一**：各端跳转真值各自为 `docs/demo-static/<端>/PAGE-FLOW.html`，**每端只保留 HTML 一份**——自包含 + 路由矩阵 + 核心用户旅程 + 导航不变量。**严禁**新增 `PAGE-FLOW.md` / `NAVIGATION-FLOW.md` 等 Markdown 版本。
  * **页面 → API 映射**：「页面 → 后端 API」映射表位于 [`docs/api/page-api-mapping.md`](./api/page-api-mapping.md)。后端实现端点时按此映射回溯原型字段；新增 / 调整原型页后须同步更新本表，**禁止**只改原型不刷映射表。
  * **改跳转先改文档**：新增页面或调整跳转关系时，正确流程是「先更新对应端的 `docs/demo-static/<端>/PAGE-FLOW.html`（路由矩阵 + 流程链 + 导航不变量）→（评审）→ 再写代码」，禁止只改代码不改文档。
  * **禁孤儿页 / 禁死胡同**：新增任何页面必须在对应端的 `PAGE-FLOW.html` 中明确其入口（谁能到达）、出口（它能去往哪）、所属导航项；严禁出现无入口（不可达）或无出口（进得去出不来）的页面。
  * **全局导航外壳常驻**：各端一级页面必须挂统一导航外壳（Web 端左侧或顶部导航栏 + 面包屑常驻；移动端底部导航栏）；头像下拉进入账户设置。
  * **CR 校验**：Code Review 阶段须对照对应端 `docs/demo-static/<端>/PAGE-FLOW.html` 跳转矩阵核对本次改动涉及页面的出入口；新增不可达页或死胡同页一律打回。同时核对 [`docs/api/page-api-mapping.md`](./api/page-api-mapping.md) 是否已同步更新「页面 → API」映射条目。

* **React Web 前端编译产物入后端 `static/`（单体交付，强制，违者代码不予合并）**：前端工程（单仓子项目 `<产品>-user/<产品>-user-react`、`<产品>-admin/<产品>-admin-react`；本骨架为 `bgssai-skeleton-user/bgssai-skeleton-user-react`、`bgssai-skeleton-admin/bgssai-skeleton-admin-react`）的 **production build 产物**必须落入**对应后端模块**的 `src/main/resources/static/`，由 Spring Boot 作为 classpath 静态资源对外提供，随后端打成**同一个可部署 jar**（单体交付）。该目录是「**编译后的前端产物**」的落地处，与上一条所述 `docs/demo-static/`（**可运行 HTML 原型** + 内联 mock 数据）是**两回事**——前者是 React 工程的编译输出，后者是产品 IA 的原型真值，**互不混淆、互不回流**。
  * **自动化、禁手工搬运（强制，违者代码不予合并）**：`build → 拷贝进 static/` 必须固化为脚本（前端工程的 `npm run build:deploy`，对应各前端工程的 `scripts/sync-static.mjs`）一键完成，**严禁**手工逐文件复制 / 拖拽搬运产物。
  * **拷贝前先清理**：覆盖前必须先清掉 `static/` 里**上一版前端产物**（`index.html` / `assets/` / 旧 `js/` 等），避免上一版残留文件被 Spring Boot 误服务；但后端在 `static/` 下**自行维护的非前端文件须保留**，清理范围只针对前端构建产物。
  * **产物归属与提交边界（强制，违者代码不予合并）**：React **源码**留在前端子项目（`*-react/`）内，**不进**后端模块；**构建产物**落入并**提交进**对应后端模块——即后端 `src/main/resources/static/` 下的 `index.html` + `assets/*` 等纳入版本管理。源码与产物**边界清晰、互不混淆**：后端模块内不放前端源码，前端源码变更与产物同步可在同一单仓 PR 中完成，但产物必须由脚本生成、不得手改。
  * **SPA 路由协同（属后端职责）**：前端使用 client-side（前端）路由时，后端须把**未匹配的「非 `/api/**`」路径回退到 `index.html`**（forwarding / resource handler 实现），使深链与刷新不 404；该转发逻辑属**后端职责**，在本仓库实现，**不得**因此放宽或拦截 `/api/**` 的正常路由。
  * **适用范围仅 Web 端**：此「编译产物入后端 `static/`」约定**仅适用于 Web 端**（`<产品>-user-react` / `<产品>-admin-react`）；iOS / Android 是各自**独立的原生工程**（未来另立，当前仓库仅有原型占位页），产物不打进后端 jar，**不适用本条**。

* **文档单一事实来源（强制）**：本仓库（单仓）根目录的 `docs/` 是**全项目文档的单一事实来源**。前端工程（`<产品>-user-react`、`<产品>-admin-react`，以及 iOS / Android 原生工程）**不承载任何需求 / 设计 / 原型 / 接口 / 架构 / 安全 / 进度 / 检查记录文档**；前端 / 各端一律依据本仓库 `docs/` 文档进行开发，不在各端工程仓库另建文档副本。各端（web 管理端 / web 用户端 / ios / android）的原型界面与页面跳转描述也全部集中在本仓库 `docs/demo-static/<端>/`（详见上条「原型与页面跳转描述」），不分散到各端工程仓库。**CR 校验**：在前端工程仓库新建任何需求 / 设计 / 原型 / 接口类文档，一律打回。
* **系统设计文档**：底层数据结构、存储方案的设计必须通过评审并沉淀为文档 。所有用例图、状态图 、时序图 、类图 、活动图 等系统架构设计文档，统一强制存放在项目根目录的 docs/architecture 文件夹中（含 ADR、模块设计、工程指南，以及 HTML + 内联 SVG 架构图集）。
* **程序接口文档**：所有系统对外的程序接口说明、API 契约等统一强制存放在项目根目录的 docs/api 文件夹中。
* **接口文档工具限制**：**全栈严禁使用 Swagger 等侵入式运行时注解工具**。所有接口文档必须采用离线 Markdown 源码或符合项目约定的静态契约文档形式，在外部生成并静态归档于 docs/api 目录中，严禁在 Java 代码中引入 Swagger 相关注解，确保生产代码的纯净性。

## 10. 页面跳转流程契约（强制，违者代码不予合并）

* **唯一权威来源**：`docs/demo-static/<端>/*.html` 是产品信息架构（IA）的可运行原型，各端页面之间「谁能跳到谁」的**唯一权威规格**为对应端目录的 `PAGE-FLOW.html`（web 管理端为 [`docs/demo-static/web/admin/PAGE-FLOW.html`](./demo-static/web/admin/PAGE-FLOW.html)，web 用户端为 [`docs/demo-static/web/user/PAGE-FLOW.html`](./demo-static/web/user/PAGE-FLOW.html)，iOS 端为 `docs/demo-static/ios/PAGE-FLOW.html`，Android 端为 `docs/demo-static/android/PAGE-FLOW.html`；**均只用 HTML 形态**，自包含 + 路由矩阵 + 核心用户旅程 + 导航不变量，不再维护任何 `.md` 版本）。实现页面导航、路由、跳转入口与返回路径时，**必须先读对应端的 `PAGE-FLOW.html`，使跳转关系与之逐条一致**；**严禁**为本契约新增 / 回流 `PAGE-FLOW.md` / `NAVIGATION-FLOW.md` 等 Markdown 版本。
* **规格先行（Spec-first）**：新增 / 删除页面或调整跳转关系时，正确流程是 **先改对应端 `docs/demo-static/<端>/PAGE-FLOW.html`（路由矩阵 + 流程链 + 导航不变量）→（评审）→ 再写实现代码**；严禁「代码先跑通、文档回头补」。提交涉及前端路由 / 导航的 PR 时，CR 阶段必须核对本次改动是否与对应端 `PAGE-FLOW.html` 的路由矩阵与核心旅程一致；同时同步刷 [`docs/api/page-api-mapping.md`](./api/page-api-mapping.md) 对应行的「页面 → API」映射。
* **必须满足的导航不变量**（逐条强制）：
  1. **列表必达详情**：任何列表 / 目录的每一项都必须可点击进入其详情页，严禁假可点。
  2. **详情必可返回**：详情页必须能返回来源页（浏览器后退 / 面包屑 / 返回按钮）。
  3. **无死胡同**：任何页面都必须至少有一条通往本端主页（骨架为 `home.html`）的路径。
  4. **无孤岛**：除登录 / 注册外，每个产品页都必须至少有一个站内父级入口（见路由矩阵「主要入口」列）。
  5. **全局导航外壳常驻**：Web 端导航栏（左侧或顶部）+ 面包屑在所有一级页面常驻；二级页面可通过面包屑或返回按钮层层返回。
  6. **权限态分支**：可见入口按当前身份的 `role_code`（骨架为管理端 `PLATFORM_ADMIN`、用户端 `USER`）渲染，权限不足时不渲染入口（而非渲染后报 403）；后端仍须独立做权限与数据范围校验，不因前端隐藏入口而放宽。

* **CR 校验扩展**：涉及原型或导航改动，CR 阶段必须核对对应端 `docs/demo-static/<端>/PAGE-FLOW.html` 跳转矩阵的入口 / 出口 / 返回路径是否一致，并核对 [`docs/api/page-api-mapping.md`](./api/page-api-mapping.md) 是否同步更新。
* **借鉴边界**：IA 可借鉴同类 B2B SaaS 产品的导航组织方式，但**只参考信息架构与交互组织**；严禁复制第三方品牌 / Logo / 视觉皮肤 / 原始文案；BGSSAI 沿用自有主色与设计语言。

## 11. 合并纪律与坏合并（bad merge）防治（强制，违者代码不予合并）

本仓库采用 AI 高频并行开发——大量短命自动化开发分支从不同基线切出、交叉合并回 `develop`。这种拓扑天然高发**坏合并**，已实际发生多起并被事后修复（历史案例、根因分型与复盘教训在对应修复 PR 的描述中记录）。本节把坏合并的**认知、预防、检测、修复**沉淀为可执行、可评审的硬规约，与 §0「每次 PR 必须是可运行版本」、§1「禁 emoji」、§8「测试规约（必须单测且 mvn test 通过）」联合生效。

### 11.1 坏合并定义

**坏合并（bad merge）** 指：两条分支合并后，git **未报文本冲突**、提交看起来「干净」（能提交、甚至能编译），但结果实际错误——丢失了一边本应保留的改动、选错了 ours/theirs、把两边内容机械拼接出重复、或两边各自单看都对但合起来自相矛盾。它比普通冲突**更隐蔽**：git 不会报警，往往要等对应功能跑出 bug、或人工通读才暴露。**「无冲突标记」不等于「合并正确」**——这是本节的认知前提。

### 11.2 坏合并分型（强制认知）

下列七型须全员认知；前五型为通用合并错误，型六 / 型七是本仓库已发生的真实案例。

| 型号 | 名称 | 典型症状 |
| --- | --- | --- |
| 型一 | 残留冲突标记 | `<<<<<<<` / `=======` / `>>>>>>>` / `\|\|\|\|\|\|\|` 被当正常代码提交 |
| 型二 | 一边改动被整段丢弃 | 无脑保留 ours 或 theirs，覆盖了另一边本应保留的修改 |
| 型三 | 选错 ours/theirs | 留了过时 / 错误的一边，丢了正确的一边 |
| 型四 | 语义冲突 | 两边都无冲突标记、各自单看都对，合起来编译不过或逻辑矛盾（一边改了方法签名 / 字段名，另一边新增对旧签名的调用；一边删了常量 / 方法，另一边还在引用） |
| 型五 | 草率删冲突标记式合并（最隐蔽） | 为快速消冲突，未对比 ours/theirs 就删空 / 保留一边，表面「干净」但一边关键改动被无声丢弃 |
| 型六 | 跨版本 / 跨文件决策未传播 | 一个决策落在某文件（如 v001），另一并行分支从「决策前基线」切出、新建了内容更全的新文件（如 v002），两边改**不同文件**，git 自动合并不报冲突，决策在新文件里被静默丢弃，造成全仓自相矛盾（真实案例：feature-019 v002，修复 commit `1e299c7`） |
| 型七 | 机械拼接 | 两边改了**同一区域**（HTML / 映射表 / 路由 JSON），被机械拼接成重复 id / 重复表行 / 重复 JS 键 / 悬空碎片，无残留标记（真实案例：PR#360，修复 commit `bde54ea`） |

### 11.3 预防规约（强制）

* **短命分支 + 先并基线**：分支尽量短命、小颗粒；长命分支合入前**先把目标分支最新改动并进来再解**（rebase 或先 merge develop 进本分支），缩短跨基线分叉窗口，从源头压低型六风险。
* **合并即可运行**：呼应 §0，合并完成后必须本地 `mvn -q -DskipTests compile` 零错误 + 核心链路手验，方可发起 / 合入 PR。
* **决策传播规约（针对型六，强制）**：当一次改动**删除 / 重命名某枚举值、字段、概念、状态、接口**时，提交者必须在合入前对**被删 / 被改的标识符做全仓 grep**（覆盖 `docs/feature`、`docs/api`、`docs/architecture`、`docs/demo-static/**`（各端目录逐一）、`src/main` 等所有层），把每一处一致地**更新或墓碑标注**（「已废弃 / 已取消」+ 日期 + 出处），**严禁只改一层就合入**。
* **同区域并行改动规约（针对型七，强制）**：当预判两分支会改同一原型页 / 同一映射表 / 同一路由 JSON 时，合并后**必须人工逐块核对**有无重复 id / 重复行 / 重复键 / 悬空碎片，而不是信任「无冲突标记」。

### 11.4 合并后检测清单（CR 合入闸门，强制）

涉及合并 / 解冲突的 PR，CR 阶段须逐条核对，任一不过即打回：

1. **扫残留冲突标记（型一 / 型五）**：

   ```bash
   grep -rnE '^(<{7}|>{7})( |$)|^={7}$|^\|{7}( |$)' src docs *.md
   ```

   命中即打回（排除把这些当字面示例的规范类文档：`BGSSAI-Standards.md`）。
2. **扫相邻重复赋值 / 语句行（型五 / 型七残留）**：核对改动文件无「连续两行完全相同的非平凡赋值 / 调用」（如重复的时间戳赋值、重复的 setter 调用）。
3. **决策删除类改动全仓核对（型六）**：对本次**被删 / 被改的标识符**做全仓 grep，确认无「仍把它当现存值列出」的残留；确属保留现状的下游分层（文档与现行 schema / 后端一致）须显式说明，不得误删造成新的不一致。

   ```bash
   grep -rn "<被删或被改的标识符>" docs/ src/
   ```
4. **原型 / 映射 / 路由合并核对（型七）**：核对无重复 id / 重复表行 / 重复 JS 键 / 悬空碎片。

   ```bash
   grep -oE 'id="[^"]+"' path/to/page.html | sort | uniq -d   # 重复 id 应为空（排除注释引用 / 互斥模板分支等误报）
   ```
5. **编译兜底（型四）**：`mvn -q -DskipTests compile` 零错误。

### 11.5 坏合并修复 SOP（强制）

* 一旦 CR 或运行期发现疑似坏合并，须**事后审计 + 精细化修复**。
* 先用 git 历史还原两边意图（`git show <merge>`、`git diff <base>...<parentX>`），对每处恢复「双方都应保留的正确融合结果」，而非简单选一边；**只还原正确的合并结果、绝不夹带新功能 / 重构 / 风格调整**。
* 修复通过 `mvn -q -DskipTests compile` 后开 **draft PR** 供人复核，**不直接合并目标分支**；每处「保留哪一边、为什么」的判断依据须写进 PR 描述，保留最后一道人工把关。
* 拿不准该保留哪一边、或问题超出「合并修复」范畴（属功能缺陷 / 设计问题）时，不擅自决断，记录后另行处理。

> 历史坏合并案例、根因分型与复盘教训在对应修复 PR 的描述中记录。本节定「今后怎么防 / 怎么测 / 怎么修」，修复 PR 描述记「发生过什么 / 根因 / 教训」，两者口径一致。

## 12. 禁止 AI 创建自唤起的循环任务（成本红线，强制，违者代码不予合并）

本节约束的是 **AI 助手的行为**，不是项目的技术选型：**禁止 AI 创建任何会在无人值守下反复唤起 AI 会话、持续消耗 token 的循环任务**。本节**不限制项目自身的任务调度**——业务定时任务、运维 cron、普通 CI 定时构建一律照常使用，不受本节约束。

### 12.1 事故背景（立规缘由）

2026-07-28，外部 AI 助手在未获授权的情况下创建了每 15 分钟运行一次的 heartbeat 轮询作业（`voiceunion-pr`），持续约一天。该作业在无人值守下反复唤起 AI 会话，**耗尽当周包含用量，并额外产生约 6 欧元 credits 费用**。

根因不是单次调用昂贵，而是**它会自己再来一次**——15 分钟间隔意味着一天自动执行 96 次、一周 672 次，无人察觉即可持续消耗。这类任务的成本上限由**运行时长**而非单次开销决定，因此对其采取**默认拒绝（Deny by default）**，与 §3 零信任原则一致。

需要特别指出：该作业是靠**平台侧定时触发器反复唤起 AI 会话**运行的，并非宿主机 `crontab`。防线因此必须落在「会话 / 平台级调度入口」上，只盯 shell 命令是拦不住的。

### 12.2 唯一判定标准

> **它是否会在无人值守下反复唤起 AI 会话、持续消耗 token？**

是则禁止，否则放行。判断时只问这一句，不看它叫不叫「定时任务」、用没用 cron 语法。

### 12.3 禁止范围

**一、会话 / 平台级的周期性调度**

1. `CronCreate` 等会话内定时任务工具的**周期性**用法（`recurring` 为真）；
2. `ScheduleWakeup` 等自唤起机制（含 `/loop` 的自定节奏模式）；
3. 平台 Routine / Scheduled Task 的 `cron_expression` 等**周期性**配置；
4. 「任务里再排下一次任务」的自我续期（self-rescheduling）——本质等价违规。

一次性用法（一次性提醒、`run_once_at` 单次 Routine、单次 `send_later`）**不在禁止之列**——它不构成循环。

**二、用系统调度器或循环反复拉起 AI CLI**

`crontab` / `systemd-run --on*` / `systemctl ... *.timer` / `at` / `batch` / `schtasks` / `launchctl`，或 `while true` / `until false` / `for ((;;))` / `watch` 循环，**其执行内容为 `claude` / `codex` / `gemini` / `aider` 等 AI CLI**；以及 AI CLI 自带的 `--cron` / `--schedule` / `--heartbeat` / `--watch` 参数与同名子命令。

**三、「定时触发 + 调用 AI」的 CI 工作流**

`.github/workflows/*.yml` 中**同时**存在周期性触发（`schedule:` / `cron:`）与 AI 调用（`anthropics/claude-code-action`、`@anthropic-ai/claude-code`、`claude` / `codex` 命令）。**只有两者同时出现才禁止**——单独的定时构建不禁止。

### 12.4 明确不在禁止之列（避免误伤）

以下**一律允许**，CR 不得据本节打回。它们不经过 AI 会话，不消耗任何 token：

1. **项目运维调度**：数据库备份 `crontab`、日志清理 `*.timer`、巡检脚本、`at` 延时作业；
2. **普通 CI 定时触发**：`.github/workflows` 中跑构建 / 测试 / 巡检的 `schedule:`，以及 Jenkins 的 `pollSCM` / `cron(...)`；
3. **应用运行时的业务定时任务**：Spring `@Scheduled`、Quartz、XXL-JOB 等；
4. **产品语义的 heartbeat / keepalive**：设备心跳上报接口、连接保活、任务状态轮询接口等对外契约；
5. **前端业务轮询**：`setInterval` / SWR / 长轮询；
6. **开发态 watch 模式**：`npm run watch` / `vite dev` / 增量编译；
7. **不调用 AI 的等待与循环**：单次 `sleep`、`while read` 逐行读取、`watch -n 900 gh pr checks` 等。

一句话：**cron 本身不是问题，cron 拉起 AI 才是问题。**

> 注：本节与各仓 `CLAUDE.md`「部署仅手动触发、不挂 SCM 触发器、不配 webhook」是**两件独立的事**——后者出于部署安全与发布纪律，由该条款自行约束；本节只管 AI 的 token 消耗，不为部署通道立规。

### 12.5 例外申请流程（强制，禁止自行开口子）

确有周期性需求时**严禁绕过闸门**：

1. **书面报备**：向仓库负责人说明用途、间隔、预计每日调用次数与消耗上限；
2. **优先改手动触发**：获批后优先改为人工触发（`workflow_dispatch` / Jenkins Build with Parameters）；
3. **确需自动周期执行**：由**仓库负责人**在 `.claude/settings.json` 中显式登记例外，并在 PR 描述中附获批记录；
4. **必须设停止条件**：任何获批的周期性作业必须同时设定**到期时间或最大执行次数**，禁止无限期常驻。

**严禁**任何角色以「便于观察 CI」「顺手盯一下 PR」「只跑一晚上」等理由自行创建——事故正是这样发生的。

### 12.6 机器强制：PreToolUse 闸门

上述禁止范围由 `.claude/hooks/block-scheduled-automation.py` 在 `PreToolUse` 阶段拦截，经 `.claude/settings.json` 注册，匹配 `Bash` / `Write` / `Edit` / `NotebookEdit` 及 `CronCreate` / `ScheduleWakeup` / `create_trigger` / `update_trigger` 等调度类工具，命中即返回 `permissionDecision=deny` 并说明原因与例外路径。闸门说明与自检方法见 `.claude/README.md`。

闸门是**兜底**而非豁免：未覆盖到的新形态自唤起同样违规，发现后应补充规则而不是视作默许放行。**严禁**为通过闸门而改写形态（如把调度命令拆成变量拼接、把循环藏进脚本文件再执行）。

### 12.7 Code Review 检查清单（本节专项）

涉及自动化 / `.claude/**` 改动的 PR，CR 阶段须核对：

1. **无 AI 自唤起**：未新增周期性 `CronCreate` / `ScheduleWakeup` / `cron_expression` Routine；
2. **无调度器拉起 AI**：脚本中无「调度器 / 循环 + `claude` / `codex` 等 AI CLI」的组合；
3. **无「定时 + AI」工作流**：`.github/workflows/**` 中周期性触发与 AI 调用未同时出现；
4. **闸门未被削弱**：`.claude/settings.json` 与闸门脚本若被修改，须核对是补强而非放宽；放宽须附负责人获批记录；
5. **例外有据**：获批的周期性作业须附报备记录，且已设定到期时间或最大执行次数（§12.5）。

**不得**据本节对项目自身的定时任务、运维 cron、普通 CI 定时构建提出整改要求（§12.4）。

### 12.8 执行口径

本条是**成本层的全局硬红线**，与 §1「严禁使用 Lombok」「全局禁用 emoji」同档次。任何「只是临时盯一下」「跑完就删」的妥协请求一律不予接受——事故中的作业当初也只打算「跑一会儿」。发现历史遗留的自唤起作业，由发现者立即停用并开整改条目，不必等待排期。

## 13. 健康检查统一方案（全产品线唯一口径，强制，违者代码不予合并）

BGSSAI 产品线有 10 个产品仓、20 个可部署后端，外加集中巡检平台 `bgssai-healthcheck` 按接口逐个巡检它们。
在本节立规之前，各仓各自实现了一套健康检查：端点路径出现过 `/actuator/health` / `/api/health` /
`/api/health/live` / `/bgssai/health/live` / `/bgssai/health/liveness` / `/bgssai/admin/health` 六种；
状态枚举出现过 `UP/DEGRADED/DOWN`、`UP/WARN/DOWN`、`UP/DEGRADED/DOWN/DISABLED` 三套；时间字段出现过
`checked_at` / `check_time` / `timestamp`，运行时长出现过 `uptime_seconds` / `uptime_ms`；配置前缀出现过
`app.health.*` / `bgss.health.*` / `bgssai.health.*` / `vpn.health.*` 四种。

后果是具体的：巡检平台无法用一套归一化规则理解各应用的自报状态，部署脚本的 `HEALTH_PATH` 必须逐仓
记忆，运维在 A 仓积累的排障经验换到 B 仓即刻作废。因此本节把健康检查从「各仓自选实现」升格为
**全产品线唯一口径**——与 §1「JSON 强制 snake_case」同档次的跨仓硬红线。

**参考实现（demo）在 `bgssai-skeleton`**：`bgssai-skeleton-{admin,user}/src/main/java/com/bgssai/skeleton/health/**`
是本节的可运行样板，新产品直接复制、既有产品回看对齐。本节与该实现冲突时以本节为准，同时须提 PR 修正实现。

### 13.1 三个端点（路径逐字固定）

每个可部署后端**必须且只能**暴露下列三个端点，路径逐字一致，**不带 `/admin` · `/user` 端前缀**
（探针不区分业务角色，管理端与用户端两端同构）：

| 端点 | 路径 | 用途 |
| --- | --- | --- |
| 存活探针 | `GET /bgssai/health/liveness` | 只证明进程与 HTTP 端口可服务，**不触碰任何依赖** |
| 就绪探针 | `GET /bgssai/health/readiness` | 只检查也只由 **critical 组件**决定结论，供部署脚本判定发布成败 |
| 全量报告 | `GET /bgssai/health` | 全部组件的状态、类型、耗时、说明与白名单指标 |

- **严禁**再引入 `/actuator/health`、`/api/health`、`/api/ping`、`/bgssai/health/live`、
  `/bgssai/health/ready`、`/bgssai/health/detail`、`/bgssai/{admin,user}/health` 等任何别名或旧路径；
  迁移时一次性改名，**不保留兼容别名**——两个路径同时存在就是两份契约，正是本节要消灭的东西。
- **严禁**引入 `spring-boot-starter-actuator` 来实现健康检查。Actuator 的 `/actuator/**` 暴露面、鉴权
  模型与响应结构自成一套，与本节的统一响应封装不兼容；已引入的仓库在迁移中移除该依赖及其配置。

### 13.2 组件模型

健康结论由**组件**聚合而成。每个组件实现同一个 `HealthIndicator` 接口（`name` / `type` / `critical`
/ `check`），由 Spring 整体注入为 List，新增组件就是新增一个类，别处不做任何 if-else 分发（§7）。

**状态枚举 `status`（三值，不得增删改名）**：

| 值 | 含义 |
| --- | --- |
| `UP` | 正常 |
| `DEGRADED` | 可用但劣化，需关注；**永不阻断部署** |
| `DOWN` | 不可用 |

历史上出现过的 `WARN`（应写作 `DEGRADED`）、`DISABLED`（应以组件不注册表达，或写作 `UP` 并在
`message` 说明未启用）**一律禁止**。

**组件类型枚举 `type`（四值，声明顺序即报告排序）**：`DATABASE` → `MIDDLEWARE` → `RUNTIME` → `EXTERNAL`。

**基线组件（每个后端必须具备，名称与 critical 属性逐字固定）**：

| `name` | `type` | `critical` | 说明 |
| --- | --- | --- | --- |
| `db` | `DATABASE` | true | 借连接 + 有界超时校验；不读任何元数据 |
| `mybatis` | `DATABASE` | true | 对本产品核心表跑一次 Example 计数，验证整条持久化链路 |
| `datasource_pool` | `MIDDLEWARE` | false | 连接池饱和度；只报 `UP` / `DEGRADED`，可达性归 `db` |
| `jvm_memory` | `RUNTIME` | false | 堆占比与线程数 |
| `disk_space` | `RUNTIME` | false | 工作目录所在卷剩余空间 |

产品自有依赖（`redis` / `mqtt` / `smtp` / `sms` / `object_storage` / `thread_pool` / `scheduled_job` /
`llm_provider` / `payment_merchant` / `provision` 等）**作为附加组件加入**，命名一律 snake_case，
`critical` 一律为 **false**——只有「没有它应用根本无法提供任何业务能力」的依赖才配 critical，实践中
就是数据库那两个。把可选依赖标成 critical 会让一次第三方抖动触发部署回滚。

### 13.3 响应契约

三个端点走**本仓既有的统一响应封装**，字段 snake_case。各仓封装字段名本就不同
（`{ code, message, success, result }` / `{ code, message, data }` / `{ success, code, message, data }`），
那是仓库级的全局 API 约定，**本节不改动它**——健康检查不该成为唯一一个自带封装的接口。本节固定的是
**健康负载**，即封装里 `result`（或 `data`）承载的那个对象：

- 封装的 `code` / `success` 恒表达「**接口调用**成功」，与健康无关。**健康结论只看健康负载的
  `status` 与 HTTP 状态码**，调用方不得用 `success` / `code` 判健康。
- **HTTP 状态码**：`result.status` 为 `DOWN` 返回 **503**，`UP` / `DEGRADED` 返回 **200**；存活探针恒 200。
  `DEGRADED` 必须留在 200——部署脚本把 5xx 判为不健康并回滚，磁盘偏紧不该被升级成一次失败的发布。
- 三个端点均为 `GET`，无请求参数、无请求体，只读：不写库、不改业务状态、不发起外部网络调用。

健康负载字段集（**逐字固定，各仓不得增删改名**；下文统称 `result`，用 `data` 封装的仓库读作 `data`）：

| 端点 | `result` 字段 |
| --- | --- |
| 存活 | `status` `app` `profile` `uptime_seconds` `checked_at` |
| 就绪 | `status` `app` `checked_at` `components[{ name, status }]` |
| 全量 | `status` `app` `profile` `uptime_seconds` `checked_at` `component_total` `up_count` `degraded_count` `down_count` `components[]` |

全量报告的 `components[]` 元素：`name` `type` `critical` `status` `latency_ms` `message` `metrics`。
`checked_at` 格式 `yyyy-MM-dd HH:mm:ss`；`uptime_seconds` 为秒（**不是**毫秒）；`metrics` 是
`Map<String, String>`，键为 snake_case——这是 §5 明确允许的 Map 情形（强类型 DTO 里一个语义本就是
开放键值袋的具名字段，值类型收窄为 `String`）。

**整体状态聚合规则**：

```text
任一 critical 组件 = DOWN                          -> DOWN
否则 任一组件 = DEGRADED 或 非 critical 组件 = DOWN  -> DEGRADED
否则                                               -> UP
```

就绪判定只看 critical 组件，非 critical 组件既不参与判定、**也不在就绪端点被检查**。

### 13.4 暴露面与安全边界

- 三个端点**公开**：不需要 `Jwttoken`、不做角色校验。调用方是部署脚本、进程守护与集中巡检平台，
  它们无法持有业务令牌。因此响应体等同于公开内容。
- 响应体**只允许**出现 §13.3 列出的字段与各组件白名单指标。以下一律不得出现（与 §6.1.6
  「白名单显式字段」同一口径）：数据库连接串 / 主机名 / IP / 端口 / 数据库账号口令 / 数据库产品与版本 /
  JWT 与各类签名密钥 / 第三方商户凭证 / SSH 与业务账号口令 / 绝对文件路径 / 异常堆栈。
- 组件检查失败时 `message` **只写异常类型名**（如 `SQLException`），检查超时写 `check timed out`。
  **严禁**回显异常原文——JDBC 异常文案常内嵌连接串与主机名，回显等于把 §6.1.6 禁止入日志的内容搬上公网。

### 13.5 可用性与自我保护（本节最容易被做错的部分）

- **单检查硬超时**：每个组件的检查跑在集中声明的 `healthProbeExecutor` 线程池上（§4），带硬截止时间
  （默认 2000ms）。**不得内联在请求线程上执行**：Druid 的 `maxWait` 默认「无限等待」，数据库不可达时
  `getConnection()` 永不返回，反复探测会把 Tomcat 线程一条条钉死，直到连存活探针都不再应答——
  健康检查自己制造了它本该报告的故障。超时的组件记为 `DOWN` + `check timed out`，端点仍正常返回。
- **结果 TTL 缓存**：整批组件结果按 TTL（默认 2000ms）缓存，使一阵密集探测共享一次组件遍历。
  **不得改用限流保护**：429 在部署脚本与 systemd 眼里读作「不健康」，防御本身会变成故障。
  就绪批次与全量批次各自独立缓存。
- **失败隔离**：单个组件抛异常、超时或线程池拒绝，只影响该组件的 `status`，**不得**让整个端点失败或挂起。
- **存活探针零依赖**：存活端点不走探针 runner、不碰数据库、不碰缓存。否则数据库故障会被 systemd
  升级成对一个本来健康的进程的重启风暴。

### 13.6 配置口径

配置前缀全产品线统一为 `bgssai.health.*`（**不得**改成 `app.health.*` / `bgss.health.*` / 产品名前缀）。
阈值是三环境不变量，按 §1 归入 `application.properties` 主配置，不进 profile 文件，且不得使用 `${}` 占位符：

```properties
bgssai.health.cache-ttl-ms=2000
bgssai.health.probe-timeout-ms=2000
bgssai.health.db-validation-timeout-seconds=2
bgssai.health.pool-active-threshold-percent=90
bgssai.health.heap-used-threshold-percent=90
bgssai.health.disk-free-threshold-mb=512
```

产品自有组件的阈值沿用同一前缀往下加键（如 `bgssai.health.mqtt-timeout-ms`）。

### 13.7 部署探针与集中巡检的对接

- **部署脚本**：`deploy/remote-deploy.sh` 与 `deploy/scripts/ship.sh` 的 `HEALTH_PATH` 默认值统一为
  `/bgssai/health/readiness`，`.github/workflows/deploy.yml` 与 Jenkins 参数的默认值同步为它。
  探测口径保持「2xx/3xx/401/403/404 视为健康，5xx 视为不健康 → 回滚上一个可用 jar」——就绪 `DOWN`
  的 503 正好命中回滚分支，而 `DEGRADED` 的 200 不会。
- **集中巡检平台** `bgssai-healthcheck` 一律以 `/bgssai/health/readiness` 作为各应用的巡检地址。
  平台能从响应体解析 `status` 字段并以它为准，因此统一后的三值枚举可被直接归一化，无需按仓写适配。
  平台自身的 `/actuator/health` 是它作为独立监控工具的实现细节，不适用本节（它不是产品线应用后端）。

### 13.8 变更纪律

健康检查属**非功能需求（可观测性）**，按 §0 不强制走原型设计 HTML 一步；但它**有对外契约**，因此
需求基线（`docs/feature`）、契约（`docs/api`）、详设（`docs/architecture`）与代码**必须同版本同提交**
（§0 四层同步）。三份文档的文件名沿用本仓 `docs/` 既有命名风格（骨架用 `health-check.md`，
大写命名的仓库用 `HEALTH-CHECK.md`），本节不统一文件名——统一的是**内容**。
契约变更是**跨 9 个仓的变更**：任何对 §13.1 路径、§13.2 枚举、§13.3 字段集的改动，必须先改本节
（`bgssai-skeleton` 的 Standards 是唯一权威），再由各仓同步，**严禁**单仓先改代码再回头补规范。

### 13.9 Code Review 检查清单（本节专项）

涉及健康检查的 PR，CR 阶段须逐条核对，任一不过即打回：

1. **路径**：三个端点路径与 §13.1 逐字一致，无别名、无端前缀、无残留旧路径；
2. **无 Actuator**：未引入 `spring-boot-starter-actuator`，未出现 `management.*` 健康配置；
3. **枚举**：`status` 仅 `UP` / `DEGRADED` / `DOWN`，`type` 仅四值，无 `WARN` / `DISABLED`；
4. **字段集**：三个端点的健康负载字段与 §13.3 表格逐字一致（含 `uptime_seconds` 为秒、`checked_at` 格式）；外层封装沿用本仓既有约定，未为健康检查另造一套；
5. **HTTP 映射**：仅 `DOWN` → 503，`DEGRADED` 留在 200，存活恒 200；
6. **critical**：只有 `db` / `mybatis` 为 critical，产品自有组件一律非 critical；
7. **自我保护**：检查走 `healthProbeExecutor` 且有硬超时，整批结果有 TTL 缓存，存活端点零依赖；
8. **暴露面**：响应体无连接串 / 主机 / 端口 / 账号口令 / 路径 / 堆栈，`message` 只有异常类型名；
9. **配置前缀**：统一 `bgssai.health.*`，写在主配置，无 `${}` 占位符；
10. **部署对接**：`HEALTH_PATH` 默认值为 `/bgssai/health/readiness`；
11. **四层同步**：需求 / 契约 / 详设 / 代码同提交更新（§13.8）。
