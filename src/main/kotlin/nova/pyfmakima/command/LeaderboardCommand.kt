package nova.pyfmakima.command

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import nova.pyfmakima.business.EmbedService
import org.springframework.stereotype.Component

@Component
class LeaderboardCommand(
    private val embedService: EmbedService,
): SlashCommand {
    override val name = "leaderboard"
    override val ephemeral = false

    override suspend fun handle(event: ChatInputInteractionEvent) {
        val guild = event.interaction.guild.awaitSingle()

        event.createFollowup()
            .withEmbeds(embedService.generateLevelLeaderboardEmbed(guild, page = 1))
            // TODO: Add buttons for pagination
            .awaitSingleOrNull()
    }
}
