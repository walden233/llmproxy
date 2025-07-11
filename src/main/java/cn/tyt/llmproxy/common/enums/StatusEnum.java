package cn.tyt.llmproxy.common.enums;

import lombok.Getter;

@Getter
public enum StatusEnum {
    UNAVAILABLE(0, "不可用/下线"),
    AVAILABLE(1, "可用/上线");

    private final int code;
    private final String description;

    StatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }
}