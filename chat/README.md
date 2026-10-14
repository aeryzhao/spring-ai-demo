# chat 模块：Spring AI 基础能力示例

`chat` 是 `spring-ai-demo` 的基础模块，把 Spring AI 最常用的一组能力拆成一组可独立调用的 HTTP 接口：ChatClient 调用、提示词、结构化输出、聊天记忆、工具调用。

它同时是 `mcp` / `rag` / `langchain4j` 三个模块的「最小对照基线」——如果你想先确认模型通路是否正常，先跑这个模块。

- 想理解「为什么这么写」：看仓库根 [`README.md`](../README.md:53) 的第 1~6 章。
- 想快速「跑起来 + 查接口」：看本文档。

## 技术栈

| 组件 | 版本 / 说明 |
| --- | --- |
| Java | 21（[`chat/pom.xml`](pom.xml:15)） |
| Spring Boot | 4.1.0（继承根 pom 的 `spring-boot-starter-parent`） |
| Spring AI | 2.0.0（`spring-ai-bom` 统一管理版本） |
| 模型接入 | `spring-ai-starter-model-openai`，走 OpenAI 兼容协议调用 DeepSeek |
| 接口文档 | `springdoc-openapi-starter-webmvc-ui` 3.1.0（根 pom 统一引入） |

> 说明：`chat/pom.xml` 自身只声明了一个依赖 —— [`spring-ai-starter-model-openai`](pom.xml:19)，Web、Actuator、springdoc 都由根 pom 的 `<dependencies>` 统一提供。

## 快速开始

### 1. 准备模型配置

模型配置来自**仓库根目录**的 [`local-config.yaml`](../local-config.yaml:7)（该文件已被 `.gitignore` 忽略，不会提交）：

```yaml
deepseek:
  api-key: sk-xxxxxxxx        # 替换为你自己的 Key
  base-url: https://api.relaycore.cn
  chat-model: deepseek-v4-flash
```

若文件不存在，先复制模板：`cp local-config.example.yaml local-config.yaml`。

模块在 [`application.yaml`](src/main/resources/application.yaml:4) 中通过 `spring.config.import` 读取它，两个路径都是 `optional:`，因此从**仓库根目录**或 **`chat/` 目录**启动都能找到配置：

```yaml
spring:
  config:
    import: optional:file:../local-config.yaml,optional:file:./local-config.yaml
  ai:
    openai:
      api-key: ${deepseek.api-key}
      base-url: ${deepseek.base-url}
      chat:
        model: ${deepseek.chat-model}
```

### 2. 启动

入口类：[`ChatApplication`](src/main/java/org/aeryzhao/chat/ChatApplication.java:7)

```bash
# 方式一：在仓库根目录构建后运行（多模块推荐）
mvn -pl chat -am clean package -DskipTests
java -jar chat/target/chat-0.0.1-SNAPSHOT.jar

# 方式二：在 chat 目录下直接运行
cd chat && mvn spring-boot:run
```

启动后监听端口 **8096**（[`application.yaml`](src/main/resources/application.yaml:13)）。

### 3. 调试入口

| 入口 | 地址 |
| --- | --- |
| Swagger UI | <http://localhost:8096/swagger-ui.html> |
| OpenAPI JSON | <http://localhost:8096/v3/api-docs> |

两者均在 [`application.yaml`](src/main/resources/application.yaml:14) 的 `springdoc` 段中配置，扫描包为 `org.aeryzhao`（[`application.yaml`](src/main/resources/application.yaml:24)）。

```bash
curl "http://localhost:8096/ai/chat-client/content?userInput=用一句话介绍Spring%20AI"
```

## 接口速查

以下路径均逐个核对自 `chat/src/main/java/org/aeryzhao/chat/controller/` 下的真实源码。所有参数都是 `@RequestParam`（GET 查询串），标注的默认值即源码中的 `defaultValue`。

### 概览

