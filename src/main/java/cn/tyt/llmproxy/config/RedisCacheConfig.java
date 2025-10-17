package cn.tyt.llmproxy.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
public class RedisCacheConfig {
    @Value("${spring.cache.redis.key-prefix}")
    private String keyPrefix;

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        // 1. 配置通用的序列化方式
        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
//                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .computePrefixWith(cacheName -> keyPrefix + ":" + cacheName + "::")
                // 2. 设置全局默认的过期时间为 1 小时
                .entryTtl(Duration.ofHours(24));

//        // 3. 针对不同缓存名称，设置不同的过期时间
//        Map<String, RedisCacheConfiguration> initialCacheConfigurations = new HashMap<>();
//
//        // "users" 缓存，过期时间 30 分钟
//        initialCacheConfigurations.put("users",
//                RedisCacheConfiguration.defaultCacheConfig()
//                        .entryTtl(Duration.ofMinutes(30))
//                        // 如果某个缓存的序列化方式不同，也可以在这里单独设置
//                        .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
//        );
//
//        // "products" 缓存，过期时间 10 分钟
//        initialCacheConfigurations.put("products",
//                RedisCacheConfiguration.defaultCacheConfig()
//                        .entryTtl(Duration.ofMinutes(10))
//        );

        // 4. 构建 RedisCacheManager
        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaultCacheConfig) // 设置默认的缓存配置
//                .withInitialCacheConfigurations(initialCacheConfigurations) // 设置针对特定缓存的配置
                .build();
    }
}
