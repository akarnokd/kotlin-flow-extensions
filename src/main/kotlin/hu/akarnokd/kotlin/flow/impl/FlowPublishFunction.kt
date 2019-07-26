package hu.akarnokd.kotlin.flow.impl

import hu.akarnokd.kotlin.flow.PublishSubject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.AbstractFlow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.FlowCollector
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

/**
 * Shares a single collector towards the upstream source and multicasts
 * values to any number of consumers which then can produce the output
 * flow of values.
 */
@FlowPreview
internal class FlowPublishFunction<T, R>(
        private val source: Flow<T>,
        private val transform: suspend (Flow<T>) -> Flow<R>
) : AbstractFlow<R>() {

    @ExperimentalCoroutinesApi
    @InternalCoroutinesApi
    override suspend fun collectSafely(collector: FlowCollector<R>) {

        val cancelled = AtomicBoolean()

        val subject = PublishSubject<T>()

        val result = transform(subject)

        result.onCompletion { cancelled.set(true) }.collect(collector)

        source.collect {
            if (cancelled.get()) {
                throw CancellationException()
            }
            subject.emit(it)
            if (cancelled.get()) {
                throw CancellationException()
            }
        }
    }
}