| 控制器 | 类级路径 | 接口数 | 主题 |
| --- | --- | --- | --- |
| [`ChatModelController`](src/main/java/org/aeryzhao/chat/controller/ChatModelController.java:17) | `/ai/chat-model` | 2 | 直接使用 `ChatModel` 底层 API |
| [`ChatClientController`](src/main/java/org/aeryzhao/chat/controller/ChatClientController.java:18) | `/ai/chat-client` | 3 | `ChatClient` 三种返回形态 |
| [`PromptController`](src/main/java/org/aeryzhao/chat/controller/PromptController.java:16) | `/ai/prompt` | 2 | System / 模板提示词 |
| [`StructuredOutputController`](src/main/java/org/aeryzhao/chat/controller/StructuredOutputController.java:18) | `/ai/structured-output` | 1 | `.entity()` 结构化输出 |
| [`ChatMemoryController`](src/main/java/org/aeryzhao/chat/controller/ChatMemoryController.java:20) | `/ai/chat-memory` | 1 | 内置内存仓库的多轮对话 |
| [`CustomChatMemoryController`](src/main/java/org/aeryzhao/chat/controller/CustomChatMemoryController.java:25) | `/ai/chat-memory/custom` | 4 | 自定义 `ChatMemoryRepository` |
| [`ToolCallController`](src/main/java/org/aeryzhao/chat/controller/ToolCallController.java:19) | `/ai/tool` | 1 | `@Tool` 工具调用 |

### 1. ChatModelController —— 直接用 ChatModel

不经过 `ChatClient`，直接注入 `OpenAiChatModel`（[构造函数](src/main/java/org/aeryzhao/chat/controller/ChatModelController.java:22)），适合观察最原始的调用与流式行为。

| 方法 | 路径 | 参数（默认值） | 返回 | 源码 |
| --- | --- | --- | --- | --- |
| GET | `/ai/chat-model/call` | `message`（讲个笑话） | `String` | [:28](src/main/java/org/aeryzhao/chat/controller/ChatModelController.java:28) |
| GET | `/ai/chat-model/stream` | `message`（讲个笑话） | `Flux<ChatResponse>`，`text/event-stream` | [:34](src/main/java/org/aeryzhao/chat/controller/ChatModelController.java:34) |

### 2. ChatClientController —— ChatClient 三种返回形态

统一的 [`ChatClient` 构造](src/main/java/org/aeryzhao/chat/controller/ChatClientController.java:23)：`ChatClient.builder(chatModel).defaultAdvisors(new SimpleLoggerAdvisor()).build()`。三个接口用的是同一套 `prompt()` 链，差别只在末端取值方式。

| 方法 | 路径 | 参数（默认值） | 返回 | 源码 |
| --- | --- | --- | --- | --- |
| GET | `/ai/chat-client/content` | `userInput`（讲个笑话） | `String`（`.content()`） | [:31](src/main/java/org/aeryzhao/chat/controller/ChatClientController.java:31) |
| GET | `/ai/chat-client/chatResponse` | `userInput`（讲个笑话） | `ChatResponse`（含 token 用量等元数据） | [:40](src/main/java/org/aeryzhao/chat/controller/ChatClientController.java:40) |
| GET | `/ai/chat-client/stream` | `userInput`（讲个笑话） | `Flux<String>`（`.stream().content()`） | [:49](src/main/java/org/aeryzhao/chat/controller/ChatClientController.java:49) |

### 3. PromptController —— System 与模板提示词

| 方法 | 路径 | 参数（默认值） | 说明 | 源码 |
| --- | --- | --- | --- | --- |
| GET | `/ai/prompt/system` | `userInput`（如何设计一个缓存接口？） | 固定 `system` 提示词 [`DEFAULT_SYSTEM_PROMPT`](src/main/java/org/aeryzhao/chat/controller/PromptController.java:19) | [:31](src/main/java/org/aeryzhao/chat/controller/PromptController.java:31) |
| GET | `/ai/prompt/template` | `topic`（Spring Boot）、`language`（Java）、`level`（入门） | 用 `u.text(...)` + `.param(...)` 渲染模板 | [:41](src/main/java/org/aeryzhao/chat/controller/PromptController.java:41) |

