package nova.pyfmakima.business

import discord4j.common.util.Snowflake
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.Member
import discord4j.core.`object`.entity.Message
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import nova.pyfmakima.UserLevelCache
import nova.pyfmakima.config.Config
import nova.pyfmakima.database.UserLevelData
import nova.pyfmakima.database.UserLevelRepository
import nova.pyfmakima.`object`.UserLevel
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import kotlin.jvm.optionals.getOrDefault
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

@Component
class LevelService(
    private val messageService: MessageService,
    private val userLevelRepository: UserLevelRepository,
    private val userLevelCache: UserLevelCache,
    private val discordClient: GatewayDiscordClient,
) {
    /////////////////////////////
    /// Calculation functions ///
    /////////////////////////////
    fun calculateXpToReachLevel(level: Int): Double {
        // Chosen to provide balance between making early levels easy to achieve and higher levels more challenging.
        val quadraticCoefficient = 100
        // Ensures that the experience required for each level increases more steeply as the level goes up
        val quadraticTerm = quadraticCoefficient * level.toDouble().pow(2)

        // Chosen to create a smoother leveling experience
        val linearCoefficient = 30
        // slightly offsets the quadratic term making the experience requirements for lower levels more accessible
        // while still maintaining the overall increasing curve.
        val linearTerm = linearCoefficient * level

        return quadraticTerm + linearTerm + 40 // 40 is a good starting place for the quadratic line for xp
    }

    fun calculateLengthScore(message: String): Float {
        val wordCountDividerConstant = 30
        val wordCount = message.trim().split(regex = Regex("\\s+")).size

        // bound between 0.1 and 1.0
        return min(1.0f, max(0.1f, wordCount.toFloat() / wordCountDividerConstant))
    }

    suspend fun calculateRateScore(member: Member): Float {
        val messagesPerHour = messageService.getMessagesPerHour(
            guildId = member.guildId,
            memberId = member.id,
            start = Instant.now().minus(Duration.ofHours(48))
        )

        return if (messagesPerHour < 0.1) 0f
        else if (messagesPerHour <= 3) min(1.0f, messagesPerHour / 3f)
        else 1.0f - min(1.0f, (messagesPerHour - 3) / 6)
    }

    fun calculateLongevityScore(member: Member): Float {
        val daysInServer = Duration.between(member.joinTime.getOrDefault(Instant.now()), Instant.now()).toDays()

        // 90 was chosen as the divisor to represent a 3-month period,
        // within which the maximum longevity_score (1.0) can be achieved.
        return min(1.0f, daysInServer / 90f)
    }

    suspend fun calculateConsistencyScore(member: Member): Float {
        // consistency_score = if (daysInServer == 0) 0.0 else min(1.0, daysActive.toDouble() / daysInServer)

        /*
        daysInServer should be bounded to when leveling was enabled in the guild in order to fairly calculate consistency
        as messages by members won't be tracked before leveling was enabled.
         */
        val daysInServer = Duration.between(member.joinTime.getOrDefault(Instant.now()), Instant.now()).toDays()
        val daysSinceLevelingEpoch = Duration.between(Config.LEVELING_EPOCH.getInstant(), Instant.now()).toDays()
        val boundedDaysInServer = min(daysInServer, daysSinceLevelingEpoch)

        // User has not been in the server long enough to calculate consistency score
        if(boundedDaysInServer == 0L) return 0f

        // Calculate days active
        val daysActive = messageService.getDaysActive(member.guildId, member.id)

        return min(1f, daysActive.toFloat() / boundedDaysInServer.toFloat())
    }

    suspend fun calculateExperienceGainedFromMessage(message: Message): Float {
        val author = message.authorAsMember.awaitSingle()

        val lengthScore = calculateLengthScore(message.content)
        val rateScore = calculateRateScore(author)
        val longevityScore = calculateLongevityScore(author)
        val consistencyScore = calculateConsistencyScore(author)

        return (lengthScore * rateScore * (longevityScore + consistencyScore) * 5)
    }

    ////////////////////////////
    /// User Level functions ///
    ////////////////////////////
    suspend fun getUserLevel(guildId: Snowflake, memberId: Snowflake): UserLevel {
        var level = userLevelCache.get(guildId, memberId)
        if (level != null) return level

        level = userLevelRepository.findByGuildIdAndMemberId(guildId.asLong(), memberId.asLong())
            .map(::UserLevel)
            .awaitSingleOrNull() ?: UserLevel(guildId, memberId, 0f)

        userLevelCache.put(guildId, memberId, level)
        return level
    }

    suspend fun upsertUserLevel(userLevel: UserLevel) {
        if (userLevelRepository.existsByGuildIdAndMemberId(userLevel.guildId.asLong(), userLevel.memberId.asLong()).awaitSingle()) {
            userLevelRepository.updateByGuildIdAndMemberId(
                guildId = userLevel.guildId.asLong(),
                memberId = userLevel.memberId.asLong(),
                xp = userLevel.xp,
            ).awaitSingleOrNull()
        } else {
            userLevelRepository.save(UserLevelData(
                guildId = userLevel.guildId.asLong(),
                memberId = userLevel.memberId.asLong(),
                xp = userLevel.xp,
            )).awaitSingleOrNull()
        }

        userLevelCache.put(userLevel.guildId, userLevel.memberId, userLevel)
    }


    //////////////////////////////
    /// Level action functions ///
    //////////////////////////////

}
