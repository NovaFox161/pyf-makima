package nova.pyfmakima.utils

import discord4j.rest.util.Color
import org.slf4j.Marker
import org.slf4j.MarkerFactory

object GlobalValues {
    var iconUrl: String? = null
    val embedColor = Color.of(252, 113, 20)
    val errorColor: Color  = Color.of(248, 38, 48)
    val warnColor: Color = Color.of(232, 150, 0)

    val DEFAULT: Marker = MarkerFactory.getMarker("BOT_WEBHOOK_DEFAULT")

    val STATUS: Marker = MarkerFactory.getMarker("BOT_WEBHOOK_STATUS")
}
