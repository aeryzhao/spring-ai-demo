# Spring AI 学习指南（基于本仓库示例）

> 面向新手的可运行示例与讲解，覆盖：Chat、提示词、结构化输出、聊天记忆（入门/自定义）、工具调用、MCP Client/Server。
>
> 代码以 Spring AI `2.0.x` + Spring Boot 4.1 风格的 [`ChatClient`](chat/src/main/java/org/aeryzhao/chat/controller/ChatClientController.java:4) 为主线，配合 Swagger 进行接口调试。

## 目录

- [项目结构](#项目结构)
- [快速开始](#快速开始)
- [1. Chat：最小可用对话](#1-chat最小可用对话)
- [2. 提示词：System / Template](#2-提示词system--template)
- [3. 结构化输出：让模型返回可反序列化的对象](#3-结构化输出让模型返回可反序列化的对象)
- [4. 聊天记忆入门：MessageWindow + InMemoryRepository](#4-聊天记忆入门messagewindow--inmemoryrepository)
- [5. 聊天记忆自定义：实现 ChatMemoryRepository](#5-聊天记忆自定义实现-chatmemoryrepository)
- [6. 工具调用：@Tool + defaultTools](#6-工具调用tool--defaulttools)
- [7. MCP：把“工具/资源/提示词”做成独立服务](#7-mcp把工具资源提示词做成独立服务)
  - [7.1 MCP Server：暴露 Tools / Resources / Prompts](#71-mcp-server暴露-tools--resources--prompts)
  - [7.2 MCP Client：发现并调用 MCP 能力](#72-mcp-client发现并调用-mcp-能力)
- [8. RAG：检索增强生成（Milvus + Ollama）](#8-rag检索增强生成milvus--ollama)
- [常见问题与进阶建议](#常见问题与进阶建议)

## 项目结构

- `chat/`：Spring AI 基础能力示例（Chat、Prompt、结构化输出、记忆、工具调用）
- `mcp-server/`：MCP Server（提供工具/资源/提示词）
- `mcp-client/`：MCP Client（连接 MCP Server，并把 MCP 工具注入到 Spring AI 的工具回调体系）
- `rag/`：RAG 示例（文档入库、向量检索、基于检索结果生成回答）

对应待办：[`TODOLIST.md`](TODOLIST.md:1)

## 快速开始

1. 配置模型（默认使用 DeepSeek，无需设置环境变量）：
   - 项目根目录的 [`local-config.yaml`](local-config.yaml) 为本地配置（已被 `.gitignore` 忽略，不会提交到 Git）
   - 若该文件不存在，先复制模板：`cp local-config.example.yaml local-config.yaml`
   - 将文件中的 `deepseek.api-key` 替换为你自己的 DeepSeek API Key 即可
   - 默认 `base-url=https://api.deepseek.com`、`chat-model=deepseek-chat`，如需修改直接编辑该文件

2. 启动模块：
   - 启动 `chat`：入口 [`ChatApplication`](chat/src/main/java/org/aeryzhao/chat/ChatApplication.java:1)
   - 启动 `mcp-server`：入口 [`McpServerApplication`](mcp-server/src/main/java/org/aeryzhao/mcpserver/McpServerApplication.java:1)
   - 启动 `mcp-client`：入口 [`McpClientApplication`](mcp-client/src/main/java/org/aeryzhao/mcpclient/McpClientApplication.java:1)
   - 启动 `rag`：入口 [`RagApplication`](rag/src/main/java/org/aeryzhao/rag/RagApplication.java:1)

3. 使用 Swagger 调试：仓库 README 给出了入口：[`README.md`](README.md:7)

> 说明：本指南重点讲“怎么用 Spring AI 组织能力”，不绑定某一家模型厂商。你只要保证 `OpenAiChatModel` 能正常注入即可。

---

## 1. Chat：最小可用对话

核心目标：用最少代码把“用户输入 → 模型输出”跑通。

示例控制器：[`ChatClientController`](chat/src/main/java/org/aeryzhao/chat/controller/ChatClientController.java:18)

关键点：

- 使用 [`ChatClient.builder()`](chat/src/main/java/org/aeryzhao/chat/controller/ChatClientController.java:24) 构建客户端
- `prompt().user(...).call().content()` 获取纯文本
- `prompt().user(...).call().chatResponse()` 获取包含元数据的响应
- `prompt().user(...).stream().content()` 获取流式输出（`Flux<String>`）

代码片段（与仓库一致）：

```java
// 见 chat/src/main/java/org/aeryzhao/chat/controller/ChatClientController.java
this.chatClient = ChatClient.builder(chatModel)
        .defaultAdvisors(new SimpleLoggerAdvisor())
        .build();

return this.chatClient.prompt()
        .user(userInput)
        .call()
        .content();
```

为什么要加 [`SimpleLoggerAdvisor`](chat/src/main/java/org/aeryzhao/chat/controller/ChatClientController.java:25)？

- 它会把 prompt/响应等信息打印出来，适合新手理解“模型到底收到了什么”。

---

## 2. 提示词：System / Template

提示词（Prompt）通常分两类：

- **System Prompt**：定义角色、风格、边界（“你是谁、怎么回答”）
- **User Prompt**：用户问题/任务

示例控制器：[`PromptController`](chat/src/main/java/org/aeryzhao/chat/controller/PromptController.java:16)

### 2.1 System Prompt

```java
// 见 chat/src/main/java/org/aeryzhao/chat/controller/PromptController.java
return this.chatClient.prompt()
        .system(DEFAULT_SYSTEM_PROMPT)
        .user(userInput)
        .call()
        .content();
```

建议：

- System Prompt 尽量稳定、可复用（例如常量 [`DEFAULT_SYSTEM_PROMPT`](chat/src/main/java/org/aeryzhao/chat/controller/PromptController.java:19)）
- 把“输出语言/格式/风格”写清楚，减少模型自由发挥

### 2.2 模板提示词（参数化）

Spring AI 的 `user(u -> u.text(...).param(...))` 让你用模板变量构造 prompt：

```java
// 见 chat/src/main/java/org/aeryzhao/chat/controller/PromptController.java
.user(u -> u.text("请用{language}给出一段关于{topic}的{level}示例代码，并补充两条说明。")
        .param("language", language)
        .param("topic", topic)
        .param("level", level))
```

这类写法的价值：

- 让 prompt 结构固定、变量可控
- 便于测试与复现（同一模板 + 同一参数 → 更稳定的输出）

---

## 3. 结构化输出：让模型返回可反序列化的对象

当你希望模型输出“可被程序消费”的结果（而不是一段随意文本），结构化输出是关键。

示例控制器：[`StructuredOutputController`](chat/src/main/java/org/aeryzhao/chat/controller/StructuredOutputController.java:18)

这里用 `record` 定义返回结构：[`BookSummary`](chat/src/main/java/org/aeryzhao/chat/controller/StructuredOutputController.java:43)

```java
// 见 chat/src/main/java/org/aeryzhao/chat/controller/StructuredOutputController.java
return this.chatClient.prompt()
        .system(DEFAULT_SYSTEM_PROMPT)
        .user(u -> u.text("请为《{bookName}》生成书籍摘要。返回字段必须包含：name、author、summary、tags。tags 请返回 3 到 5 个关键词。")
                .param("bookName", bookName))
        .call()
        .entity(BookSummary.class);
```

要点：

- `.entity(BookSummary.class)` 会尝试把模型输出映射为 `BookSummary`
- prompt 必须明确字段名与约束，否则容易反序列化失败

进阶建议：

- 对结构化输出做“失败兜底”：捕获异常并返回原始文本，或重试一次（加更严格的格式约束）
- 对字段做校验（例如 tags 数量、summary 长度），把“模型输出”当作不可信输入

---

## 4. 聊天记忆入门：MessageWindow + InMemoryRepository

多轮对话的本质：每次请求都把“历史消息 + 当前消息”一起发给模型。

Spring AI 用 `ChatMemory` 抽象这件事，并通过 Advisor 自动把历史消息注入 prompt。

示例控制器：[`ChatMemoryController`](chat/src/main/java/org/aeryzhao/chat/controller/ChatMemoryController.java:20)

### 4.1 记忆组件的组合

- `MessageWindowChatMemory`：只保留最近 N 条消息（窗口）
- `InMemoryChatMemoryRepository`：内存存储（进程内）
- `MessageChatMemoryAdvisor`：在每次调用时自动读写记忆

```java
// 见 chat/src/main/java/org/aeryzhao/chat/controller/ChatMemoryController.java
ChatMemory chatMemory = MessageWindowChatMemory.builder()
        .chatMemoryRepository(new InMemoryChatMemoryRepository())
        .maxMessages(20)
        .build();

this.chatClient = ChatClient.builder(chatModel)
        .defaultAdvisors(new SimpleLoggerAdvisor())
        .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
        .build();
```

### 4.2 conversationId：把“记忆”分会话

关键代码：通过 advisor 参数传入 [`ChatMemory.CONVERSATION_ID`](chat/src/main/java/org/aeryzhao/chat/controller/ChatMemoryController.java:47)

```java
return this.chatClient.prompt()
        .system(DEFAULT_SYSTEM_PROMPT)
        .user(userInput)
        .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationId))
        .call()
        .content();
```

理解要点：

- `conversationId` 是“记忆的分区键”
- 同一个 `conversationId` 的请求会共享历史消息

---

## 5. 聊天记忆自定义：实现 ChatMemoryRepository

当你需要把记忆存到 Redis / MySQL / Elasticsearch，或者需要自定义隔离策略时，可以实现 `ChatMemoryRepository`。

示例：[`ConcurrentMapChatMemoryRepository`](chat/src/main/java/org/aeryzhao/chat/repository/ConcurrentMapChatMemoryRepository.java:18)

它用 `ConcurrentHashMap<String, List<Message>>` 做了一个线程安全的最小实现：

- [`findConversationIds()`](chat/src/main/java/org/aeryzhao/chat/repository/ConcurrentMapChatMemoryRepository.java:22)
- [`findByConversationId()`](chat/src/main/java/org/aeryzhao/chat/repository/ConcurrentMapChatMemoryRepository.java:30)
- [`saveAll()`](chat/src/main/java/org/aeryzhao/chat/repository/ConcurrentMapChatMemoryRepository.java:35)
- [`deleteByConversationId()`](chat/src/main/java/org/aeryzhao/chat/repository/ConcurrentMapChatMemoryRepository.java:40)

控制器：[`CustomChatMemoryController`](chat/src/main/java/org/aeryzhao/chat/controller/CustomChatMemoryController.java:25)

额外提供了“查看/清空记忆”的接口：

- [`/memory-size`](chat/src/main/java/org/aeryzhao/chat/controller/CustomChatMemoryController.java:60)
- [`/memory`](chat/src/main/java/org/aeryzhao/chat/controller/CustomChatMemoryController.java:66)
- [`/clear`](chat/src/main/java/org/aeryzhao/chat/controller/CustomChatMemoryController.java:72)

进阶建议：

- 生产环境不要用进程内 Map：重启即丢、无法横向扩展
- 存储层要考虑：TTL、最大窗口、敏感信息脱敏、按用户隔离

---

## 6. 工具调用：@Tool + defaultTools

工具调用（Tool Calling）的目标：让模型在需要时调用你的 Java 方法，获得“可验证的外部能力”。

示例控制器：[`ToolCallController`](chat/src/main/java/org/aeryzhao/chat/controller/ToolCallController.java:19)

工具定义：[`TravelPlanTools`](chat/src/main/java/org/aeryzhao/chat/tool/TravelPlanTools.java:13)

### 6.1 定义工具

用 [`@Tool`](chat/src/main/java/org/aeryzhao/chat/tool/TravelPlanTools.java:15) 标注方法：

```java
@Tool(description = "根据城市、出行天数和预算生成旅游建议")
public TravelPlanResult planTrip(TravelPlanRequest request) {
    // ...
}
```

这里的入参/出参都用 `record`：

- [`TravelPlanRequest`](chat/src/main/java/org/aeryzhao/chat/tool/TravelPlanTools.java:38)
- [`TravelPlanResult`](chat/src/main/java/org/aeryzhao/chat/tool/TravelPlanTools.java:41)

这会让工具 schema 更清晰（字段名稳定），也更利于模型正确构造参数。

### 6.2 注册工具并触发调用

在构建 `ChatClient` 时注册工具：[`defaultTools(new TravelPlanTools())`](chat/src/main/java/org/aeryzhao/chat/controller/ToolCallController.java:30)

```java
this.chatClient = ChatClient.builder(chatModel)
        .defaultAdvisors(new SimpleLoggerAdvisor())
        .defaultTools(new TravelPlanTools())
        .build();
```

然后在 system prompt 中明确“优先调用工具”：[`DEFAULT_SYSTEM_PROMPT`](chat/src/main/java/org/aeryzhao/chat/controller/ToolCallController.java:23)

> 实战经验：工具调用是否稳定，往往取决于 prompt 是否明确“什么时候该调用工具、调用哪个工具、参数从哪里来”。

---

## 7. MCP：把“工具/资源/提示词”做成独立服务

MCP（Model Context Protocol）可以理解为：

- 把工具（Tools）、资源（Resources）、提示词（Prompts）从业务应用中拆出来
- 由独立的 MCP Server 统一提供
- 业务应用（MCP Client）连接 MCP Server，动态发现并调用这些能力

本仓库用 `mcp-server` + `mcp-client` 演示了一个“评论场景”。

### 7.1 MCP Server：暴露 Tools / Resources / Prompts

#### Tools：评论保存与查询

工具类：[`CommentTools`](mcp-server/src/main/java/org/aeryzhao/mcpserver/comment/CommentTools.java:15)

- [`@McpTool`](mcp-server/src/main/java/org/aeryzhao/mcpserver/comment/CommentTools.java:19) 暴露 `saveComment`
- [`@McpTool`](mcp-server/src/main/java/org/aeryzhao/mcpserver/comment/CommentTools.java:39) 暴露 `listComments`

```java
@McpTool(description = "模拟保存评论，返回生成的评论 ID、保存时间和状态。")
public SaveCommentResponse saveComment(SaveCommentRequest request) {
    // ...
}

@McpTool(description = "查询已经模拟保存的评论列表，可按 articleId 过滤。")
public CommentListResponse listComments(ListCommentsRequest request) {
    // ...
}
```

#### Resources：评论规范

资源提供：[`ResourcesProvide`](mcp-server/src/main/java/org/aeryzhao/mcpserver/comment/ResourcesProvide.java:9)

- [`@McpResource`](mcp-server/src/main/java/org/aeryzhao/mcpserver/comment/ResourcesProvide.java:12) 暴露 `resource://comment/guideline`

资源内容是 JSON 字符串（`mimeType=application/json`），适合被客户端读取后拼进 prompt。

#### Prompts：可复用的提示词模板

提示词提供：[`PromptProvide`](mcp-server/src/main/java/org/aeryzhao/mcpserver/comment/PromptProvide.java:12)

- [`@McpPrompt`](mcp-server/src/main/java/org/aeryzhao/mcpserver/comment/PromptProvide.java:13) 暴露 `comment-summary-prompt`
- 用 [`@McpArg`](mcp-server/src/main/java/org/aeryzhao/mcpserver/comment/PromptProvide.java:19) 声明参数

```java
@McpPrompt(name = "comment-summary-prompt", title = "评论总结提示词")
public String commentSummaryPrompt(
        @McpArg(name = "articleId", required = true) String articleId,
        @McpArg(name = "username", required = true) String username,
        @McpArg(name = "content", required = true) String content) {
    return "你是评论助手。请先调用 saveComment 工具保存评论...";
}
```

### 7.2 MCP Client：发现并调用 MCP 能力

控制器：[`CommentMcpClientController`](mcp-client/src/main/java/org/aeryzhao/mcpclient/controller/CommentMcpClientController.java:21)

它做了三件事：

1. **把 MCP 工具注入 Spring AI**：构建 `ChatClient` 时使用 [`defaultToolCallbacks(toolCallbackProvider)`](mcp-client/src/main/java/org/aeryzhao/mcpclient/controller/CommentMcpClientController.java:37)
2. **直接调用 MCP 协议能力**：
   - 读资源：[`readResource`](mcp-client/src/main/java/org/aeryzhao/mcpclient/controller/CommentMcpClientController.java:69)
   - 取 prompt：[`getPrompt`](mcp-client/src/main/java/org/aeryzhao/mcpclient/controller/CommentMcpClientController.java:83)
   - 列表发现：[`listResources/listPrompts/listTools`](mcp-client/src/main/java/org/aeryzhao/mcpclient/controller/CommentMcpClientController.java:177)
3. **把 MCP prompt 交给模型执行**：[`runCommentDemo`](mcp-client/src/main/java/org/aeryzhao/mcpclient/controller/CommentMcpClientController.java:98)

核心链路（简化版）：

```java
// 见 mcp-client/src/main/java/org/aeryzhao/mcpclient/controller/CommentMcpClientController.java
String promptText = extractPromptText(prompt);
String answer = this.chatClient.prompt()
        .user(promptText)
        .call()
        .content();
```

理解要点：

- MCP Server 提供“可复用能力”（工具/资源/提示词）
- MCP Client 负责“把能力接入业务应用”，并让模型在对话中使用这些能力

---

## 8. RAG：检索增强生成（Milvus + Ollama）

本仓库的 RAG 示例位于 `rag/` 模块，目标是把“外部知识库”接入模型回答流程：

1. 文档入库：把文本转成向量并写入向量库（Milvus）
2. 语义检索：把用户问题向量化，在向量库中检索 TopK 相似文档
3. 生成回答：把检索到的文档片段作为上下文，交给模型生成答案

模块说明文档：[`rag/README.md`](rag/README.md:1)

### 8.1 API 入口：/api/ask

RAG 问答接口：[`RagController.ask()`](rag/src/main/java/org/aeryzhao/rag/controller/RagController.java:58)

- 路径：`POST /api/ask`
- 入参：问题 + topK
- 出参：答案 + sources（检索到的文档内容列表）

### 8.2 核心链路：RagService.ask

核心实现：[`RagService.ask()`](rag/src/main/java/org/aeryzhao/rag/service/RagService.java:27)

它把 RAG 拆成三步（非常适合新手理解与调试）：

1）问题向量化：

- 调用 [`EmbeddingService.embed()`](rag/src/main/java/org/aeryzhao/rag/service/EmbeddingService.java:20) 生成 `List<Float>` 向量
- 当前实现固定走 Ollama embedding：[`OllamaEmbeddingService`](rag/src/main/java/org/aeryzhao/rag/service/OllamaEmbeddingService.java:1)

2）向量检索：

- 调用 [`DocumentService.searchSimilar()`](rag/src/main/java/org/aeryzhao/rag/service/DocumentService.java:106) 在 Milvus 中检索相似文档
- 返回结构包含文档与相似度分数：[`SearchResultWithScore`](rag/src/main/java/org/aeryzhao/rag/service/DocumentService.java:40)

3）拼接上下文并生成：

- 把检索结果整理成 sources（带文档 ID）：[`RagService.ask()`](rag/src/main/java/org/aeryzhao/rag/service/RagService.java:37)
- 构造 prompt：[`RagService.buildPrompt()`](rag/src/main/java/org/aeryzhao/rag/service/RagService.java:68)
- 调用模型生成答案：[`ChatClient.Builder`](rag/src/main/java/org/aeryzhao/rag/service/RagService.java:25)

关键代码（与仓库一致，略去日志）：

```java
// 见 rag/src/main/java/org/aeryzhao/rag/service/RagService.java
List<Float> questionVector = embeddingService.embed(question);
List<SearchResultWithScore> searchResults = documentService.searchSimilar(questionVector, topK);

List<String> sources = searchResults.stream()
        .map(result -> String.format("[文档ID: %s]\n%s", result.getDocument().getId(), result.getDocument().getContent()))
        .toList();

String prompt = buildPrompt(question, sources);
String answer = chatClientBuilder.build().prompt().user(prompt).call().content();
```

### 8.3 文档入库与检索：DocumentService

文档入库：[`DocumentService.insertDocument()`](rag/src/main/java/org/aeryzhao/rag/service/DocumentService.java:60)

- 先对文档内容做 embedding：[`embeddingService.embed()`](rag/src/main/java/org/aeryzhao/rag/service/DocumentService.java:68)
- 再把 `id/content/embedding/metadata` 写入 Milvus：[`milvusClient.insert(...)`](rag/src/main/java/org/aeryzhao/rag/service/DocumentService.java:89)

语义检索：[`DocumentService.searchSimilar()`](rag/src/main/java/org/aeryzhao/rag/service/DocumentService.java:106)

- 通过 `SearchParam` 指定向量字段、TopK、metricType 等：[`SearchParam.newBuilder()`](rag/src/main/java/org/aeryzhao/rag/service/DocumentService.java:112)
- 解析 Milvus 返回的 `id/score/outFields`：[`SearchResultsWrapper`](rag/src/main/java/org/aeryzhao/rag/service/DocumentService.java:124)

### 8.4 Prompt 设计：减少“幻觉”

本示例的 prompt 设计思路是“只基于参考文档回答，不知道就说不知道”：

- 约束语句：[`buildPrompt()`](rag/src/main/java/org/aeryzhao/rag/service/RagService.java:71)

```text
基于以下参考文档回答用户问题。如果参考文档中没有相关信息，请明确说明。
```

进阶建议：

- sources 过长时要做截断/分块（否则 token 成本高、且可能稀释关键信息）
- 让模型输出引用（例如“答案段落 → 文档ID”），便于可追溯
- 引入 rerank（重排序）提升相关性（向量召回 + rerank 精排）

### 8.5 运行与调试建议

- 先按 [`rag/README.md`](rag/README.md:30) 启动 Milvus 与 Ollama，并拉取 embedding/chat 模型
- 先调用“文档入库”接口写入少量可控文本，再调用 `/api/ask` 验证检索与回答
- 如果检索结果为空：优先检查 embedding 维度、Milvus collection schema、metricType 是否匹配

---

## 常见问题与进阶建议

### 1）为什么要用 Advisor？

Advisor 是 Spring AI 的“横切增强点”，适合做：

- 日志（如 [`SimpleLoggerAdvisor`](chat/src/main/java/org/aeryzhao/chat/controller/ChatClientController.java:25)）
- 记忆注入（如 [`MessageChatMemoryAdvisor`](chat/src/main/java/org/aeryzhao/chat/controller/ChatMemoryController.java:36)）
- 统一的 prompt 规范、审计、限流、重试等

### 2）结构化输出不稳定怎么办？

- prompt 明确字段名、类型、数量约束（本仓库已示范）
- 对 `.entity(...)` 做异常处理与重试
- 把模型输出当作外部输入：做校验与兜底

### 3）工具调用的“可控性”来自哪里？

- 工具 schema 清晰（入参/出参字段名稳定，推荐 `record`）
- system prompt 明确“何时调用工具、调用哪个工具、参数来源”
- 对工具实现做参数校验（例如 [`CommentTools.saveComment()`](mcp-server/src/main/java/org/aeryzhao/mcpserver/comment/CommentTools.java:20) 的必填校验）

---

## 许可证

本仓库代码以 **MIT License** 开源发布，详见仓库根目录的 [`LICENSE`](LICENSE)。

> 说明：本仓库示例依赖的第三方库（如 Spring Boot / Spring AI / Milvus SDK 等）遵循各自的许可证；本仓库许可证仅适用于本仓库内的原创代码与文档。
