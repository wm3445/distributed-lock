package com.distributed.lock;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.time.Duration;

/**
 * @author wangmeng
 */
@Configuration
public class RedisConfig {


    @Autowired
    private RedisProperties properties;


    @Bean
    JedisConnectionFactory jedisconnectionfactory() {

        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
        redisStandaloneConfiguration.setHostName(properties.getHost());
        redisStandaloneConfiguration.setPort(properties.getPort());
        redisStandaloneConfiguration.setDatabase(3);

        JedisClientConfiguration.JedisClientConfigurationBuilder clientConfigurationBuilder = JedisClientConfiguration.builder();
        clientConfigurationBuilder.connectTimeout(Duration.ofSeconds(60));
        return new JedisConnectionFactory(redisStandaloneConfiguration,clientConfigurationBuilder.build());
    }


    @Bean
    JedisPool jedisPool(){


        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxIdle(properties.getJedis().getPool().getMaxIdle());
        jedisPoolConfig.setMaxWaitMillis(properties.getJedis().getPool().getMaxWait().toMillis());
        JedisPool jedisPool = new JedisPool(jedisPoolConfig, properties.getHost(), properties.getPort(), 2000,properties.getPassword());
        return jedisPool;
    }
}
