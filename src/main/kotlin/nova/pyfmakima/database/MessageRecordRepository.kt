package nova.pyfmakima.database

import org.springframework.data.r2dbc.repository.R2dbcRepository
import reactor.core.publisher.Mono

interface MessageRecordRepository : R2dbcRepository<MessageRecordData, Long> {
    fun findByMessageId(messageId: Long): Mono<MessageRecordData>

    // TODO: Need to make the required stuffs
}
