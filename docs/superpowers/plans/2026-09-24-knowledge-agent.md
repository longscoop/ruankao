# Knowledge Agent Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans task-by-task. Keep each deliverable in a separate commit and record actual verification results.

**Goal:** 管理员上传资料后，学员能够与资料智能体进行有来源的连续问答。

**Architecture:** Java 17 模块化单体；PostgreSQL 词法 RAG；私有文件存储；现有 AiProvider、配额、日志；Vue 管理端与 uni-app 学员端。

**Tech Stack:** Spring Boot 3 / JDBC / Flyway / PDFBox 3 / Vue 3 / TypeScript。

**Spec:** `docs/superpowers/specs/2026-09-24-knowledge-agent.md`

## Global Constraints
- 不改变确定性学习引擎、标准答案、支付或会员权限。
- 仅管理员上传；REVIEW 文档不参与学员检索。
- PDF/TXT/MD；20 MiB、500 页、200 万字符；扫描件不伪装成功。
- CI 不依赖付费模型；不得提交凭据。
- 原文、对话、文件系统路径有独立服务端权限边界。

## Review Focus
1. 中文问题不能依赖 PostgreSQL 默认中文分词。
2. PDF 空白页和扫描页不能让引用页码漂移。
3. 文档撤回和智能体禁用后不能继续通过原文接口取数据。
4. 重试与并发请求不能重复写轮次或串用户历史。
5. 模型无引用、乱引用和不可用必须可识别，不显示假成功。

## Task 1 — 文档解析与分片
Create `server/src/main/java/com/longscoop/ruankao/ai/knowledge/KnowledgeText.java`, `MaterialExtractor.java`, `KnowledgeFileStore.java`, `LocalKnowledgeFileStore.java`.
Tests: `KnowledgeTextTest`, `MaterialExtractorTest`, `LocalKnowledgeFileStoreTest`.

- [ ] 写测试：中文双字与英文词、按页分片且最大 800 字符、扫描 PDF/乱码拒绝、原始页码、私有文件往返。
- [ ] 执行 `mvn -B -Dtest=KnowledgeTextTest,MaterialExtractorTest,LocalKnowledgeFileStoreTest test`，确认缺失行为失败。
- [ ] 实现 `KnowledgeText.split(List<Page>)`、`terms(String)`、`tsQuery(String)`；`MaterialExtractor.extract(String, byte[])`；UUID 命名的文件存储。
- [ ] 重跑聚焦测试与全量 `mvn -B verify`，自检并独立提交。

## Task 2 — 持久化、权限与资料问答
Create Flyway V11 and the `ai/knowledge` repository, service, chat service and DTOs; admin/learner controllers under `api`; add a grounded-answer entrypoint to `AiService`.
Tests: repository/PostgreSQL integration and chat service tests.

- [ ] 先测试库/资料/智能体 CRUD、发布过滤、中文索引、会话用户隔离、请求重试、无命中不调用模型、非法引用降级。
- [ ] 观察聚焦测试失败，再实现事务上传、来源 API、会话保存、服务端发布状态校验。
- [ ] AI 调用不持有长数据库事务；使用会话租约拒绝同会话并发。
- [ ] 运行聚焦测试与 `mvn -B verify`，自检并独立提交。

## Task 3 — 管理端与小程序
Create admin API/types/view and miniapp agent API/types/page; register navigation while preserving existing AI pages. Document setup and upload/ask acceptance steps.

- [ ] 先测试输入校验、请求 ID、文件大小/类型、引用呈现辅助逻辑。
- [ ] 后台：创建库、上传、预览、发布、创建/启用智能体、调试；小程序：选智能体、会话、提问、引用与错误状态。
- [ ] 执行两个前端的 `npm test`、`npm run typecheck` 和各自生产构建；执行后端全量验证。
- [ ] 完成整分支差异自检，记录限制并将验证结果集成回 `feat/soft-exam-v1`，不强推。
