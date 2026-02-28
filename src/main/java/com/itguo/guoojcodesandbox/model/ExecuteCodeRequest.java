package com.itguo.guoojcodesandbox.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExecuteCodeRequest {
    private List<String> inputList;
    private String code;
    private String language;
    
    // 手动添加getter和setter方法以确保兼容性
    public List<String> getInputList() {
        return inputList;
    }
    
    public void setInputList(List<String> inputList) {
        this.inputList = inputList;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getLanguage() {
        return language;
    }
    
    public void setLanguage(String language) {
        this.language = language;
    }
}
