# 岚途 LayCoo · 后端服务

GitHub：https://github.com/BlackMonsterQQ/lay-coo-backend

Spring Boot 3.2 + Spring Cloud 2023（Gateway + OpenFeign）+ MyBatis-Plus + MySQL 8。

本仓库只包含后端。前端是独立仓库 [lay-coo-frontend](https://github.com/BlackMonsterQQ/lay-coo-frontend)，通过网关 `http://localhost:8080` 联调。

## 用 IntelliJ IDEA 打开

直接打开本仓库根目录：

```text
E:\laycoo\backend
```

IDEA 会把根目录 [pom.xml](pom.xml) 识别为 Maven 多模块项目。

- Project SDK：JDK 17
- Maven：3.9+
- MySQL：8.x，数据库 `school`（默认连接 `localhost:3306`）
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

这样服务会统一连接 MySQL `school` 数据库，与启动脚本保持一致。

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
- `GET /api/v1/city/{code}/image`：城市地标背景图代理，从 Wikipedia 拉取并缓存到 `data/city-images/`
- 城市坐标种子在 `content-service/src/main/resources/seed/city-seed.json`，来源说明见同目录 `README.md`
- `POST /api/v1/plan/generate`：出发城市 + 目的地 + 出发/返程日期 + 交通偏好 → 异步生成行程
- 行程结果按天输出 3–5 个具体节点，每天至少包含具体景点/体验、具体餐饮（店名 + 菜名）和游玩说明，不只给泛泛的城市漫步
- 每天输出当天的出发/结束时间、起点与完整路线；每个节点包含离开时间、到达时间、交通方式、路上耗时和停留时长，可按时间轴直接执行
- 交通偏好支持 `AUTO` / `TRAIN` / `FLIGHT` / `BOTH` / `SELF_DRIVE`；生成结果包含去程/返程参考方案，自驾时额外返回路线、里程、油费、过路费、停车与休息建议
- `GET /api/v1/plan/{planNo}`：轮询状态 / 获取结果
- `POST /api/v1/plan/{planNo}/retry`：失败重试
- `GET /api/v1/plan/my`：当前登录用户已保存的攻略列表（需要 JWT）
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

所有服务统一连接 MySQL：

```text
jdbc:mysql://localhost:3306/school
用户名：root
密码：123456
```

- 建表脚本在各模块 `src/main/resources/schema.sql`，首次启动自动创建表。
- 应用用户表为 `sys_user`，不会修改 `school` 库里已有的 `user` 表。
- 首次启动 content-service 会导入 [content-seed.json](content-service/src/main/resources/seed/content-seed.json)，user-service 会初始化演示账号。
- 生成过的行程保存在 `travel_plan`，通过 `user_id` 关联登录用户，在「我的行程」页面查看。
- 默认连接参数可在 `.env.local` 中用 `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` 覆盖。

常用环境变量：

| 变量 | 说明 |
| --- | --- |
| `DB_URL` | MySQL JDBC URL，默认 `jdbc:mysql://localhost:3306/school?...` |
| `DB_USERNAME` / `DB_PASSWORD` | MySQL 用户名/密码，默认 `root` / `123456` |
| `JWT_SECRET` | JWT 密钥，生产环境必须替换 |
| `AI_API_KEY` | DeepSeek Key；不配置时使用内置模板生成器 |
| `AI_BASE_URL` / `AI_MODEL` | OpenAI 兼容地址与模型名 |
| `CITY_IMAGE_PROXY` | 城市地标图片抓取代理，默认 `http://127.0.0.1:7897`；代理不可用时尝试直连 |
| `CITY_IMAGE_CACHE_DIR` | 城市图片缓存目录，默认 `./data/city-images` |

## 构建与测试

```powershell
mvn -B -DskipTests package
scripts\smoke-test.cmd   # 需先启动全部服务
```

接口调试样例见 [docs/api.http](docs/api.http)，技术方案见 [docs/本地文旅智能推荐平台_技术方案.docx](docs/本地文旅智能推荐平台_技术方案.docx)。

## 演示账号

- `admin / admin123`（管理员）
- `demo / demo123`（普通用户）
