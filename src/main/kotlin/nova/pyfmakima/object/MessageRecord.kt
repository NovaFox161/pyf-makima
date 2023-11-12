package nova.pyfmakima.`object`

import discord4j.common.util.Snowflake
import nova.pyfmakima.database.MessageRecordData
import nova.pyfmakima.extensions.toSnowflake
import java.time.Instant
import java.time.ZoneOffset

data class MessageRecord(
    val messageId: Snowflake,
    val guildId: Snowflake,
    val memberId: Snowflake,
    val channelId: Snowflake,
    val wordCount: Int,
    val dayBucket: Instant,
) {
    constructor(data: MessageRecordData): this(
        messageId = data.messageId.toSnowflake(),
        guildId = data.guildId.toSnowflake(),
        memberId = data.memberId.toSnowflake(),
        channelId = data.channelId.toSnowflake(),
        wordCount = data.wordCount,
        dayBucket = data.dayBucket.atStartOfDay().toInstant(ZoneOffset.UTC),
    )
}
