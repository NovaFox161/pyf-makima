package nova.pyfmakima.business

import discord4j.common.util.Snowflake
import discord4j.core.`object`.component.ActionRow
import discord4j.core.`object`.component.Button
import discord4j.core.`object`.component.LayoutComponent
import discord4j.core.`object`.reaction.ReactionEmoji
import org.springframework.stereotype.Component

@Component
class ComponentService(
    private val levelService: LevelService,
) {
    suspend fun getLeaderboardPaginationComponents(guildId: Snowflake, currentPage: Int): Array<LayoutComponent> {
        val pageCount = levelService.getLeaderboardPageCount(guildId)


        val previousPageButton = Button.primary(
            "leaderboard-prev-$currentPage",
            ReactionEmoji.custom(Snowflake.of(1175579095157985392), "arrow_left_ts", false),
        ).disabled(currentPage <= 0)
        val nextPageButton = Button.primary(
            "leaderboard-next-$currentPage",
            ReactionEmoji.custom(Snowflake.of(1175579096126869504), "arrow_right_ts", false),
        ).disabled(currentPage >= pageCount - 1)
        val refreshButton = Button.secondary(
            "leaderboard-refresh-$currentPage",
            ReactionEmoji.custom(Snowflake.of(1175580426585247815), "refresh_ts", false)
        )

        return arrayOf(ActionRow.of(previousPageButton, nextPageButton, refreshButton))
    }
}
