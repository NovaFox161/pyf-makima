package nova.pyfmakima.listeners

import discord4j.core.event.domain.message.ReactionAddEvent
import nova.pyfmakima.business.MessageService
import nova.pyfmakima.config.Config
import nova.pyfmakima.extensions.toSnowflake
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.getBean
import org.springframework.stereotype.Component

@Component
class ReactionAddListener(
    private val beanFactory: BeanFactory,
): EventListener<ReactionAddEvent> {
    private val messageService: MessageService
        get() = beanFactory.getBean()

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
