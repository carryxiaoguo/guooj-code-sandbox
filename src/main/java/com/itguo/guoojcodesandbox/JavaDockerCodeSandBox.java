package com.itguo.guoojcodesandbox;


import cn.hutool.core.io.resource.ResourceUtil;
import com.itguo.guoojcodesandbox.model.ExecuteCodeRequest;
import com.itguo.guoojcodesandbox.model.ExecuteCodeResponse;
import com.itguo.guoojcodesandbox.utils.ExecuteMessage;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Component
public class JavaDockerCodeSandBox extends JavaCodeSandBoxTemplate {
    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        return super.executeCode(executeCodeRequest);
    }

    @Override
    public File saveCodeFile(String code) {
        return super.saveCodeFile(code);
    }

    @Override
    public boolean deleteFile(File userCodeFile) {
        return super.deleteFile(userCodeFile);
    }

    @Override
    public ExecuteCodeResponse OutputResultList(List<ExecuteMessage> executeMessageList) {
        return super.OutputResultList(executeMessageList);
    }

    @Override
    public List<ExecuteMessage> runFile(File userCodeFile, List<String> inputList) {
        return super.runFile(userCodeFile, inputList);
    }

    @Override
    public ExecuteMessage compileFile(File file) {
        return super.compileFile(file);
    }
}
