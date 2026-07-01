package com.tlcsdm.ecovault.common;

import java.io.Serializable;

/**
 * 统一 API 响应封装。
 *
 * @param <T> 响应数据类型
 * @author 梦里不知身是客
 */
public class ApiResponse<T> implements Serializable {

    /** 业务状态码，0 表示成功，非 0 表示失败 */
    private int code;

    /** 提示信息 */
    private String message;

    /** 响应数据 */
    private T data;

    public ApiResponse() {
    }

    public ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 构造成功响应。
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return 成功响应
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, "成功", data);
    }

    /**
     * 构造无数据的成功响应。
     *
     * @param <T> 数据类型
     * @return 成功响应
     */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(0, "成功", null);
    }

    /**
     * 构造失败响应。
     *
     * @param code    业务状态码
     * @param message 错误信息
     * @param <T>     数据类型
     * @return 失败响应
     */
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
