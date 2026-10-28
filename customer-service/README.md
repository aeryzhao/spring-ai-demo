# customer-service：智能客服案例（网页抓取 + RAG + 工具调用）

一个可以真正跑起来的电商智能客服 Demo：**数据自己从公开网页抓**，落到本地知识库，
再通过「RAG 检索 + 大模型 + 业务工具调用 + 多轮记忆」回答用户的订单、物流、售后问题。

- 知识库内容：从京东帮助中心（公开页面）与中国政府网抓取的 25 篇客服/政策文档（约 12 万字 → 48 个检索片段）
- 业务数据：本地模拟的订单 / 物流（`data/orders.json`、`data/logistics.json`），由工具实时查询，模型无法编造
- 交互：浏览器聊天页（`/`）+ 同步接口 + SSE 流式接口

---

## 1. 能力一览

| 能力 | 实现位置 | 说明 |
| --- | --- | --- |
| 网页抓取入库 | [`KnowledgeCrawler`](src/main/java/org/aeryzhao/customerservice/crawl/KnowledgeCrawler.java:1) | Jsoup 抽取正文 → Markdown（带 front matter）落盘 `data/knowledge/*.md` |
| 知识切块 | [`DocumentChunker`](src/main/java/org/aeryzhao/customerservice/knowledge/DocumentChunker.java:1) | 按段落装箱，块间保留重叠，避免答案被切断 |
| 向量检索 | [`KnowledgeIndexService`](src/main/java/org/aeryzhao/customerservice/knowledge/KnowledgeIndexService.java:1) | `SimpleVectorStore` + Ollama `qwen3-embedding`，索引缓存到 `data/vector-store.json` |
| 检索降级 | 同上 | Ollama 不可用时自动切到中文二元组关键词检索，功能不中断 |
| 客服应答 | [`CustomerServiceAgent`](src/main/java/org/aeryzhao/customerservice/service/CustomerServiceAgent.java:1) | 人设提示词 + 编号参考资料 + 工具 + 记忆；答案角标 `[1]` 对应引用来源 |
| 业务工具 | [`CustomerServiceTools`](src/main/java/org/aeryzhao/customerservice/tool/CustomerServiceTools.java:1) | 订单/物流/售后资格/提交退货/转人工/查工单 |
| 售后规则 | [`AfterSalePolicyService`](src/main/java/org/aeryzhao/customerservice/service/AfterSalePolicyService.java:1) | 把抓来的政策变成可执行判定：7 天无理由、15 天换货、特殊品类排除 |
| 多轮记忆 | [`AiConfig`](src/main/java/org/aeryzhao/customerservice/config/AiConfig.java:1) | `MessageWindowChatMemory`，按 `sessionId` 隔离 |
| 流式输出 | [`ChatController`](src/main/java/org/aeryzhao/customerservice/controller/ChatController.java:1) | SSE 三类事件：`meta` / `token` / `done` |
| 聊天页面 | [`static/index.html`](src/main/resources/static/index.html:1) | 无前端构建，原生 `EventSource`，展示引用与工具轨迹 |
| 接口面板 | 同上 | 页面右上「接口」按钮：列出全部 REST 接口，点条目即在线调用并展示原始响应（知识库/订单/物流/售后/工单） |

---

## 2. 数据从哪来（可以复现）

抓取源清单在 [`resources/crawl/sources.json`](src/main/resources/crawl/sources.json:1)，全部是**公开可访问、服务端直出 HTML** 的页面：

- **京东帮助中心移动版**（24 篇）：购物指南、配送方式（211 限时达/自提/运费标准）、支付方式（在线支付/货到付款/扫码支付）、
  发票、售后服务（自营/第三方/退换货政策）、常见问题、京豆/优惠券/会员、账户注销
- **中国政府网**（1 篇）：《网络购买商品七日无理由退货暂行办法》原文 —— 用于给「政策类回答」提供权威依据

落地格式（可直接 `cat` 阅读，也方便人工校对）：

```markdown
---
id: jd-tuihuanzhengce
title: "我的商品是否可以退换货？"
category: "售后服务"
source: https://in.m.jd.com/help/app/tuihuanzhengce.html
fetchedAt: 2026-10-25T17:05:10.614515+08:00
---

# 我的商品是否可以退换货？

京东承诺符合以下情况，自客户收到商品之日起7日内可以退货，15日内可以换货……
```

