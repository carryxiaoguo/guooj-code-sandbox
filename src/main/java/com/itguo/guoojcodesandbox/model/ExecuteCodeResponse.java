package com.itguo.guoojcodesandbox.model;

import lombok.Data;

import java.util.List;

@Data
public class ExecuteCodeResponse {
    /**
     * 输出信息
     */
    private List<String> outputList;

    /**
     * 接口信息
     */
    private String message;

    /**
     * 执行状态
     */
    private Integer status;

    /**
     * 判题信息
     */
    private JudgeInfo judgeInfo;
    
    // 手动添加getter和setter方法以确保兼容性
    public List<String> getOutputList() {
        return outputList;
    }
    
    public void setOutputList(List<String> outputList) {
        this.outputList = outputList;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public Integer getStatus() {
        return status;
    }
    
    public void setStatus(Integer status) {
        this.status = status;
    }
    
    public JudgeInfo getJudgeInfo() {
        return judgeInfo;
    }
    
    public void setJudgeInfo(JudgeInfo judgeInfo) {
        this.judgeInfo = judgeInfo;
    }
}
