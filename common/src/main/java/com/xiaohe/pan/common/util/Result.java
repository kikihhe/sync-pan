package com.xiaohe.pan.common.util;

import com.xiaohe.pan.common.constants.ResponseCodeConstants;

public class Result<T> {
    private Integer code;
    private String message;
    private T data;

    public Result() {
    }

    public Result(Integer code) {
        this.code = code;
    }


    public Result(Integer code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }



    public static <T> Result<T> result(Integer code, String message, T data) {
        Result<T> result = new Result<>();
        result.code = code;
        result.message = message;
        result.data = data;
        return result;
    }

    public static <T> Result<T> success(String message, T data) {
        return result(ResponseCodeConstants.SUCCESS, message, data);
    }

    public static <T> Result<T> success(String message) {
        return result(ResponseCodeConstants.SUCCESS, message, null);
    }

    public static <T> Result<T> success(T data) {
        return result(ResponseCodeConstants.SUCCESS, "success", data);
    }



    public static <T> Result<T> error(String message, T data) {
        return result(ResponseCodeConstants.INTERNAL_ERROR, message, data);
    }

    public static <T> Result<T> error(String message) {
        return result(ResponseCodeConstants.INTERNAL_ERROR, message, null);
    }

    public static <T> Result<T> error(T data) {
        return result(ResponseCodeConstants.INTERNAL_ERROR, "error", data);
    }
}
