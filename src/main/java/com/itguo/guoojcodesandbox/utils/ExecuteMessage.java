package com.itguo.guoojcodesandbox.utils;

import lombok.Data;

@Data
public class ExecuteMessage {

    private Integer exitValue;

    private String message;

    private String errorMessage;

    private Long time;

    private Long memory;
    
    // 手动添加getter和setter方法以确保兼容性
    public Integer getExitValue() {
        return exitValue;
    }
    
    public void setExitValue(Integer exitValue) {
        this.exitValue = exitValue;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public Long getTime() {
        return time;
    }
    
    public void setTime(Long time) {
        this.time = time;
    }
    
    public Long getMemory() {
        return memory;
    }
    
    public void setMemory(Long memory) {
        this.memory = memory;
    }
}
