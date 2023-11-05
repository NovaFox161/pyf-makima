package nova.pyfmakima.config

import com.fasterxml.jackson.databind.ObjectMapper
import nova.pyfmakima.DaysActiveCache
import nova.pyfmakima.MessageRecordCache
import nova.pyfmakima.UserLevelCache
import nova.pyfmakima.cache.JdkCacheRepository
import nova.pyfmakima.cache.RedisStringCacheRepository
import nova.pyfmakima.extensions.asMinutes
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.core.ReactiveStringRedisTemplate

@Configuration
class CacheConfig {
    private val messageRecordTtl = Config.CACHE_TTL_MESSAGE_RECORD_MINUTES.getLong().asMinutes()
    private val userLevelTtl = Config.CACHE_TTL_USER_LEVEL_MINUTES.getLong().asMinutes()
    private val daysActiveTtl = Config.CACHE_TTL_DAYS_ACTIVE_MINUTES.getLong().asMinutes()

    // Redis caching
    @Bean
    @Primary
    @ConditionalOnProperty("bot.cache.redis", havingValue = "true")
    fun messageRecordRedisCache(objectMapper: ObjectMapper, redisTemplate: ReactiveStringRedisTemplate): MessageRecordCache =
        RedisStringCacheRepository(objectMapper, redisTemplate, "MessageRecords", messageRecordTtl)

    @Bean
    @Primary
    @ConditionalOnProperty("bot.cache.redis", havingValue = "true")
    fun userLevelRedisCache(objectMapper: ObjectMapper, redisTemplate: ReactiveStringRedisTemplate): UserLevelCache =
        RedisStringCacheRepository(objectMapper, redisTemplate, "UserLevels", userLevelTtl)

    @Bean
    @Primary
    @ConditionalOnProperty("bot.cache.redis", havingValue = "true")
    fun daysActiveRedisCache(objectMapper: ObjectMapper, redisTemplate: ReactiveStringRedisTemplate): DaysActiveCache =
        RedisStringCacheRepository(objectMapper, redisTemplate, "DaysActive", daysActiveTtl)



    // In-memory fallback caching
    @Bean
    fun messageRecordCache(): MessageRecordCache = JdkCacheRepository(messageRecordTtl)

    @Bean
    fun userLevelCache(): UserLevelCache = JdkCacheRepository(userLevelTtl)

    @Bean
    fun daysActiveCache(): DaysActiveCache = JdkCacheRepository(daysActiveTtl)
}
