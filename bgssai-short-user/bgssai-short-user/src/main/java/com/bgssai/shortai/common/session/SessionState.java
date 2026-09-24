package com.bgssai.shortai.common.session;

/**
 * 单会话比对结果。三态而非布尔，因为「没有有效会话」与「会话被别人顶掉」要回不同的错误码：
 * 前者沿用本仓既有 token 失效码 2002，后者才是全产品线统一的 2003「账号已在其他设备登录」。
 */
public enum SessionState {

    /** token 的 sid 与库里 current_session_id 一致，放行。 */
    VALID,

    /**
     * 没有可比对的有效会话：token 不带 sid（本特性发布前签发的旧 token）、账号不存在 / 已被逻辑删除、
     * 或已服务端登出（current_session_id 已置 NULL）。这几种情形都没有「别的设备顶掉了你」这回事，
     * 报 2003 与事实不符，一律回 2002。
     */
    EXPIRED,

    /** sid 两侧都在且不相等：本账号在别处重新登录过，当前 token 已被顶下线，回 2003。 */
    SUPERSEDED
}
