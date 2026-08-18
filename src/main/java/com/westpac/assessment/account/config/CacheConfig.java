package com.westpac.assessment.account.config;

import com.westpac.assessment.account.dto.AccountResponse;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;

@Configuration
@EnableCaching
public class CacheConfig {

    private static final Duration CACHE_TTL =
            Duration.ofMinutes(10);

    /* TODO: If more domains/cache types are introduced, move account-specific
         cache serialization into dedicated domain cache configuration.
     */
    @Bean
    public CacheManager cacheManager(
            RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper) {

        /*
         * Cache:
         * accounts::{accountNumber}
         *
         * Value:
         * AccountResponse
         */
        JacksonJsonRedisSerializer<AccountResponse> accountSerializer =
                new JacksonJsonRedisSerializer<>(
                        objectMapper,
                        AccountResponse.class
                );

        RedisCacheConfiguration accountCacheConfig =
                createCacheConfiguration(
                        accountSerializer
                );


        /*
         * Cache:
         * customerAccounts::{customerId}
         *
         * Value:
         * List<AccountResponse>
         */
        JavaType accountListType =
                objectMapper
                        .getTypeFactory()
                        .constructCollectionType(
                                List.class,
                                AccountResponse.class
                        );

        JacksonJsonRedisSerializer<List<AccountResponse>>
                accountListSerializer =
                new JacksonJsonRedisSerializer<>(
                        objectMapper,
                        accountListType
                );

        RedisCacheConfiguration customerAccountsCacheConfig =
                createCacheConfiguration(
                        accountListSerializer
                );


        return RedisCacheManager
                .builder(connectionFactory)

                .withCacheConfiguration(
                        "accounts",
                        accountCacheConfig
                )

                .withCacheConfiguration(
                        "customerAccounts",
                        customerAccountsCacheConfig
                )

                .build();
    }


    private RedisCacheConfiguration createCacheConfiguration(
            JacksonJsonRedisSerializer<?> serializer) {

        return RedisCacheConfiguration
                .defaultCacheConfig()
                .entryTtl(CACHE_TTL)
                .disableCachingNullValues()
                .serializeValuesWith(
                        RedisSerializationContext
                                .SerializationPair
                                .fromSerializer(serializer)
                );
    }
}