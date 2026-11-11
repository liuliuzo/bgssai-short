# 离线 IP 数据来源

- 文件：ip2region.xdb，11,065,998 bytes；SHA256 `da54debc16a301adf49e2d96424890750376c367a7f08a5afa249bb0f918e53a`。
- 直接来源：bgssai-note 的 bgssai-note-user/bgssai-note-user/src/main/resources/ip/ip2region.xdb；来源引入提交 `330f563fe80db924b95864cde2b2cbf496af3f67`。本次按字节复用，没有修改或拼接数据。源文件未附数据库生成日期，不宣称实时或最新。
- 上游项目：[lionsoul2014/ip2region](https://github.com/lionsoul2014/ip2region)；作者 chenxin / lionsoul，Copyright (c) 2015 Lionsoul。沿用 Note 已使用的 Maven 包 `org.lionsoul:ip2region:2.7.0`，其发布 POM 声明 Apache License 2.0。本目录保留上游 LICENSE.md 的完整 Apache-2.0 OR MIT 双许可与版权声明（2026-11-11 获取），见 LICENSE-ip2region.txt；本次选择 Apache-2.0 条款。
- 本旧版数据库格式：国家|区域|省区|城市|ISP，仅 IPv4；不要直接替换为新版五段格式。升级需同时验证解析器、许可证、样本和两端打包。
- 仅供归属地参考，不是精确位置。查询完全离线，不向数据提供方发送用户地址。
- 两个后端通过 Maven resources 引用本目录的同一份数据；不要在各模块维护不同副本。
