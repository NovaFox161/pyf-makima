package nova.pyfmakima.business

import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.Member
import discord4j.core.spec.EmbedCreateSpec
import nova.pyfmakima.extensions.embedTitleSafe
import nova.pyfmakima.utils.GlobalValues.embedColor
import nova.pyfmakima.utils.GlobalValues.iconUrl
import org.springframework.stereotype.Component
import java.math.RoundingMode
import java.text.DecimalFormat
import java.time.Duration
import java.time.Instant
import kotlin.math.ceil

@Component
class EmbedService(
    private val levelService: LevelService,
    private val messageService: MessageService,
) {
    private val leaderboardPageSize = 20
    private val xpFormat = DecimalFormat("#.##")

    init {
        xpFormat.roundingMode = RoundingMode.CEILING
    }

    suspend fun generateLevelLeaderboardEmbed(guild: Guild, page: Int): EmbedCreateSpec {
        val leaders = levelService.getTopUsers(guild.id, page, leaderboardPageSize)
        val totalRecords = levelService.getTotalLeveledUserCount(guild.id)
        val formattedLeaderboard = StringBuilder()

        leaders.forEachIndexed { index, userLevel ->
            val level = levelService.calculateLevelFromXp(userLevel.xp)
            formattedLeaderboard
                .append("${index + 1}. ")
                .append("<@${userLevel.memberId.asString()}> ")
                .append("${xpFormat.format(userLevel.xp)}xp ")
                .append("lvl $level")
                .appendLine()
        }

        return EmbedCreateSpec.builder()
            .author("Makima", null, iconUrl)
            .color(embedColor)
            .title("Leaderboard for ${guild.name}".embedTitleSafe())
            .description(formattedLeaderboard.toString())
            .footer("Page ${page + 1}/${ceil(totalRecords / leaderboardPageSize.toDouble()).toInt()}", null)
            .timestamp(Instant.now())
            .build()
    }

    suspend fun generateLevelEmbed(member: Member): EmbedCreateSpec {
        val userLevel = levelService.getUserLevel(member.guildId, member.id)
        val currentRank = levelService.getCurrentRank(member.guildId, member.id)
        val currentLevel = levelService.calculateLevelFromXp(userLevel.xp)
        val xpToNextLevel = levelService.calculateXpToReachLevel(currentLevel + 1)

        val currentRateScore = levelService.calculateRateScore(member)
        val currentLongevityScore = levelService.calculateLongevityScore(member)
        val currentConsistencyScore = levelService.calculateConsistencyScore(member)
        val totalTrackedMessages = messageService.getTotalMessages(member.guildId, member.id)
        val daysActive = messageService.getDaysActive(member.guildId, member.id)
        val messagePerHour = messageService.getMessagesPerHour(
            member.guildId,
            member.id,
            start = Instant.now().minus(Duration.ofHours(48))
        )
        val totalCalculatedWordCount = messageService.getTotalCalculatedWordCount(member.guildId, member.id).toFloat()
        val averageWordCount = totalCalculatedWordCount / totalTrackedMessages
        val averageLengthScore = levelService.calculateLengthScore(averageWordCount.toInt(), hasMedia = false)


        return EmbedCreateSpec.builder()
            .author("Makima", null, iconUrl)
            .color(embedColor)
            .title("${member.displayName} - Rank #$currentRank")
            .addField("Level", "*$currentLevel*", true)
            .addField("XP", "`${xpFormat.format(userLevel.xp)}/${xpToNextLevel.toInt()}`", true)
            .addField("Progress", generateXpProgressBar(userLevel.xp, xpToNextLevel), false)
            .addField("Rate Score", "`$currentRateScore`", true)
            .addField("Longevity Score", "`$currentLongevityScore`", true)
            .addField("Consistency Score", "`$currentConsistencyScore`", false)
            .addField("Average XP Per Message", "`${userLevel.xp / totalTrackedMessages}`", true)
            .addField("Days Active", "`$daysActive`", true)
            .addField("Messages Per Hour (last 48 hours)", "`$messagePerHour`", false)
            .addField("Total Messages", "`$totalTrackedMessages`", true)
            .addField("Avg Word Count", "`$averageWordCount`", true)
            .addField("Avg Length Score", "`$averageLengthScore`", false)
            .thumbnail(member.effectiveAvatarUrl)
            .timestamp(Instant.now())
            .build()
    }

    private fun generateXpProgressBar(currentXp: Float, xpToNextLevel: Float): String {
        val progressBarLength = 10
        val progressBarFill = ceil((currentXp / xpToNextLevel) * progressBarLength).toInt()

        return StringBuilder()
            .append("`")
            .append("■".repeat(progressBarFill))
            .append("□".repeat(progressBarLength - progressBarFill))
            .append("` ")
            .append("${(currentXp / xpToNextLevel * 100).toInt()}%")
            .toString()
    }
}
