package uk.co.triska.karoohome.extension

import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.models.OnStreamState
import io.hammerhead.karooext.models.StreamState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.delay

/**
 * Wraps KarooSystemService.addConsumer in a coroutine Flow so data type implementations
 * can use idiomatic Flow operators (map, filter, collect, etc.).
 *
 * The flow is cold — the consumer is only registered when the flow is collected,
 * and automatically removed when the collector cancels.
 */
fun KarooSystemService.streamDataFlow(dataTypeId: String): Flow<StreamState> {
    return callbackFlow {
        val listenerId = addConsumer(OnStreamState.StartStreaming(dataTypeId)) { event: OnStreamState ->
            trySendBlocking(event.state)
        }
        awaitClose {
            removeConsumer(listenerId)
        }
    }
}

/**
 * Throttle a flow so that items are emitted at most once per [timeout] milliseconds.
 * Uses conflate() so that the most recent value is always preferred over queued older values.
 */
fun <T> Flow<T>.throttle(timeout: Long): Flow<T> = this
    .conflate()
    .transform {
        emit(it)
        delay(timeout)
    }
