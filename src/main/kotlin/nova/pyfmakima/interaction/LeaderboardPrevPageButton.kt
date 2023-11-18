package nova.pyfmakima.interaction

import discord4j.core.event.domain.interaction.ButtonInteractionEvent
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import nova.pyfmakima.business.ComponentService
import nova.pyfmakima.business.EmbedService
import org.springframework.stereotype.Component
import kotlin.math.max

@Component
class LeaderboardPrevPageButton(
    private val embedService: EmbedService,
    private val componentService: ComponentService,
): InteractionHandler<ButtonInteractionEvent> {
    override val ids = arrayOf("leaderboard-prev-{page}")
    override val deferEdit = true

    override suspend fun handle(event: ButtonInteractionEvent) {
        val guild = event.interaction.guild.awaitSingle()
        val currentPage = event.customId.split("-").last().toInt()
        val prevPage = max(0, currentPage - 1)

        event.message.get().edit()
            .withEmbeds(embedService.generateLevelLeaderboardEmbed(guild, prevPage))
            .withComponents(*componentService.getLeaderboardPaginationComponents(guild.id, prevPage))
            .awaitSingleOrNull()
    }
}