模板提示词的占位符与参数名一一对应（[PromptController.java:47](src/main/java/org/aeryzhao/chat/controller/PromptController.java:47)）：

```java
.user(u -> u.text("请用{language}给出一段关于{topic}的{level}示例代码，并补充两条说明。")
        .param("language", language)
        .param("topic", topic)
        .param("level", level))
```

### 4. StructuredOutputController —— 结构化输出

| 方法 | 路径 | 参数（默认值） | 返回 | 源码 |
| --- | --- | --- | --- | --- |
| GET | `/ai/structured-output/book-summary` | `bookName`（深入理解Java虚拟机） | [`BookSummary`](src/main/java/org/aeryzhao/chat/controller/StructuredOutputController.java:43) record | [:34](src/main/java/org/aeryzhao/chat/controller/StructuredOutputController.java:34) |

`.entity(BookSummary.class)`（[StructuredOutputController.java:40](src/main/java/org/aeryzhao/chat/controller/StructuredOutputController.java:40)）会把模型返回的 JSON 直接反序列化成 record；字段约束写在提示词里，而不是靠手写解析。

### 5. ChatMemoryController —— 基于内置仓库的多轮对话

| 方法 | 路径 | 参数（默认值） | 源码 |
| --- | --- | --- | --- |
| GET | `/ai/chat-memory/in-memory` | `conversationId`（demo-session）、`userInput`（我叫小明，请记住我的名字） | [:42](src/main/java/org/aeryzhao/chat/controller/ChatMemoryController.java:42) |

同一 `conversationId` 连续调用即可验证记忆生效：

```bash
curl "http://localhost:8096/ai/chat-memory/in-memory?conversationId=s1&userInput=我叫小明"
curl "http://localhost:8096/ai/chat-memory/in-memory?conversationId=s1&userInput=我叫什么？"
```

### 6. CustomChatMemoryController —— 自定义记忆仓库

类级路径 `/ai/chat-memory/custom`，底层换成自定义的 [`ConcurrentMapChatMemoryRepository`](src/main/java/org/aeryzhao/chat/repository/ConcurrentMapChatMemoryRepository.java:18)，因此额外暴露了三个「看记忆 / 清记忆」的排查接口。

| 方法 | 路径 | 参数（默认值） | 返回 | 源码 |
| --- | --- | --- | --- | --- |
| GET | `/ai/chat-memory/custom/chat` | `conversationId`（custom-session）、`userInput`（我最喜欢的编程语言是Java，请记住） | `String` | [:49](src/main/java/org/aeryzhao/chat/controller/CustomChatMemoryController.java:49) |
| GET | `/ai/chat-memory/custom/memory-size` | `conversationId`（custom-session） | `int`，当前会话消息条数 | [:61](src/main/java/org/aeryzhao/chat/controller/CustomChatMemoryController.java:61) |
| GET | `/ai/chat-memory/custom/memory` | `conversationId`（custom-session） | `List<Message>`，会话原始消息 | [:67](src/main/java/org/aeryzhao/chat/controller/CustomChatMemoryController.java:67) |
| GET | `/ai/chat-memory/custom/clear` | `conversationId`（custom-session） | `String`，清空提示 | [:73](src/main/java/org/aeryzhao/chat/controller/CustomChatMemoryController.java:73) |

### 7. ToolCallController —— @Tool 工具调用

| 方法 | 路径 | 参数（默认值） | 源码 |
| --- | --- | --- | --- |
| GET | `/ai/tool/travel-plan` | `city`（杭州）、`days`（3）、`budget`（3000） | [:36](src/main/java/org/aeryzhao/chat/controller/ToolCallController.java:36) |

工具实现在 [`TravelPlanTools`](src/main/java/org/aeryzhao/chat/tool/TravelPlanTools.java:13)，通过 `defaultTools(new TravelPlanTools())` 注册（[ToolCallController.java:30](src/main/java/org/aeryzhao/chat/controller/ToolCallController.java:30)）。

