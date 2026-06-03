# ReviewHub — 本地生活点评平台

基于 Spring Boot 构建的本地商户点评与发现平台，类似"大众点评"。支持商户浏览、点评发布、社交关注、秒杀优惠券等功能，深度整合 Redis 解决高并发场景下的缓存问题。

## 功能模块

- **用户系统** — 基于 Redis 的短信验证码登录，ThreadLocal + 拦截器实现会话管理
- **商户发现** — 按分类浏览商铺、查看详情，结合 Redis Geo 实现附近商户搜索
- **缓存策略** — 封装通用缓存工具类 `CacheClient`，统一处理缓存穿透（空对象）、缓存击穿（互斥锁 / 逻辑过期）、缓存雪崩
- **点评 Feed** — 发布图文点评，查看关注用户的动态时间线，点赞与评论互动
- **社交关注** — 关注 / 取关用户，查看共同关注
- **秒杀优惠券** — 限量抢购优惠券，库存扣减与一人一单控制
- **商家后台** — 创建与管理优惠券

## 技术栈

| 层 | 技术选型 |
|---|---------|
| 框架 | Spring Boot 2.3.12 |
| ORM | MyBatis-Plus 3.4.3 |
| 缓存 & 会话 | Redis（Lettuce 连接池） |
| 数据库 | MySQL 5.7 |
| 工具库 | Hutool 5.7, Lombok |
| 语言 | Java 8 |

## 缓存问题解决方案

| 问题 | 方案 | 核心实现 |
|------|------|---------|
| 缓存穿透 | 缓存空对象 | `CacheClient.queryWithPassThrough()` 对不存在的 key 写入空值并设置短 TTL |
| 缓存击穿 | 互斥锁 + 逻辑过期 | 互斥锁：`SETNX` 加锁后重建缓存；逻辑过期：异步线程池重建，旧数据先返回 |
| 缓存一致性 | 先写库后删缓存 | `@Transactional` 保证数据库操作原子性，操作完成后删除对应缓存 key |

## 快速开始

### 环境要求

- JDK 8+
- MySQL 5.7+
- Redis 6+
- Maven 3.6+

### 本地运行

1. **克隆项目**
   ```bash
   git clone https://github.com/yccc1106/reviewhub.git
   cd reviewhub
   ```

2. **初始化数据库**
   ```bash
   mysql -u root -p < src/main/resources/db/reviewhub.sql
   ```

3. **修改配置**

   编辑 `src/main/resources/application.yaml`，配置你本地的 MySQL 和 Redis 连接信息。

4. **构建并启动**
   ```bash
   mvn clean package -DskipTests
   java -jar target/reviewhub-0.0.1-SNAPSHOT.jar
   ```
   服务默认运行在 `http://localhost:8081`。

## API 速查

| 接口 | 说明 |
|------|------|
| `POST /user/code` | 发送短信验证码 |
| `POST /user/login` | 验证码登录 |
| `GET /shop-type/list` | 商铺分类列表 |
| `GET /shop/{id}` | 商铺详情（含缓存策略） |
| `GET /shop/of/type` | 按分类查商铺 |
| `GET /shop/of/name` | 按名称搜索商铺 |
| `GET /voucher/list/{shopId}` | 商铺优惠券列表 |
| `POST /voucher-order/seckill/{id}` | 秒杀抢购优惠券 |
| `GET /blog/hot` | 热门点评 |
| `GET /blog/of/user` | 用户点评列表 |
| `PUT /blog/like/{id}` | 点赞点评 |
| `PUT /follow/{id}/{isFollow}` | 关注 / 取关 |
| `GET /follow/common/{id}` | 共同关注 |

## 项目结构

```
src/main/java/com/hmdp/
├── config/          # Spring 配置（MVC、MyBatis、全局异常处理）
├── controller/      # REST 接口层
├── dto/             # 数据传输对象
├── entity/          # 数据库实体
├── mapper/          # MyBatis-Plus Mapper 接口
├── service/         # 业务逻辑层（接口 + 实现）
└── utils/           # 工具类（登录拦截器、缓存客户端、Redis 常量和数据封装）
```

## License

MIT
