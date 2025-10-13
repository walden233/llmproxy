package cn.tyt.llmproxy.common.enums;
import lombok.Getter;


@Getter
public enum RoleEnum {
    ROOT_ADMIN("root_admin"),
    MODEL_ADMIN("model_admin"),
    USER("user");

    private final String value;

    RoleEnum(String value) {
        this.value = value;
    }
}
