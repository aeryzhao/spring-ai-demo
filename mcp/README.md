# mcp：MCP Server 与 MCP Client 示例

本目录是聚合模块，下挂两个子模块，分别演示 MCP 的两端：

| 子模块 | 角色 | 端口 | 依赖 |
| --- | --- | --- | --- |
| [`mcp-server`](mcp-server/pom.xml:1) | 暴露 Tools / Resources / Prompts | 8081 | `spring-ai-starter-mcp-server-webmvc` |
| [`mcp-client`](mcp-client/pom.xml:1) | 连接 Server，把工具接入 Spring AI | 8090 | `spring-ai-starter-model-openai` + `spring-ai-starter-mcp-client` |

> 原理性讲解（MCP 是什么、在 Spring AI 中如何定位）见仓库根 [`README.md`](../README.md:1) 第 7 章。

## 目录结构

```text
mcp/
├── pom.xml                          # 聚合父模块（packaging=pom），只声明 <modules>
├── mcp-server/
│   ├── pom.xml
│   └── src/
│       ├── main/java/org/aeryzhao/mcpserver/
│       │   ├── McpServerApplication.java        # 启动入口
│       │   └── comment/
│       │       ├── CommentTools.java            # @McpTool：saveComment / listComments
│       │       ├── ResourcesProvide.java        # @McpResource：resource://comment/guideline
│       │       └── PromptProvide.java           # @McpPrompt：comment-summary-prompt
│       ├── main/resources/application.yaml      # 端口 8081 + MCP Server 配置
│       └── test/java/org/aeryzhao/mcpserver/
└── mcp-client/
    ├── pom.xml
    └── src/
        ├── main/java/org/aeryzhao/mcpclient/
        │   ├── McpClientApplication.java        # 启动入口
        │   └── controller/
        │       └── CommentMcpClientController.java  # /comments/** 接口
        ├── main/resources/application.yaml      # 端口 8090 + OpenAI + MCP Client 配置
        └── test/java/org/aeryzhao/mcpclient/
```

## 启动

**先起 server，再起 client**（client 会通过 streamable-http 连 8081）：

```bash
# 终端 1：server（8081）
mvn -pl mcp/mcp-server spring-boot:run

# 终端 2：client（8090）
mvn -pl mcp/mcp-client spring-boot:run
```

两个入口都是标准 `@SpringBootApplication`，在 IDE 里**直接运行 `main` 方法**即可，无需任何参数：

- server：[`McpServerApplication`](mcp-server/src/main/java/org/aeryzhao/mcpserver/McpServerApplication.java:14)
- client：[`McpClientApplication`](mcp-client/src/main/java/org/aeryzhao/mcpclient/McpClientApplication.java:7)

server 启动成功的标志：

```text
Registered tools: 2
Registered resources: 1
Registered prompts: 1
```

### 模型配置

只有 client 需要模型配置，来自仓库根的 [`local-config.yaml`](../local-config.yaml)（该文件被 `.gitignore` 忽略，可从 `local-config.example.yaml` 复制）：

```yaml
deepseek:
  api-key: sk-xxxxxxxx
  base-url: https://api.relaycore.cn
  chat-model: deepseek-v4-flash
```

client 在 [`application.yaml`](mcp-client/src/main/resources/application.yaml:11) 用 `spring.config.import` 读取它，三个候选路径都是 `optional:`，因此从仓库根、`mcp/` 或 `mcp/mcp-client/` 启动都能找到。

server 不依赖该文件：它只暴露 MCP 能力，不调用模型。

## MCP 三要素

### Tools：模型可调用的函数

[`CommentTools`](mcp-server/src/main/java/org/aeryzhao/mcpserver/comment/CommentTools.java:15) 用 [`@McpTool`](mcp-server/src/main/java/org/aeryzhao/mcpserver/comment/CommentTools.java:19) 暴露两个工具：

| 工具 | 说明 | 源码 |
| --- | --- | --- |
| `saveComment` | 保存评论，返回评论 ID、保存时间、当前总数 | [:20](mcp-server/src/main/java/org/aeryzhao/mcpserver/comment/CommentTools.java:20) |
| `listComments` | 查询已保存的评论，可按 `articleId` 过滤 | [:40](mcp-server/src/main/java/org/aeryzhao/mcpserver/comment/CommentTools.java:40) |

入参与返回值都用 `record`，让工具的 JSON Schema 字段名稳定。数据存在 `CopyOnWriteArrayList` 里，进程重启即清空。

### Resources：可供读取的上下文数据

[`ResourcesProvide`](mcp-server/src/main/java/org/aeryzhao/mcpserver/comment/ResourcesProvide.java:10) 用 [`@McpResource`](mcp-server/src/main/java/org/aeryzhao/mcpserver/comment/ResourcesProvide.java:12) 暴露 `resource://comment/guideline`，返回一段评论规范 JSON（`mimeType=application/json`）。

### Prompts：服务端提供的提示词模板

[`PromptProvide`](mcp-server/src/main/java/org/aeryzhao/mcpserver/comment/PromptProvide.java:12) 用 [`@McpPrompt`](mcp-server/src/main/java/org/aeryzhao/mcpserver/comment/PromptProvide.java:13) 暴露 `comment-summary-prompt`，参数用 [`@McpArg`](mcp-server/src/main/java/org/aeryzhao/mcpserver/comment/PromptProvide.java:19) 声明（`articleId` / `username` / `content` 均为必填）。

## Client 侧

控制器 [`CommentMcpClientController`](mcp-client/src/main/java/org/aeryzhao/mcpclient/controller/CommentMcpClientController.java:23)，基础路径 `@RequestMapping("/comments")`。

