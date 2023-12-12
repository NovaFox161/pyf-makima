package nova.pyfmakima.config

import java.io.FileReader
import java.time.Instant
import java.util.*

enum class Config(private val key: String, private var value: Any? = null) {
    // Basic spring settings
    APP_NAME("spring.application.name"),

    // Redis cache settings
    REDIS_HOST("spring.data.redis.host"),
    REDIS_PASSWORD("spring.data.redis.password", ""),
    REDIS_PORT("spring.data.redis.port"),
    CACHE_REDIS_IS_CLUSTER("redis.cluster", false),
    CACHE_USE_REDIS("bot.cache.redis", false),
    CACHE_PREFIX("bot.cache.prefix", "makima"),
    CACHE_TTL_SETTINGS_MINUTES("bot.cache.ttl-minutes.settings", 60),
    CACHE_TTL_MESSAGE_RECORD_MINUTES("bot.cache.ttl-minutes.message-record", 60),
    CACHE_TTL_USER_LEVEL_MINUTES("bot.cache.ttl-minutes.user-level", 60),
    CACHE_TTL_DAYS_ACTIVE_MINUTES("bot.cache.ttl-minutes.days-active", 60),
    CACHE_TTL_LEVELED_USER_MINUTES("bot.cache.ttl-minutes.leveled-user", 60),
    CACHE_TTL_RULE_9_MESSAGE_MINUTES("bot.cache.ttl-minutes.rule-9-message", 15),

    // Global bot timings
    TIMING_MESSAGE_REACT_SECONDS("bot.timing.message-react.seconds", 60),
    TIMING_MESSAGE_DELETE_SECONDS("bot.timing.message-delete.seconds", 90),

    // Bot secrets
    SECRET_BOT_TOKEN("bot.secret.token"),
    SECRET_CLIENT_SECRET("bot.secret.client-secret"),
    SECRET_WEBHOOK_DEBUG("bot.secret.debug-webhook"),
    SECRET_WEBHOOK_STATUS("bot.secret.status-webhook"),

    // Various URLs
    URL_BASE("bot.url.base"),
    URL_SUPPORT("bot.url.support", "https://discord.gg/2TFqyuy"),
    URL_INVITE("bot.url.invite"),


    // Everything else
    SHARD_COUNT("bot.sharding.count"),
    SHARD_INDEX("bot.sharding.index"),
    LOGGING_WEBHOOKS_USE("bot.logging.webhooks.use", false),
    LOGGING_WEBHOOKS_ALL_ERRORS("bot.logging.webhooks.all-errors", false),
    INITIAL_STATUS_MESSAGE("bot.initial-status-message"),
    AUDIT_LOG_CHANNEL("bot.audit-log.channel"),
    MOD_ROLE("bot.mod-role"),

    // Rule 9 enforcement
    MESSAGE_DELETE_CHANNEL("bot.message-delete.channel"),
    MESSAGE_DELETE_REACTION_ROLE("bot.message-delete.reaction-role"),

    EMOTE_WARNING_ID("bot.emote.warning.id"),
    EMOTE_WARNING_NAME("bot.emote.warning.name"),
    EMOTE_WARNING_ANIMATED("bot.emote.warning.animated"),
    EMOTE_APPROVED_ID("bot.emote.approved.id"),
    EMOTE_APPROVED_NAME("bot.emote.approved.name"),
    EMOTE_APPROVED_ANIMATED("bot.emote.approved.animated"),

    // Leveling
    LEVELING_EPOCH("bot.leveling.epoch"),
    LEVELING_LEADERBOARD_PAGE_SIZE("bot.leveling.leaderboard.page-size", 10),

    LEVELING_IGNORED_CHANNELS("bot.leveling.ignored-channels", ""),
    LEVELING_TRACKED_ROLES("bot.leveling.tracked-roles", ""),
    LEVELING_IDEAL_WORD_COUNT("bot.leveling.ideal-word-count", 64),
    ;

    companion object {
        fun init() {
            val props = Properties()
            props.load(FileReader("application.properties"))

            entries.forEach { it.value = props.getProperty(it.key, it.value?.toString()) }
        }
    }


    fun getString() = value.toString()

    fun getInt() = getString().toInt()

    fun getLong() = getString().toLong()

    fun getBoolean() = getString().toBoolean()

    fun getInstant() = Instant.parse(getString())!!
}
