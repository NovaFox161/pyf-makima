package nova.pyfmakima.database

import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import reactor.core.publisher.Mono

interface MessageRecordRepository : R2dbcRepository<MessageRecordData, Long> {
    fun findByMessageId(messageId: Long): Mono<MessageRecordData>

    @Query("""
        SELECT COUNT(DISTINCT day_bucket)
            FROM message_records
            WHERE member_id = :memberId
            AND guild_id = :guildId
    """)
    fun countDaysActiveByMemberIdAndGuildId(memberId: Long, guildId: Long): Mono<Long>

    fun countByMemberIdAndGuildIdAndMessageIdGreaterThanEqualAndMessageIdLessThanEqual(memberId: Long, guildId: Long, startMessageId: Long, endMessageId: Long): Mono<Long>
}
