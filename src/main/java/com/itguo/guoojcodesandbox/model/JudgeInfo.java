package com.itguo.guoojcodesandbox.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JudgeInfo {

    /**
     * 程序执行信息
     */
    private String message;
    /**
     * 消耗的内存
     */
    private Long memory;
    /**
     * 执行的时间
     */
    private Long time;
    
    // 手动添加getter和setter方法以确保兼容性
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public Long getMemory() {
        return memory;
    }
    
    public void setMemory(Long memory) {
        this.memory = memory;
    }
    
    public Long getTime() {
        return time;
    }
    
    public void setTime(Long time) {
        this.time = time;
    }
}
