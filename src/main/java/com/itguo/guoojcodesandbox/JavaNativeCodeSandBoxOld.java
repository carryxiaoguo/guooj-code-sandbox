package com.itguo.guoojcodesandbox;
/*

  JAVA原生实现

 */

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import com.itguo.guoojcodesandbox.constant.CodeConstant;
import com.itguo.guoojcodesandbox.model.ExecuteCodeRequest;
import com.itguo.guoojcodesandbox.model.ExecuteCodeResponse;
import com.itguo.guoojcodesandbox.model.JudgeInfo;
import com.itguo.guoojcodesandbox.utils.ExecuteMessage;
import com.itguo.guoojcodesandbox.utils.ProcessUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class JavaNativeCodeSandBoxOld implements CodeSandBox {
    public static void main(String[] args) {
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        executeCodeRequest.setInputList(Arrays.asList("1 2", "1 4"));
        String code = ResourceUtil.readStr("testCode/simpleComputeArgs/Main.java", StandardCharsets.UTF_8);
        executeCodeRequest.setCode(code);
        executeCodeRequest.setLanguage("Java");
        JavaNativeCodeSandBoxOld javaNativeCodeSandBox = new JavaNativeCodeSandBoxOld();
        ExecuteCodeResponse executeCodeResponse = javaNativeCodeSandBox.executeCode(executeCodeRequest);
        System.out.println(executeCodeResponse);
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();
        String userDir = System.getProperty(CodeConstant.GLOBAL_CODE_DIR_NAME);
        //利用File.separator解决不同操作系统路径划线的不一致问题
        /*
         * The system-dependent default name-separator character.
         * This field is initialized to contain the first character of
         * the value of the system property file. separator.
         * On UNIX systems the value of this field is '/'; on Microsoft Windows systems it is '\\'.*/
        String globalCodePathName = userDir + File.separator + CodeConstant.TEMP_CODE;
        //判断全局代码目录是否存在  注意:第一次肯定没有
        if (FileUtil.exist(globalCodePathName)) {
            FileUtil.mkdir(globalCodePathName);
        }
        //同一个文件夹下不能全是Main.java我们要隔离开来
        String userCodeParentPath = globalCodePathName + File.separatorChar + UUID.randomUUID();
        //传入用户code
        File file = FileUtil.writeString(code, userCodeParentPath + File.separator + CodeConstant.MAIN_JAVA, StandardCharsets.UTF_8);
        //2:编译代码
        String compileCmd = String.format("javac -encoding utf-8 %s", file.getAbsolutePath());
        try {
            Process compileProcess = Runtime.getRuntime().exec(compileCmd);
        /*    //退出码
            int exit = compileProcess.waitFor();
            if (exit == 0) {
                System.out.println("编译成功");
                //分批获取正常进程输出
                InputStreamReader inputStreamReader = new InputStreamReader(compileProcess.getInputStream());
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                StringBuffer successStringBuffer = new StringBuffer();
                //逐行读取
                String compileOutputLine;
                while ((compileOutputLine = bufferedReader.readLine()) != null){
                    successStringBuffer.append(compileOutputLine);
                }
                System.out.println(compileOutputLine);
            } else {
                System.out.println("编译失败,错误编译码:" + exit);
                // 分批获取error进程输出
                InputStreamReader inputStreamReader = new InputStreamReader(compileProcess.getErrorStream());
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                StringBuffer errorStringBuffer = new StringBuffer();
                // 逐行读取
                String compileerrorLine;
                while ((compileerrorLine = bufferedReader.readLine()) != null){
                    errorStringBuffer.append(compileerrorLine);
                }
                System.out.println(compileerrorLine);
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        } */  //已封装为ProcessUtils//
            ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(compileProcess, "编译");
            System.out.println(executeMessage);
        } catch (Exception e) {
            return getErrorResponse(e);
        }
        //3. 执行代码得到输出结果
        List<ExecuteMessage> executeMessageList = new ArrayList<>();//获取输出列表
        for (String stringListArgs : inputList) {
            String executeCmd = String.format("java -Dfile.encoding=UTF-8 -cp %s Main %s", userCodeParentPath, stringListArgs);
            try {
                Process executeProcess = Runtime.getRuntime().exec(executeCmd);
                ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(executeProcess, "运行");
                System.out.println(executeMessage);
                executeMessageList.add(executeMessage);
            } catch (Exception e) {
                return getErrorResponse(e);
            }
        }
        //4.整理输出
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        List<String> outList = new ArrayList<>(); //输出
        //TODO 这里取得最大值
        long maxTime = 0;
        for (ExecuteMessage executeMessage : executeMessageList) {
            String errorMessage = executeMessage.getErrorMessage();//获得错误信息
            if (StrUtil.isNotBlank(errorMessage)) {
                executeCodeResponse.setMessage(errorMessage);//执行中存在信息
                executeCodeResponse.setStatus(3);// 注意注意 这里不是判题 而是代码运行结果出现了错误 不是判题 不是判题 不是判题
                break;
            }
            outList.add(executeMessage.getMessage());
            Long time = executeMessage.getTime();
            if (time != null) {
                maxTime = Math.max(time, maxTime);
            }
        }
        //执行完成后
        if (outList.size() == executeMessageList.size()) {
            executeCodeResponse.setStatus(1);
        }

        executeCodeResponse.setOutputList(outList);
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setTime(maxTime);
        executeCodeResponse.setJudgeInfo(judgeInfo);
        //5.文件清理 防止服务器攻坚不足
        if (file.getParentFile() != null) {
            boolean del = FileUtil.del(file);
            System.out.println("删除" + (del ? "成功" : "失败"));
        }

        return executeCodeResponse;
    }
        //6:错误处理 提高程序健壮性
    /**
     * 异常处理
     *
     * @param e
     * @return
     */
    private ExecuteCodeResponse getErrorResponse(Throwable e) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setOutputList(new ArrayList<>());
        executeCodeResponse.setMessage(e.getMessage());
        executeCodeResponse.setStatus(2);//编译就出错
        executeCodeResponse.setJudgeInfo(new JudgeInfo());
        return executeCodeResponse;

    }
}
