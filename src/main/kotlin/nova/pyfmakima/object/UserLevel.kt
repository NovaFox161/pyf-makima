package nova.pyfmakima.`object`

import discord4j.common.util.Snowflake
import nova.pyfmakima.database.UserLevelData
import nova.pyfmakima.extensions.toSnowflake

data class UserLevel(
    val guildId: Snowflake,
    val memberId: Snowflake,
    val xp: Float,
) {
    constructor(data: UserLevelData): this (
        guildId = data.guildId.toSnowflake(),
        memberId = data.memberId.toSnowflake(),
        xp = data.xp,
    )
}