## 关键机制

### 用 ChatClient.builder 包一层

`ChatClient` 是 Spring AI 的推荐入口：把「模型 + 默认顾问 + 默认工具」固化成可复用对象，业务代码只写 `.prompt().user(...).call()`。本项目每个控制器都在构造函数里各自 `builder` 一次，因此**各控制器的顾问/工具互不影响**，非常适合对照学习。

```java
this.chatClient = ChatClient.builder(chatModel)
        .defaultAdvisors(new SimpleLoggerAdvisor())
        .build();
```

见 [ChatClientController.java:24](src/main/java/org/aeryzhao/chat/controller/ChatClientController.java:24)。

### SimpleLoggerAdvisor：把请求/响应打到日志

[SimpleLoggerAdvisor](src/main/java/org/aeryzhao/chat/controller/ChatClientController.java:25) 是 Spring AI 内置的调试顾问，把发给模型的消息和返回内容打到日志，是所有控制器共用的第一层 `defaultAdvisors`。排查「提示词到底发出去了什么」时，先看它的日志。

### 提示词：System 常驻 + 模板占位

两种写法都覆盖到了：

- **System**：用 `DEFAULT_SYSTEM_PROMPT` 常量固定角色（[PromptController.java:19](src/main/java/org/aeryzhao/chat/controller/PromptController.java:19)），每次请求只变 `user` 内容。`ChatClientController` 等控制器省略了 `.system(...)`，直接只发用户消息。
- **模板**：`u.text("...{var}...").param("var", value)`，占位符在调用时替换，避免手工字符串拼接。

### .entity()：结构化输出

`.call().entity(BookSummary.class)`（[StructuredOutputController.java:40](src/main/java/org/aeryzhao/chat/controller/StructuredOutputController.java:40)）让 Spring AI 负责「提示模型按 schema 输出 + 反序列化」。目标类型是 record `BookSummary(String name, String author, String summary, List<String> tags)`（[StructuredOutputController.java:43](src/main/java/org/aeryzhao/chat/controller/StructuredOutputController.java:43)）。

> 提示：字段要求在提示词里写清楚（该接口在 prompt 中显式列出 `name、author、summary、tags`），否则模型可能漏字段导致反序列化失败。

### 记忆：MessageWindowChatMemory + MessageChatMemoryAdvisor

两个记忆控制器的装配方式一致，只有仓库实现不同：

```java
ChatMemory chatMemory = MessageWindowChatMemory.builder()
        .chatMemoryRepository(new InMemoryChatMemoryRepository())  // 自定义仓库时换成 ConcurrentMapChatMemoryRepository
        .maxMessages(20)
        .build();

this.chatClient = ChatClient.builder(chatModel)
        .defaultAdvisors(new SimpleLoggerAdvisor())
        .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
        .build();
```

见 [ChatMemoryController.java:29](src/main/java/org/aeryzhao/chat/controller/ChatMemoryController.java:29) 与 [CustomChatMemoryController.java:36](src/main/java/org/aeryzhao/chat/controller/CustomChatMemoryController.java:36)。

- `MessageWindowChatMemory` 负责**窗口裁剪策略**（只保留最近 20 条）。
- `MessageChatMemoryAdvisor` 负责**在调用前注入历史、调用后写回历史**。
- `conversationId` 由调用方通过顾问参数传入，是「记忆分会话」的唯一标识：

```java
.advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationId))
```

见 [ChatMemoryController.java:47](src/main/java/org/aeryzhao/chat/controller/ChatMemoryController.java:47)。**不传这个参数，历史记忆不会落到你期望的会话里。**

### 自定义 ChatMemoryRepository：只实现 4 个方法

[ConcurrentMapChatMemoryRepository](src/main/java/org/aeryzhao/chat/repository/ConcurrentMapChatMemoryRepository.java:18) 用 `ConcurrentHashMap<String, List<Message>>` 实现 `ChatMemoryRepository` 接口的 4 个方法：

