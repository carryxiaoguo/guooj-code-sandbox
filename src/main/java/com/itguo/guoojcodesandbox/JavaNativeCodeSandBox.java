package com.itguo.guoojcodesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.itguo.guoojcodesandbox.constant.CodeConstant;
import com.itguo.guoojcodesandbox.model.ExecuteCodeRequest;
import com.itguo.guoojcodesandbox.model.ExecuteCodeResponse;
import com.itguo.guoojcodesandbox.model.JudgeInfo;
import com.itguo.guoojcodesandbox.utils.ExecuteMessage;
import com.itguo.guoojcodesandbox.utils.ProcessExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Java原生代码沙箱实现（不使用Docker）
 */
@Slf4j
@Component
public class JavaNativeCodeSandBox implements CodeSandBox {

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();
        
        File userCodeFile = null;
        try {
            // 1.把用户代码保存到文件
            userCodeFile = saveCodeFile(code);

            // 2.编译代码
            ExecuteMessage compileResult = compileFile(userCodeFile);
            
            // 3.执行代码,输出结果
            List<ExecuteMessage> executeMessageList = runFile(userCodeFile, inputList);

            // 4.整理输出
            ExecuteCodeResponse executeCodeResponse = OutputResultList(executeMessageList);

            // 5.文件清理
            boolean b = deleteFile(userCodeFile);
            if (!b) {
                log.error("delete file error, userCodeFile: {}", userCodeFile.getAbsolutePath());
            }

            return executeCodeResponse;
        } catch (Exception e) {
            log.error("代码执行异常", e);
            // 确保清理文件
            if (userCodeFile != null) {
                try {
                    deleteFile(userCodeFile);
                } catch (Exception cleanupEx) {
                    log.error("清理文件失败", cleanupEx);
                }
            }
            return getErrorResponse(e);
        }
    }

    /**
     * 1.把用户代码保存到文件
     */
    public File saveCodeFile(String code) {
        String userDir = System.getProperty(CodeConstant.GLOBAL_CODE_DIR_NAME);
        String globalCodePathName = userDir + File.separator + CodeConstant.TEMP_CODE;
        
        // 判断全局代码目录是否存在
        if (!FileUtil.exist(globalCodePathName)) {
            FileUtil.mkdir(globalCodePathName);
        }
        
        // 隔离不同用户的代码
        String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID();
        
        // 写入用户代码
        return FileUtil.writeString(code, userCodeParentPath + File.separator + CodeConstant.MAIN_JAVA, StandardCharsets.UTF_8);
    }

    /**
     * 2.编译代码
     */
    public ExecuteMessage compileFile(File file) {
        String compileCmd = String.format(CodeConstant.JAVAC_CMD, file.getAbsolutePath());
        try {
            // 使用ProcessExecutor编译
            ProcessExecutor.ExecuteResult result = ProcessExecutor.execute(compileCmd, null, 30);
            
            // 转换为ExecuteMessage
            ExecuteMessage executeMessage = new ExecuteMessage();
            executeMessage.setExitValue(result.getExitCode());
            executeMessage.setMessage(result.getOutput());
            executeMessage.setErrorMessage(result.getError());
            executeMessage.setTime(result.getTime());
            
            // 如果编译失败，记录详细的错误信息
            if (result.getExitCode() != 0) {
                log.error("代码编译失败: {}", result.getError());
                log.error("编译的文件路径: {}", file.getAbsolutePath());
                throw new RuntimeException("代码编译失败: " + (StrUtil.isNotBlank(result.getError()) ? result.getError() : "未知编译错误"));
            }
            
            log.info("代码编译成功: {}", file.getAbsolutePath());
            return executeMessage;
        } catch (Exception e) {
            log.error("编译过程发生异常", e);
            throw new RuntimeException("编译过程发生异常: " + e.getMessage(), e);
        }
    }

    /**
     * 3.原生Java实现执行代码
     */
    public List<ExecuteMessage> runFile(File userCodeFile, List<String> inputList) {
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        
        for (String inputArgs : inputList) {
            String executeCmd = String.format("java -Dfile.encoding=UTF-8 -cp %s Main", userCodeParentPath);
            
            try {
                log.info("执行命令: {}", executeCmd);
                log.info("输入数据: {}", inputArgs);
                
                // 使用新的ProcessExecutor
                ProcessExecutor.ExecuteResult result = ProcessExecutor.execute(executeCmd, inputArgs, 10);
                
                // 转换为ExecuteMessage
                ExecuteMessage executeMessage = new ExecuteMessage();
                executeMessage.setExitValue(result.getExitCode());
                executeMessage.setMessage(result.getOutput());
                executeMessage.setErrorMessage(result.getError());
                executeMessage.setTime(result.getTime());
                executeMessage.setMemory(result.getMemory()); // 使用实际的内存监控数据
                
                log.info("执行结果: exitValue={}, output=[{}], error=[{}], time={}ms, memory={}KB", 
                    result.getExitCode(), 
                    result.getOutput(), 
                    result.getError(),
                    result.getTime(),
                    result.getMemory());
                
                executeMessageList.add(executeMessage);
            } catch (Exception e) {
                log.error("执行代码异常", e);
                throw new RuntimeException("执行代码异常: " + e.getMessage(), e);
            }
        }
        
        if (executeMessageList.isEmpty()) {
            throw new RuntimeException("程序执行结果异常");
        }
        return executeMessageList;
    }

    /**
     * 4.整理输出
     */
    public ExecuteCodeResponse OutputResultList(List<ExecuteMessage> executeMessageList) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        JudgeInfo judgeInfo = new JudgeInfo();
        List<String> outList = new ArrayList<>();
        
        // 取最大执行时间和内存
        long maxTime = 0L;
        long maxMemory = 0L;
        boolean hasError = false;
        
        for (ExecuteMessage messageList : executeMessageList) {
            // 先收集时间和内存信息
            Long time = messageList.getTime();
            if (time != null && time != 0) {
                maxTime = Math.max(time, maxTime);
            }
            
            Long memory = messageList.getMemory();
            if (memory != null && memory != 0) {
                maxMemory = Math.max(memory, maxMemory);
            }
            
            // 检查是否有错误
            String errorMessage = messageList.getErrorMessage();
            if (StrUtil.isNotBlank(errorMessage)) {
                executeCodeResponse.setMessage(errorMessage);
                executeCodeResponse.setStatus(3); // 运行错误
                hasError = true;
                break;
            }
            outList.add(messageList.getMessage());
        }
        
        // 执行完成后
        if (!hasError && outList.size() == executeMessageList.size()) {
            executeCodeResponse.setStatus(1); // 成功
        }
        
        executeCodeResponse.setOutputList(outList);
        judgeInfo.setTime(maxTime);
        judgeInfo.setMemory(maxMemory);
        // 设置初始message,后续由判题策略根据测试用例结果修改
        if (!hasError) {
            judgeInfo.setMessage("accepted"); // 代码执行成功,初始设为accepted
        } else {
            judgeInfo.setMessage("running error"); // 运行错误
        }
        executeCodeResponse.setJudgeInfo(judgeInfo);
        
        log.info("整理输出完成: status={}, time={}ms, memory={}KB", 
            executeCodeResponse.getStatus(), maxTime, maxMemory);
        
        if (judgeInfo == null) {
            throw new RuntimeException("判题信息异常");
        }
        if (executeCodeResponse == null) {
            throw new RuntimeException("返回结果异常");
        }
        return executeCodeResponse;
    }

    /**
     * 5.文件删除
     */
    public boolean deleteFile(File userCodeFile) {
        if (userCodeFile.getParentFile() != null) {
            boolean del = FileUtil.del(userCodeFile.getParentFile());
            log.info("删除文件{}: {}", del ? "成功" : "失败", userCodeFile.getParentFile().getAbsolutePath());
            return del;
        }
        return true;
    }

    /**
     * 6.错误处理
     */
    public ExecuteCodeResponse getErrorResponse(Throwable e) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setOutputList(new ArrayList<>());
        
        String errorMessage = e.getMessage();
        // 判断是否是编译错误
        if (errorMessage != null && errorMessage.contains("编译失败")) {
            executeCodeResponse.setMessage("编译错误: " + errorMessage);
            executeCodeResponse.setStatus(2); // 编译错误
            
            JudgeInfo judgeInfo = new JudgeInfo();
            judgeInfo.setMessage("compile error");
            judgeInfo.setTime(0L);
            judgeInfo.setMemory(0L);
            executeCodeResponse.setJudgeInfo(judgeInfo);
        } else {
            executeCodeResponse.setMessage("系统错误: " + errorMessage);
            executeCodeResponse.setStatus(3); // 系统错误
            executeCodeResponse.setJudgeInfo(new JudgeInfo());
        }
        
        return executeCodeResponse;
    }
}
