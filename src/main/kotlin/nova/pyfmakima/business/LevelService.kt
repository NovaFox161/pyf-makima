package nova.pyfmakima.business

import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.Member
import discord4j.core.`object`.entity.Message
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import nova.pyfmakima.UserLevelCache
import nova.pyfmakima.config.Config
import nova.pyfmakima.database.UserLevelData
import nova.pyfmakima.database.UserLevelRepository
import nova.pyfmakima.`object`.UserLevel
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import kotlin.jvm.optionals.getOrDefault
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

@Component
class LevelService(
    private val messageService: MessageService,
    private val userLevelRepository: UserLevelRepository,
    private val userLevelCache: UserLevelCache,
) {
    // Chosen to provide balance between making early levels easy to achieve and higher levels more challenging.
    private val QUADRATIC_COEFFICIENT = 100
    //  Chosen to create a smoother leveling experience
    private val LINEAR_COEFFICIENT = -30
    //  Chosen to create a good starting place for the quadratic line for xp
    private val CONSTANT = 40

    /////////////////////////////
    /// Calculation functions ///
    /////////////////////////////

    /**
     * Calculates the amount of experience required to reach the given level.
     *
     * Quadratic formula in use: f(x) = ax^2 + bx + c
     *
     * Where a is the quadratic coefficient, b is the linear coefficient, and c is a constant.
     * @param level The level to calculate the experience required to reach.
     * @return The amount of experience required to reach the given level.
     */
    fun calculateXpToReachLevel(level: Int): Float {
        // Calculate quadratic term (level squared times quadratic coefficient)
        val quadraticTerm = QUADRATIC_COEFFICIENT * level.toFloat().pow(2)

        // Calculate linear term (level times linear coefficient). This slightly offsets the quadratic term making the
        // experience requirements for lower levels more accessible while still maintaining the overall increasing curve.
        val linearTerm = LINEAR_COEFFICIENT * level

        // Sum the quadratic term, linear term, and constant to get total experience
        return quadraticTerm + linearTerm + CONSTANT
    }

    /**
     * Calculates the level based on the given experience points, using the quadratic formula to reverse-engineer the level.
     *
     * Quadratic formula: x = (-b +/- sqrt(b^2 - 4ac)) / 2a
     *
     * Since the level cannot be negative, only the positive version of the formula is used.
     * @param xp The experience points for which to calculate the level
     * @return The level corresponding to the specified experience points.
     */
    fun calculateLevelFromXp(xp: Float): Int {
        val a = QUADRATIC_COEFFICIENT
        val b = LINEAR_COEFFICIENT.toFloat()
        val c = CONSTANT - xp

        return ((-b + sqrt(b.pow(2) - 4 * a * c)) / (2 * a)).toInt()
    }

    fun calculateLengthScore(message: String, hasMedia: Boolean): Float {
        val wordCountDividerConstant = 30
        val idealWordCount = Config.LEVELING_IDEAL_WORD_COUNT.getInt()

        val wordCount = message.trim().split(regex = Regex("\\s+")).size + if (hasMedia) (idealWordCount * 0.75).toInt() else 0

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
        val hasMedia = message.embeds.isNotEmpty()
            || message.attachments.isNotEmpty()
            || message.stickersItems.isNotEmpty()

        val lengthScore = calculateLengthScore(message.content, hasMedia)
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

    suspend fun getTopUsers(guildId: Snowflake, page: Int, pageSize: Int): List<UserLevel> {
        return userLevelRepository.findAllByGuildIdOrderByXpDesc(guildId.asLong(), Pageable.ofSize(pageSize).withPage(page))
            .map(::UserLevel)
            .collectList()
            .awaitSingle()
    }

    suspend fun getTotalLeveledUserCount(guildId: Snowflake): Long {
        return userLevelRepository.countByGuildId(guildId.asLong()).awaitSingle()
    }

    suspend fun getCurrentRank(guildId: Snowflake, memberId: Snowflake): Long {
        return userLevelRepository.calculateRankByGuildIdAndMemberId(guildId. asLong(), memberId.asLong())
            .awaitSingleOrNull() ?: 0
    }

    //////////////////////////////
    /// Level action functions ///
    //////////////////////////////
    suspend fun handleQualifyingMessage(message: Message) {
        val author = message.authorAsMember.awaitSingle()
        val userLevel = getUserLevel(message.guildId.get(), author.id)
        val currentLevel = calculateLevelFromXp(userLevel.xp)

        val xpGained = calculateExperienceGainedFromMessage(message)
        val newLevel = calculateLevelFromXp(userLevel.xp + xpGained)

        if (newLevel > currentLevel) {
            // TODO: Add handling for level up
        }

        upsertUserLevel(userLevel.copy(xp = userLevel.xp + xpGained))
    }
}

