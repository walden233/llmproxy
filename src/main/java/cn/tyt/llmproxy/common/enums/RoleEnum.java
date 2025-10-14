package cn.tyt.llmproxy.common.enums;
import lombok.Getter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;


@Getter
public enum RoleEnum {
    ROOT_ADMIN("root_admin"),
    MODEL_ADMIN("model_admin"),
    USER("user");

    private final String value;

    RoleEnum(String value) {
        this.value = value;
    }

    // 1. 创建一个静态的、不可变的 Set 来存储所有枚举的名称
    private static final Set<String> ROLES = new HashSet<>(
            Arrays.stream(values())
                    .map(RoleEnum::getValue)
                    .collect(Collectors.toSet())
    );

    // 2. 提供一个公开的静态方法进行检查
    public static boolean contains(String role) {
        if (role == null) {
            return false;
        }
        return ROLES.contains(role);
    }
}
