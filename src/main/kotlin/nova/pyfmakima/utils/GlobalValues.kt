package nova.pyfmakima.utils

import discord4j.rest.util.Color
import org.slf4j.Marker
import org.slf4j.MarkerFactory

object GlobalValues {
    var iconUrl: String? = null

    val defaultEmbedColor: Color = Color.of(25, 77, 238)
    val levelEmbedColor: Color = Color.of(126, 211, 33)
    val modEmbedColor: Color = Color.of(126, 89, 40)
    val errorColor: Color  = Color.of(248, 38, 48)
    val warnColor: Color = Color.of(232, 150, 0)

    val DEFAULT: Marker = MarkerFactory.getMarker("BOT_WEBHOOK_DEFAULT")

    val STATUS: Marker = MarkerFactory.getMarker("BOT_WEBHOOK_STATUS")
}
