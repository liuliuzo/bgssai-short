package com.bgssai.shortai.common.exception;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.ratelimit.RateLimitExceededException;

/**
 * 统一异常出口。返回 {@link ResponseEntity} 并显式 {@code Content-Type: application/json}，
 * 避免 MCP/SSE 客户端携带 {@code Accept: text/event-stream} 时，内容协商失败触发
 * {@code Failure in @ExceptionHandler}。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Unauthorized: HTTP 200 + body code 2002, matching the JwtTokenAspect reply and the
     * documented contract (docs/api/README.md). The envelope carries the failure, not the
     * HTTP status, so the frontend interceptor sees code 2002 wherever it was produced.
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse> handleUnauthorized(UnauthorizedException ex, HttpServletRequest request) {
        log.warn("[GlobalException] unauthorized path={} code={} msg={}",
                request.getRequestURI(), ex.getCode(), ex.getMessage());
        return json(HttpStatus.OK, envelope(ex.getCode(), ex.getMessage()));
    }

    /**
     * Forbidden: HTTP 200 + body code 4003, matching the login-time role rejection and the
     * documented contract (docs/api/README.md).
     */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse> handleForbidden(ForbiddenException ex, HttpServletRequest request) {
        log.warn("[GlobalException] forbidden path={} code={} msg={}",
                request.getRequestURI(), ex.getCode(), ex.getMessage());
        return json(HttpStatus.OK, envelope(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiResponse> handleBizException(BizException ex, HttpServletRequest request) {
        String msg = com.bgssai.shortai.aiprovider.TokenHubClientSupport.userFacingMessage(ex.getMessage());
        // Preserve ordinary business copy when the scrubber would only rewrite hub leaks.
        String raw = ex.getMessage() == null ? "" : ex.getMessage();
        String lower = raw.toLowerCase();
        boolean hubLeak = lower.contains("tokenhub") || lower.contains("token hub")
                || lower.contains("tencentmaas") || lower.contains("api key missing")
                || lower.contains("secret_ref") || lower.contains("bearer ");
        if (!hubLeak && !raw.isBlank()) {
            msg = raw.length() > 240 ? raw.substring(0, 240) : raw;
        }
        log.warn("[GlobalException] biz path={} code={} msg={}",
                request.getRequestURI(), ex.getCode(), msg);
        return json("2003".equals(String.valueOf(ex.getCode())) ? HttpStatus.UNAUTHORIZED : HttpStatus.OK, envelope(ex.getCode(), msg));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors();
        String detail = fieldErrors.stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("[GlobalException] invalid_param(body) path={} detail={}", request.getRequestURI(), detail);
        String message = detail.isEmpty() ? CommonErrorCode.INVALID_PARAM.message() : detail;
        return json(HttpStatus.OK, envelope(CommonErrorCode.INVALID_PARAM.code(), message));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse> handleBindException(BindException ex, HttpServletRequest request) {
        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors();
        String detail = fieldErrors.stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("[GlobalException] invalid_param(bind) path={} detail={}", request.getRequestURI(), detail);
        String message = detail.isEmpty() ? CommonErrorCode.INVALID_PARAM.message() : detail;
        return json(HttpStatus.OK, envelope(CommonErrorCode.INVALID_PARAM.code(), message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse> handleConstraintViolation(ConstraintViolationException ex,
            HttpServletRequest request) {
        String detail = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        log.warn("[GlobalException] invalid_param(constraint) path={} detail={}", request.getRequestURI(), detail);
        String message = detail.isEmpty() ? CommonErrorCode.INVALID_PARAM.message() : detail;
        return json(HttpStatus.OK, envelope(CommonErrorCode.INVALID_PARAM.code(), message));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse> handleMissingParam(MissingServletRequestParameterException ex,
            HttpServletRequest request) {
        String detail = "missing required parameter: " + ex.getParameterName();
        log.warn("[GlobalException] missing_param path={} detail={}", request.getRequestURI(), detail);
        return json(HttpStatus.OK, envelope(CommonErrorCode.INVALID_PARAM.code(), detail));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {
        String detail = "parameter type mismatch: " + ex.getName();
        log.warn("[GlobalException] type_mismatch path={} detail={}", request.getRequestURI(), detail);
        return json(HttpStatus.OK, envelope(CommonErrorCode.INVALID_PARAM.code(), detail));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse> handleNotReadable(HttpMessageNotReadableException ex,
            HttpServletRequest request) {
        log.warn("[GlobalException] body_unreadable path={} msg={}", request.getRequestURI(), ex.getMessage());
        return json(HttpStatus.OK, envelope(CommonErrorCode.INVALID_PARAM.code(), "request body format error"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {
        log.warn("[GlobalException] method_not_supported path={} method={}",
                request.getRequestURI(), request.getMethod());
        return json(HttpStatus.METHOD_NOT_ALLOWED,
                envelope(CommonErrorCode.RESOURCE_NOT_FOUND.code(),
                        "unsupported method: " + request.getMethod()));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex,
            HttpServletRequest request) {
        log.warn("[GlobalException] media_type_unsupported path={} msg={}",
                request.getRequestURI(), ex.getMessage());
        return json(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                envelope(CommonErrorCode.INVALID_PARAM.code(), "unsupported media type"));
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ApiResponse> handleMediaTypeNotAcceptable(HttpMediaTypeNotAcceptableException ex,
            HttpServletRequest request) {
        // MCP/SSE clients often send Accept: text/event-stream; still answer with JSON envelope.
        log.warn("[GlobalException] media_type_unacceptable path={} msg={}",
                request.getRequestURI(), ex.getMessage());
        return json(HttpStatus.NOT_ACCEPTABLE,
                envelope(CommonErrorCode.INVALID_PARAM.code(), "unacceptable media type"));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse> handleNoHandler(NoHandlerFoundException ex, HttpServletRequest request) {
        log.warn("[GlobalException] no_handler path={}", request.getRequestURI());
        return json(HttpStatus.NOT_FOUND,
                envelope(CommonErrorCode.RESOURCE_NOT_FOUND.code(),
                        "resource not found: " + request.getRequestURI()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse> handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
        // Spring Boot 3 / Spring 6: unmapped paths surface as NoResourceFoundException,
        // not NoHandlerFoundException. Without this handler they fall into system 500.
        log.warn("[GlobalException] no_resource path={}", request.getRequestURI());
        return json(HttpStatus.NOT_FOUND,
                envelope(CommonErrorCode.RESOURCE_NOT_FOUND.code(),
                        "resource not found: " + request.getRequestURI()));
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiResponse> handleRateLimit(RateLimitExceededException ex, HttpServletRequest request) {
        log.warn("[GlobalException] rate_limit path={} retryAfterSec={}",
                request.getRequestURI(), ex.getRetryAfterSeconds());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
                .body(envelope(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse> handleMaxUploadSize(MaxUploadSizeExceededException ex,
            HttpServletRequest request) {
        log.warn("[GlobalException] upload_too_large path={} maxBytes={}",
                request.getRequestURI(), ex.getMaxUploadSize());
        return json(HttpStatus.PAYLOAD_TOO_LARGE,
                envelope(CommonErrorCode.REQUEST_TOO_LARGE.code(),
                        CommonErrorCode.REQUEST_TOO_LARGE.message()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse> handleIllegalArgument(IllegalArgumentException ex,
            HttpServletRequest request) {
        log.warn("[GlobalException] invalid_argument path={} msg={}", request.getRequestURI(), ex.getMessage());
        return json(HttpStatus.BAD_REQUEST, envelope(CommonErrorCode.INVALID_PARAM.code(), ex.getMessage()));
    }

    /**
     * 客户端已断开（MCP/SSE 探测、浏览器取消）时不要再写 body，否则会二次触发
     * Failure in @ExceptionHandler。
     */
    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public ResponseEntity<ApiResponse> handleAsyncNotUsable(AsyncRequestNotUsableException ex,
            HttpServletRequest request, HttpServletResponse response) {
        log.debug("[GlobalException] client_gone path={} msg={}", request.getRequestURI(), ex.toString());
        if (!response.isCommitted()) {
            return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).build();
        }
        return null;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleException(Exception ex, HttpServletRequest request) {
        if (isClientAbort(ex)) {
            log.debug("[GlobalException] client_gone path={} msg={}", request.getRequestURI(), ex.toString());
            return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).build();
        }
        log.error("[GlobalException] system path={} method={}",
                request.getRequestURI(), request.getMethod(), ex);
        return json(HttpStatus.INTERNAL_SERVER_ERROR,
                envelope(CommonErrorCode.SYSTEM_ERROR.code(), CommonErrorCode.SYSTEM_ERROR.message()));
    }

    private static boolean isClientAbort(Throwable ex) {
        Throwable cur = ex;
        while (cur != null) {
            String name = cur.getClass().getName();
            if (name.contains("ClientAbortException")
                    || name.contains("AsyncRequestNotUsableException")
                    || name.contains("EofException")) {
                return true;
            }
            String msg = cur.getMessage();
            if (msg != null) {
                String lower = msg.toLowerCase();
                if (lower.contains("broken pipe")
                        || lower.contains("connection reset")
                        || lower.contains("async request not usable")) {
                    return true;
                }
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static ApiResponse envelope(String code, String message) {
        ApiResponse response = new ApiResponse();
        response.setCode(code);
        response.setMessage(message);
        response.setSuccess(false);
        return response;
    }

    private static ResponseEntity<ApiResponse> json(HttpStatus status, ApiResponse body) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(body);
    }
}
