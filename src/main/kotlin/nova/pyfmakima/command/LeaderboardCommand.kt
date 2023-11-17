package nova.pyfmakima.command

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import nova.pyfmakima.business.ComponentService
import nova.pyfmakima.business.EmbedService
import org.springframework.stereotype.Component

@Component
class LeaderboardCommand(
    private val embedService: EmbedService,
    private val componentService: ComponentService,
): SlashCommand {
    override val name = "leaderboard"
    override val ephemeral = false

    override suspend fun handle(event: ChatInputInteractionEvent) {
        val guild = event.interaction.guild.awaitSingle()

        event.createFollowup()
            .withEmbeds(embedService.generateLevelLeaderboardEmbed(guild, page = 0))
            //.withComponents(*componentService.getLeaderboardPaginationComponents(guild.id, currentPage = 0))
            .awaitSingleOrNull()
    }
}
