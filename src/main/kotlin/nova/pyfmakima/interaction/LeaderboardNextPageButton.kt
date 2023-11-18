package nova.pyfmakima.interaction

import discord4j.core.event.domain.interaction.ButtonInteractionEvent
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import nova.pyfmakima.business.ComponentService
import nova.pyfmakima.business.EmbedService
import nova.pyfmakima.business.LevelService
import org.springframework.stereotype.Component
import kotlin.math.min

@Component
class LeaderboardNextPageButton(
    private val levelService: LevelService,
    private val embedService: EmbedService,
    private val componentService: ComponentService,
): InteractionHandler<ButtonInteractionEvent> {
    override val ids = arrayOf("leaderboard-next-{page}")
    override val deferEdit = true

    override suspend fun handle(event: ButtonInteractionEvent) {
        val guild = event.interaction.guild.awaitSingle()
        val currentPage = event.customId.split("-").last().toInt()
        val totalPages = levelService.getLeaderboardPageCount(guild.id)
        val nextPage = min(currentPage + 1, totalPages - 1)

        event.message.get().edit()
            .withEmbeds(embedService.generateLevelLeaderboardEmbed(guild, nextPage))
            .withComponents(*componentService.getLeaderboardPaginationComponents(guild.id, nextPage))
            .awaitSingleOrNull()
    }
}
