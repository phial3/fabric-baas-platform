package com.anhui.fabricbaasweb.advice;

import com.anhui.fabricbaasweb.bean.CommonResponse;
import com.anhui.fabricbaasweb.bean.ResultCode;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CommonExceptionAdvice {
    @SuppressWarnings({"unchecked", "rawtypes"})
    @ExceptionHandler(Throwable.class)
    public CommonResponse response(Throwable t) {
        // 处理所有Service抛出的异常
        // 可用instanceof来对异常类型进行判断
        // t.printStackTrace();
        return new CommonResponse(ResultCode.ERROR.getValue(), t.getMessage(), null);
    }
}
