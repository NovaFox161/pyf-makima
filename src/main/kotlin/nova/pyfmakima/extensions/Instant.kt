package nova.pyfmakima.extensions

import java.time.Instant

fun Instant.isExpiredTtl(): Boolean = Instant.now().isAfter(this)
