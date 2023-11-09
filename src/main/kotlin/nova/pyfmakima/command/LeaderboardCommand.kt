package nova.pyfmakima.command

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import org.springframework.stereotype.Component

@Component
class LeaderboardCommand(

): SlashCommand {
    override val name = "leaderboard"
    override val ephemeral = false

    override suspend fun handle(event: ChatInputInteractionEvent) {
        TODO("Not yet implemented")
    }
}
