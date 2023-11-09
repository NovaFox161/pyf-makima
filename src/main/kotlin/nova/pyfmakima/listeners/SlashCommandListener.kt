package nova.pyfmakima.listeners

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import kotlinx.coroutines.reactor.awaitSingleOrNull
import nova.pyfmakima.business.MetricService
import nova.pyfmakima.command.SlashCommand
import nova.pyfmakima.logger.LOGGER
import nova.pyfmakima.utils.GlobalValues.DEFAULT
import org.springframework.stereotype.Component
import org.springframework.util.StopWatch

@Component
class SlashCommandListener(
    private val commands: List<SlashCommand>,
    private val metricService: MetricService,
): EventListener<ChatInputInteractionEvent> {
    override suspend fun handle(event: ChatInputInteractionEvent) {
        val timer = StopWatch()
        timer.start()

        if (!event.interaction.guildId.isPresent) {
            event.reply("Sorry, this command is not available in DMs").awaitSingleOrNull()
            return
        }

        val command = commands.firstOrNull { it.name == event.commandName }

        if (command != null) {
            event.deferReply().withEphemeral(command.ephemeral).awaitSingleOrNull()

            try {
                command.handle(event)
            } catch (e: Exception) {
                LOGGER.error(DEFAULT, "Error handling slash command | $event", e)

                // Attempt to provide a message if there's an unhandled exception
                event.createFollowup("An unknown error has occurred, please message Tilly for help.")
                    .withEphemeral(command.ephemeral)
                    .awaitSingleOrNull()
            }
        } else {
            event.createFollowup("An error has occurred, please message Tilly for help.")
                .withEphemeral(true)
                .awaitSingleOrNull()
        }

        // I lose visibility to sub-command level performance... will want to investigate that eventually
        timer.stop()
        metricService.recordInteractionDuration(event.commandName, "chat-input", timer.totalTimeMillis)
    }
}
