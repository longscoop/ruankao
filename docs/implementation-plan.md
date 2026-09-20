# 软考AI V1 Implementation Roadmap

> 规格基准：`docs/specs/soft-exam-v1.md`

## 执行原则
- 每个 Task：测试先行 -> 实现 -> 测试 -> 自检 -> 独立 commit。
- 每个阶段必须形成可独立验证的软件增量。
- 不跨阶段提前实现 V2/V3。
- `main` 不直接开发；当前功能分支：`feat/soft-exam-v1`。

## Phase 1 — Server Foundation + Learning Engine
目标：建立 Java 17 / Spring Boot 3 后端骨架和确定性的 Learning Engine，使最核心算法可测试、可复用。

交付：
- Maven/Spring Boot 工程和 CI。
- 领域枚举和值对象。
- mastery delta。
- retention/effective mastery。
- priority score。
- daily plan allocation。
- Learning Engine 单元测试。

详细计划：
`docs/superpowers/plans/2026-09-19-foundation-learning-engine.md`

## Phase 2 — Exam / Knowledge / Question Persistence ✅
目标：将考试、知识点、题库和用户答题持久化到 PostgreSQL。

详细计划：
`docs/superpowers/plans/2026-09-19-core-persistence.md`

交付：
- Flyway V1 schema。
- Exam / Knowledge / Question repository/service。
- QuestionKnowledge 权重校验。
- AnswerRecord 和 mastery idempotency。
- 错题 ACTIVE/MASTERED 生命周期。

## Phase 3 — Daily Learning API ✅
目标：把 Learning Engine 接入用户考试档案和每日计划。

交付：
- exam profile。
- assessment 基础流程。
- `GET /api/v1/learning/today`。
- study_plan / study_task 幂等生成。
- 答题提交 -> mastery -> wrong question 的事务链。
- authenticated principal 边界与 learner REST API。

详细计划：
`docs/superpowers/plans/2026-09-20-daily-learning-workflow.md`

## Phase 4 — Course / Video
目标：课程和短视频可运营、可播放、可记录进度。

交付：
- course/chapter/video。
- knowledge-video 关联。
- video progress。
- >=85% 完成判定。
- 完成视频的弱 mastery 证据。
- StorageProvider。

## Phase 5 — AI Layer
目标：AI 能解释题目和知识点，但不控制核心业务。

交付：
- AiProvider。
- FakeAiProvider。
- provider 配置。
- AI usage log。
- question explain / knowledge explain / chat / quiz。
- 配额校验和降级行为。

## Phase 6 — Admin Content Workflow
目标：运营人员可维护核心内容。

交付：
- Vue 3 admin。
- 考试/知识点/题库/课程/视频管理。
- Excel Dry Run + confirm。
- AI enrich -> review -> publish。
- 内容状态机。

## Phase 7 — Miniapp Learning Flow
目标：微信小程序跑通核心学习体验。

交付：
- onboarding。
- 首页。
- 今日学习。
- 学习/视频。
- 题库/答题/解析。
- 错题/收藏。
- AI入口。
- 我的/周报。

## Phase 8 — Mock Exam + Membership
目标：补齐 V1 付费和模拟考试闭环。

交付：
- 模拟考试/答题卡/报告。
- membership product。
- entitlement。
- 微信支付 provider + 幂等回调。
- Pro 权限。

## Phase 9 — Integration / Release
目标：验证 V1 四条主链并准备部署。

交付：
- 关键集成测试。
- Docker 本地依赖。
- 环境变量模板。
- CI 全量校验。
- 部署文档。
- V1 验收清单。
