# RAG Project - 检索增强生成系统

## 项目简介

本项目是一个基于 Spring Boot 和 Spring AI 的检索增强生成（Retrieval-Augmented Generation，RAG）系统。系统支持文档的向量化存储、语义搜索和基于检索的智能问答功能。

### 核心功能

- 文档向量化存储：将文本文档转换为向量并存储到 Milvus
- 语义搜索：基于向量相似度进行文档检索
- RAG 问答：结合检索到的文档生成智能回答

### 技术亮点

- 使用 Spring AI 的 `VectorStore` 抽象接口操作向量数据库
- 使用 Spring AI 的 `EmbeddingModel` 自动配置进行文本向量化
- 使用 Spring AI 的 `QuestionAnswerAdvisor` 实现自动 RAG 流程
- 使用 Spring AI 的 `TokenTextSplitter` 进行文档分块

## 技术栈

- **Java 21**
- **Spring Boot 3.4.13**
- **Spring AI 1.1.2** - AI 集成框架
  - `spring-ai-starter-model-ollama` - Ollama Chat + Embedding 模型
  - `spring-ai-starter-vector-store-milvus` - Milvus 向量存储
  - `spring-ai-advisors-vector-store` - QuestionAnswerAdvisor
- **Milvus** - 向量数据库
- **Ollama（本地部署）** - Chat 模型（默认 `gemma4:latest`）+ Embedding 模型（默认 `qwen3-embedding`）
- **Apache PDFBox / POI** - 文档解析（PDF、Word、Excel）
- **Lombok** - 简化代码

## 环境要求

- JDK 21 或更高版本
- Maven 3.6+
- Milvus 2.3+ 向量数据库
- Ollama（本地运行，默认端口 `11434`）

## 快速开始

### 1. 安装 Milvus

使用 Docker 安装 Milvus：

```bash
docker run -d --name milvus-standalone \
  -p 19530:19530 \
  -p 9091:9091 \
  milvusdb/milvus:v2.3.4
```

### 2. 安装并启动 Ollama

1) 安装 Ollama（macOS / Linux / Windows 参考官方文档）

2) 启动 Ollama 服务（默认监听 `http://127.0.0.1:11434`）

3) 拉取项目默认使用的模型：

```bash
# Chat 模型
ollama pull gemma4:latest

# Embedding 模型
ollama pull qwen3-embedding
```

### 3. 启动应用

```bash
# 编译项目
mvn clean package -DskipTests

# 启动应用
java -jar target/rag-0.0.1-SNAPSHOT.jar
```

或使用 Maven 直接运行：

```bash
mvn spring-boot:run
```

应用将在 `http://localhost:8080` 启动。首次启动时，Spring AI 会自动在 Milvus 中创建集合。

## 接口测试页面

项目内置了一个用于手动调试接口的页面，启动应用后可直接访问：

```
http://localhost:8080/api-test.html
```

页面支持以下接口的在线测试：

- `POST /api/documents` - 插入文档
- `GET /api/documents/search` - 语义搜索
- `POST /api/documents/upload` - 上传文档
- `POST /api/documents/upload/batch` - 批量上传
- `POST /api/ask` - RAG 问答

## API 接口文档

### 1. 插入文档

将文档向量化并存储到 Milvus。

```bash
curl -X POST http://localhost:8080/api/documents \
  -H "Content-Type: application/json" \
  -d '{
    "content": "Spring Boot 是一个用于简化 Spring 应用初始搭建以及开发过程的框架。",
    "metadata": {
      "source": "技术文档",
      "category": "Spring"
    }
  }'
```

### 2. 语义搜索

基于查询文本进行语义相似度搜索。

```bash
curl -X GET "http://localhost:8080/api/documents/search?query=什么是Spring%20Boot&topK=5"
```

### 3. 上传文档文件

支持 PDF、Word、Excel、TXT 等格式，自动解析、分块和向量化。

```bash
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@document.pdf" \
  -F "chunkSize=2000"
```

### 4. RAG 问答

基于检索到的文档生成智能回答。

```bash
curl -X POST http://localhost:8080/api/ask \
  -H "Content-Type: application/json" \
  -d '{
    "question": "Spring Boot 有什么特点？",
    "topK": 3
  }'
```

