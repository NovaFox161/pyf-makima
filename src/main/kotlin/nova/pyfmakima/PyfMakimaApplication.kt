package nova.pyfmakima

import nova.pyfmakima.config.Config
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PyfMakimaApplication

fun main(args: Array<String>) {
    Config.init()

    runApplication<PyfMakimaApplication>(*args)
}
