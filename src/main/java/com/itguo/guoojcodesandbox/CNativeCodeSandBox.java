package com.itguo.guoojcodesandbox;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.springframework.stereotype.Component;

import com.itguo.guoojcodesandbox.model.ExecuteCodeRequest;
import com.itguo.guoojcodesandbox.model.ExecuteCodeResponse;
import com.itguo.guoojcodesandbox.model.JudgeInfo;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

/**
 * C语言原生代码沙箱实现（使用Judge0 API）
 */
@Component
public class CNativeCodeSandBox implements CodeSandBox {
    
    // Judge0 API配置
    private static final String JUDGE0_API_URL = "https://ce.judge0.com";  // 使用官方免费实例
    private static final int C_LANGUAGE_ID = 50; // C语言在Judge0中的ID
    private static final int MAX_RETRIES = 20; // 最大轮询次数
    private static final int POLL_INTERVAL = 1000; // 轮询间隔(毫秒)
    private static final int REQUEST_TIMEOUT = 30000; // 请求超时时间(毫秒)
    
    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();
        
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        List<String> outputList = new ArrayList<>();
        JudgeInfo judgeInfo = new JudgeInfo();
        
        long maxTime = 0L;
        long maxMemory = 0L;
        
        try {
            System.out.println("开始执行C语言代码，输入用例数量: " + inputList.size());
            
            for (int i = 0; i < inputList.size(); i++) {
                String input = inputList.get(i);
                System.out.println("=== 执行测试用例 " + (i + 1) + " ===");
                System.out.println("输入: " + input);
                // 1. 提交代码到Judge0
                String token = submitCode(code, input);
                if (token == null) {
                    executeCodeResponse.setMessage("提交代码失败");
                    executeCodeResponse.setStatus(3);
                    executeCodeResponse.setOutputList(new ArrayList<>());
                    judgeInfo.setMessage("system error");
                    judgeInfo.setTime(0L);
                    judgeInfo.setMemory(0L);
                    executeCodeResponse.setJudgeInfo(judgeInfo);
                    return executeCodeResponse;
                }
                
                // 2. 轮询获取执行结果
                JSONObject result = getSubmissionResult(token);
                if (result == null) {
                    executeCodeResponse.setMessage("获取执行结果失败");
                    executeCodeResponse.setStatus(3);
                    executeCodeResponse.setOutputList(new ArrayList<>());
                    judgeInfo.setMessage("system error");
                    judgeInfo.setTime(0L);
                    judgeInfo.setMemory(0L);
                    executeCodeResponse.setJudgeInfo(judgeInfo);
                    return executeCodeResponse;
                }
                
                // 3. 解析结果
                Integer statusId = result.getInt("status_id");
                
                // 处理新的响应格式
                if (statusId == null && result.containsKey("status")) {
                    JSONObject status = result.getJSONObject("status");
                    statusId = status.getInt("id");
                }
                
                String stdout = decodeBase64(result.getStr("stdout"));
                String stderr = decodeBase64(result.getStr("stderr"));
                String compileOutput = decodeBase64(result.getStr("compile_output"));
                
                Double time = result.getDouble("time");
                Integer memory = result.getInt("memory");
                
                System.out.println("执行结果: statusId=" + statusId + ", time=" + time + "s, memory=" + memory + "KB");
                System.out.println("stdout: " + stdout);
                System.out.println("stderr: " + stderr);
                System.out.println("compile_output: " + compileOutput);
                
                // 4. 处理不同的执行状态
                if (statusId == 3) { // Accepted
                    String output = stdout != null ? stdout.trim() : "";
                    outputList.add(output);
                    System.out.println("输出: " + output);
                    System.out.println("执行成功");
                    
                    if (time != null) {
                        maxTime = Math.max(maxTime, (long) (time * 1000)); // 转换为毫秒
                    }
                    if (memory != null) {
                        maxMemory = Math.max(maxMemory, memory.longValue());
                    }
                } else if (statusId == 6) { // Compilation Error
                    executeCodeResponse.setMessage("编译错误: " + (compileOutput != null ? compileOutput : "未知编译错误"));
                    executeCodeResponse.setStatus(2);
                    judgeInfo.setMessage("compile error");
                    judgeInfo.setTime(0L);
                    judgeInfo.setMemory(0L);
                    executeCodeResponse.setJudgeInfo(judgeInfo);
                    return executeCodeResponse;
                } else if (statusId == 5) { // Time Limit Exceeded
                    executeCodeResponse.setMessage("超时错误");
                    executeCodeResponse.setStatus(3);
                    executeCodeResponse.setOutputList(new ArrayList<>());
                    judgeInfo.setMessage("time limit exceeded");
                    judgeInfo.setTime(5000L); // 设置为超时时间
                    judgeInfo.setMemory(0L);
                    executeCodeResponse.setJudgeInfo(judgeInfo);
                    return executeCodeResponse;
                } else if (statusId == 4 || statusId == 7 || statusId == 8 || statusId == 9 || statusId == 10 || statusId == 11 || statusId == 12) { 
                    // Wrong Answer (4) 或各种运行时错误 (7-12)
                    String errorMsg = stderr != null ? stderr : (compileOutput != null ? compileOutput : "未知运行时错误");
                    System.out.println("运行时错误: " + errorMsg);
                    executeCodeResponse.setMessage("运行时错误: " + errorMsg);
                    executeCodeResponse.setStatus(3);
                    executeCodeResponse.setOutputList(new ArrayList<>());
                    judgeInfo.setMessage("running error");
                    judgeInfo.setTime(time != null ? (long) (time * 1000) : 0L);
                    judgeInfo.setMemory(memory != null ? memory.longValue() : 0L);
                    executeCodeResponse.setJudgeInfo(judgeInfo);
                    return executeCodeResponse;
                } else {
                    // 其他错误状态
                    String errorMsg = stderr != null ? stderr : (compileOutput != null ? compileOutput : "未知错误");
                    System.out.println("执行错误: " + errorMsg);
                    executeCodeResponse.setMessage("执行错误: " + errorMsg);
                    executeCodeResponse.setStatus(3);
                    executeCodeResponse.setOutputList(new ArrayList<>());
                    judgeInfo.setMessage("system error");
                    judgeInfo.setTime(time != null ? (long) (time * 1000) : 0L);
                    judgeInfo.setMemory(memory != null ? memory.longValue() : 0L);
                    executeCodeResponse.setJudgeInfo(judgeInfo);
                    return executeCodeResponse;
                }
            }
            
            // 5. 设置成功结果
            executeCodeResponse.setOutputList(outputList);
            executeCodeResponse.setStatus(1);
            judgeInfo.setTime(maxTime);
            judgeInfo.setMemory(maxMemory);
            judgeInfo.setMessage("accepted");
            executeCodeResponse.setJudgeInfo(judgeInfo);
            
            System.out.println("C语言代码执行完成: 最大时间=" + maxTime + "ms, 最大内存=" + maxMemory + "KB");
            
        } catch (Exception e) {
            System.err.println("C语言代码执行异常: " + e.getMessage());
            e.printStackTrace();
            executeCodeResponse.setMessage("系统错误: " + e.getMessage());
            executeCodeResponse.setStatus(3);
            executeCodeResponse.setOutputList(new ArrayList<>());
            judgeInfo.setMessage("system error");
            judgeInfo.setTime(0L);
            judgeInfo.setMemory(0L);
            executeCodeResponse.setJudgeInfo(judgeInfo);
        }
        
