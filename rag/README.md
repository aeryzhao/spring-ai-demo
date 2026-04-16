# RAG Project - 检索增强生成系统

## 项目简介

本项目是一个基于 Spring Boot 和 Milvus 的检索增强生成（Retrieval-Augmented Generation，RAG）系统。系统支持文档的向量化存储、语义搜索和基于检索的智能问答功能。

### 核心功能

- 文档向量化存储：将文本文档转换为向量并存储到 Milvus
- 语义搜索：基于向量相似度进行文档检索
- RAG 问答：结合检索到的文档生成智能回答

## 技术栈

- **Java 21**
- **Spring Boot 3.4.13**
- **Spring AI 1.1.2** - AI 集成框架
- **Milvus 2.3.4** - 向量数据库
- **Ollama（本地部署）** - Chat 模型（默认 `gemma4:latest`）
- **Ollama Embedding** - 向量化模型（默认 `qwen3-embedding`）
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
# 拉取 Milvus 镜像
docker pull milvusdb/milvus:v2.3.4

# 启动 Milvus Standalone
docker run -d --name milvus-standalone \
  -p 19530:19530 \
  -p 9091:9091 \
  milvusdb/milvus:v2.3.4
```

或使用 Docker Compose：

```bash
# 下载 docker-compose.yml
wget https://github.com/milvus-io/milvus/releases/download/v2.3.4/milvus-standalone-docker-compose.yml -O docker-compose.yml

# 启动 Milvus
docker-compose up -d
```

### 2. 安装并启动 Ollama

1) 安装 Ollama（macOS / Linux / Windows 参考官方文档）

2) 启动 Ollama 服务（默认监听 `http://127.0.0.1:11434`）

3) 拉取项目默认使用的模型：

```bash
# Chat 模型
ollama pull gemma4:latest

# Embedding 模型（与 application.yml 保持一致）
ollama pull qwen3-embedding
```

### 3. 配置环境变量

创建环境变量或直接修改 `application.yml`：

```bash
# Ollama 配置（Chat + Embedding 共用）
export OLLAMA_BASE_URL=http://127.0.0.1:11434
export OLLAMA_CHAT_MODEL=gemma4:latest

# Milvus 配置
export MILVUS_HOST=localhost
export MILVUS_PORT=19530
export MILVUS_DATABASE=default
export MILVUS_COLLECTION_NAME=rag_documents
```

### 4. 启动应用

```bash
# 编译项目
mvn clean package -DskipTests

# 启动应用
java -jar target/rag-project-1.0.0-SNAPSHOT.jar
```

或使用 Maven 直接运行：

```bash
mvn spring-boot:run
```

应用将在 `http://localhost:8080` 启动。

## 接口测试页面

项目内置了一个用于手动调试接口的页面，启动应用后可直接访问：

```text
http://localhost:8080/api-test.html
```

页面支持以下接口的在线测试：

- `POST /api/documents`
- `GET /api/documents/search`
- `POST /api/documents/upload`
- `POST /api/documents/upload/batch`
- `POST /api/ask`

如果要测试其他环境，可以在页面顶部 `Base URL` 中填写目标地址（例如 `http://127.0.0.1:8080`）。

## API 接口文档

### 1. 插入文档

将文档向量化并存储到 Milvus。

**请求**

```http
POST /api/documents
Content-Type: application/json
```

**请求体**

```json
{
  "content": "Spring Boot 是一个用于简化 Spring 应用初始搭建以及开发过程的框架。",
  "metadata": {
    "source": "文档来源",
    "category": "技术文档"
  }
}
```

**响应**

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "message": "Document inserted successfully"
}
```

**curl 示例**

```bash
curl -X POST http://localhost:8080/api/documents \
  -H "Content-Type: application/json" \
  -d '{
    "content": "Spring Boot 是一个用于简化 Spring 应用初始搭建以及开发过程的框架。它提供了自动配置、起步依赖等特性。",
    "metadata": {
      "source": "技术文档",
      "category": "Spring"
    }
  }'
