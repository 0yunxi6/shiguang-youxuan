# 拾光优选

[![CI](https://github.com/0yunxi6/shiguang-youxuan/actions/workflows/ci.yml/badge.svg)](https://github.com/0yunxi6/shiguang-youxuan/actions/workflows/ci.yml)

拾光优选是一个前后端分离的电商平台示例项目，包含用户端商城、购物车、订单、商品评价、收藏、优惠券以及后台管理等功能。

## 技术栈

### 前端

- Vue 3
- Vite
- Vue Router
- Pinia
- Element Plus
- Axios
- ECharts

### 后端

- Java 17
- Spring Boot 3.1
- Spring Security
- JWT
- MyBatis Plus
- MySQL 8
- Redis 7
- Maven

### 部署

- Docker
- Docker Compose
- Nginx

## 功能模块

- 用户注册、登录、JWT 鉴权
- 商品列表、分类筛选、搜索、详情
- 购物车、下单、订单管理
- 商品收藏、商品评价
- 优惠券管理
- 个人资料与密码修改
- 后台管理：仪表盘、商品、分类、订单、用户、评价、收藏、优惠券
- 文件上传与静态资源访问

## 项目结构

```text
.
├── backend/                  # Spring Boot 后端
│   ├── src/main/java/com/ecommerce/
│   │   ├── config/           # 配置
│   │   ├── controller/       # 控制器
│   │   ├── dto/              # 请求/响应 DTO
│   │   ├── entity/           # 实体
│   │   ├── exception/        # 全局异常处理
│   │   ├── mapper/           # MyBatis Mapper
│   │   ├── security/         # JWT 和安全配置
│   │   ├── service/          # 业务逻辑
│   │   └── util/             # 工具类
│   ├── src/main/resources/
│   │   ├── db/init.sql       # 数据库初始化脚本
│   │   └── mapper/           # MyBatis XML
│   └── pom.xml
├── frontend/                 # Vue 前端
│   ├── src/
│   │   ├── api/              # API 封装
│   │   ├── components/       # 公共组件
│   │   ├── router/           # 路由
│   │   ├── store/            # Pinia 状态
│   │   ├── styles/           # 全局样式
│   │   ├── utils/            # 工具方法
│   │   └── views/            # 页面
│   ├── nginx.conf
│   ├── package.json
│   └── vite.config.js
├── docker-compose.yml
├── .env.example
└── README.md
```

## 环境要求

本地开发建议安装：

- JDK 17+
- Maven 3.8+
- Node.js 18+
- npm 9+
- MySQL 8
- Docker / Docker Compose（推荐）

## 快速启动：Docker Compose 推荐

1. 复制环境变量文件：

```bash
cp .env.example .env
```

2. 修改 `.env` 中的数据库密码、JWT 密钥等配置。

3. 启动所有服务：

```bash
docker compose up -d --build
```

4. 访问服务：

```text
前端：http://localhost
后端：http://localhost:8080/api
```

5. 停止服务：

```bash
docker compose down
```

## 本地开发启动

### 1. 初始化数据库

创建数据库并导入初始化脚本：

```sql
CREATE DATABASE ecommerce DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

```bash
mysql -u root -p ecommerce < backend/src/main/resources/db/init.sql
```

开发环境默认数据库配置位于：

```text
backend/src/main/resources/application.yml
```

### 2. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端默认运行在：

```text
http://localhost:8080/api
```

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认运行在：

```text
http://localhost:5173
```

## 常用命令

### 后端

```bash
cd backend
mvn test
mvn clean package
```

### 前端

```bash
cd frontend
npm ci
npm run build
npm run preview
```

### Docker

```bash
docker compose config
docker compose up -d --build
docker compose logs -f
docker compose down
```

## 环境变量

主要环境变量见 `.env.example`：

| 变量 | 说明 |
| --- | --- |
| `MYSQL_ROOT_PASSWORD` | MySQL root 密码 |
| `MYSQL_DATABASE` | 默认数据库名 |
| `SPRING_DATASOURCE_URL` | 后端数据库连接地址 |
| `SPRING_DATASOURCE_USERNAME` | 数据库用户名 |
| `SPRING_DATASOURCE_PASSWORD` | 数据库密码 |
| `SPRING_REDIS_HOST` | Redis 主机名 |
| `JWT_SECRET` | JWT 签名密钥，生产环境必须替换 |
| `APP_CORS_ALLOWED_ORIGINS` | 允许跨域访问的前端地址 |
| `BACKEND_PORT` | 后端映射端口 |
| `FRONTEND_PORT` | 前端映射端口 |

> 注意：不要把 `.env` 提交到 GitHub。

## CI/CD

项目已配置 GitHub Actions：

```text
.github/workflows/ci.yml
```

每次推送到 `main` 分支或提交 Pull Request 时会自动执行：

- 后端 Maven 单元测试
- 前端依赖安装与生产构建
- Docker Compose 配置校验

## 安全建议

- 生产环境必须修改 `.env` 中的数据库密码和 `JWT_SECRET`
- 不要提交 `.env`、日志、构建产物和上传文件
- 对公网部署时建议使用 HTTPS
- 管理员账号和测试数据仅用于开发演示，正式环境请清理或修改

## 许可证

本项目暂未指定开源许可证。如需公开商用或分发，请先补充 LICENSE。
