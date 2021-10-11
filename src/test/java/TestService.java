import moon.mlock.annotation.CheckLock;
import moon.mlock.annotation.Idempotent;
import moon.mlock.annotation.Lock;
import moon.mlock.common.enums.LockTypeEnum;
import moon.mlock.common.exception.LockException;
import moon.mlock.template.ILockCallback;
import moon.mlock.template.impl.LockTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 测试类
 *
 * @author moon
 */
@Service
public class TestService {
    @Lock(domain = "lockTest", keys = {"#pojo.id"}, lockType = LockTypeEnum.LOCK_REDIS, waitTime = 60000)
    public void lockTest(Pojo pojo) {
        // todo
    }

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

    @CheckLock(domain = "checkLockTest", keys = {"#pojo.id"}, lockType = LockTypeEnum.LOCK_REDIS_FORCE)
    public void checkLockTest(Pojo pojo) {
        // todo
    }

    @Idempotent(domain = "IdempotentTest", keys = {"#token"})
    public void IdempotentTest(String token) {
        // todo
    }
}
