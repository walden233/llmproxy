package cn.tyt.llmproxy.common.exception;

import cn.tyt.llmproxy.common.domain.Result;
import cn.tyt.llmproxy.common.enums.ResultCode;
import cn.tyt.llmproxy.common.utils.TraceIdUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String GENERIC_ERROR_MESSAGE = "系统开小差了，请稍后重试";

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常: {} - path={} - traceId={}", e.getMessage(), request.getRequestURI(), TraceIdUtil.getTraceId());
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        log.warn("参数异常: {} - path={} - traceId={}", e.getMessage(), request.getRequestURI(), TraceIdUtil.getTraceId());
        return Result.error(ResultCode.PARAM_ERROR.getCode(), e.getMessage());
    }

    /**
     * 处理参数校验异常 - @Valid 注解校验失败
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleValidationException(MethodArgumentNotValidException e, HttpServletRequest request) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        log.warn("参数校验失败: {} - path={} - traceId={}", errorMessage, request.getRequestURI(), TraceIdUtil.getTraceId());
        return Result.error(ResultCode.PARAM_ERROR.getCode(), errorMessage);
    }

    /**
     * 处理绑定异常 - 表单绑定失败
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleBindException(BindException e, HttpServletRequest request) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        log.warn("参数绑定失败: {} - path={} - traceId={}", errorMessage, request.getRequestURI(), TraceIdUtil.getTraceId());
        return Result.error(ResultCode.PARAM_ERROR.getCode(), errorMessage);
    }

    /**
     * 处理约束违反异常 - @Validated 注解校验失败
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleConstraintViolationException(ConstraintViolationException e, HttpServletRequest request) {
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        String errorMessage = violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));

        log.warn("约束校验失败: {} - path={} - traceId={}", errorMessage, request.getRequestURI(), TraceIdUtil.getTraceId());
        return Result.error(ResultCode.PARAM_ERROR.getCode(), errorMessage);
    }

    /**
     * 处理缺少请求参数异常
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleMissingParameterException(MissingServletRequestParameterException e, HttpServletRequest request) {
        String errorMessage = String.format("缺少必需的请求参数: %s", e.getParameterName());
        log.warn("缺少请求参数: {} - path={} - traceId={}", errorMessage, request.getRequestURI(), TraceIdUtil.getTraceId());
        return Result.error(ResultCode.PARAM_ERROR.getCode(), errorMessage);
    }

    /**
     * 处理参数类型不匹配异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleTypeMismatchException(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        String errorMessage = String.format("参数类型不匹配: %s", e.getName());
        log.warn("参数类型不匹配: {} - path={} - traceId={}", errorMessage, request.getRequestURI(), TraceIdUtil.getTraceId());
        return Result.error(ResultCode.PARAM_ERROR.getCode(), errorMessage);
    }

    /**
     * 处理HTTP消息不可读异常 - JSON格式错误
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleHttpMessageNotReadableException(HttpMessageNotReadableException e, HttpServletRequest request) {
        log.warn("HTTP消息不可读: {} - path={} - traceId={}", e.getMessage(), request.getRequestURI(), TraceIdUtil.getTraceId());
        return Result.error(ResultCode.PARAM_ERROR.getCode(), "请求参数格式错误");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public Result<?> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e, HttpServletRequest request) {
        log.warn("媒体类型不支持: {} - path={} - traceId={}", e.getMessage(), request.getRequestURI(), TraceIdUtil.getTraceId());
        return Result.error(ResultCode.UNSUPPORTED_MEDIA_TYPE.getCode(), "请求类型不支持");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public Result<?> handleMaxUploadSize(MaxUploadSizeExceededException e, HttpServletRequest request) {
        log.warn("上传文件过大 - path={} - traceId={}", request.getRequestURI(), TraceIdUtil.getTraceId());
        return Result.error(ResultCode.PARAM_ERROR.getCode(), "上传文件过大");
    }

    /**
     * 处理HTTP请求方法不支持异常
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result<?> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        String errorMessage = String.format("不支持的请求方法: %s", e.getMethod());
        log.warn("请求方法不支持: {} - path={} - traceId={}", errorMessage, request.getRequestURI(), TraceIdUtil.getTraceId());
        return Result.error(ResultCode.METHOD_NOT_ALLOWED.getCode(), errorMessage);
    }

    /**
     * 处理404异常
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<?> handleNoHandlerFoundException(NoHandlerFoundException e, HttpServletRequest request) {
        log.warn("找不到处理器: {} - path={} - traceId={}", e.getMessage(), request.getRequestURI(), TraceIdUtil.getTraceId());
        return Result.error(ResultCode.NOT_FOUND.getCode(), "请求的资源不存在");
    }

    /**
     * 处理SQL完整性约束违反异常 - 数据库约束错误
     */
    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleSQLIntegrityConstraintViolationException(SQLIntegrityConstraintViolationException e, HttpServletRequest request) {
        log.warn("数据库约束违反: {} - path={} - traceId={}", e.getMessage(), request.getRequestURI(), TraceIdUtil.getTraceId());

        String errorMessage = "数据操作失败";
        if (e.getMessage().contains("Duplicate entry")) {
            errorMessage = "数据已存在，请勿重复操作";
        } else if (e.getMessage().contains("foreign key constraint")) {
            errorMessage = "数据关联错误，请检查相关数据";
        }

        return Result.error(ResultCode.DATA_INTEGRITY_ERROR.getCode(), errorMessage);
    }

    @ExceptionHandler({AuthorizationDeniedException.class, AccessDeniedException.class})
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<?> handleAuthorizationDeniedException(Exception e, HttpServletRequest request) {
        log.warn("权限不足: {} - path={} - traceId={}", e.getMessage(), request.getRequestURI(), TraceIdUtil.getTraceId());
        return Result.error(ResultCode.FORBIDDEN.getCode(), "无权限访问");
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<?> handleAuthenticationException(AuthenticationException e, HttpServletRequest request) {
        log.warn("认证失败: {} - path={} - traceId={}", e.getMessage(), request.getRequestURI(), TraceIdUtil.getTraceId());
        return Result.error(ResultCode.UNAUTHORIZED.getCode(), "请登录后再试");
    }

    /**
     * 处理运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        log.error("运行时异常 - path={} - traceId={} - messsge={}", request.getRequestURI(), TraceIdUtil.getTraceId(), e.toString());
        String message = GENERIC_ERROR_MESSAGE;
        if(StringUtils.hasText(e.getMessage()))
            message=e.getMessage();
        return Result.error(ResultCode.INTERNAL_SERVER_ERROR.getCode(), message);
    }

    /**
     * 处理其他所有异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> handleException(Exception e, HttpServletRequest request) {
        log.error("未知异常 - path={} - traceId={} - messsge={}", request.getRequestURI(), TraceIdUtil.getTraceId(), e.toString());
        String message = GENERIC_ERROR_MESSAGE;
        if(StringUtils.hasText(e.getMessage()))
            message=e.getMessage();
        return Result.error(ResultCode.INTERNAL_SERVER_ERROR.getCode(), message);
    }
}
