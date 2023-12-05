package nova.pyfmakima.database

import discord4j.core.`object`.entity.Guild
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface TierRepository : R2dbcRepository<TierData, Long> {
    fun findByGuildIdAndId(guildId: Long, id: Long): Mono<TierData>

    fun findAllByGuildId(guildId: Long): Flux<TierData>

    fun deleteByGuildIdAndId(guildId: Long, id: Long): Mono<Int>

    @Query("""
        UPDATE tiers
        SET name = :name,
            level_equivalent = :levelEquivalent,
            role_id = :roleId,
            remove_previous_roles = :removePreviousRoles
        WHERE id = :id
            AND guild_id = :guildId
        
    """)
    fun updateByIdAnAndGuildId(
        id: Long,
        guildId: Long,
        name: String,
        levelEquivalent: Int,
        roleId: Long?,
        removePreviousRoles: Boolean,
    ): Mono<Int>
}
