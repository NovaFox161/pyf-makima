package nova.pyfmakima.listeners

import discord4j.core.event.domain.lifecycle.ReadyEvent
import nova.pyfmakima.logger.LOGGER
import nova.pyfmakima.utils.GlobalValues.STATUS
import org.springframework.stereotype.Component

@Component
class ReadyEventListener: EventListener<ReadyEvent> {
    override suspend fun handle(event: ReadyEvent) {
        LOGGER.info(STATUS, "Ready Event  ${event.sessionId}")
    }
}