### 2.1 抓取命令

```bash
cd customer-service
# 增量抓取：本地已有的文档跳过
mvn -pl customer-service -am spring-boot:run -Dspring-boot.run.profiles=crawl
# 全量覆盖
java -jar target/customer-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=crawl --force
```

也可以服务起来后调 `POST /api/knowledge/crawl?force=true`（会顺带重建索引）。
抓取有 800ms 间隔限速，25 篇约 20 秒。

### 2.2 抓取实现里的几个坑（已处理，供参考）

1. **证书链**：`gov.cn` 的证书链不在 JDK 默认信任库，Jsoup 直接抛 `PKIX path building failed`。
   现在主链路用 Jsoup 取页面，只有命中 `SSLException` 时才按 `customer-service.crawl.lenient-tls=true`
   改用宽松 TLS 的 `HttpClient` 兜底（每个站点的兜底原因会打 WARN 日志）。
2. **客户端指纹**：京东对 Java `HttpClient` 会返回精简版页面（无标题、无正文容器），而 Jsoup（`HttpURLConnection`）拿得到完整页。
   所以**不要**把主链路换成 `HttpClient`。
3. **标题**：直接用 `<title>`，去掉「帮助中心」这类通用标题、去掉 `5.1` 这类章节编号、去掉 `_中国政府网` 后缀，
   正文里重复出现的同名标题会被剔除。

> 说明：抓取内容仅用于本地学习演示，版权归原站点所有；脚本已做限速，请勿高频重复抓取。

---

## 3. 请求链路

```
浏览器 / curl
   │  POST /api/chat            GET /api/chat/stream(SSE)
   ▼
ChatController
   ▼
CustomerServiceAgent.prepare()
   ├── KnowledgeIndexService.search(question, topK)
   │      ├── VECTOR：SimpleVectorStore(Ollama qwen3-embedding)  ← data/vector-store.json 缓存
   │      └── KEYWORD：中文二元组覆盖度打分（Ollama 不可用时兜底）
   └── 系统提示词 + 【参考资料】[1][2]… 拼装
   ▼
ChatClient
   ├── MessageChatMemoryAdvisor（按 sessionId 带上下文）
   └── CustomerServiceTools（模型自主决定是否调用）
          ├── OrderService       订单 / 物流（本地模拟数据）
          ├── AfterSalePolicyService  售后资格判定（抓到的政策 → 规则）
          └── TicketService      售后单 / 转人工工单（落盘 data/tickets.json）
   ▼
回答（带 [1] 角标）+ sources + toolCalls
```

---

## 4. 快速开始

前置：JDK 21、Maven 3.9+、可用的 DeepSeek API Key、本机 Ollama（可选，装了才有向量检索）。

1. **配置模型**：项目根目录的 `local-config.yaml`（参见 `local-config.example.yaml`）

   ```yaml
   deepseek:
     api-key: sk-xxx
     base-url: https://api.deepseek.com
     chat-model: deepseek-chat
   ollama:
     base-url: http://127.0.0.1:11434
     embedding-model: qwen3-embedding
   ```

2. **准备向量模型**（可选，不装则自动降级为关键词检索）：

   ```bash
   ollama pull qwen3-embedding
   ```

3. **启动服务**（默认 8098）：

   ```bash
   mvn -pl customer-service spring-boot:run
   # 或在 IDE 里直接运行 CustomerServiceApplication
   ```

   首次启动会在后台建索引（25 篇 → 48 片段，Ollama 约 90 秒），日志出现
   `知识库索引完成：25 篇文档 / 48 个片段，模式 VECTOR` 即可。之后启动直接读缓存。

4. **打开聊天页**：<http://localhost:8098/> ，或调试接口：<http://localhost:8098/swagger-ui/index.html>

   页面右上角「接口」按钮会展开 REST 接口面板：左侧是接口清单（GET/POST + 路径 + 说明），
   点任意条目即发起真实调用，右侧显示状态码/耗时/格式化后的响应；面板里还会直接列出
   知识文档、订单（可一键查物流与售后资格）、工单，方便对照聊天回答验证后端数据。