## 项目结构

```
rag/
├── src/main/java/org/aeryzhao/rag/
│   ├── config/
│   │   ├── JacksonConfig.java              # Jackson 序列化配置
│   │   ├── RestClientConfig.java           # RestClient 配置
│   │   └── SwaggerConfig.java              # Swagger API 文档配置
│   ├── controller/
│   │   ├── DocumentController.java         # 文档管理接口
│   │   ├── PageController.java             # 页面跳转
│   │   └── RagController.java              # RAG 问答接口
│   ├── dto/
│   │   ├── DocumentRequest.java            # 文档插入请求
│   │   ├── DocumentResponse.java           # 文档插入响应
│   │   ├── FileUploadResponse.java         # 文件上传响应
│   │   ├── RagRequest.java                 # RAG 问答请求
│   │   ├── RagResponse.java                # RAG 问答响应
│   │   ├── SearchRequest.java              # 搜索请求
│   │   ├── SearchResponse.java             # 搜索响应
│   │   └── SearchResult.java               # 搜索结果项
│   ├── exception/
│   │   ├── ErrorResponse.java              # 错误响应
│   │   └── GlobalExceptionHandler.java     # 全局异常处理
│   ├── service/
│   │   ├── DocumentParserService.java      # 文档解析与分块
│   │   └── RagService.java                 # RAG 问答服务
│   └── RagApplication.java                 # 应用入口
├── src/main/resources/
│   ├── application.yml                     # 应用配置
│   └── static/api-test.html                # 接口测试页面
└── pom.xml
```

## 配置说明

### application.yml 主要配置项

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `server.port` | 应用端口 | 8080 |
| `spring.ai.ollama.base-url` | Ollama 服务地址 | http://127.0.0.1:11434 |
| `spring.ai.ollama.chat.options.model` | Chat 模型 | gemma4:latest |
| `spring.ai.ollama.embedding.options.model` | Embedding 模型 | qwen3-embedding |
| `spring.ai.vectorstore.milvus.uri` | Milvus 地址 | http://localhost:19530 |
| `spring.ai.vectorstore.milvus.collection-name` | 集合名称 | rag_documents |
| `spring.ai.vectorstore.milvus.embedding-dimension` | 向量维度 | 4096 |
| `spring.ai.vectorstore.milvus.metric-type` | 相似度度量 | COSINE |
| `rag.document.chunk-size` | 文档分块大小（字符） | 2000 |

## 架构说明

本项目使用 Spring AI 推荐的 RAG 架构：

```
文档上传 → DocumentParserService（解析 + TokenTextSplitter 分块）
         → VectorStore.add()（自动 embedding + 存储 Milvus）

RAG 问答 → VectorStore.similaritySearch()（检索相关文档）
         → QuestionAnswerAdvisor（自动注入文档到 Prompt）
         → ChatClient（调用 LLM 生成回答）
```

## 注意事项

1. **模型服务**：本项目默认使用本地 Ollama。Chat 模型为 `gemma4:latest`，Embedding 模型为 `qwen3-embedding`。如需更换模型，请同步修改 `application.yml` 中的对应配置。

2. **Milvus 连接**：确保 Milvus 服务已启动并可访问。首次启动时 Spring AI 会自动创建集合（需配置 `initialize-schema: true`）。

3. **向量维度**：`embedding-dimension` 配置必须与 Embedding 模型的实际输出维度一致（`qwen3-embedding` 为 4096）。

4. **性能优化**：
   - `qwen3-embedding` 是 7.6B 参数的大模型，本地推理较慢（约 30 秒/chunk）。如需更快的 embedding 速度，可考虑使用轻量模型如 `nomic-embed-text`（137M 参数）
   - 可通过增大 `chunk-size` 来减少 chunk 数量

5. **错误处理**：所有 API 都有统一的错误响应格式：
   ```json
   {
     "timestamp": "2024-01-01T12:00:00",
     "status": 400,
     "error": "Bad Request",
     "message": "具体错误信息",
     "path": "/api/documents"
   }
   ```

## 许可证

MIT License