```

### 2. 语义搜索

基于查询文本进行语义相似度搜索。

**请求**

```http
GET /api/documents/search?query={query}&topK={topK}
```

**参数**

- `query` (必填): 查询文本
- `topK` (可选): 返回结果数量，默认 5，范围 1-100

**响应**

```json
{
  "results": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "content": "Spring Boot 是一个用于简化 Spring 应用初始搭建以及开发过程的框架。",
      "metadata": {
        "source": "技术文档",
        "category": "Spring"
      },
      "score": 0.95
    }
  ]
}
```

**curl 示例**

```bash
curl -X GET "http://localhost:8080/api/documents/search?query=什么是Spring%20Boot&topK=5"
```

### 3. RAG 问答

基于检索到的文档生成智能回答。

**请求**

```http
POST /api/ask
Content-Type: application/json
```

**请求体**

```json
{
  "question": "Spring Boot 有什么特点？",
  "topK": 3
}
```

**响应**

```json
{
  "answer": "Spring Boot 是一个用于简化 Spring 应用开发的框架，主要特点包括：\n1. 自动配置：根据项目依赖自动配置 Spring 应用\n2. 起步依赖：简化依赖管理\n3. 内嵌服务器：无需部署 WAR 文件\n4. 生产就绪：提供健康检查、指标监控等功能",
  "sources": [
    "[文档ID: 550e8400-e29b-41d4-a716-446655440000]\nSpring Boot 是一个用于简化 Spring 应用初始搭建以及开发过程的框架。"
  ]
}
```

**curl 示例**

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
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── rag/
│       │           ├── config/              # 配置类
│       │           │   ├── JacksonConfig.java      # Jackson 序列化配置
│       │           │   └── MilvusConfig.java       # Milvus 客户端配置
│       │           ├── controller/          # 控制器层
│       │           │   ├── DocumentController.java # 文档管理接口
│       │           │   └── RagController.java      # RAG 问答接口
│       │           ├── dto/                 # 数据传输对象
│       │           │   ├── DocumentRequest.java
│       │           │   ├── DocumentResponse.java
│       │           │   ├── RagRequest.java
│       │           │   ├── RagResponse.java
│       │           │   ├── SearchRequest.java
│       │           │   ├── SearchResponse.java
│       │           │   └── SearchResult.java
│       │           ├── entity/              # 实体类
│       │           │   └── Document.java
│       │           ├── exception/           # 异常处理
│       │           │   ├── ErrorResponse.java
│       │           │   └── GlobalExceptionHandler.java
│       │           ├── milvus/              # Milvus 相关
│       │           │   ├── CollectionInitializer.java    # 集合初始化
│       │           │   └── MilvusCollectionManager.java  # 集合管理
│       │           ├── service/             # 服务层
│       │           │   ├── DocumentService.java   # 文档服务
│       │           │   ├── EmbeddingService.java  # 向量化服务
│       │           │   └── RagService.java        # RAG 服务
│       │           └── RagApplication.java  # 应用入口
│       └── resources/
│           └── application.yml             # 应用配置
└── pom.xml                                 # Maven 配置
```

## 配置说明

### application.yml 主要配置项

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `server.port` | 应用端口 | 8080 |
| `spring.ai.ollama.base-url` | Ollama 服务地址 | http://127.0.0.1:11434 |
| `spring.ai.ollama.chat.options.model` | Chat 模型 | gemma4:latest |
| `milvus.host` | Milvus 主机地址 | localhost |
| `milvus.port` | Milvus 端口 | 19530 |
| `rag.embedding.model-type` | 嵌入模型类型 | ollama |
| `rag.embedding.ollama.model` | Ollama Embedding 模型 | qwen3-embedding |
| `rag.vector.dimension` | 向量维度 | 4096 |
| `rag.vector.collection-name` | 集合名称 | rag_documents |

## 注意事项

1. **模型服务**：本项目默认使用本地 Ollama：Chat 模型为 `gemma4:latest`，Embedding 模型为 `qwen3-embedding`。如需更换模型，请同步修改 `application.yml` 中的对应配置。

2. **Milvus 连接**：确保 Milvus 服务已启动并可访问，应用启动时会自动创建所需的 Collection。

3. **向量维度**：默认向量维度为 4096（与当前 Embedding 模型配置保持一致）。如更换 Embedding 模型，需同步修改 `rag.vector.dimension` 配置。

4. **性能优化**：
   - 生产环境建议调整 Milvus 的索引参数（`nlist`）
   - 可根据数据规模选择合适的索引类型（IVF_FLAT、IVF_SQ8、HNSW 等）

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
