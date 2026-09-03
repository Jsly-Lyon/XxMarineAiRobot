# 瀚海知问 AI（XxMarineAiRobot）

面向**海洋科学领域**的智能对话平台：基于 Spring AI 构建，深度融合 RAG 混合检索、多层记忆管理与意图识别路由，打通外部公开海洋知识与内部课题数据的语义关联，帮助科研团队提升知识检索效率、降低信息整合成本、提高研究成果复用率。

## 背景与解决的问题

海洋课题推进过程中普遍存在**知识检索效率低、信息整合难、研究成果复用不足**等痛点：

- 传统关键词搜索难以理解自然语言语义，检索「海洋环流对渔业的影响」这类问题召回质量差；
- 通用大模型缺乏海洋领域专业知识，且无法访问内部课题数据，问答准确性与时效性不足；
- 检索、问答、记忆彼此割裂，研究过程中的结论与素材难以沉淀复用。

本平台以「检索增强问答」为核心：公开领域知识走 **RAG 混合检索**，内部课题数据与用户画像走 **多层记忆**，并对会话数据与文档库按登录用户做严格隔离。

## 主要特性

- 🧭 **意图识别与路由**：对话请求经 Advisor 链进行上下文注入与领域路由，配合联网搜索开关选择检索来源。
- 📚 **文档知识库 + RAG**：支持 **Markdown / TXT / Word / PPT / PDF / HTML** 多格式上传，系统内置固定文档在启动时自动向量化；上传后进入“待向量化 → 向量化中 → 完成/失败”状态机（内容哈希幂等，重复上传覆盖不堆量）。
- 🔒 **文档检索严格隔离**：入库向量统一携带 `ownerUserId`，检索仅命中 **系统内置文档（owner=0）或当前登录用户本人上传** 的文档，杜绝跨用户泄露。
- 🧠 **多层记忆管理**：短期消息窗口、结构化「工作区」记忆（目标/事实/假设），以及用户固定槽记忆（偏好/语言/规则/禁忌），AI 回复开头的结构化记忆块会被剥离并以 JSON 落库。
- ⚡ **SSE 流式对话**：基于 `@microsoft/fetch-event-source` 逐块渲染，支持代码高亮 / 一键复制 / 下载。
- 🔐 **登录与数据隔离**：登录/注册以弹窗形式集成在页面内；Sa-Token 全量接口鉴权；用户信息缓存 Redis；会话按 `user_id` 归属，非本人 uuid 一律视为“会话不存在”。
- 🗂️ **对话与文档管理**：聊天历史分页上翻加载；侧边栏历史会话分页加载、**删除（二次确认）/重命名摘要**；预留「下载记录」入口。
- 🎨 **产品化前端**：ChatGPT 式居中对话、模型/联网开关、明暗主题（右上角切换，暗色为 Islands Dark 风格，跟随系统并本地持久化）、Pinia 全局状态、输入框自动增高与回车发送、空态引导等。

## 仓库结构

```
├── project
│   ├── xinxin-ai-robot-springboot   # 后端：Spring AI + Sa-Token + MyBatis-Plus + Redis + Milvus
│   │   └── src/main/resources/doucument/MarineScience   # 系统内置固定文档（markdown/txt/word/ppt/pdf，启动自动向量化）
│   └── xinxin-ai-robot-vue3         # 前端：Vue3 + Vite + Tailwind + Ant Design Vue + Pinia
└── examples                         # 配套示例工程（RAG / 向量检索 / MCP 等，需自备配置）
```

## 技术栈

| 端 | 技术 |
| --- | --- |
| 后端 | Java 21、Spring Boot（Spring AI 2.0）、Sa-Token、MyBatis-Plus、PostgreSQL、Redis、Milvus、Ollama(BGE-M3) |
| 文档解析 | Spring AI MarkdownReader、Jsoup(HTML)、Apache PDFBox(PDF)、Apache Tika(Word/PPT 兜底) |
| 前端 | Vue 3、Vite、Tailwind CSS、Ant Design Vue、Pinia、@microsoft/fetch-event-source、highlight.js、markdown-it |
| 外部服务 | OpenAI 兼容模型网关（可替换 DeepSeek 等）、SearXNG（联网搜索） |

## 快速开始

> 配置涉及模型 API Key / 数据库 / Redis 口令，**均不入库**（见「安全」章节）。

### 1. 后端

前置：JDK 21、PostgreSQL、Redis、可访问的 OpenAI 兼容模型网关（可选 SearXNG）。

