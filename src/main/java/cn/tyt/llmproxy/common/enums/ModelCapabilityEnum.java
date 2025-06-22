package cn.tyt.llmproxy.common.enums;

import lombok.Getter;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public enum ModelCapabilityEnum {
    TEXT_TO_TEXT("text-to-text", "文生文"),
    TEXT_TO_IMAGE("text-to-image", "文生图"),
    IMAGE_TO_TEXT("image-to-text", "图生文"),
    IMAGE_TO_IMAGE("image-to-image", "图生图");

    private final String value;
    private final String description;

    ModelCapabilityEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public static boolean isValid(String capability) {
        return Arrays.stream(values()).anyMatch(e -> e.getValue().equals(capability));
    }

    public static Set<String> getAllValues() {
        return Arrays.stream(values()).map(ModelCapabilityEnum::getValue).collect(Collectors.toSet());
    }
}