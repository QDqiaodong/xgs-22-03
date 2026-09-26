package com.example.lightmanager.dto;

public class ResponseDTO<T> {
    
    private Integer code;
    
    private String message;
    
    private T data;
    
    private Long total;
    
    public Integer getCode() { return code; }
    public void setCode(Integer code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    public Long getTotal() { return total; }
    public void setTotal(Long total) { this.total = total; }
    
    public static <T> ResponseDTO<T> success(T data) {
        ResponseDTO<T> response = new ResponseDTO<>();
        response.setCode(200);
        response.setMessage("success");
        response.setData(data);
        return response;
    }
    
    public static <T> ResponseDTO<T> success(T data, Long total) {
        ResponseDTO<T> response = success(data);
        response.setTotal(total);
        return response;
    }
    
    public static <T> ResponseDTO<T> error(String message) {
        ResponseDTO<T> response = new ResponseDTO<>();
        response.setCode(500);
        response.setMessage(message);
        return response;
    }
    
    public static <T> ResponseDTO<T> error(Integer code, String message) {
        ResponseDTO<T> response = new ResponseDTO<>();
        response.setCode(code);
        response.setMessage(message);
        return response;
    }
}