package com.distributed.lock;

import com.distributed.lock.util.ScriptUtil;
import io.lettuce.core.RedisClient;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.SetArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.api.sync.RedisStringCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;

import java.util.concurrent.TimeUnit;

/**
 * @author wangmeng
 */
public class RedisLock {

    private static Logger logger = LoggerFactory.getLogger(RedisLock.class);

    private String prefix;

    private JedisConnectionFactory jedisConnectionFactory;

    private RedisClient redisClient;

    private static final String OK = "OK";

    private static final Long UN_LOCK = 1L;


    /**
     * time millisecond
     */
    private static final int SECOND = 1000;


    private int sleepTime;

    private RedisLock(Builder builder) {
        this.prefix = builder.prefix;
        this.sleepTime = builder.sleepTime;
        this.redisClient = builder.redisClient;
    }

    private RedisClient getRedisClient() {
        return this.redisClient;
    }

    private RedisStringCommands getRedisStringCommands() {
        StatefulRedisConnection<String, String> connection = getRedisClient().connect();
        RedisStringCommands<String, String> sync = connection.sync();
        return sync;
    }

    private SetArgs getDefaultSetArgs(int second) {
        return SetArgs.Builder.nx().px(second * SECOND);
    }

    private SetArgs getDefaultSetArgs() {
        return SetArgs.Builder.nx().px(10 * SECOND);
    }

    public boolean lock(String key, String request) {
        RedisStringCommands commands = getRedisStringCommands();
        SetArgs defaultSetArgs = getDefaultSetArgs();
        String result;
        for (; ; ) {
            result = commands.set(prefix + key, request, defaultSetArgs);
            if (OK.equals(result)) {
                logger.info(Thread.currentThread().getName() + "获取到锁");
                break;
            }
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                // e.printStackTrace();
            }
        }
        return OK.equals(result);
    }

    public boolean lock(String key, String request, int milliseconds) {
        // 1毫秒 = 1纳米 * 1000 * 1000; 使用纳米计算时间差参考AQS
        long deadTime = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(milliseconds) ;
        RedisStringCommands commands = getRedisStringCommands();
        SetArgs defaultSetArgs = getDefaultSetArgs(30);
        String result = "";
        while (deadTime - System.nanoTime() > 0) {
            result = commands.set(prefix + key, request, defaultSetArgs);
            if (OK.equals(result)) {
                logger.info(Thread.currentThread().getName() + "获取到锁");
                // jedis.close();
                break;
            }
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return OK.equals(result);
    }


    public boolean tryLock(String key, String request, int expireTime) {
        RedisStringCommands commands = getRedisStringCommands();
        SetArgs defaultSetArgs = getDefaultSetArgs();
        String result = commands.set(prefix + key, request, defaultSetArgs);
        return OK.equals(result);
    }


    public boolean unLock(String key, String request) {
        RedisStringCommands<String, String> commands = getRedisStringCommands();
        String script = null;
        Long result = 0L;
        script = ScriptUtil.getScript("lock.lua");
        String[] keys = new String[]{prefix + key};
        result = ((RedisCommands<String, String>) commands).eval(script, ScriptOutputType.INTEGER, keys, request);

        logger.info(Thread.currentThread().getName() + "释放了锁" + UN_LOCK.equals(result));

        return UN_LOCK.equals(result);
    }

    public static class Builder {

        private String prefix;

        private int sleepTime = 50;

        private RedisClient redisClient;

        Builder(RedisClient redisClient) {
            this.redisClient = redisClient;
            this.prefix = "lock";
        }


        public Builder lockPrefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        public Builder sleepTime(int sleepTime) {
            this.sleepTime = sleepTime;
            return this;
        }


        RedisLock build() {
            return new RedisLock(this);
        }
    }
}
