# 岚途 LayCoo · 后端服务

GitHub：https://github.com/BlackMonsterQQ/lay-coo-backend

Spring Boot 3.2 + Spring Cloud 2023（Gateway + OpenFeign）+ MyBatis-Plus + H2（本地演示）。

本仓库只包含后端。前端是独立仓库 [lay-coo-frontend](https://github.com/BlackMonsterQQ/lay-coo-frontend)，通过网关 `http://localhost:8080` 联调。

## 用 IntelliJ IDEA 打开

直接打开本仓库根目录：

```text
E:\lay-coo-backend
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
content-service/     # 内容、分类、标签、搜索、审核
interaction-service/ # 收藏、点赞、评论、评分
recommend-service/   # 推荐、榜单、相似内容
ai-service/          # AI 内容生成、审核辅助
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

## 数据库与配置

- 每个服务使用独立 H2 文件库，位于根目录 `data/`。
- 建表脚本在各模块 `src/main/resources/schema.sql`。
- 首次启动 content-service 会导入 [content-seed.json](content-service/src/main/resources/seed/content-seed.json)。
- 删除根目录 `data/` 后重启可恢复初始数据。

常用环境变量：

| 变量 | 说明 |
| --- | --- |
| `JWT_SECRET` | JWT 密钥，生产环境必须替换 |
| `AI_API_KEY` | 大模型 Key；不配置时使用内置模板生成器 |
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