> 仓库里已经带了抓好的 `data/knowledge/*.md`，所以**不联网也能演示**（抓取只是为了可复现）。

### 4.1 数据目录怎么定位

`customer-service.data-dir` 默认 `./data`。为了避免「从仓库根启动（IDE 默认工作目录）时读到 `<repo>/data` 而不是模块自己的
`customer-service/data`」，相对路径会按下面顺序自动定位（见 [`DataDirectoryResolver`](src/main/java/org/aeryzhao/customerservice/config/DataDirectoryResolver.java:1)）：

1. 当前工作目录下的 `data-dir`
2. **模块根目录**下的 `data-dir`（模块根由启动类 code source 推断，`target/classes` 与 `target/*.jar` 都指向 `customer-service/`）
3. 当前工作目录下 `customer-service/data-dir`

启动日志会打印最终结果，例如：

```
o.a.c.config.DataDirectoryResolver : 数据目录：/Users/xxx/spring-ai-demo/customer-service/data（工作目录：/Users/xxx/spring-ai-demo）
```

`GET /api/knowledge/stats` 也会返回 `dataDir` / `knowledgeDir` / `vectorStoreFile` 三个绝对路径，排查路径问题看这里最快。

---

## 5. 接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/chat` | 同步问答，返回 `answer` / `sources` / `toolCalls` / `retrievalMode` |
| GET | `/api/chat/stream?message=&sessionId=` | SSE 流式问答（`meta` → `token`* → `done`） |
| GET | `/api/knowledge/documents` | 知识文档清单（不含正文） |
| GET | `/api/knowledge/stats` | 检索模式、文档/片段数、索引时间 |
| POST | `/api/knowledge/crawl?force=` | 重新抓取并重建索引 |
| POST | `/api/knowledge/reindex` | 只重建索引 |
| GET | `/api/orders`、`/api/orders/{orderNo}` | 模拟订单查询 |
| GET | `/api/orders/{orderNo}/logistics` | 物流轨迹 |
| GET | `/api/orders/{orderNo}/after-sale-eligibility` | 售后资格判定结果 |
| GET | `/api/tickets`、`/api/tickets/{ticketNo}` | 售后/转人工工单 |

> 页面右上「接口」按钮里同样可以调用下表所有接口（不必开 Swagger，也不必写 curl），
> 响应支持状态码、耗时与 JSON 美化；`POST /api/knowledge/reindex`、`crawl` 这类耗时操作
> 会禁用按钮并显示「重建中…/抓取中…」。

示例：

```bash
curl -s -X POST http://localhost:8098/api/chat \
  -H 'Content-Type: application/json' \
  -d '{"sessionId":"demo-1","message":"我的订单 JD202610120001 还能七天无理由退货吗？"}'
```

返回（节选）：

```json
{
  "answer": "**可以退**：这单是 10 月 23 日签收的，现在还在 7 天无理由期内，最后期限是 **2026-10-30**……[1][2]",
  "retrievalMode": "VECTOR",
  "sources": [{"title": "京东自营商品售后", "source": "https://in.m.jd.com/help/app/ziyingshouhou.html", "score": 0.692}],
  "toolCalls": ["查询订单 JD202610120001", "判定售后资格 JD202610120001"]
}
```

---

## 6. 演示脚本（建议按顺序问）

模拟数据里准备了覆盖各种分支的订单（见 `data/orders.json`）：

| 订单号 | 场景 | 预期表现 |
| --- | --- | --- |
| `JD202610120001` | 手机，签收 2 天 | 可七天无理由退货，截止日期按签收日 +7 天算 |
| `JD202610090002` | 净化器，签收 10 天 | 超 7 天无理由，提示仍在 15 天换货期（需质量问题） |
| `JD202609280003` | 大闸蟹（生鲜），签收 20 天 | 品类排除 + 超期，引导转人工评估 |
| `JD202610240004` | 吹风机，已发货未签收 | 不能退货，可取消/拒收；物流轨迹可查 |
| `JD202610200007` | 内衣，签收 3 天 | 品类排除（贴身用品），说明质量问题另走换货 |
| `JD202610250005` | 图书，待发货 | 直接取消订单即可 |

可以这样聊：

