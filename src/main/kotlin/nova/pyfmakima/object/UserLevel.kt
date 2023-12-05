package nova.pyfmakima.`object`

import discord4j.common.util.Snowflake
import nova.pyfmakima.database.UserLevelData
import nova.pyfmakima.extensions.toSnowflake

data class UserLevel(
    val guildId: Snowflake,
    val memberId: Snowflake,
    val xp: Float,
    val tierXp: Float,
    val currentTierId: Long?,
    val tierPaused: Boolean,
) {
    constructor(data: UserLevelData): this (
        guildId = data.guildId.toSnowflake(),
        memberId = data.memberId.toSnowflake(),
        xp = data.xp,
        tierXp = data.tierXp,
        currentTierId = data.currentTierId,
        tierPaused = data.tierPaused,
    )
}
