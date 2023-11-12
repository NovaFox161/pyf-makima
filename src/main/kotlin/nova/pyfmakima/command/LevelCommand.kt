package nova.pyfmakima.command

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import org.springframework.stereotype.Component

@Component
class LevelCommand(

): SlashCommand {
    override val name = "level"
    override val ephemeral = false

    override suspend fun handle(event: ChatInputInteractionEvent) {
        TODO("Not yet implemented")
    }

}
