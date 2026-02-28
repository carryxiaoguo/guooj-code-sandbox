package com.itguo.guoojcodesandbox.utils;

import java.io.*;
import java.util.concurrent.*;

/**
 * 进程执行器 - 支持标准输入
 */
public class ProcessExecutor {
    
    /**
     * 执行进程并获取结果
     * @param command 命令
     * @param input 标准输入内容
     * @param timeoutSeconds 超时时间(秒)
     * @return 执行结果
     */
    public static ExecuteResult execute(String command, String input, int timeoutSeconds) {
        ExecuteResult result = new ExecuteResult();
        long startTime = System.currentTimeMillis();
        
        // 创建内存监控器
        MemoryMonitor memoryMonitor = new MemoryMonitor();
        
        try {
            Process process = Runtime.getRuntime().exec(command);
            
            // 开始监控内存
            memoryMonitor.startMonitoring();
            
            // 使用线程池管理IO线程
            ExecutorService executor = Executors.newFixedThreadPool(3);
            
            // 1. 写入标准输入的任务
            Future<?> inputFuture = executor.submit(() -> {
                if (input != null && !input.isEmpty()) {
                    try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(process.getOutputStream()), true)) {
                        writer.println(input);
                    } catch (Exception e) {
                        System.err.println("写入输入失败: " + e.getMessage());
                    }
                }
            });
            
            // 2. 读取标准输出的任务
            Future<String> outputFuture = executor.submit(() -> {
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (sb.length() > 0) sb.append("\n");
                        sb.append(line);
                    }
                } catch (Exception e) {
                    System.err.println("读取输出失败: " + e.getMessage());
                }
                return sb.toString();
            });
            
            // 3. 读取错误输出的任务
            Future<String> errorFuture = executor.submit(() -> {
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (sb.length() > 0) sb.append("\n");
                        sb.append(line);
                    }
                } catch (Exception e) {
                    System.err.println("读取错误失败: " + e.getMessage());
                }
                return sb.toString();
            });
            
            // 等待进程结束
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            
            // 停止内存监控
            memoryMonitor.stopMonitoring();
            long maxMemory = memoryMonitor.getMaxMemoryUsed();
            
            if (!finished) {
                // 超时,强制终止
                process.destroyForcibly();
                result.setExitCode(-1);
                result.setError("执行超时(" + timeoutSeconds + "秒)");
            } else {
                // 正常结束
                result.setExitCode(process.exitValue());
                
                // 获取输出(最多等待1秒)
                try {
                    result.setOutput(outputFuture.get(1, TimeUnit.SECONDS));
                } catch (TimeoutException e) {
                    result.setOutput("");
                }
                
                try {
                    result.setError(errorFuture.get(1, TimeUnit.SECONDS));
                } catch (TimeoutException e) {
                    result.setError("");
                }
            }
            
            // 设置内存使用
            result.setMemory(maxMemory);
            
            // 关闭线程池
            executor.shutdownNow();
            
        } catch (Exception e) {
            result.setExitCode(-1);
            result.setError("执行异常: " + e.getMessage());
            e.printStackTrace();
        }
        
        result.setTime(System.currentTimeMillis() - startTime);
        return result;
    }
    
    /**
     * 执行结果
     */
    public static class ExecuteResult {
        private int exitCode;
        private String output = "";
        private String error = "";
        private long time;
        private long memory;
        
        public int getExitCode() { return exitCode; }
        public void setExitCode(int exitCode) { this.exitCode = exitCode; }
        
        public String getOutput() { return output; }
        public void setOutput(String output) { this.output = output == null ? "" : output; }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error == null ? "" : error; }
        
        public long getTime() { return time; }
        public void setTime(long time) { this.time = time; }
        
        public long getMemory() { return memory; }
        public void setMemory(long memory) { this.memory = memory; }
        
        public boolean isSuccess() { return exitCode == 0; }
    }
}
