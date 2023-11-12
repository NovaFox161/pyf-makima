package nova.pyfmakima.listeners

import discord4j.core.event.domain.lifecycle.ReadyEvent
import discord4j.rest.util.Image
import kotlinx.coroutines.reactor.awaitSingle
import nova.pyfmakima.logger.LOGGER
import nova.pyfmakima.utils.GlobalValues
import nova.pyfmakima.utils.GlobalValues.STATUS
import org.springframework.stereotype.Component

@Component
class ReadyEventListener: EventListener<ReadyEvent> {
    override suspend fun handle(event: ReadyEvent) {
        LOGGER.info(STATUS, "Ready Event  ${event.sessionId}")

        val iconUrl = event.client.applicationInfo
            .map { it.getIconUrl(Image.Format.PNG).orElse("") }
            .awaitSingle()
        GlobalValues.iconUrl = iconUrl
    }
}
