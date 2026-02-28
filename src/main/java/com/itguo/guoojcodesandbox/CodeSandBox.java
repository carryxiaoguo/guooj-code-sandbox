package com.itguo.guoojcodesandbox;


import com.itguo.guoojcodesandbox.model.ExecuteCodeRequest;
import com.itguo.guoojcodesandbox.model.ExecuteCodeResponse;

public interface CodeSandBox {
    //定义代码沙箱接口 输入ExecuteCodeRequest(请求) 输出ExecuteCodeResponse(响应)
    ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest);
}
