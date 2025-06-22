package cn.tyt.llmproxy.common.domain;

import lombok.Data;

@Data
public class Result<T> {
    private int code;
    private String message;
    private T data;

    public Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }
    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功", null);
    }


    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }
    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }
}