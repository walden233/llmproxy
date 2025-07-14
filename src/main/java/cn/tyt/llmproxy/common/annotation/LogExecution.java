package cn.tyt.llmproxy.common.annotation;

import java.lang.annotation.*;

/**
 * 日志执行注解
 * 用于标记需要记录详细执行日志的方法
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogExecution {

    /**
     * 日志描述
     */
    String value() default "";

    /**
     * 是否记录方法参数
     */
    boolean logArgs() default true;

    /**
     * 是否记录返回值
     */
    boolean logResult() default true;

    /**
     * 日志级别
     */
    LogLevel level() default LogLevel.INFO;

    /**
     * 日志级别枚举
     */
    enum LogLevel {
        DEBUG, INFO, WARN, ERROR
    }
}