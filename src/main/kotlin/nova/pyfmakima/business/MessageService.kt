package nova.pyfmakima.business

import discord4j.common.util.Snowflake
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.Member
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.reaction.ReactionEmoji
import discord4j.core.retriever.EntityRetrievalStrategy
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.reactor.mono
import nova.pyfmakima.config.Config
import nova.pyfmakima.extensions.asSeconds
import nova.pyfmakima.extensions.extractUrls
import nova.pyfmakima.extensions.toSnowflake
import nova.pyfmakima.logger.LOGGER
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import java.util.concurrent.CopyOnWriteArrayList

@Component
class DefaultMessageService(
    private val discordClient: GatewayDiscordClient,
    private val metricService: MetricService,
): MessageService {
    private val trackedMessages = CopyOnWriteArrayList<Snowflake>()

    override suspend fun qualifiesForRuleNine(message: Message): Boolean {
        LOGGER.debug("Checking if message ${message.id.asString()} qualifies...")

        val monitoredChannels = Config.MESSAGE_DELETE_CHANNEL.getString()
            .split(",")
            .filter(String::isNotBlank)
            .map(Snowflake::of)

        // Filter messages that will never qualify
        if (!message.guildId.isPresent) return false
        if (!monitoredChannels.contains(message.channelId)) return false
        if (message.authorAsMember.map(Member::isBot).awaitSingle()) return false

        // Check if a mod has already reacted with the correct reaction (making it allowed no matter the content)
        val hasModReacted= message.getReactors(getApprovedEmote())
            .flatMap { it.asMember(message.guildId.get()) }
            .flatMap { it.roleIds.toFlux() }
            .filter { it == Config.MESSAGE_DELETE_REACTION_ROLE.getLong().toSnowflake() }
            .next()
            .map { true }
            .awaitFirstOrNull() ?: false
        if (hasModReacted) return false

        // Check if the message has disallowed media types
        if (message.attachments.isNotEmpty()) return true
        if (message.embeds.isNotEmpty()) return true
        if (message.stickersItems.isNotEmpty()) return true

        // Check if the message contains links that do not link to discord
        val urlMatchCount = message.content.extractUrls()
            // Filter out discord links, they don't count
            .asSequence()
            .map { LOGGER.debug("URL Match found: {}", it); it }
            .filter { !it.host.startsWith("discord.com") }
            .filter { !it.host.startsWith("canary.discord.com") }
            .filter { !it.host.startsWith("ptb.discord.com") }
            .count()
        if (urlMatchCount > 0) return true

        return false
    }

    override suspend fun addToQueue(message: Message) {
        LOGGER.debug("Adding message ${message.id.asString()} to queue")

        trackedMessages.add(message.id)
        val messageReactDelay = Config.TIMING_MESSAGE_REACT_SECONDS.getLong().asSeconds()
        val messageDeleteDelay = Config.TIMING_MESSAGE_DELETE_SECONDS.getLong().asSeconds()

        val reactMono = Mono.delay(messageReactDelay)
            .flatMap { mono { doReaction(message.id, message.channelId) } }
        val deleteMono = Mono.delay(messageDeleteDelay)
            .flatMap { mono { doDelete(message.id, message.channelId) } }

        metricService.incrementMessageQualifyRuleNine()

        Mono.`when`(reactMono, deleteMono).awaitSingleOrNull()
    }

    override suspend fun doReaction(messageId: Snowflake, channelId: Snowflake) {
        LOGGER.debug("Running reaction add task for message ${messageId.asString()}")

        val message = discordClient
            .getMessageById(channelId, messageId)
            .awaitSingleOrNull() ?: return

        if (!qualifiesForRuleNine(message)) return // No longer qualifies

        LOGGER.debug("Message still qualifies for reaction ${messageId.asString()}")

        message.addReaction(getWarningEmote())
            .doOnError { LOGGER.error("Failed to react to message ${message.id.asString()}", it) }
            .onErrorResume { Mono.empty() }
            .awaitSingleOrNull()
    }

    override suspend fun doDelete(messageId: Snowflake, channelId: Snowflake) {
        LOGGER.debug("Running delete task for message ${messageId.asString()}")

        if (!trackedMessages.contains(messageId)) return
        trackedMessages.remove(messageId)

        val message = discordClient
            .getMessageById(channelId, messageId)
            .awaitSingleOrNull() ?: return

        if (!qualifiesForRuleNine(message)) return // No longer qualifies
        LOGGER.debug("Message still qualifies to be deleted ${messageId.asString()}")

        message.delete("Violates rule 3 and was not manually deleted before the timeout")
            .doOnError { LOGGER.error("Failed to delete message ${message.id.asString()}", it) }
            .onErrorResume { Mono.empty() }
            .awaitSingleOrNull()

        metricService.incrementMessageDeleted()
    }

    override suspend fun doWarningReactionRemoval(messageId: Snowflake, channelId: Snowflake) {
        LOGGER.debug("Running reaction remove task for message ${messageId.asString()}")

        val message = discordClient
            .withRetrievalStrategy(EntityRetrievalStrategy.REST)
            .getMessageById(channelId, messageId)
            .awaitSingleOrNull() ?: return

        if (!message.reactions.map { it.emoji }.contains(getWarningEmote())) return // Doesn't even have warning emote

        if (qualifiesForRuleNine(message)) return // No reason to remove the reaction

        trackedMessages.remove(messageId)
        message.removeSelfReaction(getWarningEmote())
            .doOnError { LOGGER.error("Failed to remove warning reaction ${message.id.asString()}", it) }
            .onErrorResume { Mono.empty() }
            .awaitSingleOrNull()
    }

    override fun getWarningEmote(): ReactionEmoji {
        val id = Config.EMOTE_WARNING_ID.getLong().toSnowflake()
        val name = Config.EMOTE_WARNING_NAME.getString()
        val animated = Config.EMOTE_WARNING_ANIMATED.getBoolean()

        return ReactionEmoji.custom(id, name, animated)
    }

    override fun getApprovedEmote(): ReactionEmoji {
        val id = Config.EMOTE_APPROVED_ID.getLong().toSnowflake()
        val name = Config.EMOTE_APPROVED_NAME.getString()
        val animated = Config.EMOTE_WARNING_ANIMATED.getBoolean()

        return ReactionEmoji.custom(id, name, animated)
    }
}

interface MessageService {
    suspend fun qualifiesForRuleNine(message: Message): Boolean

    suspend fun addToQueue(message: Message)

    suspend fun doReaction(messageId: Snowflake, channelId: Snowflake)

    suspend fun doDelete(messageId: Snowflake, channelId: Snowflake)

    suspend fun doWarningReactionRemoval(messageId: Snowflake, channelId: Snowflake)

    fun getWarningEmote(): ReactionEmoji

    fun getApprovedEmote(): ReactionEmoji
}


