# ReviewHub

A local business review and discovery platform built with Spring Boot, featuring user reviews, social feed, flash-sale vouchers, and geo-based shop search.

## Features

- **User System** — SMS-based login, user profiles, and session management via Redis
- **Shop Discovery** — Browse shops by category, view shop details with Redis caching
- **Geo Search** — Find nearby shops using Redis Geo
- **Review Feed** — Publish blog-style reviews with images, browse a timeline feed of followed users
- **Social** — Follow/unfollow users, like reviews, leave comments
- **Flash Sale Vouchers** — Seckill-style limited-stock vouchers with inventory management
- **Coupon Management** — Create and distribute discount vouchers for shops

## Tech Stack

| Layer | Technology |
|-------|------------|
| Framework | Spring Boot 2.3 |
| ORM | MyBatis-Plus 3.4 |
| Cache & Session | Redis (Lettuce) |
| Database | MySQL 5.x |
| Utilities | Hutool 5.7, Lombok |
| Language | Java 8 |

## Getting Started

### Prerequisites

- JDK 8+
- MySQL 5.7+
- Redis 6+
- Maven 3.6+

### Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/your-username/reviewhub.git
   cd reviewhub
   ```

2. **Initialize the database**
   ```bash
   mysql -u root -p < src/main/resources/db/reviewhub.sql
   ```

3. **Configure the application**

   Edit `src/main/resources/application.yaml` to match your MySQL and Redis setup.

4. **Build and run**
   ```bash
   mvn clean package -DskipTests
   java -jar target/reviewhub-0.0.1-SNAPSHOT.jar
   ```
   The server starts on `http://localhost:8081`.

### API Quick Reference

| Endpoint | Description |
|----------|-------------|
| `POST /user/code` | Send SMS login code |
| `POST /user/login` | Login with code |
| `GET /shop-type/list` | List shop categories |
| `GET /shop/{id}` | Get shop details |
| `GET /shop/of/type` | Shops by category |
| `GET /shop/of/name` | Search shops by name |
| `GET /voucher/list/{shopId}` | List vouchers for a shop |
| `POST /voucher-order/seckill/{id}` | Snap up a flash-sale voucher |
| `GET /blog/hot` | Hot reviews feed |
| `GET /blog/of/user` | User's reviews |
| `PUT /blog/like/{id}` | Like a review |
| `PUT /follow/{id}/{isFollow}` | Follow / unfollow user |
| `GET /follow/common/{id}` | Common follows |

## Project Structure

```
src/main/java/com/hmdp/
├── config/          # Spring configuration (MVC, MyBatis, exception handling)
├── controller/      # REST controllers
├── dto/             # Data transfer objects
├── entity/          # Domain entities
├── mapper/          # MyBatis-Plus mappers
├── service/         # Business logic interfaces & implementations
└── utils/           # Utility classes (interceptor, Redis helpers, constants)
```

## License

MIT
