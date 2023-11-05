package nova.pyfmakima.business

import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.Member
import nova.pyfmakima.config.Config
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import kotlin.jvm.optionals.getOrDefault
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

@Component
class LevelService(
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

    fun calculateRateScore() {
        TODO("Not yet implemented")
    }

    fun calculateLongevityScore(member: Member): Float {
        val daysInServer = Duration.between(member.joinTime.getOrDefault(Instant.now()), Instant.now()).toDays()

        // 90 was chosen as the divisor to represent a 3-month period,
        // within which the maximum longevity_score (1.0) can be achieved.
        return min(1.0f, daysInServer / 90f)
    }

    fun calculateConsistencyScore(member: Member): Float {
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
        val daysActive = 0 // TODO: Implement

        return min(1f, daysActive.toFloat() / boundedDaysInServer.toFloat())
    }

    fun calculateExperienceGainedFromMessage() {
        TODO("Not yet implemented")
    }

    ////////////////////////////
    /// User Level functions ///
    ////////////////////////////
    // TODO: add CRUD methods for UserLevel object


    //////////////////////////////
    /// Level action functions ///
    //////////////////////////////
}
