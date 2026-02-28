package com.itguo.guoojcodesandbox;


import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.itguo.guoojcodesandbox.constant.CodeConstant;
import com.itguo.guoojcodesandbox.model.ExecuteCodeRequest;
import com.itguo.guoojcodesandbox.model.ExecuteCodeResponse;
import com.itguo.guoojcodesandbox.model.JudgeInfo;
import com.itguo.guoojcodesandbox.utils.ExecuteMessage;
import com.itguo.guoojcodesandbox.utils.ProcessUtils;
import org.springframework.util.StopWatch;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class JavaDockerCodeSandBoxOld implements CodeSandBox {
    public static void main(String[] args) {
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        executeCodeRequest.setInputList(Arrays.asList("1 2", "1 4"));
        String code = ResourceUtil.readStr("testCode/simpleComputeArgs/Main.java", StandardCharsets.UTF_8);
        executeCodeRequest.setCode(code);
        executeCodeRequest.setLanguage("Java");
        JavaDockerCodeSandBoxOld javaNativeCodeSandBox = new JavaDockerCodeSandBoxOld();
        ExecuteCodeResponse executeCodeResponse = javaNativeCodeSandBox.executeCode(executeCodeRequest);
        System.out.println(executeCodeResponse);
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();
        String userDir = System.getProperty(CodeConstant.GLOBAL_CODE_DIR_NAME);
        // 利用File.separator解决不同操作系统路径划线的不一致问题
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
        //1.同一个文件夹下不能全是Main.java我们要隔离开来
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
        //创建容器 把文件交给容器
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();
        //拉取镜像 第一次拉取
        String images = CodeConstant.IMAGE;
        if (CodeConstant.FIRST_INIT) {
            PullImageCmd pullImageCmd = dockerClient.pullImageCmd(images);
            //拉取完成后的任务 回调 异步
            PullImageResultCallback pullImageResultCallback = new PullImageResultCallback() {
                @Override
                public void onNext(PullResponseItem item) {
                    super.onNext(item);
                    System.out.println("状态:" + item.getStatus());
                }
            };
            try {
                pullImageCmd
                        .exec(pullImageResultCallback)
                        .awaitCompletion();//阻塞 直到镜像下载完成
            } catch (InterruptedException e) {
                System.out.println("拉取镜像异常");
                throw new RuntimeException(e);
            }
        }
        System.out.println(CodeConstant.DOWNLOAD_OVER);
        //创建容器
        CreateContainerCmd containerCmd = dockerClient.createContainerCmd(images);

        HostConfig hostConfig = new HostConfig();
        //限制内存
        hostConfig.withMemory(100*1000*1000L);
        hostConfig.withMemorySwap(0L);
        //限制CPU
        hostConfig.withCpuCount(1L);
       // hostConfig.withSecurityOpts(Arrays.asList("seccomp =安全管理配置字符串 "));
        //将本地资源映射到容器中
        hostConfig.setBinds(new Bind(userCodeParentPath,new Volume("/app")));
        CreateContainerResponse createContainerResponse = containerCmd
                .withNetworkDisabled(true) //网络资源未关闭 防止用户用你的带宽
                .withHostConfig(hostConfig)
                .withReadonlyRootfs(true) //禁止用户使用root
                .withAttachStdin(true) //docker和你本地的终端进行连接 能够获取输入,在终端输出
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withTty(true)// 创建一个交互终端
                // 设置主进程为tail -f /dev/null，确保容器持续运行
                .withCmd("tail", "-f", "/dev/null")
                .exec();
        System.out.println(createContainerResponse);
        String containerId = createContainerResponse.getId();
        //启动容器
        dockerClient.startContainerCmd(containerId).exec();
        ExecuteMessage executeMessage = new ExecuteMessage();
        //添加状态信息
        final String[] message = {null};
        final String[] errMessage = {null};
        Long time = 0L;
        //获取输出列表
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        //自动执行代码
        //docker exec pedantic_solomon java -cp /app Main 1 3
        for (String inputArgs : inputList) {
            //开始定时4
            StopWatch stopWatch = new StopWatch();

            String[] inputArgsArray = inputArgs.split(" ");
            String[] arrayCmd = ArrayUtil.append(new String[]{"java","-cp","/app","Main","1","3"}, inputArgsArray) ;
            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                    .withCmd(arrayCmd)
                    .withAttachStdin(true) //docker和你本地的终端进行连接 能够获取输入,在终端输出
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .exec();
            System.out.println("创建执行命令" + execCreateCmdResponse);
            String id = execCreateCmdResponse.getId();
            // 如何判断程序是否超时?     我们假设它超时  如果完成就是没超时重写 onComplete方法
            final boolean[] timeout = {true};
            ExecStartResultCallback execStartResultCallback = new ExecStartResultCallback(){
                @Override
                public void onComplete() {
                    //如果完成执行
                    timeout[0] = false;
                    super.onComplete();
                }

                @Override
                public void onNext(Frame frame) {
                    StreamType streamType = frame.getStreamType();
                    if (StreamType.STDERR.equals(streamType)) {
                        errMessage[0] = new String(frame.getPayload());
                        System.out.println("输出错误信息:"+ errMessage[0]);
                    }else{
                        message[0] = new String(frame.getPayload());
                        System.out.println("输出信息:"+ message[0]);
                    }
                    super.onNext(frame);
                }
            };
            final Long[] maxMemory = {0L};
            //获取执行内存
            StatsCmd statsCmd = dockerClient.statsCmd(containerId);
            ResultCallback<Statistics> statisticsResultCallback = statsCmd.exec(new ResultCallback<Statistics>() {
                @Override
                public void onNext(Statistics statistics) {
                    //获取内存占用
                    System.out.println("内存占用"+statistics.getMemoryStats().getUsage());
                    maxMemory[0] = Math.max(statistics.getMemoryStats().getUsage(), maxMemory[0]);
                }

                @Override
                public void close() throws IOException {

                }

                @Override
                public void onStart(Closeable closeable) {

                }


                @Override
                public void onError(Throwable throwable) {

                }

                @Override
                public void onComplete() {

                }
            });
            statsCmd.exec(statisticsResultCallback);
            try {
                stopWatch.start();
                dockerClient.execStartCmd(id)
                        .exec(execStartResultCallback)
                        .awaitCompletion(CodeConstant.TIME_OUT, TimeUnit.MICROSECONDS);
                stopWatch.stop();
                //执行时间
                time = stopWatch.getLastTaskTimeMillis();

            } catch (InterruptedException e) {
                System.out.println("程序执行异常");
                throw new RuntimeException(e);
            }
            //添加状态信息
            executeMessage.setMessage(message[0]);
            executeMessage.setErrorMessage(errMessage[0]);
            executeMessage.setTime(time);
            executeMessage.setMemory(maxMemory[0]);
            executeMessageList.add(executeMessage);
        }

        //4.整理输出
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        List<String> outList = new ArrayList<>(); //输出
        // 这里取得最大值
        long maxTime = 0;
        for (ExecuteMessage messageList : executeMessageList) {
            String errorMessage = messageList.getErrorMessage();//获得错误信息
            if (StrUtil.isNotBlank(errorMessage)) {
                executeCodeResponse.setMessage(errorMessage);//执行中存在信息
                executeCodeResponse.setStatus(3);// 注意注意 这里不是判题 而是代码运行结果出现了错误 不是判题 不是判题 不是判题
                break;
            }
            outList.add(messageList.getMessage());
            time = messageList.getTime();
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
