package nova.pyfmakima

import discord4j.common.util.Snowflake
import nova.pyfmakima.cache.CacheRepository
import nova.pyfmakima.`object`.MessageRecord
import nova.pyfmakima.`object`.UserLevel

// Cache
typealias MessageRecordCache = CacheRepository<Snowflake, MessageRecord>
typealias UserLevelCache = CacheRepository<Snowflake, UserLevel>
typealias DaysActiveCache = CacheRepository<Snowflake, Long>
typealias LeveledUserCountCache = CacheRepository<Snowflake, Long>
typealias Rule9TrackedMessageCache = CacheRepository<Snowflake, Snowflake>
