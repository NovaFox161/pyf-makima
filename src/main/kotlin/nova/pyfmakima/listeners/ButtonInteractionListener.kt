package nova.pyfmakima.listeners

import discord4j.core.event.domain.interaction.ButtonInteractionEvent
import kotlinx.coroutines.reactor.awaitSingleOrNull
import nova.pyfmakima.interaction.InteractionHandler
import nova.pyfmakima.logger.LOGGER
import nova.pyfmakima.utils.GlobalValues.DEFAULT
import org.springframework.stereotype.Component

@Component
class ButtonInteractionListener(
    private val buttons: List<InteractionHandler<ButtonInteractionEvent>>
): EventListener<ButtonInteractionEvent> {

    override suspend fun handle(event: ButtonInteractionEvent) {
        if (!event.interaction.guildId.isPresent) {
            event.reply("Sorry, this interaction is not available in DMs").awaitSingleOrNull()
            return
        }

        val button = buttons.firstOrNull { it.ids.contains(event.customId) }

        if (button != null) {
            try {
                if (button.defer) event.deferReply().awaitSingleOrNull()

                button.handle(event)
            } catch (e: Exception) {
                LOGGER.error(DEFAULT, "Error handling button interaction | $event", e)

                // Attempt to provide a message if there's an unhandled exception
                event.createFollowup("An unknown error has occurred, please message Tilly for help.")
                    .withEphemeral(true)
                    .awaitSingleOrNull()
            }
        } else {
            event.createFollowup("An error has occurred, please message Tilly for help.")
                .withEphemeral(true)
                .awaitSingleOrNull()
        }
    }
}
