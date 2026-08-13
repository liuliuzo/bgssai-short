package com.bgssai.shortdrama.common.exception;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import com.bgssai.shortdrama.common.ApiResponse;
import com.bgssai.shortdrama.common.ratelimit.RateLimitExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Unauthorized: HTTP 200 + body code 2002, matching the JwtTokenAspect reply and the
     * documented contract (docs/api/README.md). The envelope carries the failure, not the
     * HTTP status, so the frontend interceptor sees code 2002 wherever it was produced.
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ApiResponse handleUnauthorized(UnauthorizedException ex, HttpServletRequest request) {
        log.warn("[GlobalException] unauthorized path={} code={} msg={}",
                request.getRequestURI(), ex.getCode(), ex.getMessage());
        ApiResponse response = new ApiResponse();
        response.setCode(ex.getCode());
        response.setMessage(ex.getMessage());
        response.setSuccess(false);
        return response;
    }

    /**
     * Forbidden: HTTP 200 + body code 4003, matching the login-time role rejection and the
     * documented contract (docs/api/README.md).
     */
    @ExceptionHandler(ForbiddenException.class)
    public ApiResponse handleForbidden(ForbiddenException ex, HttpServletRequest request) {
        log.warn("[GlobalException] forbidden path={} code={} msg={}",
                request.getRequestURI(), ex.getCode(), ex.getMessage());
        ApiResponse response = new ApiResponse();
        response.setCode(ex.getCode());
        response.setMessage(ex.getMessage());
        response.setSuccess(false);
        return response;
    }

    @ExceptionHandler(BizException.class)
    public ApiResponse handleBizException(BizException ex, HttpServletRequest request) {
        log.warn("[GlobalException] biz path={} code={} msg={}",
                request.getRequestURI(), ex.getCode(), ex.getMessage());
        ApiResponse response = new ApiResponse();
        response.setCode(ex.getCode());
        response.setMessage(ex.getMessage());
        response.setSuccess(false);
        return response;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors();
        String detail = fieldErrors.stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("[GlobalException] invalid_param(body) path={} detail={}", request.getRequestURI(), detail);
        ApiResponse response = new ApiResponse();
        response.setCode(CommonErrorCode.INVALID_PARAM.code());
        response.setMessage(detail.isEmpty() ? CommonErrorCode.INVALID_PARAM.message() : detail);
        response.setSuccess(false);
        return response;
    }

    @ExceptionHandler(BindException.class)
    public ApiResponse handleBindException(BindException ex, HttpServletRequest request) {
        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors();
        String detail = fieldErrors.stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("[GlobalException] invalid_param(bind) path={} detail={}", request.getRequestURI(), detail);
        ApiResponse response = new ApiResponse();
        response.setCode(CommonErrorCode.INVALID_PARAM.code());
        response.setMessage(detail.isEmpty() ? CommonErrorCode.INVALID_PARAM.message() : detail);
        response.setSuccess(false);
        return response;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResponse handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        String detail = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        log.warn("[GlobalException] invalid_param(constraint) path={} detail={}", request.getRequestURI(), detail);
        ApiResponse response = new ApiResponse();
        response.setCode(CommonErrorCode.INVALID_PARAM.code());
        response.setMessage(detail.isEmpty() ? CommonErrorCode.INVALID_PARAM.message() : detail);
        response.setSuccess(false);
        return response;
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ApiResponse handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest request) {
        String detail = "missing required parameter: " + ex.getParameterName();
        log.warn("[GlobalException] missing_param path={} detail={}", request.getRequestURI(), detail);
        ApiResponse response = new ApiResponse();
        response.setCode(CommonErrorCode.INVALID_PARAM.code());
        response.setMessage(detail);
        response.setSuccess(false);
        return response;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ApiResponse handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String detail = "parameter type mismatch: " + ex.getName();
        log.warn("[GlobalException] type_mismatch path={} detail={}", request.getRequestURI(), detail);
        ApiResponse response = new ApiResponse();
        response.setCode(CommonErrorCode.INVALID_PARAM.code());
        response.setMessage(detail);
        response.setSuccess(false);
        return response;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResponse handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("[GlobalException] body_unreadable path={} msg={}", request.getRequestURI(), ex.getMessage());
        ApiResponse response = new ApiResponse();
        response.setCode(CommonErrorCode.INVALID_PARAM.code());
        response.setMessage("request body format error");
        response.setSuccess(false);
        return response;
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ApiResponse handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        log.warn("[GlobalException] method_not_supported path={} method={}",
                request.getRequestURI(), request.getMethod());
        ApiResponse response = new ApiResponse();
        response.setCode(CommonErrorCode.RESOURCE_NOT_FOUND.code());
        response.setMessage("unsupported method: " + request.getMethod());
        response.setSuccess(false);
        return response;
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse handleNoHandler(NoHandlerFoundException ex, HttpServletRequest request) {
        log.warn("[GlobalException] no_handler path={}", request.getRequestURI());
        ApiResponse response = new ApiResponse();
        response.setCode(CommonErrorCode.RESOURCE_NOT_FOUND.code());
        response.setMessage("resource not found: " + request.getRequestURI());
        response.setSuccess(false);
        return response;
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ApiResponse handleRateLimit(RateLimitExceededException ex, HttpServletRequest request,
                                    HttpServletResponse httpResponse) {
        log.warn("[GlobalException] rate_limit path={} retryAfterSec={}",
                request.getRequestURI(), ex.getRetryAfterSeconds());
        httpResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        httpResponse.setHeader("Retry-After", String.valueOf(ex.getRetryAfterSeconds()));
        ApiResponse response = new ApiResponse();
        response.setCode(ex.getCode());
        response.setMessage(ex.getMessage());
        response.setSuccess(false);
        return response;
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public ApiResponse handleMaxUploadSize(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        log.warn("[GlobalException] upload_too_large path={} maxBytes={}",
                request.getRequestURI(), ex.getMaxUploadSize());
        ApiResponse response = new ApiResponse();
        response.setCode(CommonErrorCode.REQUEST_TOO_LARGE.code());
        response.setMessage(CommonErrorCode.REQUEST_TOO_LARGE.message());
        response.setSuccess(false);
        return response;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("[GlobalException] invalid_argument path={} msg={}", request.getRequestURI(), ex.getMessage());
        ApiResponse response = new ApiResponse();
        response.setCode(CommonErrorCode.INVALID_PARAM.code());
        response.setMessage(ex.getMessage());
        response.setSuccess(false);
        return response;
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse handleException(Exception ex, HttpServletRequest request) {
        log.error("[GlobalException] system path={} method={}",
                request.getRequestURI(), request.getMethod(), ex);
        ApiResponse response = new ApiResponse();
        response.setCode(CommonErrorCode.SYSTEM_ERROR.code());
        response.setMessage(CommonErrorCode.SYSTEM_ERROR.message());
        response.setSuccess(false);
        return response;
    }
}
