package nova.pyfmakima.business

import discord4j.common.util.Snowflake
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.channel.GuildMessageChannel
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import nova.pyfmakima.config.Config
import nova.pyfmakima.extensions.toSnowflake
import nova.pyfmakima.logger.LOGGER
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.getBean
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class ModerationService(
    private val embedService: EmbedService,
    private val beanFactory: BeanFactory,
) {
    private val discordClient: GatewayDiscordClient
        get() = beanFactory.getBean()

    private val auditLogChannelId = Config.AUDIT_LOG_CHANNEL.getLong().toSnowflake()

    suspend fun addModRole(guildId: Snowflake, userId: Snowflake, roleId: Snowflake, reason: String): Boolean {
        val user = discordClient.getMemberById(guildId, userId).awaitSingleOrNull() ?: return false
        if (user.roleIds.contains(roleId)) return false // Role already added, no action to take

        val success = user.addRole(roleId, reason)
            .then(Mono.fromCallable { true })
            .doOnError { LOGGER.error("Failed to update user's role | guild:$guildId | user:$user | role:$roleId", it) }
            .onErrorResume { Mono.just(false) }
            .awaitSingle()

        // Post to audit log channel
        if (success) {
            val embed = embedService.generateModRoleAddEmbed(user, roleId, reason)

            discordClient.getChannelById(auditLogChannelId)
                .ofType(GuildMessageChannel::class.java)
                .flatMap { it.createMessage(embed) }
                .awaitSingleOrNull()
        }

        return success
    }

}
