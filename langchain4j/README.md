# LangChain4j 示例模块

本模块是 [`spring-ai-demo`](../README.md) 的子模块，用 **LangChain4j 1.20.0** 实现与 `chat` 模块对应的一整套示例，
方便和 Spring AI 做对照学习。

> 一句话区别：**Spring AI 是 Spring 生态的一部分，LangChain4j 是一个独立的 Java AI 框架。**
> 前者靠 starter 自动装配、`ChatClient` 链式调用；后者靠手写 builder、`AiServices` 声明式接口。

## 目录

- [快速开始](#快速开始)
- [与 Spring AI 的对照表](#与-spring-ai-的对照表)
- [示例清单](#示例清单)
- [为什么需要 settings.xml](#为什么需要-settingsxml)
- [常见问题](#常见问题)

## 快速开始

```bash
# 1. 确保根目录 local-config.yaml 中的 deepseek.api-key 有效
# 2. 在 langchain4j 目录下启动（注意必须带 -s settings.xml，原因见下文）
cd langchain4j
mvn -s settings.xml spring-boot:run
```

启动后：

- 示例导航：<http://localhost:8097/lc4j>
- Swagger UI：<http://localhost:8097/swagger-ui.html>
- 流式体验：`curl -N "http://localhost:8097/lc4j/stream?message=写一首五言绝句"`

### 切换到本地 Ollama（无需 API Key）

```bash
mvn -s settings.xml spring-boot:run \
  -Dspring-boot.run.arguments="--langchain4j.provider=ollama \
    --langchain4j.base-url=http://127.0.0.1:11434 \
    --langchain4j.chat-model=qwen2.5:7b \
    --langchain4j.embedding-model=qwen3-embedding"
```

`provider` 支持 `openai`（任何 OpenAI 兼容接口）与 `ollama` 两种取值，业务代码零改动。

## 与 Spring AI 的对照表

| 能力 | Spring AI | LangChain4j |
| --- | --- | --- |
| 依赖 | `spring-ai-starter-model-openai`（自动装配） | `langchain4j-open-ai`（手动 new builder） |
| 对话入口 | `ChatClient` / `ChatModel` | `ChatModel` 接口 |
| 流式 | `.stream().content()` 返回 `Flux<String>` | 独立的 `StreamingChatModel` + 回调处理器 |
| 声明式接口 | 无（用 `ChatClient` 链式拼装） | **`AiServices`**：只写接口，动态代理生成实现 |
| 提示词模板 | `.param("k", v)` | `@SystemMessage` / `@UserMessage` + `@V` |
| 结构化输出 | `.entity(Class)` | 方法直接返回 POJO，或返回 `Result<T>` |
| 聊天记忆 | `ChatMemory` + `ChatMemoryRepository` | `ChatMemory` + `ChatMemoryStore` + `ChatMemoryProvider` |
| 多会话隔离 | 通过 `conversationId` 参数 | **`@MemoryId`** 注解参数 |
| 工具调用 | `@Tool` + `@ToolParam`，`defaultTools(...)` 注册 | `@Tool` + `@P`，`AiServices.tools(...)` 注册 |
| RAG | `QuestionAnswerAdvisor` 一句话接入 | 手动串 `Splitter → EmbeddingModel → EmbeddingStore` |
| 异常体系 | Spring 的异常 | 统一的 `dev.langchain4j.exception.*` |

## 示例清单

| # | 主题 | 入口类 | 接口 |
| --- | --- | --- | --- |
| 1 | ChatModel 基础对话 | [`ChatModelController`](src/main/java/org/aeryzhao/langchain4j/controller/ChatModelController.java) | `GET /lc4j/chat/simple`、`/detailed`、`/multi-turn` |
| 2 | 流式输出（SSE） | [`StreamingChatController`](src/main/java/org/aeryzhao/langchain4j/controller/StreamingChatController.java) | `GET /lc4j/stream` |
| 3 | AiServices 声明式接口 | [`AiServicesController`](src/main/java/org/aeryzhao/langchain4j/controller/AiServicesController.java) | `GET /lc4j/ai-services/chat`、`/java-expert`、`/translate` |
| 4 | 结构化输出 | [`AiServicesController`](src/main/java/org/aeryzhao/langchain4j/controller/AiServicesController.java) | `GET /lc4j/ai-services/extract-person`、`/review-code` |
| 5 | 聊天记忆（多会话隔离） | [`ChatMemoryController`](src/main/java/org/aeryzhao/langchain4j/controller/ChatMemoryController.java) | `GET /lc4j/memory/chat`、`/messages`、`DELETE /clear` |
| 6 | 工具调用 | [`ToolCallController`](src/main/java/org/aeryzhao/langchain4j/controller/ToolCallController.java) | `GET /lc4j/tools/plan` |
| 7 | RAG 检索增强生成 | [`RagController`](src/main/java/org/aeryzhao/langchain4j/controller/RagController.java) | `GET /lc4j/rag/search`、`/ask`、`POST /documents` |

核心装配代码集中在两个类：

- [`ModelConfig`](src/main/java/org/aeryzhao/langchain4j/config/ModelConfig.java)：创建 `ChatModel` / `StreamingChatModel` / `EmbeddingModel`
- [`AiServicesConfig`](src/main/java/org/aeryzhao/langchain4j/config/AiServicesConfig.java)：把助手接口 + 记忆 + 工具组装成 Bean

## 为什么需要 settings.xml

本模块自带一份 [`settings.xml`](settings.xml)，构建时请用 `mvn -s settings.xml ...`。原因是当前开发机上：

1. 全局 `settings.xml` 把 `localRepository` 指向 `/Users/codance/repo/mvn`，该路径在沙箱工作区之外，无法写入新下载的依赖；
2. 全局配置里若包含不可达的**内网 Nexus 仓库**，每个依赖都要先等它超时，构建会变得极慢。

模块内的 `settings.xml` 把依赖缓存改到 `langchain4j/.m2-repo/`（已 gitignore），并只保留可达的镜像。

> 换到你自己的机器或 CI 时，如果全局 Maven 配置正常，**可以直接删掉这个文件**，用常规的 `mvn compile` 即可。

## 常见问题

**Q：启动报 `INVALID_API_KEY` / `AuthenticationException`？**
A：`local-config.yaml` 里的 `deepseek.api-key` 无效或已过期。
本模块对这类错误做了统一处理，接口会返回可读的 JSON 提示而不是一堆堆栈：

```json
{"success":false,"error":"模型鉴权失败","hint":"请检查项目根目录 local-config.yaml 中的 deepseek.api-key 是否有效、是否已过期。"}
```

**Q：为什么 RAG 接口报错，但其它示例正常？**
A：RAG 需要 Embedding 模型（本项目配置为 `text-embedding-v3`）。如果网关没有该模型，可以把
`langchain4j.embedding-model` 换成网关支持的向量模型，或切到本地 Ollama 的 `qwen3-embedding`。
知识库采用**懒加载**，所以 Embedding 不可用不会影响应用启动，也不会拖垮其它示例。

**Q：AiServices 接口的方法参数名能直接用吗？**
A：本模块 POM 已开启 `-parameters` 编译参数，理论上可用；但仍**推荐用 `@V("name")` 显式绑定**，
这样不依赖编译配置，更稳妥。
