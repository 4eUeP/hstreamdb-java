package io.hstream.impl

import com.google.protobuf.Empty
import io.hstream.ConsumerBuilder
import io.hstream.HStreamClient
import io.hstream.ProducerBuilder
import io.hstream.QueryerBuilder
import io.hstream.Stream
import io.hstream.Subscription
import io.hstream.internal.DeleteStreamRequest
import io.hstream.internal.DeleteSubscriptionRequest
import io.hstream.internal.HStreamApiGrpcKt
import io.hstream.util.GrpcUtils
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicReference

class HStreamClientKtImpl(bootstrapServerUrls: List<String>) : HStreamClient {

    private val logger = LoggerFactory.getLogger(HStreamClientKtImpl::class.java)

    companion object ConnectionManager {
        val channelProvider = ChannelProvider()
        val clusterServerUrls: AtomicReference<List<String>> = AtomicReference(null)

        fun <Resp> unaryCallAsync(call: suspend (stub: HStreamApiGrpcKt.HStreamApiCoroutineStub) -> Resp): CompletableFuture<Resp> {
            return unaryCallAsync(clusterServerUrls, channelProvider, call)
        }

        fun <Resp> unaryCall(call: suspend (stub: HStreamApiGrpcKt.HStreamApiCoroutineStub) -> Resp): Resp {
            return unaryCall(clusterServerUrls, channelProvider, call)
        }

        suspend fun <Resp> unaryCallCoroutine(call: suspend (stub: HStreamApiGrpcKt.HStreamApiCoroutineStub) -> Resp): Resp {
            return unaryCallCoroutine(clusterServerUrls, channelProvider, call)
        }
    }

    init {

        logger.info("bootstrapServerUrls: {}", bootstrapServerUrls)
        val describeClusterResponse = unaryCallWithCurrentUrls(bootstrapServerUrls, channelProvider) { stub -> stub.describeCluster(Empty.newBuilder().build()) }
        val serverNodes = describeClusterResponse.serverNodesList
        val serverUrls: ArrayList<String> = ArrayList(serverNodes.size)
        clusterServerUrls.compareAndSet(null, serverUrls)
        for (serverNode in serverNodes) {
            val host = serverNode.host
            val port = serverNode.port
            logger.info("serverUrl: {}", "$host:$port")
            serverUrls.add("$host:$port")
        }
    }

    override fun close() {
        channelProvider.close()
    }

    override fun newProducer(): ProducerBuilder {
        return ProducerBuilderImpl(clusterServerUrls, channelProvider)
    }

    override fun newConsumer(): ConsumerBuilder {
        return ConsumerBuilderImpl()
    }

    override fun newQueryer(): QueryerBuilder {
        return QueryerBuilderImpl(this, clusterServerUrls.get(), channelProvider)
    }

    override fun createStream(stream: String?) {
        createStream(stream, 1)
    }

    override fun createStream(stream: String?, replicationFactor: Short) {
        checkNotNull(stream)
        check(replicationFactor in 1..15)

        unaryCall { it.createStream(GrpcUtils.streamToGrpc(Stream(stream, replicationFactor.toInt()))) }
    }

    override fun deleteStream(stream: String?) {

        val deleteStreamRequest = DeleteStreamRequest.newBuilder().setStreamName(stream).build()
        unaryCall { it.deleteStream(deleteStreamRequest) }
    }

    override fun listStreams(): List<Stream> {
        val listStreamsResponse = unaryCall { it.listStreams(Empty.getDefaultInstance()) }
        return listStreamsResponse.streamsList.map(GrpcUtils::streamFromGrpc)
    }

    override fun createSubscription(subscription: Subscription?) {
        unaryCall { it.createSubscription(GrpcUtils.subscriptionToGrpc(subscription)) }
    }

    override fun listSubscriptions(): List<Subscription> {
        return unaryCall { it.listSubscriptions(Empty.getDefaultInstance()).subscriptionList.map(GrpcUtils::subscriptionFromGrpc) }
    }

    override fun deleteSubscription(subscriptionId: String?) {
        return unaryCall { it.deleteSubscription(DeleteSubscriptionRequest.newBuilder().setSubscriptionId(subscriptionId).build()) }
    }
}
