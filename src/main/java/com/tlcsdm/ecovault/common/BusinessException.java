package com.tlcsdm.ecovault.common;

/**
 * 业务异常。
 *
 * <p>用于在业务逻辑中主动抛出可预期的错误，由全局异常处理器统一转换为友好的响应。</p>
 *
 * @author unknowIfGuestInDream
 */
public class BusinessException extends RuntimeException {

    /** 业务状态码 */
    private final int code;

    public BusinessException(String message) {
        this(400, message);
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
