package nova.pyfmakima.listeners

import discord4j.core.event.domain.message.MessageCreateEvent
import nova.pyfmakima.business.LevelService
import nova.pyfmakima.business.MessageService
import nova.pyfmakima.logger.LOGGER
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.getBean
import org.springframework.stereotype.Component

@Component
class MessageCreateListener(
    private val beanFactory: BeanFactory,
    private val levelService: LevelService,
): EventListener<MessageCreateEvent> {
    private val messageService: MessageService
        get() = beanFactory.getBean()

    override suspend fun handle(event: MessageCreateEvent) {
        handleRuleNineEnforcement(event)
        handleLeveling(event)
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
