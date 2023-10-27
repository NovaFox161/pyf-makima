package nova.pyfmakima.business

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

@Component
class MetricService(
    private val meterRegistry: MeterRegistry,
) {
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
