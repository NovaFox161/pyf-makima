package nova.pyfmakima.listeners

import discord4j.core.event.domain.message.MessageUpdateEvent
import nova.pyfmakima.business.MessageService
import org.springframework.stereotype.Component

@Component
class MessageEditListener(
    private val messageService: MessageService,
): EventListener<MessageUpdateEvent> {


    override suspend fun handle(event: MessageUpdateEvent) {

        // Let message service handle checking if any reactions should be removed
        messageService.doWarningReactionRemoval(event.messageId, event.channelId)
    }
}