1. `我的订单 JD202610120001 还能七天无理由退货吗？` → 工具查订单 + 政策引用
2. `那这单的物流到哪了？` → 记忆生效，自动复用上一轮订单号
3. `配送费怎么收？自提要钱吗？` → 纯知识库回答（带 `[1]` 角标）
4. `我要退货，订单 JD202610120001，原因是屏幕有坏点` → 自动建售后单，返回工单号
5. `我要投诉，请转人工` → 建人工工单（`GET /api/tickets` 可查）
6. `你们公司明天股价怎么样？` → 知识库没有的内容会坦白并转人工，不瞎编

---

## 7. 关键设计说明

### 7.1 为什么检索结果要编号，而不是塞进 user message

参考资料拼在 **system** 里（`CustomerServiceAgent.renderContext`），好处是：记忆里只留用户原话与回答，
不会把几百字的资料反复带进下一轮上下文；同时模型能按 `[1]` 角标指出依据，前端也能展示来源。

### 7.2 政策「文」到规则「码」

抓来的政策是自然语言，模型只负责解释，**能不能退的判定交给代码**：
`AfterSalePolicyService.judge()` 依据签收时间、订单状态、商品品类给出 `SUPPORTED / EXPIRED / EXCLUDED_CATEGORY / NOT_DELIVERED / CANCELLED`，
工具把这套结论喂给模型。这样即使模型漂移，也不会出现「系统说能退、实际超期」的幻觉。

### 7.3 检索降级

`KnowledgeIndexService` 有两种模式：

- `VECTOR`：`SimpleVectorStore`（内存 + JSON 落盘），embedding 来自 Ollama，语义召回更好
- `KEYWORD`： Ollama 没起/模型没拉时自动降级，中文按二元组、英文按词做覆盖度打分

`GET /api/knowledge/stats` 会显示当前模式；发生降级时 `lastError` 给出原因。

### 7.4 SSE 事件协议

```
event:meta   data:{"sessionId":"...","retrievalMode":"VECTOR","sources":[...]}
event:token  data:逐段增量文本
event:done   data:{"toolCalls":["查询订单 JD...","判定售后资格 JD..."]}
event:error  data:{"message":"..."}
```

### 7.5 Mock 与真实业务的分界线

`OrderService` 只依赖 `data/*.json`，真实项目里把这一层换成订单中心/物流网关调用即可，
prompt、工具 schema、售后规则都不需要改。

---

## 8. 测试

```bash
mvn -pl customer-service test
```

16 个用例，均不依赖网络与大模型：

- `DocumentChunkerTest`：切块长度、重叠、空文本
- `KnowledgeStoreTest`：front matter 解析与兜底
- `OrderServiceTest`：用真实的 `data/*.json` 验证订单/物流查询
- `AfterSalePolicyServiceTest`：可退、超期、品类排除、未签收、已取消、订单不存在

> 本机若因内网镜像缺少 surefire 依赖导致 `mvn test` 失败，可加 `-Dmaven-surefire-plugin.version=3.5.4`。

---

## 9. 常见问题

| 现象 | 原因与处理 |
| --- | --- |
| 启动日志告警「embedding 服务不可用，已降级为关键词检索」 | Ollama 没启动或没拉 `qwen3-embedding`；`ollama pull qwen3-embedding` 后调 `POST /api/knowledge/reindex` |
| `/api/knowledge/stats` 里 `documents=0` | `data/knowledge` 为空，先执行抓取 |
| 启动告警「知识库目录不存在：…/spring-ai-demo/data/knowledge」 | 数据目录解析到了仓库根。已修复为自动定位模块目录；若仍出现，先看启动日志的「数据目录：」或 `GET /api/knowledge/stats` 的 `dataDir`，必要时用绝对路径配置 `customer-service.data-dir` |
| 抓取报 `PKIX path building failed` | 目标站点证书链不在 JDK 信任库；把 `customer-service.crawl.lenient-tls` 设为 `true`（默认已开） |
| 抓取某页「正文为空」 | 页面改版或正文是图片流程图；在 `sources.json` 里调整或删除该源 |
| 回答里没有角标 | 该问题没命中知识库（如闲聊），模型应转人工；可调大 `customer-service.top-k` |
| 想换知识库 | 改 `sources.json`（可加自定义站点与抽取器）→ `POST /api/knowledge/crawl?force=true` |
