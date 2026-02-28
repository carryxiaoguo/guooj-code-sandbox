package com.itguo.guoojcodesandbox;

import com.itguo.guoojcodesandbox.model.ExecuteCodeRequest;
import com.itguo.guoojcodesandbox.model.ExecuteCodeResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.Arrays;

/**
 * C语言代码沙箱测试
 */
@SpringBootTest
public class CNativeCodeSandBoxTest {
    
    @Resource
    private CNativeCodeSandBox cNativeCodeSandBox;
    
    @Test
    public void testSimpleCCode() {
        // 简单的C程序：读取两个整数并输出它们的和
        String code = "#include <stdio.h>\n" +
                      "int main() {\n" +
                      "    int a, b;\n" +
                      "    scanf(\"%d %d\", &a, &b);\n" +
                      "    printf(\"%d\", a + b);\n" +
                      "    return 0;\n" +
                      "}";
        
        ExecuteCodeRequest request = new ExecuteCodeRequest();
        request.setCode(code);
        request.setLanguage("c");
        request.setInputList(Arrays.asList("1 2", "3 4", "10 20"));
        
        ExecuteCodeResponse response = cNativeCodeSandBox.executeCode(request);
        
        System.out.println("执行状态: " + response.getStatus());
        System.out.println("输出结果: " + response.getOutputList());
        if (response.getJudgeInfo() != null) {
            System.out.println("执行时间: " + response.getJudgeInfo().getTime() + "ms");
            System.out.println("内存使用: " + response.getJudgeInfo().getMemory() + "KB");
            System.out.println("判题信息: " + response.getJudgeInfo().getMessage());
        }
    }
    
    @Test
    public void testCompileError() {
        // 有编译错误的C程序
        String code = "#include <stdio.h>\n" +
                      "int main() {\n" +
                      "    printf(\"Hello World\"  // 缺少分号和右括号\n" +
                      "    return 0;\n" +
                      "}";
        
        ExecuteCodeRequest request = new ExecuteCodeRequest();
        request.setCode(code);
        request.setLanguage("c");
        request.setInputList(Arrays.asList(""));
        
        ExecuteCodeResponse response = cNativeCodeSandBox.executeCode(request);
        
        System.out.println("编译错误测试:");
        System.out.println("执行状态: " + response.getStatus());
        System.out.println("错误信息: " + response.getMessage());
        if (response.getJudgeInfo() != null) {
            System.out.println("判题信息: " + response.getJudgeInfo().getMessage());
        }
    }
    
    @Test
    public void testRuntimeError() {
        // 有运行时错误的C程序（除零错误）
        String code = "#include <stdio.h>\n" +
                      "int main() {\n" +
                      "    int a = 10;\n" +
                      "    int b = 0;\n" +
                      "    printf(\"%d\", a / b);\n" +
                      "    return 0;\n" +
                      "}";
        
        ExecuteCodeRequest request = new ExecuteCodeRequest();
        request.setCode(code);
        request.setLanguage("c");
        request.setInputList(Arrays.asList(""));
        
        ExecuteCodeResponse response = cNativeCodeSandBox.executeCode(request);
        
        System.out.println("运行时错误测试:");
        System.out.println("执行状态: " + response.getStatus());
        System.out.println("错误信息: " + response.getMessage());
        if (response.getJudgeInfo() != null) {
            System.out.println("判题信息: " + response.getJudgeInfo().getMessage());
        }
    }
}