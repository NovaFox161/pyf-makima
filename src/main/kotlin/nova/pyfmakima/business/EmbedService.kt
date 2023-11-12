package nova.pyfmakima.business

import discord4j.core.`object`.entity.Guild
import discord4j.core.spec.EmbedCreateSpec
import nova.pyfmakima.extensions.embedTitleSafe
import nova.pyfmakima.utils.GlobalValues.embedColor
import nova.pyfmakima.utils.GlobalValues.iconUrl
import org.springframework.stereotype.Component
import java.time.Instant
import kotlin.math.ceil

@Component
class EmbedService(
    private val levelService: LevelService,
) {
    private val leaderboardPageSize = 20

    suspend fun generateLevelLeaderboardEmbed(guild: Guild, page: Int): EmbedCreateSpec {
        val leaders = levelService.getTopUsers(guild.id, page, leaderboardPageSize)
        val totalRecords = levelService.getTotalLeveledUserCount(guild.id)
        val formattedLeaderboard = StringBuilder()

        leaders.forEachIndexed { index, userLevel ->
            val level = levelService.calculateLevelFromXp(userLevel.xp)
            formattedLeaderboard
                .append("${index + 1}. ")
                .append("<@${userLevel.memberId}> ")
                .append("${userLevel.xp} ")
                .append("lvl $level")
                .appendLine()
        }

        return EmbedCreateSpec.builder()
            .author("Makima", null, iconUrl)
            .color(embedColor)
            .title("Leaderboard for ${guild.name}".embedTitleSafe())
            .description(formattedLeaderboard.toString())
            .footer("Page $page/${ceil(totalRecords / leaderboardPageSize.toDouble())}", null)
            .timestamp(Instant.now())
            .build()
    }
}
