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
            ReactionEmoji.custom(Snowflake.of(1175574267379847318L), "arrow_left_ts", false),
        ).disabled(currentPage <= 0)
        val nextPageButton = Button.primary(
            "leaderboard-next-$currentPage",
            ReactionEmoji.custom(Snowflake.of(1175573385724579900L), "arrow_right_ts", false),
        ).disabled(currentPage >= pageCount - 1)
        val refreshButton = Button.secondary(
            "leaderboard-refresh-$currentPage",
            ReactionEmoji.unicode("\uD83D\uDD04"), // refresh emote
        )

        return arrayOf(ActionRow.of(previousPageButton, nextPageButton, refreshButton))
    }
}
