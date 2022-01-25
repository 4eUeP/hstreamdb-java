package io.hstream.impl

import io.hstream.Responder
import io.hstream.internal.RecordId
import io.hstream.internal.StreamingFetchRequest
import kotlinx.coroutines.flow.MutableSharedFlow
import org.slf4j.LoggerFactory

class ResponderImpl(
    private val subscriptionId: String,
    private val ackFlow: MutableSharedFlow<StreamingFetchRequest>,
    private val consumerId: String,
    private val recordId: RecordId,
    private val orderingKey: String
) : Responder {
    override fun ack() {
        val request = StreamingFetchRequest.newBuilder()
            .setSubscriptionId(subscriptionId)
            .setConsumerName(consumerId)
            .addAckIds(recordId)
            .setOrderingKey(orderingKey)
            .build()
        logger.info("req key:${request.orderingKey}")
        futureForIO { ackFlow.emit(request) }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ResponderImpl::class.java)
    }
}
