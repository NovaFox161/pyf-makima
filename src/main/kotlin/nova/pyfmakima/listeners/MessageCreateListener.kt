package nova.pyfmakima.listeners

import discord4j.core.event.domain.message.MessageCreateEvent
import nova.pyfmakima.business.LevelService
import nova.pyfmakima.business.MessageService
import nova.pyfmakima.logger.LOGGER
import org.springframework.stereotype.Component

@Component
class MessageCreateListener(
    private val messageService: MessageService,
    private val levelService: LevelService,
): EventListener<MessageCreateEvent> {

    override suspend fun handle(event: MessageCreateEvent) {
        try {
            handleRuleNineEnforcement(event)
            handleLeveling(event)
        } catch (e: Exception) {
            LOGGER.error("Error handling message create event", e)
        }
    }


    private suspend fun handleRuleNineEnforcement(event: MessageCreateEvent) {
        if (messageService.qualifiesForRuleNine(event.message)) {
            LOGGER.debug("Message qualifies for rule 3 enforcement - ${event.message.id.asString()}")
            messageService.addToQueue(event.message)
        }
    }

    private suspend fun handleLeveling(event: MessageCreateEvent) {
        if (messageService.qualifiesForLeveling(event.message)) {
            LOGGER.debug("Message qualifies for leveling - ${event.message.id.asString()}")
            messageService.recordMessage(event.message)
            levelService.handleQualifyingMessage(event.message)
        }
    }
}
