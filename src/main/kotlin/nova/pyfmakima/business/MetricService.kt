package nova.pyfmakima.business

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class MetricService(
    private val meterRegistry: MeterRegistry,
) {
    fun recordInteractionDuration(handler: String, type: String, duration: Long) {
        meterRegistry.timer(
            "bot.interaction.duration",
            listOf(Tag.of("handler", handler), Tag.of("type", type))
        ).record(Duration.ofMillis(duration))
    }

    fun incrementMessageDeleted() {
        meterRegistry.counter(
            "bot.pyf-makima.message.deleted",
        ).increment()
    }

    fun incrementMessageQualifyRuleNine() {
        meterRegistry.counter(
            "bot.pyf-makima.message.qualify-rule-nine",
        ).increment()
    }
}
