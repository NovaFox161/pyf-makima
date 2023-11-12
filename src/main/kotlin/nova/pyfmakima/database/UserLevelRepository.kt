package nova.pyfmakima.database

import org.springframework.data.domain.Pageable
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface UserLevelRepository : R2dbcRepository<UserLevelData, Long> {
    fun existsByGuildIdAndMemberId(guildId: Long, memberId: Long): Mono<Boolean>

    fun findByGuildIdAndMemberId(guildId: Long, memberId: Long): Mono<UserLevelData>

    fun findAllByGuildIdOrderByXpDesc(guildId: Long, pageable: Pageable): Flux<UserLevelData>

    fun countByGuildId(guildId: Long): Mono<Long>

    @Query("""
        UPDATE user_levels
        SET xp = :xp
        WHERE guild_id = :guildId 
            AND member_id = :memberId
    """)
    fun updateByGuildIdAndMemberId(
        guildId: Long,
        memberId: Long,
        xp: Float,
    ): Mono<Int>
}
