# 软考AI 微信小程序

V1 学员端，技术栈为 uni-app + Vue 3 + TypeScript + Pinia，首发考试为“系统架构设计师”。

## 本地开发

要求 Node.js 22+。

```bash
npm install
cp .env.example .env.local
npm run dev:mp-weixin
```

微信开发者工具导入 `dist/dev/mp-weixin`。生产构建：

```bash
npm run check
```

构建产物位于 `dist/build/mp-weixin`。

## 配置

`.env.local` 至少配置：

```bash
VITE_API_BASE_URL=https://your-api.example.cn
VITE_API_TIMEOUT_MS=10000
```

正式环境必须使用真实 HTTPS API 域名。微信 `appid` 请在发布环境/开发者工具中配置，不要把 AppSecret、支付密钥或 AI Key 提交到仓库。

## 已实现页面

- onboarding：微信登录、目标考试/日期/每日时长/基础设置、可跳过摸底测试。
- 首页与今日学习：服务端每日计划、任务入口，不把计划时长冒充已学习时长。
- 摸底：题目、置信度、提交、结果。
- 课程与视频：课程/章节、播放、字幕、进度恢复与上报，观看 >= 85% 显示完成。
- 知识点：无证据统一显示“待评估”。
- 题库：单选/多选/文本作答、答后解析、错题入口、本机收藏。
- AI 助教：只作为解释层；后端不可用时明确降级，不生成伪回复。
- 我的/周报：学习设置、收藏、退出登录、周统计与薄弱知识点。

## 当前后端能力边界

小程序不会用 mock 业务数据掩盖后端缺口。当前分支已能直接对接：

- `PUT /api/v1/users/me/exam-profile`
- `POST/GET /api/v1/assessments...`
- `GET /api/v1/learning/today`

以下前端契约已完成，但当前服务端还未暴露或对应 Phase 尚未实现，页面会显示明确的不可用状态：

- 微信登录、考试目录。
- Course / Video / Knowledge learner REST。
- 通用 question-session 与错题列表 REST。
- AI Layer。
- 周报聚合接口。

这意味着“小程序客户端 Phase 7”已具备完整交互与契约，但在上述服务端接口补齐前，相应功能不会伪装成可用。

## 测试原则

纯业务规则使用 Node 内置测试运行器，CI 继续执行 TypeScript 类型检查和微信小程序生产构建。掌握度只由服务端 Learning Engine 计算；客户端不得自行修改或推断 mastery。
