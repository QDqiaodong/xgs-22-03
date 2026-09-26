package com.example.lightmanager.exception;

import com.example.lightmanager.dto.ResponseDTO;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 并发冲突：返回 409 与最新记录内容 */
    @ExceptionHandler(ConflictException.class)
    public ResponseDTO<Object> handleConflict(ConflictException e) {
        ResponseDTO<Object> response = ResponseDTO.error(409, e.getMessage());
        response.setData(e.getLatestData());
        return response;
    }

    /** 归档并发冲突：返回 409 与最新归档详情（含最新意见、完整历史） */
    @ExceptionHandler(ArchiveConflictException.class)
    public ResponseDTO<Object> handleArchiveConflict(ArchiveConflictException e) {
        ResponseDTO<Object> response = ResponseDTO.error(409, e.getMessage());
        response.setData(e.getLatestData());
        return response;
    }

    /** 归档引用阻断：返回 412 与具体阻断记录，前端直接呈现 */
    @ExceptionHandler(ReferenceBlockException.class)
    public ResponseDTO<Object> handleReferenceBlock(ReferenceBlockException e) {
        ResponseDTO<Object> response = ResponseDTO.error(412, e.getMessage());
        response.setData(e.getBlocks());
        return response;
    }

    /** 业务规则异常 */
    @ExceptionHandler(BusinessException.class)
    public ResponseDTO<Void> handleBusiness(BusinessException e) {
        return ResponseDTO.error(400, e.getMessage());
    }

    /** 参数校验异常 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseDTO<Void> handleValidation(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "参数校验失败";
        return ResponseDTO.error(400, message);
    }

    /** 兜底异常 */
    @ExceptionHandler(Exception.class)
    public ResponseDTO<Void> handleException(Exception e) {
        return ResponseDTO.error(500, e.getMessage() != null ? e.getMessage() : "系统异常");
    }
}
