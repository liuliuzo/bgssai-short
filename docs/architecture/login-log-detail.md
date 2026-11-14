# 登录日志明细设计

`login_log` 增加可空列，旧插入仍可只写原来的动作、IP 和 User-Agent。已有库执行 `sql/upgrade/20261114_login_log_context.sql`（可重复，只加列）。新库以 `sql/DDL.sql` 为准。应用不在启动时改表。若明细列尚未加上，写入会退回只保存动作、IP 和 User-Agent。

两端 `LoginAuditService` 在密码、验证码和第三方登录的成功与失败处写一行。用户端渠道默认 `web`，管理端默认 `admin`。原因和方式用固定枚举，标识先脱敏。请求事实来自当前请求里已经有的头和连接，查询串不入库。地区快照调用现有 `LoginIpLocationResolver.lookup`，运营商只用离线库第五段，没有则为空。

成功登录在写入前读取该用户最近 30 条成功记录，只拿已经保存的设备键和城市做对照。会话标识是登录签发的 sid，不是 JWT。过期时间等于签发时间加上现有 JWT 有效期。鉴权通过后最多每 10 分钟回写一次 `session_last_seen_at`。`POST /bgssai/user/logout` 与 `POST /bgssai/admin/logout` 清掉 `current_session_id`，再给这条会话写 `logout_time`。

列表仍走原来的分页和权限。`LoginLogPresenter` 把库里的列映到响应；旧行缺结果时按动作补文案，缺设备时解析已存 User-Agent，缺地区快照时用当前离线库补全并带 `ip_geo_source`。`ip_location` 继续是实时参考归属地。