| 方法 | 行为 | 源码 |
| --- | --- | --- |
| `findConversationIds()` | 返回全部会话 ID（自然序） | [:23](src/main/java/org/aeryzhao/chat/repository/ConcurrentMapChatMemoryRepository.java:23) |
| `findByConversationId(String)` | 返回**副本**，避免外部修改内部列表 | [:30](src/main/java/org/aeryzhao/chat/repository/ConcurrentMapChatMemoryRepository.java:30) |
| `saveAll(String, List<Message>)` | 整体覆盖该会话消息 | [:35](src/main/java/org/aeryzhao/chat/repository/ConcurrentMapChatMemoryRepository.java:35) |
| `deleteByConversationId(String)` | 删除该会话 | [:40](src/main/java/org/aeryzhao/chat/repository/ConcurrentMapChatMemoryRepository.java:40) |

它没有加 `@Component`，而是由 `CustomChatMemoryController` 在构造时 `new` 出来（[CustomChatMemoryController.java:35](src/main/java/org/aeryzhao/chat/controller/CustomChatMemoryController.java:35)），并被控制器直接持有，从而支撑 `/memory-size`、`/memory`、`/clear` 这三个排查接口。

> 换成 Redis / JDBC 时，只需再实现同样 4 个方法，其余装配代码不变。

### 工具调用：@Tool + defaultTools

[TravelPlanTools.planTrip](src/main/java/org/aeryzhao/chat/tool/TravelPlanTools.java:16) 用 `@Tool(description = "...")` 描述能力（[:15](src/main/java/org/aeryzhao/chat/tool/TravelPlanTools.java:15)），入参是 record [`TravelPlanRequest(String city, int days, int budget)`](src/main/java/org/aeryzhao/chat/tool/TravelPlanTools.java:38)，返回 [`TravelPlanResult`](src/main/java/org/aeryzhao/chat/tool/TravelPlanTools.java:41)。方法体内是纯本地计算（按天数摊预算、生成行程与提示），不依赖外部服务，方便观察「模型决定调用工具 → 工具返回结构化结果 → 模型组织成自然语言」的完整链路。

注册方式只有一行 `defaultTools`（[ToolCallController.java:30](src/main/java/org/aeryzhao/chat/controller/ToolCallController.java:30)），模型是否真正触发调用取决于 `description` 写得好不好以及用户话术——所以该接口的 `DEFAULT_SYSTEM_PROMPT` 特意要求「优先调用可用工具」，参数则通过模板提示词注入（[ToolCallController.java:41](src/main/java/org/aeryzhao/chat/controller/ToolCallController.java:41)）。

## 模块结构

```
chat/
├── pom.xml                              # 仅依赖 spring-ai-starter-model-openai
└── src/main/
    ├── java/org/aeryzhao/chat/
    │   ├── ChatApplication.java          # 启动入口
    │   ├── controller/                   # 7 个控制器（见上方接口速查）
    │   ├── repository/                   # 自定义 ChatMemoryRepository
    │   └── tool/                         # @Tool 工具定义
    └── resources/application.yaml        # 端口 8096 + springdoc + 模型配置导入
```

## 常见问题

**启动报找不到 `deepseek.api-key`？**
仓库根目录缺少 `local-config.yaml`。执行 `cp local-config.example.yaml local-config.yaml` 并填入真实 API Key。

**`conversationId` 换一个就「失忆」？**
这是预期行为：记忆按 `conversationId` 隔离。对比两次同 ID / 不同 ID 的调用即可验证（见上方 `/ai/chat-memory/in-memory` 的示例）。

**工具调用没被触发？**
先看 `SimpleLoggerAdvisor` 的日志确认模型是否收到工具定义与 `description`，再检查用户话术是否明确给了城市、天数、预算三个参数。

**换了模型厂商？**
只要目标服务兼容 OpenAI 协议，改根目录 `local-config.yaml` 的 `base-url` / `chat-model` 即可，Java 代码无需改动。
