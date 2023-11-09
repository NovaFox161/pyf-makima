package nova.pyfmakima.command

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent

interface SlashCommand {
    val name: String

    val ephemeral: Boolean

    suspend fun handle(event: ChatInputInteractionEvent)
}
