package nova.pyfmakima.runners

import discord4j.core.GatewayDiscordClient
import jakarta.annotation.PreDestroy
import nova.pyfmakima.logger.LOGGER
import nova.pyfmakima.utils.GlobalValues
import org.springframework.stereotype.Component

@Component
class ShutdownHook(private val discordClient: GatewayDiscordClient) {
    @PreDestroy
    fun onShutdown() {
        LOGGER.info(GlobalValues.STATUS, "Shutting down shard")

        discordClient.logout().subscribe()
    }
}
