package nova.pyfmakima.business

import discord4j.common.util.Snowflake
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.Member
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.channel.CategorizableChannel
import discord4j.core.`object`.entity.channel.Channel
import discord4j.core.`object`.entity.channel.TextChannel
import discord4j.core.`object`.entity.channel.ThreadChannel
import discord4j.core.`object`.reaction.ReactionEmoji
import discord4j.core.retriever.EntityRetrievalStrategy
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.reactor.mono
import nova.pyfmakima.DaysActiveCache
import nova.pyfmakima.MessageRecordCache
import nova.pyfmakima.config.Config
import nova.pyfmakima.database.MessageRecordData
import nova.pyfmakima.database.MessageRecordRepository
import nova.pyfmakima.extensions.asSeconds
import nova.pyfmakima.extensions.extractUrls
import nova.pyfmakima.extensions.isThread
import nova.pyfmakima.extensions.toSnowflake
import nova.pyfmakima.logger.LOGGER
import nova.pyfmakima.`object`.MessageRecord
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.getBean
import org.springframework.stereotype.Component
import org.springframework.util.CollectionUtils
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.concurrent.CopyOnWriteArrayList

@Component
class MessageService(
    private val messageRecordRepository: MessageRecordRepository,
    private val messageRecordCache: MessageRecordCache,
    private val daysActiveCache: DaysActiveCache,
    private val metricService: MetricService,
    private val beanFactory: BeanFactory,
) {
    private val discordClient: GatewayDiscordClient
        get() = beanFactory.getBean()

    private val trackedMessages = CopyOnWriteArrayList<Snowflake>()

    ////////////////////////////////
    /// Rule 9 message functions ///
    ////////////////////////////////
    suspend fun qualifiesForRuleNine(message: Message): Boolean {
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

    suspend fun addToQueue(message: Message) {
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

    suspend fun doReaction(messageId: Snowflake, channelId: Snowflake) {
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

    suspend fun doDelete(messageId: Snowflake, channelId: Snowflake) {
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

    suspend fun doWarningReactionRemoval(messageId: Snowflake, channelId: Snowflake) {
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

    fun getWarningEmote(): ReactionEmoji {
        val id = Config.EMOTE_WARNING_ID.getLong().toSnowflake()
        val name = Config.EMOTE_WARNING_NAME.getString()
        val animated = Config.EMOTE_WARNING_ANIMATED.getBoolean()

        return ReactionEmoji.custom(id, name, animated)
    }

    fun getApprovedEmote(): ReactionEmoji {
        val id = Config.EMOTE_APPROVED_ID.getLong().toSnowflake()
        val name = Config.EMOTE_APPROVED_NAME.getString()
        val animated = Config.EMOTE_WARNING_ANIMATED.getBoolean()

        return ReactionEmoji.custom(id, name, animated)
    }

    //////////////////////////////////
    /// Message leveling functions ///
    //////////////////////////////////
    suspend fun qualifiesForLeveling(message: Message): Boolean {
        val ignoredChannels = Config.LEVELING_IGNORED_CHANNELS.getString()
            .split(",")
            .filter(String::isNotBlank)
            .map(Snowflake::of)
        val trackedRoles = Config.LEVELING_TRACKED_ROLES.getString()
            .split(",")
            .filter(String::isNotBlank)
            .map(Snowflake::of)

        // Filter messages that will never qualify
        if (!message.guildId.isPresent) return false
        if (ignoredChannels.contains(message.channelId)) return false

        // Check author requirements
        val author = message.authorAsMember.awaitSingleOrNull() ?: return false
        if (author.isBot) return false
        if (trackedRoles.isNotEmpty() && !CollectionUtils.containsAny(author.roleIds, trackedRoles)) return false


        // Check if channel is in ignored category
        val channelType = message.channel.ofType(Channel::class.java).awaitSingle().type
        if (channelType.isThread()) {
            // Check if thread is in ignored channel
            val thread = message.channel.ofType(ThreadChannel::class.java).awaitSingle()
            if (ignoredChannels.contains(thread.parentId.get())) return false

            // Check if parent is in ignored category
            val parent = thread.client
                .getChannelById(thread.parentId.get())
                .ofType(CategorizableChannel::class.java)
                .awaitSingle()
            if (parent.categoryId.map(ignoredChannels::contains).orElse(false)) return false
        } else {
            // Can be categorized, check if category is ignored
            val channel = message.channel.ofType(TextChannel::class.java).awaitSingle()
            if (channel.categoryId.map(ignoredChannels::contains).orElse(false)) return false
        }

        return true
    }

    ////////////////////////////////
    /// Message Record functions ///
    ////////////////////////////////
    suspend fun recordMessage(message: Message): MessageRecord {
        LOGGER.debug("Creating message record for message ${message.id.asString()}")

        val messageRecord = messageRecordRepository.save(MessageRecordData(
            messageId = message.id.asLong(),
            guildId = message.guildId.get().asLong(),
            memberId = message.authorAsMember.awaitSingle().id.asLong(),
            channelId = message.channelId.asLong(),
            wordCount = message.content.trim().split(regex = Regex("\\s+")).size,
            dayBucket = LocalDate.ofInstant(message.timestamp, ZoneOffset.UTC)
        )).map(::MessageRecord).awaitSingle()

        messageRecordCache.put(message.guildId.get(), key = message.id, messageRecord)

        return messageRecord
    }

    suspend fun getDaysActive(guildId: Snowflake, memberId: Snowflake): Long {
        var daysActive = daysActiveCache.get(guildId, memberId)
        if (daysActive != null) return daysActive

        daysActive = messageRecordRepository.countDaysActiveByMemberIdAndGuildId(
            memberId = memberId.asLong(),
            guildId = guildId.asLong()
        ).awaitSingleOrNull() ?: 0

        daysActiveCache.put(guildId, memberId, daysActive)

        return daysActive
    }

    suspend fun getMessagesPerHour(guildId: Snowflake, memberId: Snowflake, start: Instant, end: Instant = Instant.now()): Float {
        val lookbackStart = Snowflake.of(start)
        val lookbackEnd = Snowflake.of(end)
        val lookbackWindow = Duration.between(start, end).toHours()

        // TODO: I wonder if there's a way to cache this kind of thing
        val totalMessages = messageRecordRepository.countByMemberIdAndGuildIdAndMessageIdGreaterThanEqualAndMessageIdLessThanEqual(
            memberId = memberId.asLong(),
            guildId = guildId.asLong(),
            startMessageId = lookbackStart.asLong(),
            endMessageId = lookbackEnd.asLong()
        ).awaitSingleOrNull() ?: 0

        // Average messages per hour over the lookback window
        return totalMessages.toFloat() / lookbackWindow.toFloat()
    }

    suspend fun getTotalMessages(guildId: Snowflake, memberId: Snowflake): Long {
        return messageRecordRepository.countByGuildIdAndMemberId(
            guildId = guildId.asLong(),
            memberId = memberId.asLong()
        ).awaitSingle()
    }

    suspend fun getTotalCalculatedWordCount(guildId: Snowflake, memberId: Snowflake): Long {
        return messageRecordRepository.sumWordsByGuildIdAndMemberId(
            guildId = guildId.asLong(),
            memberId = memberId.asLong()
        ).awaitSingle()
    }
}
