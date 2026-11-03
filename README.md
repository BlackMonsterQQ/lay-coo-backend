# 岚途 LayCoo · 后端服务

GitHub：https://github.com/BlackMonsterQQ/lay-coo-backend

Spring Boot 3.2 + Spring Cloud 2023（Gateway + OpenFeign）+ MyBatis-Plus + H2（本地演示）。

本仓库只包含后端。前端是独立仓库 [lay-coo-frontend](https://github.com/BlackMonsterQQ/lay-coo-frontend)，通过网关 `http://localhost:8080` 联调。

## 用 IntelliJ IDEA 打开

直接打开本仓库根目录：

```text
E:\laycoo\backend
```

IDEA 会把根目录 [pom.xml](pom.xml) 识别为 Maven 多模块项目。

- Project SDK：JDK 17
- Maven：3.9+
- 开启 Lombok 注解处理：Settings → Build, Execution, Deployment → Compiler → Annotation Processors

## 目录

```text
common/              # 公共模块：统一响应、异常、JWT、用户上下文
gateway-service/     # 网关：路由、鉴权、CORS
user-service/        # 用户、角色
content-service/     # 内容、城市、分类、标签、搜索、审核
interaction-service/ # 收藏、点赞、评论、评分
recommend-service/   # 推荐、榜单、相似内容
ai-service/          # AI 内容生成、多日行程规划、审核辅助
scripts/             # 启动、停止、冒烟测试
docs/                # 技术方案与接口样例
```

## 启动顺序

| 顺序 | 模块 | 启动类 | 端口 |
| --- | --- | --- | --- |
| 1 | user-service | `UserApplication` | 8081 |
| 2 | content-service | `ContentApplication` | 8082 |
| 3 | interaction-service | `InteractionApplication` | 8085 |
| 4 | recommend-service | `RecommendApplication` | 8083 |
| 5 | ai-service | `AiApplication` | 8084 |
| 6 | gateway-service | `GatewayApplication` | 8080 |

`common` 是公共 Jar，没有启动类，不需要运行。

在 IDEA 运行配置里，把 **Working directory** 设为仓库根目录：

```text
E:\laycoo\backend
```

这样 H2 数据库会统一写到 `data/` 下，与启动脚本保持一致。

也可以直接在仓库根目录执行：

```powershell
scripts\start-backend.cmd
```

## 验证

```text
http://localhost:8080/actuator/health
http://localhost:8080/api/v1/content/list?page=1&size=3
```

## AI 多日行程规划

新增能力：

- `GET /api/v1/city/list`：国内 339 个地级及以上城市（含港澳）与经纬度
- 城市坐标种子在 `content-service/src/main/resources/seed/city-seed.json`，来源说明见同目录 `README.md`
- `POST /api/v1/plan/generate`：城市 + 开始/结束日期 + 偏好 → 异步生成行程
- `GET /api/v1/plan/{planNo}`：轮询状态 / 获取结果
- `POST /api/v1/plan/{planNo}/retry`：失败重试
- `GET /internal/content/plan-context`：content-service 向 ai-service 提供内容上下文

默认使用 DeepSeek：

```yaml
ai:
  provider: deepseek
  base-url: https://api.deepseek.com
  model: deepseek-chat
```

### 配置 DeepSeek Key

不要把 Key 写进代码或提交到 Git。本地开发推荐：

1. 复制 [.env.example](.env.example) 为 `.env.local`
2. 填写 `AI_API_KEY=你的新 Key`
3. 重新执行 `scripts\start-backend.cmd`

`.env.local` 已在 `.gitignore` 中。IDEA 运行时请在 Run Configuration 的
Environment variables 里配置 `AI_API_KEY`。

未配置 Key 时自动使用 `MockPlanGenerator`，仍可完整演示多日行程流程。

> Key 一旦在聊天、日志或截图中泄露，请立即到 DeepSeek 控制台吊销并重新生成。

## 数据库与配置

- 每个服务使用独立 H2 文件库，位于根目录 `data/`。
- 建表脚本在各模块 `src/main/resources/schema.sql`。
- 首次启动 content-service 会导入 [content-seed.json](content-service/src/main/resources/seed/content-seed.json)。
- 删除根目录 `data/` 后重启可恢复初始数据。

常用环境变量：

| 变量 | 说明 |
| --- | --- |
| `JWT_SECRET` | JWT 密钥，生产环境必须替换 |
| `AI_API_KEY` | DeepSeek Key；不配置时使用内置模板生成器 |
| `AI_BASE_URL` / `AI_MODEL` | OpenAI 兼容地址与模型名 |
| `DB_DIR` | H2 数据目录，默认 `./data` |

## 构建与测试

```powershell
mvn -B -DskipTests package
scripts\smoke-test.cmd   # 需先启动全部服务
```

接口调试样例见 [docs/api.http](docs/api.http)，技术方案见 [docs/本地文旅智能推荐平台_技术方案.docx](docs/本地文旅智能推荐平台_技术方案.docx)。

## 演示账号

- `admin / admin123`（管理员）
- `demo / demo123`（普通用户）
