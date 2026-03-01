package com.itguo.guoojcodesandbox.controller;

import com.itguo.guoojcodesandbox.JavaNativeCodeSandBox;
import com.itguo.guoojcodesandbox.CNativeCodeSandBox;
import com.itguo.guoojcodesandbox.model.ExecuteCodeRequest;
import com.itguo.guoojcodesandbox.model.ExecuteCodeResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 代码沙箱主控制器
 */
@Slf4j
@RestController
@RequestMapping
public class MainController {

    public static final String AUTH_REQUEST_HEADER = "auth";

    @Value("${codesandbox.secret:secretKey}")
    private String authSecret;

    @Resource
    private JavaNativeCodeSandBox javaNativeCodeSandBox;

    @Resource
    private CNativeCodeSandBox cNativeCodeSandBox;

    @GetMapping("/health")
    public String healthCode() {
        return "OK";
    }

    @PostMapping("/execute")
    public ExecuteCodeResponse executeMessage(@RequestBody ExecuteCodeRequest executeCodeRequest,
                                            HttpServletRequest httpServletRequest,
                                            HttpServletResponse httpServletResponse) {
        // 1. 鉴权检查
        String authHeader = httpServletRequest.getHeader(AUTH_REQUEST_HEADER);
        if (!authSecret.equals(authHeader)) {
            httpServletResponse.setStatus(403);
            ExecuteCodeResponse errorResponse = new ExecuteCodeResponse();
            errorResponse.setMessage("鉴权失败");
            errorResponse.setStatus(3);
            return errorResponse;
        }

        // 2. 参数校验
        if (executeCodeRequest == null) {
            throw new RuntimeException("请求参数为空");
        }
        if (executeCodeRequest.getCode() == null || executeCodeRequest.getCode().trim().isEmpty()) {
            throw new RuntimeException("代码为空");
        }
        if (executeCodeRequest.getLanguage() == null || executeCodeRequest.getLanguage().trim().isEmpty()) {
            throw new RuntimeException("编程语言错误");
        }
        if (executeCodeRequest.getInputList() == null || executeCodeRequest.getInputList().isEmpty()) {
            throw new RuntimeException("输入用例为空");
        }

        // 3. 根据语言类型选择对应的代码沙箱
        String language = executeCodeRequest.getLanguage().toLowerCase().trim();
        log.info("收到代码执行请求: {}", language);

        ExecuteCodeResponse executeCodeResponse;
        switch (language) {
            case "java":
                executeCodeResponse = javaNativeCodeSandBox.executeCode(executeCodeRequest);
                break;
            case "c":
                executeCodeResponse = cNativeCodeSandBox.executeCode(executeCodeRequest);
                break;
            default:
                throw new RuntimeException("不支持的编程语言: " + language);
        }

        log.info("代码执行完成，状态: {}", executeCodeResponse.getStatus());

        if (executeCodeResponse == null) {
            throw new RuntimeException("代码执行返回结果为空");
        }

        return executeCodeResponse;
    }
}
