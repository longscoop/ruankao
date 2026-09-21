# 软考AI V1 产品与技术规格

## 1. 产品定义
软考AI V1 面向“系统架构设计师”考试，以知识点为内容中心，将短视频、题库、错题、AI讲解和规则驱动的个性化学习计划串成闭环。

核心闭环：

```text
摸底测试
  -> 每日计划
  -> 视频 / 知识点学习
  -> 做题
  -> AI解析
  -> 掌握度更新
  -> 薄弱点识别
  -> 重新规划
```

V1 首发：微信小程序 + Web 管理后台。只运营“系统架构设计师”一个考试。

## 2. V1 必须实现

### 学员端
- 微信登录、用户资料。
- 目标考试、考试日期、每日学习时长。
- 可跳过的摸底测试；无证据知识点显示“待评估”。
- 今日学习计划、今日任务、完成反馈。
- 课程、章节、知识点、短视频、字幕、播放进度。
- 历年真题、章节练习、专项练习、错题、收藏。
- AI 题目解析、知识点讲解、有限自由问答、少量 AI 强化题。
- 模拟考试与知识点分析。
- 周学习报告、薄弱知识点。
- 免费版 + 单考试 Pro。

### 管理端
- 考试管理。
- 知识点树管理。
- 题库 CRUD、Excel Dry Run + 确认导入。
- PDF 内容导入：支持题库、知识串讲、混合资料；保留原 PDF 页证据，Dry Run 后人工审核再发布。
- PDF 串讲内容按 lesson / content block 组织，可关联知识点并在小程序展示。
- AI 辅助知识点标注、解析和 PDF 结构审核建议，必须人工审核后发布；AI 不得覆盖 PDF 原题干、原选项、原答案或原解析。
- 课程/章节/视频管理。
- 视频异步处理状态、字幕、摘要、知识点关联、审核发布。
- 用户、会员商品、订单、学习数据、AI 成本。

## 3. V1 明确不做
- 社区、评论、排行榜、组队、直播、老师入驻。
- AI 论文批改、AI 案例题批改。
- 原生 iOS / Android。
- 多考试内容运营。
- 微服务、MQ、注册中心、服务治理。
- DKT/Transformer 等深度知识追踪。

## 4. 技术架构

### Server
- Java 17
- Spring Boot 3
- Maven
- PostgreSQL
- Redis
- Flyway
- MyBatis-Plus

采用模块化单体，按业务领域组织：
`auth`, `user`, `exam`, `knowledge`, `question`, `course`, `learning`, `mock`, `ai`, `membership`, `analytics`, `common`。

### Admin
- Vue 3
- TypeScript
- Vite
- Element Plus
- Pinia

### Miniapp
- uni-app
- Vue 3
- TypeScript
- Pinia

### Providers
AI、对象存储、支付均通过可替换 Provider 接口隔离。CI 不依赖真实第三方服务。

## 5. 核心内容模型

### KnowledgePoint
树结构，字段至少包含：
- id
- exam_id
- parent_id
- level
- code
- name
- description
- importance: 1..5
- exam_frequency: 0..100
- estimated_minutes
- sort_order
- status

### Question
来源：
- REAL_EXAM
- CHAPTER
- SIMULATION
- AI_GENERATED
- MANUAL

题型模型支持：
- SINGLE_CHOICE
- MULTIPLE_CHOICE
- CASE
- ESSAY

V1 前台主要运营客观题。

### QuestionKnowledge
一道题允许绑定多个知识点：
- question_id
- knowledge_id
- weight
- primary_flag

同一道题所有知识点权重之和必须为 1.0。

### Video
视频必须绑定至少一个知识点。建议核心知识点视频 5~15 分钟。
用户有效观看 >= 85% 记为完成。

## 6. 答题记录
每次答题永久保留，不能只保存最后结果。

至少包含：
- user_id
- question_id
- session_id
- answer
- correct
- duration_seconds
- confidence
- source
- mastery_applied
- answered_at

source：
- ASSESSMENT
- DAILY_PLAN
- CHAPTER
- REAL_EXAM
- WRONG_REVIEW
- MOCK_EXAM
- AI_QUIZ

confidence：
- GUESS
- UNCERTAIN
- CONFIDENT

同一答题记录最多应用一次 mastery 更新。

## 7. Learning Engine

Learning Engine 必须是确定性的 Java 逻辑。AI 不得计算或直接修改掌握度。

### 7.1 Stored mastery
每个用户/知识点保存 `mastery_score`: 0..100。

无有效证据时 `evidence_count = 0`，前台显示“待评估”，不得伪造默认百分比。

### 7.2 Base delta
正确：+5  
错误：-6

### 7.3 Difficulty factor
正确：
- EASY 0.8
- MEDIUM 1.0
- HARD 1.2

错误：
- EASY 1.3
- MEDIUM 1.0
- HARD 0.7

### 7.4 Confidence factor
正确：
- GUESS 0.6
- UNCERTAIN 0.8
- CONFIDENT 1.0

错误：
- GUESS 0.8
- UNCERTAIN 1.0
- CONFIDENT 1.2

