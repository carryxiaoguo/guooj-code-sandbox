import java.util.Base64;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

/**
 * 简单的Judge0 API测试
 */
public class SimpleJudge0Test {
    
    private static final String JUDGE0_API_URL = "https://ce.judge0.com";
    private static final int C_LANGUAGE_ID = 50;
    
    public static void main(String[] args) {
        System.out.println("=== Judge0 API 测试 ===");
        
        // 测试代码
        String sourceCode = "#include <stdio.h>\n" +
                           "int main() {\n" +
                           "    int a, b;\n" +
                           "    scanf(\"%d %d\", &a, &b);\n" +
                           "    printf(\"%d\", a + b);\n" +
                           "    return 0;\n" +
                           "}";
        
        String input = "1 2";
        
        try {
            // 1. 提交代码
            System.out.println("1. 提交代码...");
            JSONObject requestBody = new JSONObject();
            requestBody.set("language_id", C_LANGUAGE_ID);
            requestBody.set("source_code", Base64.getEncoder().encodeToString(sourceCode.getBytes()));
            requestBody.set("stdin", Base64.getEncoder().encodeToString(input.getBytes()));
            
            HttpResponse response = HttpRequest.post(JUDGE0_API_URL + "/submissions?base64_encoded=true&wait=true")
                    .header("Content-Type", "application/json")
                    .body(requestBody.toString())
                    .timeout(30000)
                    .execute();
            
            System.out.println("响应状态码: " + response.getStatus());
            System.out.println("响应内容: " + response.body());
            
            if (response.getStatus() == 200) {
                JSONObject result = JSONUtil.parseObj(response.body());
                
                // 解析结果
                Integer statusId = null;
                if (result.containsKey("status")) {
                    JSONObject status = result.getJSONObject("status");
                    statusId = status.getInt("id");
                } else {
                    statusId = result.getInt("status_id");
                }
                
                String stdout = decodeBase64(result.getStr("stdout"));
                String stderr = decodeBase64(result.getStr("stderr"));
                Double time = result.getDouble("time");
                Integer memory = result.getInt("memory");
                
                System.out.println("状态ID: " + statusId);
                System.out.println("输出: " + stdout);
                System.out.println("执行时间: " + time + "s");
                System.out.println("内存使用: " + memory + "KB");
                
                if (statusId == 3) {
                    System.out.println("✅ 代码执行成功!");
                    if ("3".equals(stdout.trim())) {
                        System.out.println("✅ 输出结果正确!");
                    } else {
                        System.out.println("❌ 输出结果不正确");
                    }
                } else {
                    System.out.println("❌ 代码执行失败，状态: " + statusId);
                    if (stderr != null && !stderr.isEmpty()) {
                        System.out.println("错误信息: " + stderr);
                    }
                }
            } else {
                System.out.println("❌ 提交失败");
            }
            
        } catch (Exception e) {
            System.err.println("测试异常: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static String decodeBase64(String encoded) {
        if (encoded == null || encoded.trim().isEmpty()) {
            return "";
        }
        try {
            return new String(Base64.getDecoder().decode(encoded));
        } catch (Exception e) {
            return encoded;
        }
    }
}