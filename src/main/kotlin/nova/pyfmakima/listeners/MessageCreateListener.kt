package nova.pyfmakima.listeners

import discord4j.core.event.domain.message.MessageCreateEvent
import kotlinx.coroutines.reactor.awaitSingleOrNull
import nova.pyfmakima.business.MessageService
import nova.pyfmakima.logger.LOGGER
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.getBean
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class MessageCreateListener(
    private val beanFactory: BeanFactory,
): EventListener<MessageCreateEvent> {
    private val messageService: MessageService
        get() = beanFactory.getBean()

    override suspend fun handle(event: MessageCreateEvent) {
        handleRuleThreeEnforcement(event)
        handleMarisms(event)
    }


    private suspend fun handleRuleThreeEnforcement(event: MessageCreateEvent) {
        if (messageService.qualifiesForRuleThree(event.message)) {
            LOGGER.debug("Message qualifies for rule 3 enforcement - ${event.message.id.asString()}")
            messageService.addToQueue(event.message)
        }
    }

    private suspend fun handleMarisms(event: MessageCreateEvent) {
        if (messageService.containsMarisms(event.message)) {
            event.message.addReaction(messageService.getMariEmote())
                .doOnError { LOGGER.error("Failed to add Mari-ism reaction", it) }
                .onErrorResume { Mono.empty() }
                .awaitSingleOrNull()
        }
    }
}
