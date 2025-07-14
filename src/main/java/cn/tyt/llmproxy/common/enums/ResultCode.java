package cn.tyt.llmproxy.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 结果代码枚举
 * <p>
 * 错误码规范：
 * - 2xx: 成功
 * - 4xx: 客户端错误 (HTTP标准状态码)
 * - 5xx: 服务端错误 (HTTP标准状态码)
 * - 400xxx: 参数错误
 * - 401xxx: 认证授权错误
 * - 403xxx: 权限错误
 * - 500xxx: 业务逻辑错误
 * - 600xxx: 模型相关错误
 * - 700xxx: 代理/外部服务错误
 * - 800xxx: 文件相关错误
 * - 900xxx: 基础设施错误 (如数据库、缓存)
 * </p>
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    // ========================= 通用成功 =========================
    SUCCESS(200, "操作成功"),

    // ========================= 客户端错误 (HTTP Status) =========================
    BAD_REQUEST(400, "请求错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不允许"),
    UNSUPPORTED_MEDIA_TYPE(415, "不支持的媒体类型"),
    TOO_MANY_REQUESTS(429, "请求过于频繁，请稍后再试"),

    // ========================= 参数错误 (400xxx) =========================
    PARAM_ERROR(400001, "参数错误"),
    PARAM_MISSING(400002, "缺少必需参数"),
    PARAM_INVALID(400003, "参数格式不正确"),
    PARAM_TYPE_ERROR(400004, "参数类型错误"),
    PARAM_BIND_ERROR(400005, "参数绑定失败"),

    // ========================= 认证授权错误 (401xxx, 403xxx) =========================
    AUTH_ERROR(401001, "认证失败"),
    TOKEN_INVALID(401002, "Token无效"),
    TOKEN_EXPIRED(401003, "Token已过期"),
    LOGIN_REQUIRED(401004, "需要登录"),
    ACCOUNT_LOCKED(401005, "账户已锁定"),
    ACCOUNT_DISABLED(401006, "账户已禁用"),
    PERMISSION_DENIED(403001, "权限不足"),

    // ========================= 业务错误 (500xxx) =========================
    BUSINESS_ERROR(500001, "业务处理失败"),
    DATA_NOT_FOUND(500002, "数据不存在"),
    DATA_ALREADY_EXISTS(500003, "数据已存在"),
    DATA_INTEGRITY_ERROR(500004, "数据完整性错误"),
    OPERATION_FAILED(500005, "操作失败"),
    ILLEGAL_STATE(500006, "非法操作状态"),

    // ========================= 模型相关错误 (600xxx) =========================
    MODEL_NOT_FOUND(600001, "模型不存在"),
    MODEL_OFFLINE(600002, "模型已下线"),
    MODEL_CONFIG_ERROR(600003, "模型配置错误"),
    MODEL_INFERENCE_ERROR(600004, "模型推理失败"),

    // ========================= 代理/外部服务错误 (700xxx) =========================
    PROXY_ERROR(700001, "代理服务错误"),
    LANGCHAIN_ERROR(700002, "Langchain服务错误"),
    MODEL_INVOKE_ERROR(700003, "模型调用失败"),
    UPSTREAM_SERVICE_ERROR(700004, "上游服务错误"),

    // ========================= 文件上传错误 (800xxx) =========================
    UPLOAD_ERROR(800001, "文件上传失败"),
    UPLOAD_SIZE_EXCEEDED(800002, "文件大小超过限制"),
    UPLOAD_TYPE_ERROR(800003, "文件类型不支持"),
    FILE_NOT_FOUND_ON_SERVER(800004, "服务端文件不存在"),

    // ========================= 系统/基础设施错误 (5xx, 9xxxxx) =========================
    INTERNAL_SERVER_ERROR(500, "系统内部错误"),
    SERVICE_UNAVAILABLE(503, "服务不可用"),
    GATEWAY_TIMEOUT(504, "网关超时"),

    DB_ERROR(900001, "数据库操作失败"),
    DB_CONNECTION_ERROR(900002, "数据库连接失败"),
    DB_TIMEOUT(900003, "数据库操作超时"),
    DUPLICATE_KEY_ERROR(900004, "数据键值重复");

    /**
     * 结果代码
     */
    private final int code;

    /**
     * 结果信息
     */
    private final String message;

    /**
     * 根据code查找对应的ResultCode枚举实例
     *
     * @param code 错误码
     * @return 对应的ResultCode实例，如果找不到则返回null
     */
    public static ResultCode findByCode(int code) {
        return Arrays.stream(ResultCode.values())
                .filter(resultCode -> resultCode.getCode()==code)
                .findFirst()
                .orElse(null);
    }
}