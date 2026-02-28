package com.itguo.guoojcodesandbox;

import com.itguo.guoojcodesandbox.utils.ExecuteMessage;
import com.itguo.guoojcodesandbox.utils.ProcessUtils;

import java.io.File;
import java.io.FileWriter;

/**
 * 测试进程执行和标准输入
 */
public class ProcessTest {
    public static void main(String[] args) throws Exception {
        // 创建测试代码
        String testCode = "import java.util.Scanner;\n" +
                "\n" +
                "public class Main {\n" +
                "    public static void main(String[] args) {\n" +
                "        Scanner scanner = new Scanner(System.in);\n" +
                "        int a = scanner.nextInt();\n" +
                "        int b = scanner.nextInt();\n" +
                "        System.out.println(a + b);\n" +
                "        scanner.close();\n" +
                "    }\n" +
                "}";
        
        // 写入文件
        File tempDir = new File("tempTest");
        tempDir.mkdirs();
        File javaFile = new File(tempDir, "Main.java");
        try (FileWriter writer = new FileWriter(javaFile)) {
            writer.write(testCode);
        }
        
        // 编译
        System.out.println("编译中...");
        Process compileProcess = Runtime.getRuntime().exec("javac -encoding utf-8 " + javaFile.getAbsolutePath());
        ExecuteMessage compileResult = ProcessUtils.runProcessAndGetMessage(compileProcess, "编译");
        System.out.println("编译结果: " + compileResult.getExitValue());
        
        if (compileResult.getExitValue() != 0) {
            System.out.println("编译失败: " + compileResult.getErrorMessage());
            return;
        }
        
        // 执行
        System.out.println("\n执行中...");
        Process executeProcess = Runtime.getRuntime().exec("java -cp " + tempDir.getAbsolutePath() + " Main");
        ExecuteMessage executeResult = ProcessUtils.runProcessAndGetMessage(executeProcess, "运行", "1 2");
        
        System.out.println("执行结果:");
        System.out.println("  退出码: " + executeResult.getExitValue());
        System.out.println("  输出: " + executeResult.getMessage());
        System.out.println("  错误: " + executeResult.getErrorMessage());
        System.out.println("  耗时: " + executeResult.getTime() + "ms");
        
        // 清理
        javaFile.delete();
        new File(tempDir, "Main.class").delete();
        tempDir.delete();
    }
}
