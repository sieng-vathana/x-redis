# vyntra-redis

Shared **Redis cache library** for Vyntra microservices.

Use this to avoid hitting MySQL on every read. Domain services depend on this library; Redis itself runs as infrastructure (local Docker / K3s `redis` pod).

## What it provides

| Piece | Purpose |
|-------|---------|
| `VyntraRedisAutoConfiguration` | Auto-configures Redis `CacheManager` + `RedisTemplate` |
| `CacheNames` | Shared cache region names (`product-by-id`, `products`, …) |
| `RedisCacheService` | Imperative get / put / evict / getOrLoad helper |
| `vyntra.redis.*` properties | Enable flag, key prefix, default + per-cache TTLs |

## Request path with cache

```text
Client → Gateway → BFF → Product Service
                            │
                            ├─ cache HIT  → Redis → return
                            └─ cache MISS → MySQL → write Redis → return
```

## Add to a service

### 1. Install the library locally (until published)

```bash
cd vyntra-redis
./mvnw clean install -DskipTests
```

### 2. Depend on it

```xml
<dependency>
  <groupId>com.vyntra</groupId>
  <artifactId>vyntra-redis</artifactId>
  <version>0.0.1-SNAPSHOT</version>
</dependency>
```

### 3. Configure Redis

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}

vyntra:
  redis:
    enabled: true
    key-prefix: vyntra
    default-ttl: 10m
    caches:
      product-by-id: 15m
      products: 5m
```

Disable without removing the dependency:

```yaml
vyntra:
  redis:
    enabled: false
```

### 4. Annotate service methods

```java
import com.vyntra.redis.cache.CacheNames;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;

@Cacheable(cacheNames = CacheNames.PRODUCT_BY_ID, key = "#id")
public Product getProductById(Long id) { ... }

@Caching(evict = {
    @CacheEvict(cacheNames = CacheNames.PRODUCT_BY_ID, key = "#id"),
    @CacheEvict(cacheNames = CacheNames.PRODUCTS, allEntries = true)
})
public void deleteProduct(Long id) { ... }
```

Or use the helper:

```java
return redisCacheService.getOrLoad(
    CacheNames.PRODUCT_BY_ID,
    id,
    Product.class,
    () -> repository.findById(id).orElseThrow());
```

## Cache names

| Constant | Value | Typical use |
|----------|-------|-------------|
| `PRODUCTS` | `products` | Full product list |
| `PRODUCT_BY_ID` | `product-by-id` | Single product |
| `USER_BY_USERNAME` | `user-by-username` | Auth lookup |
| `BUSINESS_BY_ID` | `business-by-id` | Business by id |
| `SHOP_BY_ID` | `shop-by-id` | Shop by id |
| `SHOPS_BY_BUSINESS` | `shops-by-business` | Shops for a business |

## Local Redis

```bash
docker run -d --name vyntra-redis -p 6379:6379 redis:8-alpine
```

K3s already ships a `redis` service (used by the API gateway rate limiter). Point services at:

```text
REDIS_HOST=redis
REDIS_PORT=6379
```

## Build & test

```bash
./mvnw clean test
./mvnw clean install
```

## Notes

- This module is a **library**, not a deployable pod.
- Prefer short TTLs for list caches; longer TTLs for stable by-id reads.
- Always **evict** on create / update / delete so clients never see stale data longer than necessary.
- JPA entities with lazy associations should avoid serializing Hibernate proxies (ignore `hibernateLazyInitializer` / unload lazy graphs, or cache DTOs).
```
