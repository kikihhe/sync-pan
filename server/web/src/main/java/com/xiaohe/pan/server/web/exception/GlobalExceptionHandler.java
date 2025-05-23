package com.xiaohe.pan.server.web.exception;

import com.xiaohe.pan.common.exceptions.BusinessException;
import com.xiaohe.pan.common.util.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(value = BusinessException.class)
    public Result handlerBusinessException(BusinessException exception) {
        exception.printStackTrace();
        String message = exception.getMessage();
        return Result.error(message);
    }

    @ExceptionHandler(value = Exception.class)
    public Result handlerException(Exception exception) {
        exception.printStackTrace();
        String message = exception.getMessage();
        return Result.error(message);
    }
}
