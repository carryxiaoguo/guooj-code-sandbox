# 蝈蝈在线判题系统 - 代码沙箱（guooj-code-sandbox）

## 项目简介

蝈蝈在线判题系统的代码沙箱服务，负责接收用户提交的代码，在隔离环境中编译、执行，并返回运行结果。支持 Java 和 C 语言，提供原生进程执行和 Docker 容器执行两种模式，确保代码运行的安全性和资源隔离。

##  技术栈

| 技术 | 说明 |
|------|------|
| Java 8 | 开发语言 |
| Spring Boot 2.7.6 | 应用框架 |
| Docker Java 3.3.0 | Docker 容器管理 |
| Hutool | Java 工具库 |
| Lombok | 简化代码 |

## 项目结构

```
src/main/java/com/itguo/guoojcodesandbox/
├── config/           # 配置类
├── constant/         # 常量定义（编译/运行超时等）
├── controller/       # 控制器（代码执行入口）
├── docker/           # Docker 相关工具
├── model/            # 数据模型
│   ├── ExecuteCodeRequest.java    # 执行请求
│   ├── ExecuteCodeResponse.java   # 执行响应
│   └── JudgeInfo.java             # 判题信息
├── service/          # 业务逻辑
├── utils/            # 工具类
│   ├── ProcessExecutor.java       # 进程执行器
│   ├── ProcessUtils.java          # 进程工具
│   └── MemoryMonitor.java         # 内存监控
├── CodeSandBox.java               # 沙箱接口
├── JavaCodeSandBoxTemplate.java   # Java 沙箱模板（模板方法模式）
├── JavaNativeCodeSandBox.java     # Java 原生沙箱实现
├── JavaDockerCodeSandBox.java     # Java Docker 沙箱实现
└── CNativeCodeSandBox.java        # C 语言原生沙箱实现
```

##  API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/health` | 健康检查 |
| POST | `/execute` | 执行代码 |

### 请求鉴权

所有 `/execute` 请求需要在 Header 中携带鉴权信息：

```
auth: secretKey
```

### 执行请求示例

```json
{
  "code": "public class Main { public static void main(String[] args) { ... } }",
  "language": "java",
  "inputList": ["1 2", "3 4"]
}
```

### 执行响应示例

```json
{
  "outputList": ["3", "7"],
  "message": "成功",
  "status": 1,
  "judgeInfo": {
    "time": 120,
    "memory": 15432
  }
}
```

## 执行流程

```
接收代码 → 鉴权校验 → 保存代码到临时文件 → 编译代码
→ 逐个执行测试用例 → 收集输出结果 → 清理临时文件 → 返回结果
```

### 支持的执行模式

| 模式 | 类 | 说明 |
|------|------|------|
| Java 原生执行 | JavaNativeCodeSandBox | 直接通过 javac + java 命令执行 |
| Java Docker 执行 | JavaDockerCodeSandBox | 在 Docker 容器中编译执行，资源隔离 |
| C 原生执行 | CNativeCodeSandBox | 通过 gcc 编译 + 执行 |

### 安全措施
- 代码执行超时限制
- 内存使用监控
- Docker 容器资源隔离（Docker 模式）
- 临时文件自动清理
- 请求鉴权校验

##  快速启动

### 环境要求
- JDK 8+
- Maven 3.6+
- GCC（C 语言支持）
- Docker（可选，Docker 模式需要）

### 启动步骤

```bash
# 1. 编译项目
mvn clean compile -DskipTests

# 2. 启动服务
mvn spring-boot:run
```

服务启动后监听端口：`8090`

### Docker 模式

如需使用 Docker 沙箱模式，确保：
1. Docker 已安装并运行
2. 当前用户有 Docker 操作权限
