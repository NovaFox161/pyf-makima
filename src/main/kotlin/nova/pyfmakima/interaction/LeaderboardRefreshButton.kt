package nova.pyfmakima.interaction

import discord4j.core.event.domain.interaction.ButtonInteractionEvent
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import nova.pyfmakima.business.ComponentService
import nova.pyfmakima.business.EmbedService
import nova.pyfmakima.business.LevelService
import org.springframework.stereotype.Component
import kotlin.math.max
import kotlin.math.min

@Component
class LeaderboardRefreshButton(
    private val levelService: LevelService,
    private val embedService: EmbedService,
    private val componentService: ComponentService,
): InteractionHandler<ButtonInteractionEvent> {
    override val ids = arrayOf("leaderboard-refresh-")
    override val deferEdit = true

    override suspend fun handle(event: ButtonInteractionEvent) {
        val guild = event.interaction.guild.awaitSingle()
        val currentPage = event.customId.split("-").last().toInt()
        val totalPages = levelService.getLeaderboardPageCount(guild.id)

        val page = max(0, min(currentPage, totalPages - 1))

        event.message.get().edit()
            .withEmbeds(embedService.generateLevelLeaderboardEmbed(guild, page))
            .withComponents(*componentService.getLeaderboardPaginationComponents(guild.id, page))
            .awaitSingleOrNull()
    }
}
