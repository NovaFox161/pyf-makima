package nova.pyfmakima.config

import org.springframework.context.annotation.Configuration

@Configuration
class CacheConfig {
    /*
    //private val settingsTtl = Config.CACHE_TTL_SETTINGS_MINUTES.getLong().asMinutes()



    // Redis caching
    @Bean
    @Primary
    @ConditionalOnProperty("bot.cache.redis", havingValue = "true")
    fun guildSettingsRedisCache(objectMapper: ObjectMapper, redisTemplate: ReactiveStringRedisTemplate): GuildSettingsCache =
        RedisStringCacheRepository(objectMapper, redisTemplate, "GuildSettings", settingsTtl)



    // In-memory fallback caching
    @Bean
    fun guidSettingsFallbackCache(): GuildSettingsCache = JdkCacheRepository(settingsTtl)
     */
}
