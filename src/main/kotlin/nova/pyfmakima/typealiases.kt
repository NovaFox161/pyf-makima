package nova.pyfmakima

import discord4j.common.util.Snowflake
import nova.pyfmakima.cache.CacheRepository
import nova.pyfmakima.`object`.MessageRecord
import nova.pyfmakima.`object`.Tier
import nova.pyfmakima.`object`.UserLevel

// Cache
typealias MessageRecordCache = CacheRepository<Snowflake, MessageRecord>
typealias UserLevelCache = CacheRepository<Snowflake, UserLevel>
typealias DaysActiveCache = CacheRepository<Snowflake, Long>
typealias LeveledUserCountCache = CacheRepository<Snowflake, Long>
typealias TierCache = CacheRepository<Long, Tier> // Do I want to do a multimap like TicketBird handles projects???
typealias Rule9TrackedMessageCache = CacheRepository<Snowflake, Snowflake>
