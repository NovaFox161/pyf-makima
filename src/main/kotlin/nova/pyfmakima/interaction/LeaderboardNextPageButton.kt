package nova.pyfmakima.interaction

import discord4j.core.event.domain.interaction.ButtonInteractionEvent
import org.springframework.stereotype.Component

@Component
class LeaderboardNextPageButton(

): InteractionHandler<ButtonInteractionEvent> {
    override val ids = arrayOf("leaderboard-next-{page}")
    override val defer = true

    override suspend fun handle(event: ButtonInteractionEvent) {

        TODO("Not yet implemented")
    }
}
