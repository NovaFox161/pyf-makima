package nova.pyfmakima.database

import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import reactor.core.publisher.Mono

interface MessageRecordRepository : R2dbcRepository<MessageRecordData, Long> {
    @Query("""
        SELECT COUNT(DISTINCT day_bucket)
            FROM message_records
            WHERE member_id = :memberId
            AND guild_id = :guildId
    """)
    fun countDaysActiveByMemberIdAndGuildId(memberId: Long, guildId: Long): Mono<Long>

    fun countByMemberIdAndGuildIdAndMessageIdGreaterThanEqualAndMessageIdLessThanEqual(memberId: Long, guildId: Long, startMessageId: Long, endMessageId: Long): Mono<Long>

    fun countByGuildIdAndMemberId(guildId: Long, memberId: Long): Mono<Long>

    @Query("""
        SELECT SUM(word_count)
            FROM message_records
            WHERE member_id = :memberId
            AND guild_id = :guildId
    """)
    fun sumWordsByGuildIdAndMemberId(guildId: Long, memberId: Long): Mono<Long>
}
