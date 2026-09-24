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

本地开发可在 `.env.local` 配置 API 地址：

```bash
VITE_API_BASE_URL=
VITE_API_TIMEOUT_MS=10000
```

开发构建默认请求 `http://localhost:8080`，生产构建默认使用 `https://www.e68q.cn`。如果 `.env.local` 中设置了 `VITE_API_BASE_URL`，它会覆盖默认值；构建前应确认它是正确的地址。微信 `appid` 请在发布环境/开发者工具中配置，不要把 AppSecret、支付密钥或 AI Key 提交到仓库。

## 已实现页面

- onboarding：微信登录、目标考试/日期/每日时长/基础设置、可跳过摸底测试。
- 首页与今日学习：服务端每日计划、任务入口，不把计划时长冒充已学习时长。
- 摸底：题目、置信度、提交、结果。
- 课程与视频：课程/章节、播放、字幕、进度恢复与上报，观看 >= 85% 显示完成。
- 知识点：无证据统一显示“待评估”。
- 题库：单选/多选/文本作答、答后解析、错题入口、本机收藏。
- AI 助教：只作为解释层；后端不可用时明确降级，不生成伪回复。
- 我的/周报：学习设置、收藏、退出登录、周统计与薄弱知识点。

## 后端运行配置

当前分支已提供小程序所有页面需要的 learner API。要让真实微信、视频和 AI 在环境中可用，请配置服务端环境变量：

- `WECHAT_APP_ID` / `WECHAT_APP_SECRET`：微信 code2session。
- `STORAGE_PUBLIC_BASE_URL`：视频 objectKey 的公开/CDN 基础地址；若 objectKey 本身是 http(s) URL 则无需配置。
- `AI_BASE_URL` / `AI_API_KEY` / `AI_MODEL` / `AI_PROVIDER_NAME`：OpenAI-compatible AI Provider，可用于 Qwen、DeepSeek、豆包兼容端点。
- `AI_DAILY_REQUEST_LIMIT`：每日 AI 请求上限，默认 20。

## 测试原则

纯业务规则使用 Node 内置测试运行器，CI 继续执行 TypeScript 类型检查和微信小程序生产构建。掌握度只由服务端 Learning Engine 计算；客户端不得自行修改或推断 mastery。
