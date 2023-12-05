package nova.pyfmakima.`object`

import discord4j.common.util.Snowflake
import nova.pyfmakima.database.TierData
import nova.pyfmakima.extensions.toSnowflake

data class Tier(
    val id: Long = 0,
    val guildId: Snowflake,
    val name: String,
    val levelEquivalent: Int,
    val roleId: Snowflake?,
    val removePreviousRoles: Boolean,
) {
    constructor(data: TierData): this(
        id = data.id ?: throw IllegalStateException("Tier ID cannot be null"),
        guildId = data.guildId.toSnowflake(),
        name = data.name,
        levelEquivalent = data.levelEquivalent,
        roleId = data.roleId?.toSnowflake(),
        removePreviousRoles = data.removePreviousRoles,
    )
}
