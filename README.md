# 分布式锁 MLock

## 1.基于

使用java+redis实现了常用的分布式锁，提供注解、代码两种使用形式，方便简单

## 2.特性

1. 支持分布式锁
2. 支持分布式检查锁
3. 支持分布式幂等
4. 支持注解用法和模板用法
5. 支持自动续约

## 3.快速开始

#### 3.1依赖引入
```xml
<dependencies>
    <!-- 开发测试 -->
    <dependency>
        <groupId>moon</groupId>
        <artifactId>mlock</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>

    <!-- 正式版本 -->
    <dependency>
        <groupId>moon</groupId>
        <artifactId>mlock</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

#### 3.2 使用

1. 分布式锁 waitMills（等待锁时间默认0，即只尝试一次去获取锁） 注意：Lock需要代码里面幂等，分布式幂等锁不需要 注解用法

```java
@Service
public class TestService {
    @Lock(domain = "lockTest", keys = {"#pojo.id"}, lockType = LockTypeEnum.LOCK_REDIS, waitTime = 60000)
    public void lockTest(Pojo pojo) {
        // todo 
    }
}
```

2. 模板用法

```java
@Service
public class TestService {
    public void lockTemplateTest(Pojo pojo) {
        LockTemplate<Boolean> lockTemplate = new LockTemplate<>();
        String lockKey = String.valueOf(pojo.getId());
        Boolean lockResult = lockTemplate.execute(
                LockTypeEnum.LOCK_REDIS,
                "lockTest",
                lockKey,
                60000,
                TimeUnit.SECONDS,
                new ILockCallback<Boolean>() {
                    @Override
                    public Boolean success() throws LockException {
                        // 这里放需要被加锁的代码
                        return null;
                    }

                    @Override
                    public Boolean fail() throws LockException {
                        return null;
                    }

                    @Override
                    public Boolean ex(Exception e) throws LockException {
                        return null;
                    }
                }
        );
    }
}
```

3. 分布式检查锁 检查锁的存在，不加锁，若已加锁，跳过执行或抛出指定异常

```java
@Service
public class TestService {
    @CheckLock(domain = "checkLockTest", keys = {"#pojo.id"}, lockType = LockTypeEnum.LOCK_REDIS_FORCE)
    public void checkLockTest(Pojo pojo) {
        // todo 
    }
}
```

4. 分布式幂等 检查指定key是否已被操作，若已操作，跳过执行或抛出指定异常，默认幂等默认保留10分钟，适用于避免重复点击、支付、创建、MQ重复消费等

```java
@Service
public class TestService {
    @Idempotent(domain = "IdempotentTest", keys = {"#token"})
    public void IdempotentTest(String token) {
        // todo
    }
}
```
  
