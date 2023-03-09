package nova.pyfmakima.extensions

import java.net.MalformedURLException
import java.net.URL

fun String.embedTitleSafe(): String = this.substring(0, (256).coerceAtMost(this.length))

fun String.embedDescriptionSafe(): String = this.substring(0, (4096).coerceAtMost(this.length))

fun String.embedFieldSafe(): String = this.substring(0, (1024).coerceAtMost(this.length))

fun String.messageContentSafe(): String = this.substring(0, (2000).coerceAtMost(this.length))

fun String.extractUrls(): List<URL> {
    val urls = mutableListOf<URL>()

    this.split(" ", "\n").forEach {
        try {
            val url = URL(it)
            urls.add(url)
        } catch (_: MalformedURLException) { }
    }

    return urls
}