```bash
cd project/xinxin-ai-robot-springboot

# ① 准备本地配置（示例含占位符，真实密钥绝不提交）
cp src/main/resources/application-dev.example.yml src/main/resources/application-dev.yml
# ② 编辑 application-dev.yml，填写模型网关、数据库、Redis 连接，
#    并按需调整 customer-service.md-storage-path（用户上传落盘目录）与 seed-docs-pattern
# ③ 首次运行前执行一次表结构迁移（已在 t_chat 增加 user_id 归属列）
psql -U postgres -d robot -f src/main/resources/sql/V1__chat_user_ownership.sql

# ④ （可选）在 src/main/resources/doucument/MarineScience/ 下的 markdown/txt/word/ppt/pdf 放系统内置固定文档
#    启动时会自动读取并向量化到 Milvus（owner=0，所有登录用户可检索）

# ⑤ 启动
mvn spring-boot:run
```

后端默认端口 `8080`；需保证 Ollama（BGE-M3）与 Milvus 可访问，文档上传/内置向量化依赖它们。

### 2. 前端

前置：Node.js ≥ 22。

```bash
cd project/xinxin-ai-robot-vue3
npm install
npm run dev
```

前端开发服务器把 `/api` 代理到 `http://localhost:8080`。浏览器访问 `http://localhost:5173`（或终端提示的端口）。

### 3. 体验流程

1. 首次进入可点击侧边栏底部「登录 / 注册」或「开启新对话」弹出登录窗；注册成功自动登录。
2. 在首页输入问题并发送，系统新建会话并跳转到对话页，AI 以 SSE 流式回复。
3. 左侧「历史对话」仅显示**当前登录账号**的会话；支持删除/重命名；退出登录后自动回到首页并隐藏历史。
4. 上传课题文档（Markdown/Word/PPT/PDF/HTML 等）到个人文档库，客服问答即可在**系统内置知识 + 本人文档**范围内回答（严格隔离）。
5. 右上角按钮可切换明暗主题；会话切换模型 / 联网搜索后发送即可。

> 上传示例：
> ```bash
> curl -X POST http://localhost:8080/api/customer-service/document/upload \
>   -H "Authorization: Bearer <token>" \
>   -F "file=@./marine-knowledge.docx"
> ```

## 接口一览（均以 `/api` 为前缀，经代理转发）

| 方法 | 路径 | 鉴权 | 说明 |
| --- | --- | --- | --- |
| POST | `/auth/login` | 公开 | 登录，返回 `{token}` |
| POST | `/auth/register` | 公开 | 注册 |
| POST | `/auth/logout` | 登录 | 登出 |
| GET | `/auth/info` | 登录 | 当前用户信息（Redis 缓存） |
| POST | `/chat/new` | 登录 | 新建会话（归属当前用户） |
| POST | `/chat/completion` | 登录+归属 | SSE 流式对话 |
| POST | `/chat/message/list` | 登录+归属 | 某会话的历史消息 |
| POST | `/chat/list` | 登录 | 当前用户的会话列表 |
| POST | `/chat/summary/rename` | 登录+归属 | 重命名摘要 |
| POST | `/chat/delete` | 登录+归属 | 删除会话（含消息与关联记忆） |
| POST | `/customer-service/document/upload` | 登录 | 上传多格式文档（multipart `file`），返回 `fileId` |
| POST | `/customer-service/md/upload` | 登录 | 上传 Markdown（兼容旧入口） |
| POST | `/customer-service/md/list` | 登录 | 本人上传文档分页列表（含状态） |
| POST | `/customer-service/md/delete` | 登录+归属 | 删除文档（本地文件 + 记录 + 向量） |

除 `login/register/error` 外，其余接口均需在请求头携带：

```
Authorization: Bearer <token>
```

## 安全

- 所有含敏感信息的配置文件（`application-dev.yml`、`application-prod.yml` 及 examples 的 `application.yml`）均已加入 `.gitignore`，仓库中只保留脱敏示例。
- Sa-Token 拦截器保证「除登录/注册外一律鉴权」；会话消息查询/删除/重命名/流式对话均做 `user_id` 归属校验，未登录或非本人统一返回「会话不存在 / 未登录」，避免越权与信息泄露。
- **文档检索严格隔离**：系统内置文档 `owner=0` 共享；用户上传文档 `owner=userId`，检索/删除仅限本人；上传目录为运行时数据，不入版本库（根 `markdown/` 已被 `.gitignore` 排除）。
- 建议生产环境通过环境变量注入密钥（配置示例已支持 `${ENV}` 占位符），并定期轮换。

## examples

`examples/` 下为配套示例（如向量库检索、SearXNG 联网检索、MCP 服务等），用于演示与扩展。它们各自的连接配置未纳入版本库，**需要按示例目录内注释/代码自行准备本地配置后再运行**。

## 更新日志

见 [CHANGELOG.md](./CHANGELOG.md)。
