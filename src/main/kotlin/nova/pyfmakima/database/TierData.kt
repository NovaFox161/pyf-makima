package nova.pyfmakima.database

import org.springframework.data.relational.core.mapping.Table

@Table("tiers")
data class TierData(
    val id: Long? = null,
    val guildId: Long,
    val name: String,
    val levelEquivalent: Int,
    val roleId: Long?,
    val removePreviousRoles: Boolean,
)
