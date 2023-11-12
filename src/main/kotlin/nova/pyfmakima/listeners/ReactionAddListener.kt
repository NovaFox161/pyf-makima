package nova.pyfmakima.listeners

import discord4j.core.event.domain.message.ReactionAddEvent
import nova.pyfmakima.business.MessageService
import nova.pyfmakima.config.Config
import nova.pyfmakima.extensions.toSnowflake
import org.springframework.stereotype.Component

@Component
class ReactionAddListener(
    private val messageService: MessageService
): EventListener<ReactionAddEvent> {

    private val requiredRole = Config.MESSAGE_DELETE_REACTION_ROLE.getLong().toSnowflake()

    override suspend fun handle(event: ReactionAddEvent) {
        // Minimize API calls by doing some small sanity checks
        if (!event.guildId.isPresent) return

        if (event.emoji != messageService.getApprovedEmote()) return

        val hasRequiredRole = event.member.get().roleIds.contains(requiredRole)
        if (!hasRequiredRole) return

        // Attempt to remove our reaction
        messageService.doWarningReactionRemoval(event.messageId, event.channelId)
    }
}
