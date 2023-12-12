package nova.pyfmakima.command

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.`object`.command.ApplicationCommandInteractionOption
import discord4j.core.`object`.command.ApplicationCommandInteractionOptionValue
import discord4j.core.`object`.entity.Member
import kotlinx.coroutines.reactor.awaitSingleOrNull
import nova.pyfmakima.business.ModerationService
import nova.pyfmakima.config.Config
import nova.pyfmakima.extensions.toSnowflake
import org.springframework.stereotype.Component

@Component
class ModCommand(
    private val moderationService: ModerationService,
): SlashCommand {
    override val name = "mod"
    override val ephemeral = true

    private val modRoleId = Config.MOD_ROLE.getLong().toSnowflake()
    private val auditLogChannelId = Config.AUDIT_LOG_CHANNEL.getLong().toSnowflake()

    override suspend fun handle(event: ChatInputInteractionEvent) {
        // Check permissions server-side just in case
        val memberRoles = event.interaction.member.map(Member::getRoleIds).get()
        if (!memberRoles.contains(modRoleId)) {
            event.createFollowup("Only users with the <@&$modRoleId> role can use this command.")
                .withEphemeral(ephemeral)
                .awaitSingleOrNull()
            return
        }

        when (event.options[0].name) {
            "role" -> {
                when (event.options[0].options[0].name) {
                    "add" -> handleModRoleAdd(event)
                    else -> throw IllegalArgumentException("Unknown subcommand")
                }
            } else -> throw IllegalArgumentException("Unknown subcommand")
        }
    }

    private suspend fun handleModRoleAdd(event: ChatInputInteractionEvent) {
        // Pull user, role, and reason out and pass that to the service to do the heavy lifting
        val user = event.options[0].options[0].getOption("user")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asSnowflake)
            .get()
        val role = event.options[0].options[0].getOption("role")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asSnowflake)
            .get()
        val reason = event.options[0].options[0].getOption("reason")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asString)
            .get()

        val success = moderationService.addModRole(event.interaction.guildId.get(), user, role, reason)

        if (success) event.createFollowup("A mod message has been posted in <#${auditLogChannelId.asString()}>.")
            .withEphemeral(ephemeral)
            .awaitSingleOrNull()
        else event.createFollowup("Makima failed to update the user's roles. Do they already have the role?")
            .withEphemeral(ephemeral)
            .awaitSingleOrNull()
    }
}