若 confidence 未采集，使用 1.0。

### 7.5 Streak factor
连续正确：
- 第 1 次 1.0
- 第 2 次 1.1
- 第 3 次及以后 1.2

连续错误：
- 第 1 次 1.0
- 第 2 次 1.2
- 第 3 次及以后 1.4

### 7.6 Delta
```text
delta =
base_delta
* difficulty_factor
* confidence_factor
* streak_factor
* question_knowledge_weight
```

结果使用四舍五入到两位小数，最终 clamp 到 0..100。

### 7.7 Effective mastery
数据库不每天衰减 stored mastery。计划与推荐时计算：

```text
effective_mastery = mastery_score * retention_factor
```

retention：
- 0~3 天: 1.00
- 4~7 天: 0.97
- 8~14 天: 0.93
- 15~30 天: 0.88
- 31~60 天: 0.80
- >60 天: 0.72

### 7.8 Priority
```text
weakness = 100 - effective_mastery
importance_score = importance * 20
forgetting = 100 * (1 - retention_factor)
recent_wrong_score = min(100, recent_14d_wrong_count * 20)

priority =
weakness * 0.35
+ importance_score * 0.25
+ forgetting * 0.20
+ exam_frequency * 0.15
+ recent_wrong_score * 0.05
```

## 8. Daily Plan
用户每日目标：15 / 30 / 60 / 90 分钟。

普通阶段：
- 错题复习 20%
- 薄弱知识点 35%
- 新知识点 25%
- 真题 20%

距离考试 <= 14 天：
- 错题复习 30%
- 薄弱知识点 35%
- 新知识点 10%
- 真题 25%

若某一任务类型无可执行内容，其时间优先转给“薄弱知识点”。

约束：
- 每用户/考试/日期生成幂等。
- 同一天重复请求不重复建任务。
- 当天动态追加总时长不得超过用户目标时长的 20%。

## 9. 视频与掌握度
完成视频只提供弱证据：单知识点一次有效完成最多 +3 mastery。
后续练习仍按标准答题算法更新。不得因为看完视频直接判定“已掌握”。

## 10. 错题
答错后进入 ACTIVE。
后续该题连续正确 2 次且相关知识点 effective mastery >= 70，可转 MASTERED。
历史错题始终可查询。

## 11. AI
统一 `AiProvider`，首期可接 Qwen / DeepSeek / Doubao 中任意配置实现。

AI 可做：
- 题目解析
- 错项解释
- 知识点解释
- 举例
- 学习建议文案
- AI 强化题
- 视频摘要

AI 不可做：
- 覆盖数据库标准答案
- 计算 mastery
- 修改支付状态
- 绕过会员权限
- 未经审核发布运营内容
- 把 AI_GENERATED 标成真题

所有 AI 使用记录至少保存 provider/model/function/tokens/latency/cost/success。

## 12. API
统一前缀 `/api/v1`。

首期核心：
- `POST /auth/wechat/login`
- `GET /exams`
- `PUT /users/me/exam-profile`
- `POST /assessments`
- `GET /learning/today`
- `POST /question-sessions/{id}/answers`
- `GET /users/me/wrong-questions`
- `GET /knowledge/tree`
- `GET /knowledge/{id}`
- `GET /courses`
- `GET /videos/{id}`
- `PUT /videos/{id}/progress`
- `POST /ai/question-explain`
- `POST /ai/chat`
- `GET /users/me/stats/weekly`
- `GET /membership/products`

## 13. Idempotency
以下必须具备服务端幂等：
- 答题提交
- mastery 应用
- 每日计划生成
- 支付回调
- Excel 导入确认
- PDF 内容导入确认

## 14. 测试
Learning Engine 完成时行覆盖率目标 >= 90%。

必须覆盖：
- 正确/错误 delta
- 难度
- confidence
- streak
- 多知识点权重
- clamp
- retention
- priority
- 普通/考前 daily plan 比例
- 幂等相关领域服务

AI 自动化测试使用 FakeAiProvider。

最终至少有一条集成链：
登录 -> 设置考试 -> 摸底 -> 今日计划 -> 答题 -> mastery -> 错题 -> 复习。

## 15. V1 验收主链
用户链：
微信登录 -> 设置考试 -> 摸底 -> 今日计划 -> 看视频 -> 做题 -> 答错 -> AI解析 -> 错题 -> mastery 更新 -> 次日新计划。

内容链：
知识点 -> Excel/PDF 导入 -> Dry Run -> 源页/结构审核 -> AI 辅助建议 -> 人工确认 -> 显式发布。

PDF 串讲链：
PDF -> 页文本/页图提取 -> 章节/知识讲解/典型真题识别 -> 人工审核 -> lesson/知识点/题目关联 -> 发布 -> 小程序阅读/练习。

视频链：
上传 -> 处理 -> 字幕 -> AI 摘要 -> 知识点关联 -> 审核 -> 发布 -> 播放。

会员链：
免费用户 -> 访问 Pro 内容 -> 下单 -> 支付 -> entitlement 生效。
