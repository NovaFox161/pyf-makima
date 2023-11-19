package nova.pyfmakima.interaction

import discord4j.core.event.domain.interaction.InteractionCreateEvent

interface InteractionHandler<T : InteractionCreateEvent> {
    val ids: Array<String>
    val deferEdit: Boolean

    suspend fun handle(event: T)
}
