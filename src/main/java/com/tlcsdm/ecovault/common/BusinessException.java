package com.tlcsdm.ecovault.common;

/**
 * 业务异常。
 *
 * <p>
 * 用于在业务逻辑中主动抛出可预期的错误，由全局异常处理器统一转换为友好的响应。
 * </p>
 *
 * @author unknowIfGuestInDream
 * @since 1.0.0
 */
public class BusinessException extends RuntimeException {

	/** 业务状态码 */
	private final int code;

	/**
	 * 构造BusinessException实例并注入所需依赖。
	 * @param message message参数。
	 */
	public BusinessException(String message) {
		this(400, message);
	}

	/**
	 * 构造BusinessException实例并注入所需依赖。
	 * @param code code参数。
	 * @param message message参数。
	 */
	public BusinessException(int code, String message) {
		super(message);
		this.code = code;
	}

	/**
	 * 获取相关属性值。
	 * @return 方法执行结果。
	 */
	public int getCode() {
		return code;
	}

}
