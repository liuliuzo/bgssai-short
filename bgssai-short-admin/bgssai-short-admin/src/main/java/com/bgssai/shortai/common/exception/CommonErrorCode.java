package com.bgssai.shortai.common.exception;

/**
 * Single source of truth for response codes, mirrored by the error-code table in
 * docs/api/README.md. Every layer that reports one of these conditions (login services,
 * JwtTokenAspect, GlobalExceptionHandler, role guards) must use these constants rather than a
 * literal, so one condition never ends up with two codes.
 */
public enum CommonErrorCode implements ErrorCode {

    SUCCESS("0", "OK"),
    SYSTEM_ERROR("999999", "系统异常，请稍后重试"),
    BIZ_ERROR("100000", "业务处理失败"),
    INVALID_PARAM("50000001", "请求参数不正确"),
    INVALID_CREDENTIALS("4001", "账号或密码不正确"),
    UNAUTHORIZED("2002", "登录状态已失效，请重新登录"),
    FORBIDDEN("4003", "无权限访问"),
    RESOURCE_NOT_FOUND("404000", "资源不存在"),
    REQUEST_TOO_LARGE("413000", "请求内容过大"),
    RATE_LIMIT_EXCEEDED("429000", "请求过于频繁，请稍后重试"),
    UPSTREAM_ERROR("502000", "上游服务异常"),
    UPSTREAM_TIMEOUT("504000", "上游服务超时"),
    RULE_REJECT("4006", "当前状态不可执行该操作"),
    QUOTA_INSUFFICIENT("4007", "额度不足"),
    VERSION_CONFLICT("4009", "内容已被更新，请刷新后重试"),
    FILM_EXPIRED("4012", "成片已过期，请重新合成后下载"),
    DUPLICATE_SUBMIT("4013", "已提交审核"),
    ABILITY_UNAVAILABLE("4014", "当前无可用模型"),
    MONTHLY_LIMIT("4015", "本月用量已达上限"),
    MEDIA_TOO_LARGE("4017", "素材过大，请使用 LINK_ONLY"),
    UNSUPPORTED_PAY_CHANNEL("60004", "不支持的支付渠道"),
    NOTIFY_VERIFY_FAILED("60005", "支付通知验签失败"),
    PAYMENT_NOT_CONFIGURED("60013", "支付未配置");

    private final String code;

    private final String message;

    CommonErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}
