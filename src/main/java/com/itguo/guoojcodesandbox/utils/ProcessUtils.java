package com.itguo.guoojcodesandbox.utils;

import org.springframework.util.StopWatch;

import java.io.*;
import java.util.concurrent.TimeUnit;

/**
 * 进程工具类
 */
public class ProcessUtils {
    /**
     * 执行进程并获取消息(不带输入)
     */
    public static ExecuteMessage runProcessAndGetMessage(Process process, String opName) {
        return runProcessAndGetMessage(process, opName, null);
    }
    
    /**
     * 执行进程并获取消息(带输入)
     * 支持Scanner从标准输入读取
     */
    public static ExecuteMessage runProcessAndGetMessage(Process process, String opName, String input) {
        ExecuteMessage executeMessage = new ExecuteMessage();
        StopWatch stopWatch = new StopWatch();
        
        try {
            stopWatch.start();
            
            // 创建输出读取线程(必须异步读取,否则缓冲区满了会阻塞)
            StringBuilder output = new StringBuilder();
            StringBuilder errorOutput = new StringBuilder();
            
            Thread outputThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                } catch (IOException e) {
                    System.err.println("读取标准输出失败: " + e.getMessage());
                }
            });
            
            Thread errorThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        errorOutput.append(line).append("\n");
                    }
                } catch (IOException e) {
                    System.err.println("读取错误输出失败: " + e.getMessage());
                }
            });
            
            // 启动读取线程
            outputThread.start();
            errorThread.start();
            
            // 写入标准输入
            if (input != null && !input.trim().isEmpty()) {
                try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()))) {
                    writer.write(input);
                    writer.newLine();
                    writer.flush();
                } catch (IOException e) {
                    System.err.println("写入标准输入失败: " + e.getMessage());
                }
            }
            
            // 等待进程结束(最多10秒)
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            
            if (!finished) {
                process.destroyForcibly();
                executeMessage.setExitValue(-1);
                executeMessage.setErrorMessage("程序执行超时(10秒)");
                stopWatch.stop();
                executeMessage.setTime(stopWatch.getLastTaskTimeMillis());
                return executeMessage;
            }
            
            // 等待读取线程结束
            outputThread.join(1000);
            errorThread.join(1000);
            
            stopWatch.stop();
            
            int exitValue = process.exitValue();
            long executionTime = stopWatch.getLastTaskTimeMillis();
            
            // 确保时间至少为1ms（避免0值）
            if (executionTime == 0) {
                executionTime = 1L;
            }
            
            executeMessage.setExitValue(exitValue);
            executeMessage.setTime(executionTime);
            executeMessage.setMemory(0L); // 不监控内存
            
            if (exitValue == 0) {
                System.out.println(opName + "成功");
                executeMessage.setMessage(output.toString());
                System.out.println(opName + "执行时间: " + executionTime + " ms");
            } else {
                System.out.println(opName + "失败,错误码:" + exitValue);
                executeMessage.setErrorMessage(errorOutput.toString());
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            executeMessage.setExitValue(-1);
            executeMessage.setErrorMessage("执行异常: " + e.getMessage());
        }
        
        return executeMessage;
    }
}