**把 MCP 工具注入 Spring AI**：构造函数把 `ToolCallbackProvider` 注册为 `ChatClient` 的默认工具（[源码](mcp-client/src/main/java/org/aeryzhao/mcpclient/controller/CommentMcpClientController.java:37)）：

```java
this.chatClient = ChatClient.builder(openAiChatModel)
        .defaultTools(toolCallbackProvider)
        .build();
```

模型因此可以在任意对话中主动调用 `saveComment` / `listComments`，业务代码不需要写分支。

**直接调用协议能力**（不经模型）：注入的 `List<McpSyncClient>` 用于读资源 [`readResource`](mcp-client/src/main/java/org/aeryzhao/mcpclient/controller/CommentMcpClientController.java:70)、取提示词 [`getPrompt`](mcp-client/src/main/java/org/aeryzhao/mcpclient/controller/CommentMcpClientController.java:84)、能力发现 [`listTools` 等](mcp-client/src/main/java/org/aeryzhao/mcpclient/controller/CommentMcpClientController.java:175)。

### 接口清单

| 接口 | 用途 | 参数 |
| --- | --- | --- |
| [`GET /comments/tools`](mcp-client/src/main/java/org/aeryzhao/mcpclient/controller/CommentMcpClientController.java:41) | 列出发现到的工具名，并给出示例提示词 | `articleId`、`username`、`content` |
| [`GET /comments/resource`](mcp-client/src/main/java/org/aeryzhao/mcpclient/controller/CommentMcpClientController.java:66) | 读取 `resource://comment/guideline` | 无 |
| [`GET /comments/prompt`](mcp-client/src/main/java/org/aeryzhao/mcpclient/controller/CommentMcpClientController.java:79) | 获取 `comment-summary-prompt` | `articleId`、`username`、`content` |
| [`GET /comments/demo`](mcp-client/src/main/java/org/aeryzhao/mcpclient/controller/CommentMcpClientController.java:98) | 完整链路：读资源 + 取提示词 + 调模型 + 触发工具 | `articleId`、`username`、`content` |
| [`GET /comments/mcp/list`](mcp-client/src/main/java/org/aeryzhao/mcpclient/controller/CommentMcpClientController.java:174) | 一次性列出 resources / prompts / tools | 无 |
| [`GET /comments/sse/client`](mcp-client/src/main/java/org/aeryzhao/mcpclient/controller/CommentMcpClientController.java:188) | 自由对话：`message` 直接交给带 MCP 工具的 ChatClient | `message`（**必填**） |

## 验证

```bash
curl -s "http://localhost:8090/comments/tools"     # toolCount=2，tools=['listComments','saveComment']
curl -s "http://localhost:8090/comments/resource"  # resource://comment/guideline
curl -s "http://localhost:8090/comments/prompt"    # comment-summary-prompt 正文
curl -s "http://localhost:8090/comments/mcp/list"  # tools / prompts / resources 三项
curl -s "http://localhost:8090/comments/demo?articleId=article-001&username=alice&content=验证"
curl -s "http://localhost:8090/comments/sse/client?message=请保存一条评论：articleId=article-001, username=alice, content=来自 sse/client"
```

前 4 条是纯协议交互，输出稳定；后 2 条会真实调用模型，措辞随模型而变，但只要工具被触发，再次调用就能看到评论数增加。

## 测试

```bash
mvn -pl mcp/mcp-server test
mvn -pl mcp/mcp-client test
```

| 测试 | 说明 |
| --- | --- |
| `CommentToolsTest` | 工具方法纯单测（保存、按 articleId 过滤、空内容校验） |
| `McpServerApplicationTests` | server 上下文冒烟 |
| `CommentMcpClientControllerTest` | 反射校验提示词拼装 |
| `McpClientApplicationTests` | client 上下文冒烟 |

4 个测试都不依赖外部服务。client 冒烟把 MCP 连接地址覆盖为连不上的 `http://127.0.0.1:1/mcp` 并给 `spring.ai.openai.*` 填占位值——这是安全的，因为 MCP 连接是懒建立的，启动上下文时不会真正发起网络请求。

> 不要用 `spring.ai.mcp.client.enabled=false` 来「避免连接」：关掉后 `ToolCallbackProvider` 不再注册，而 [`CommentMcpClientController`](mcp-client/src/main/java/org/aeryzhao/mcpclient/controller/CommentMcpClientController.java:23) 的构造函数强依赖该 bean，上下文会启动失败。

## 常见问题

**`/comments/tools` 返回 `toolCount=0`**

- server 没起来：连接地址固定在 [`application.yaml`](mcp-client/src/main/resources/application.yaml:29) 的 `http://localhost:8081/mcp`，先确认 8081 在监听（`lsof -iTCP:8081 -sTCP:LISTEN`），启动顺序必须是 server → client
- server 侧注解扫描被关闭：确认 [`application.yaml`](mcp-server/src/main/resources/application.yaml:1) 里 `annotation-scanner.enabled: true`

**端口被占用**

| 角色 | 端口 | 配置 |
| --- | --- | --- |
| server | 8081 | [`application.yaml`](mcp-server/src/main/resources/application.yaml:26) |
| client | 8090 | [`application.yaml`](mcp-client/src/main/resources/application.yaml:35) |

临时换端口（无需改配置）：

```bash
mvn -pl mcp/mcp-client spring-boot:run -Dspring-boot.run.arguments=--server.port=8092
```

`lsof -iTCP:<端口> -sTCP:LISTEN` 可查占用者；若显示 `com.docke`，说明是 Docker Desktop 的端口转发，用 `docker ps` 可查到具体容器。

**client 报 `deepseek.api-key` 相关错误**

仓库根缺少 `local-config.yaml`。执行 `cp local-config.example.yaml local-config.yaml` 并填入真实 API Key。
