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
            ReactionEmoji.unicode("\u2B05\uFE0F"), // left arrow emote
            "Previous"
        ).disabled(currentPage <= 0)
        val nextPageButton = Button.primary(
            "leaderboard-next-$currentPage",
            ReactionEmoji.unicode("\u27A1\uFE0F"), // right arrow emote
            "Next"
        ).disabled(currentPage >= pageCount - 1)
        val refreshButton = Button.secondary(
            "leaderboard-refresh-$currentPage",
            ReactionEmoji.unicode("\u1F504"), // counter-clockwise arrows emote
        )

        return arrayOf(ActionRow.of(previousPageButton, nextPageButton, refreshButton))
    }
}
