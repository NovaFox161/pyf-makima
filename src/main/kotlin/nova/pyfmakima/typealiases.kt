package nova.pyfmakima

import discord4j.common.util.Snowflake
import nova.pyfmakima.cache.CacheRepository
import nova.pyfmakima.`object`.MessageRecord

// Cache
typealias MessageRecordCache = CacheRepository<Snowflake, MessageRecord>
