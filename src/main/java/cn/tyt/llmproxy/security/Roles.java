package cn.tyt.llmproxy.security;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Central place to manage Spring Security authority names.
 */
public final class Roles {

    private Roles() {}

    public static final String ROOT_ADMIN = "ROLE_ROOT_ADMIN";
    public static final String MODEL_ADMIN = "ROLE_MODEL_ADMIN";
    public static final String USER = "ROLE_USER";

    private static final Set<String> ALL;

    static {
        Set<String> set = new HashSet<>();
        set.add(ROOT_ADMIN);
        set.add(MODEL_ADMIN);
        set.add(USER);
        ALL = Collections.unmodifiableSet(set);
    }

    public static boolean contains(String role) {
        return role != null && ALL.contains(role);
    }
}