        return executeCodeResponse;
    }
    
    /**
     * 提交代码到Judge0
     */
    private String submitCode(String sourceCode, String stdin) {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.set("language_id", C_LANGUAGE_ID);
            requestBody.set("source_code", Base64.getEncoder().encodeToString(sourceCode.getBytes()));
            
            if (stdin != null && !stdin.trim().isEmpty()) {
                requestBody.set("stdin", Base64.getEncoder().encodeToString(stdin.getBytes()));
            }
            
            // 设置资源限制
            requestBody.set("cpu_time_limit", 5.0); // 5秒CPU时间限制
            requestBody.set("memory_limit", 128000); // 128MB内存限制
            requestBody.set("max_file_size", 1024); // 1MB文件大小限制
            
            System.out.println("提交C语言代码到Judge0");
            
            HttpResponse response = HttpRequest.post(JUDGE0_API_URL + "/submissions?base64_encoded=true&wait=false")
                    .header("Content-Type", "application/json")
                    .body(requestBody.toString())
                    .timeout(REQUEST_TIMEOUT)
                    .execute();
            
            if (response.getStatus() == 200 || response.getStatus() == 201) {
                JSONObject responseJson = JSONUtil.parseObj(response.body());
                String token = responseJson.getStr("token");
                System.out.println("代码提交成功，token: " + token);
                return token;
            } else {
                System.err.println("提交代码失败，状态码: " + response.getStatus() + ", 响应: " + response.body());
                return null;
            }
        } catch (Exception e) {
            System.err.println("提交代码异常: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 获取提交结果
     */
    private JSONObject getSubmissionResult(String token) {
        try {
            int retryCount = 0;
            
            while (retryCount < MAX_RETRIES) {
                HttpResponse response = HttpRequest.get(JUDGE0_API_URL + "/submissions/" + token + "?base64_encoded=true")
                        .timeout(REQUEST_TIMEOUT)
                        .execute();
                
                if (response.getStatus() == 200) {
                    JSONObject result = JSONUtil.parseObj(response.body());
                    Integer statusId = result.getInt("status_id");
                    
                    // 处理新的响应格式
                    if (statusId == null && result.containsKey("status")) {
                        JSONObject status = result.getJSONObject("status");
                        statusId = status.getInt("id");
                    }
                    
                    // 状态ID说明：
                    // 1: In Queue, 2: Processing, 3: Accepted, 4: Wrong Answer, 5: Time Limit Exceeded, 6: Compilation Error
                    if (statusId != 1 && statusId != 2) {
                        // 执行完成
                        System.out.println("获取执行结果成功，状态: " + statusId);
                        return result;
                    }
                    
                    // 还在处理中，等待后重试
                    Thread.sleep(POLL_INTERVAL);
                    retryCount++;
                } else {
                    System.err.println("获取执行结果失败，状态码: " + response.getStatus() + ", 响应: " + response.body());
                    return null;
                }
            }
            
            System.err.println("获取执行结果超时，已重试" + MAX_RETRIES + "次");
            return null;
        } catch (Exception e) {
            System.err.println("获取执行结果异常: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Base64解码
     */
    private String decodeBase64(String encoded) {
        if (encoded == null || encoded.trim().isEmpty()) {
            return "";
        }
        try {
            return new String(Base64.getDecoder().decode(encoded));
        } catch (Exception e) {
            System.out.println("Base64解码失败: " + encoded);
            return encoded;
        }
    }
}