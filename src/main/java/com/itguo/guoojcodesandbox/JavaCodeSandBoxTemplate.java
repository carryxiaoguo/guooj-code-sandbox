package com.itguo.guoojcodesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.itguo.guoojcodesandbox.constant.CodeConstant;
import com.itguo.guoojcodesandbox.model.ExecuteCodeRequest;
import com.itguo.guoojcodesandbox.model.ExecuteCodeResponse;
import com.itguo.guoojcodesandbox.model.JudgeInfo;
import com.itguo.guoojcodesandbox.utils.ExecuteMessage;
import com.itguo.guoojcodesandbox.utils.ProcessUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 模板模式 - 原生Java实现
 */

@Slf4j
public class JavaCodeSandBoxTemplate implements CodeSandBox {
    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();
        
        File userCodeFile = null;
        try {
            //1.把用户代码保存到文件
            userCodeFile = saveCodeFile(code);

            //2:编译代码
            ExecuteMessage compileResult = compileFile(userCodeFile);
            
            //3.执行代码,输出结果
            List<ExecuteMessage> executeMessageList = runFile(userCodeFile, inputList);

            //4.整理输出
            ExecuteCodeResponse executeCodeResponse = OutputResultList(executeMessageList);

            //5.文件清理 防止服务器空间不足
            boolean b = deleteFile(userCodeFile);
            if (!b) {
                log.error("delete file error ,userCodeFile:{}", userCodeFile.getAbsolutePath());
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
     *
     * @param code
     * @return
     */
    public File saveCodeFile(String code) {
        String userDir = System.getProperty(CodeConstant.GLOBAL_CODE_DIR_NAME);
        // 利用File.separator解决不同操作系统路径划线的不一致问题
        String globalCodePathName = userDir + File.separator + CodeConstant.TEMP_CODE;
        //判断全局代码目录是否存在  注意:第一次肯定没有
        if (FileUtil.exist(globalCodePathName)) {
            FileUtil.mkdir(globalCodePathName);
        }
        //1.同一个文件夹下不能全是Main.java我们要隔离开来
        String userCodeParentPath = globalCodePathName + File.separatorChar + UUID.randomUUID();
        //传入用户code
        return FileUtil.writeString(code, userCodeParentPath + File.separator + CodeConstant.MAIN_JAVA, StandardCharsets.UTF_8);
    }

    /**
     * 2.编译代码
     *
     * @param file
     * @return
     */
    public ExecuteMessage compileFile(File file) {
        //"javac -encoding utf-8 %s"
        String compileCmd = String.format(CodeConstant.JAVAC_CMD, file.getAbsolutePath());
        try {
            Process compileProcess = Runtime.getRuntime().exec(compileCmd);
            ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(compileProcess, "编译");
            
            // 如果编译失败，记录详细的错误信息
            if (executeMessage.getExitValue() != 0) {
                String errorMsg = executeMessage.getErrorMessage();
                log.error("代码编译失败: {}", errorMsg);
                log.error("编译的文件路径: {}", file.getAbsolutePath());
                throw new RuntimeException("代码编译失败: " + (StrUtil.isNotBlank(errorMsg) ? errorMsg : "未知编译错误"));
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
     * @param userCodeFile
     * @param inputList
     * @return
     */
    public List<ExecuteMessage> runFile(File userCodeFile, List<String> inputList) {
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        
        for (String inputArgs : inputList) {
            String executeCmd = String.format("java -Dfile.encoding=UTF-8 -cp %s Main %s", userCodeParentPath, inputArgs);
            try {
                log.info("执行命令: {}", executeCmd);
                Process executeProcess = Runtime.getRuntime().exec(executeCmd);
                ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(executeProcess, "运行");
                log.info("执行结果: {}", executeMessage);
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
     *  4.整理输出
     * @param executeMessageList
     * @return
     */
    public ExecuteCodeResponse OutputResultList(List<ExecuteMessage> executeMessageList){
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        JudgeInfo judgeInfo = new JudgeInfo();
        List<String> outList = new ArrayList<>(); //输出
        // 这里取得最大值
        long maxTime = 0L;
        boolean hasError = false;
        
        for (ExecuteMessage messageList : executeMessageList) {
            String errorMessage = messageList.getErrorMessage();//获得错误信息
            if (StrUtil.isNotBlank(errorMessage)) {
                executeCodeResponse.setMessage(errorMessage);
                executeCodeResponse.setStatus(3);// 注意注意 这里不是判题 而是代码运行结果出现了错误 不是判题 不是判题 不是判题
                hasError = true;
                break;
            }
            outList.add(messageList.getMessage());
            Long time = messageList.getTime();
            if (time != 0) {
                maxTime = Math.max(time, maxTime);
            }
            //Docker获取内存占用
            judgeInfo.setMemory(messageList.getMemory());
        }
        //执行完成后
        if (outList.size() == executeMessageList.size()) {
            executeCodeResponse.setStatus(1);
        }
        executeCodeResponse.setOutputList(outList);

        judgeInfo.setTime(maxTime);
        // 设置初始message,后续由判题策略根据测试用例结果修改
        if (!hasError) {
            judgeInfo.setMessage("accepted"); // 代码执行成功,初始设为accepted
        } else {
            judgeInfo.setMessage("running error"); // 运行错误
        }
        executeCodeResponse.setJudgeInfo(judgeInfo);
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
     * @param userCodeFile
     * @return
     */
    public boolean deleteFile(File userCodeFile){
        boolean del = FileUtil.del(userCodeFile);
        if (userCodeFile.getParentFile() != null) {
            System.out.println("删除" + (del ? "成功" : "失败"));
        }
        return del;
    }
    /**
     * 6:错误处理 提高程序健壮性 异常处理
     *
     * @param e
     * @return
     */
    public ExecuteCodeResponse getErrorResponse(Throwable e) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setOutputList(new ArrayList<>());
        
        String errorMessage = e.getMessage();
        // 判断是否是编译错误
        if (errorMessage != null && errorMessage.contains("编译失败")) {
            executeCodeResponse.setMessage("编译错误: " + errorMessage);
            executeCodeResponse.setStatus(2); // 编译错误
            
            // 创建判题信息，标记为编译错误
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
