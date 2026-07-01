package com.tlcsdm.ecovault.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解。
 *
 * <p>
 * 标注在 Controller 方法上，由 {@code OperationLogAspect} 切面拦截并自动记录操作日志。
 * </p>
 *
 * @author unknowIfGuestInDream
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLogRecord {

	/**
	 * 所属模块名称。
	 * @return 模块名称
	 */
	String module() default "";

	/**
	 * 操作描述。
	 * @return 操作描述
	 */
	String operation() default "";

}
