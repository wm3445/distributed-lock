package com.distributed.lock;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.lettuce.core.RedisClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.test.context.junit4.SpringRunner;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.concurrent.*;
import java.util.concurrent.locks.LockSupport;

@RunWith(SpringRunner.class)
@SpringBootTest
public class RedisLockWithLettuceTests {


    private static RedisLock redisLock;

    private static ThreadPoolExecutor executor;
    private static CountDownLatch latch = new CountDownLatch(10);

    public static void main(String[] args) {
        RedisClient client = RedisClient.create("redis://192.168.1.111");
        redisLock = new RedisLock.Builder(client).build();
        redisLock.lock("wang","123456",5000);
    }

    @Test
    public void contextLoads() {
    }

    @Before
    public void setBefore() {
        init();

    }

    public void init() {

        RedisClient client = RedisClient.create("redis://192.168.1.111");


        redisLock = new RedisLock.Builder(client).build();
        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()
                .setNameFormat("current-thread-%d").build();
        executor = new ThreadPoolExecutor(350, 350, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(200), namedThreadFactory, new ThreadPoolExecutor.AbortPolicy());
    }

    @Test
    public void redisLockTest() {



        for (int i = 0; i < 10; i++) {
            int finalI = i;
            executor.execute(() -> {

                try {
                    redisLock.lock("123", finalI + "");
                    if (finalI % 2 == 0) {
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    redisLock.unLock("123", finalI + "");

                } finally {
                    latch.countDown();
                }


            });
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("-=-=-=-=-=-=-=全部执行完毕-=-=-=-=-=-===-=");

    }

}
