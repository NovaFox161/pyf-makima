package nova.pyfmakima.runners

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import discord4j.discordjson.json.ApplicationCommandRequest
import discord4j.rest.RestClient
import nova.pyfmakima.logger.LOGGER
import nova.pyfmakima.utils.GlobalValues.DEFAULT
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.stereotype.Component

@Component
class GlobalCommandRegistrar(
    private val restClient: RestClient,
    private val objectMapper: ObjectMapper,
) : ApplicationRunner {

    override fun run(args: ApplicationArguments?) {
        val matcher = PathMatchingResourcePatternResolver()
        val applicationService = restClient.applicationService
        val applicationId = restClient.applicationId.block()!!

        val commands = mutableListOf<ApplicationCommandRequest>()
        for (res in matcher.getResources("commands/*.json")) {
            val request = objectMapper.readValue<ApplicationCommandRequest>(res.inputStream)
            commands.add(request)
        }

        applicationService.bulkOverwriteGlobalApplicationCommand(applicationId, commands)
            .doOnNext { LOGGER.debug("Bulk overwrite read: ${it.name()}") }
            .doOnError { LOGGER.error(DEFAULT, "Bulk overwrite failed", it) }
            .subscribe()
    }
}
