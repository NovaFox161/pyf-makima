package nova.pyfmakima.business

import discord4j.common.util.Snowflake
import discord4j.core.`object`.component.ActionRow
import discord4j.core.`object`.component.Button
import discord4j.core.`object`.component.LayoutComponent
import discord4j.core.`object`.reaction.ReactionEmoji
import nova.pyfmakima.config.Config
import org.springframework.stereotype.Component
import kotlin.math.ceil

@Component
class ComponentService(
    private val levelService: LevelService,
) {
    suspend fun getLeaderboardPaginationComponents(guildId: Snowflake, currentPage: Int): Array<LayoutComponent> {
        val leaderboardPageSize = Config.LEVELING_LEADERBOARD_PAGE_SIZE.getInt()
        val totalRecords = levelService.getTotalLeveledUserCount(guildId)
        val pages = ceil(totalRecords / leaderboardPageSize.toDouble()).toInt()


        val previousPageButton = Button.primary(
            "leaderboard-prev-$currentPage",
            ReactionEmoji.unicode("\u2B05\uFE0F"), // left arrow emote
            "Previous"
        ).disabled(currentPage <= 0)
        val nextPageButton = Button.primary(
            "leaderboard-next-$currentPage",
            ReactionEmoji.unicode("\u27A1\uFE0F"), // right arrow emote
            "Next"
        ).disabled(currentPage >= pages - 1)


        return arrayOf(ActionRow.of(previousPageButton, nextPageButton))
    }
}
