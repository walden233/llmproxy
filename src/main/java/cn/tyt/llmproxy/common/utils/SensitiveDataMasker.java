package cn.tyt.llmproxy.common.utils;

import org.springframework.util.StringUtils;

import java.lang.reflect.Array;
import java.util.*;

/**
 * 敏感信息脱敏工具，避免在日志里输出密钥/大字段.
 */
public final class SensitiveDataMasker {

    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password", "pwd", "token", "secret", "apikey", "apiKey", "accessKey", "authorization"
    );
    private static final int MAX_STRING_LENGTH = 256;

    private SensitiveDataMasker() {
    }

    /**
     * 根据常见字段名和内容特征进行脱敏.
     */
    public static Object mask(Object source) {
        if (source == null) {
            return null;
        }
        if (source instanceof Map<?, ?> map) {
            return maskMap(map);
        }
        if (source instanceof Collection<?> collection) {
            return maskCollection(collection);
        }
        if (source.getClass().isArray()) {
            return maskArray(source);
        }
        if (source instanceof CharSequence sequence) {
            return maskString(sequence.toString(), null);
        }
        return source;
    }

    public static String maskString(String raw, String fieldName) {
        if (!StringUtils.hasText(raw)) {
            return raw;
        }
        String lowerField = fieldName == null ? "" : fieldName.toLowerCase(Locale.ROOT);
        boolean keySensitive = SENSITIVE_KEYS.stream().anyMatch(lowerField::contains);
        boolean valueSensitive = containsSensitiveKeyword(raw);
        if (keySensitive || valueSensitive) {
            return "***FILTERED***";
        }
        if (raw.length() > MAX_STRING_LENGTH) {
            return raw.substring(0, MAX_STRING_LENGTH) + "...";
        }
        return raw;
    }

    private static boolean containsSensitiveKeyword(String raw) {
        String lower = raw.toLowerCase(Locale.ROOT);
        if (lower.startsWith("bearer ") || lower.contains("sk-") || lower.contains("ak-")) {
            return true;
        }
        // 粗略判断 base64，过长的字母数字串默认脱敏
        return raw.length() > MAX_STRING_LENGTH && raw.matches("^[A-Za-z0-9+/=]+$");
    }

    private static Map<String, Object> maskMap(Map<?, ?> source) {
        Map<String, Object> masked = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            String key = String.valueOf(entry.getKey());
            Object value = entry.getValue();
            if (value instanceof CharSequence sequence) {
                masked.put(key, maskString(sequence.toString(), key));
            } else {
                masked.put(key, mask(value));
            }
        }
        return masked;
    }

    private static Collection<Object> maskCollection(Collection<?> source) {
        List<Object> masked = new ArrayList<>(source.size());
        for (Object item : source) {
            masked.add(mask(item));
        }
        return masked;
    }

    private static Object maskArray(Object source) {
        int length = Array.getLength(source);
        List<Object> masked = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            masked.add(mask(Array.get(source, i)));
        }
        return masked;
    }
}
