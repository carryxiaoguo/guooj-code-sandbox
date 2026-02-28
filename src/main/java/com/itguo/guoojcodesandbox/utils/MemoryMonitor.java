package com.itguo.guoojcodesandbox.utils;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 内存监控器
 */
public class MemoryMonitor {
    
    private final AtomicBoolean monitoring = new AtomicBoolean(false);
    private final AtomicLong maxMemoryUsed = new AtomicLong(0);
    private Thread monitorThread;
    
    /**
     * 开始监控内存使用
     */
    public void startMonitoring() {
        if (monitoring.compareAndSet(false, true)) {
            maxMemoryUsed.set(0);
            
            monitorThread = new Thread(() -> {
                MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
                
                while (monitoring.get()) {
                    try {
                        // 获取当前内存使用量（字节）
                        long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
                        // 转换为KB
                        long usedMemoryKB = usedMemory / 1024;
                        
                        // 更新最大内存使用量
                        long currentMax = maxMemoryUsed.get();
                        if (usedMemoryKB > currentMax) {
                            maxMemoryUsed.compareAndSet(currentMax, usedMemoryKB);
                        }
                        
                        // 每50ms检查一次
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        // 忽略监控异常
                    }
                }
            });
            
            monitorThread.setDaemon(true);
            monitorThread.start();
        }
    }
    
    /**
     * 停止监控
     */
    public void stopMonitoring() {
        monitoring.set(false);
        if (monitorThread != null) {
            monitorThread.interrupt();
        }
    }
    
    /**
     * 获取最大内存使用量（KB）
     */
    public long getMaxMemoryUsed() {
        return maxMemoryUsed.get();
    }
}