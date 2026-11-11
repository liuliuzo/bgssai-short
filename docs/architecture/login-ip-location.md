# 登录日志 IP 归属地设计

双端 ListLoginLogsService 完成原有权限校验、Example 查询和 PageHelper 分页后，对每行已有 ip_address 调用 LoginIpLocationResolver。DTO 新增 String ipLocation，Jackson 按现有策略输出 ip_location。数据库/Entity/Mapper 不变，查询不回填或修改历史记录。

## 离线数据

复用 bgssai-note 的 org.lionsoul:ip2region:2.7.0 与 IPv4 xdb。仓库 shared-resources/ip 是唯一数据源，双端 Maven resources 将其装入 classpath /ip/ip2region.xdb；无需运行时网络或新增运维配置。来源、SHA256 和许可证见 shared-resources/ip/README.md。Searcher 使用内存数据，按 Note 的策略串行访问。加载缺失阻止启动，打包资源测试覆盖；单次未命中/解析异常返回未知，不记录原始 IP。

IpLiteral 仅接受完整数字 IP 字面量，不查 DNS，不拆转发列表，不接受端口或 zone id。优先分类回环、私网、链路本地、共享/保留/文档地址；公网 IPv4 查询五段旧版数据 Country|Region|Province|City|ISP，取国家、省区、城市，空值 0 过滤且相邻相同名称去重。公网 IPv6 当前库不覆盖，如实提示。结果是当次离线库的参考归属地，不是登录当时的历史精确定位。

## 审计地址边界

现有双端写入盲取 XFF 首项，可能接受客户端伪造的前缀。本次仅把审计取 IP 收敛至 LoginClientIp，不改变登录认证、会话或限流。

当前权威 nginx 模板 bgssai-workflows/jenkins/install/provision-nginx-vhosts.py 使用 proxy_pass 回环和 X-Forwarded-For $proxy_add_x_forwarded_for。2026-11-11 已只读核验 Short dev user/admin 80/443 实际规则符合这一契约。新逻辑仅在 getRemoteAddr 为回环时读取最右端合法 IP；远端直连忽略 XFF/X-Real-IP；最后一项无效或头过长则保留连接地址。多级代理场景显示最后一个可信同机反代看到的直接来源，不猜测更早的客户端地址。prod 实际 nginx 需发布验收按同一契约复核。

测试覆盖离线库公开样本、国家/地区/城市格式、私网与保留地址、IPv6与非法值、伪造头、角色/所有权、分页与 snake_case 响应。前端表格增加横向滚动适应窄屏。
