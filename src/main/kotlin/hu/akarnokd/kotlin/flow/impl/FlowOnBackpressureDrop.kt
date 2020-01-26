package hu.akarnokd.kotlin.flow.impl

import hu.akarnokd.kotlin.flow.Resumable
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.AbstractFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.collect
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

@FlowPreview
internal class FlowOnBackpressureDrop<T>(private val source: Flow<T>) : AbstractFlow<T>() {
    @ExperimentalCoroutinesApi
    @InternalCoroutinesApi
    override suspend fun collectSafely(collector: FlowCollector<T>) {
        coroutineScope {
            val consumerReady = AtomicBoolean()
            val producerReady = Resumable()
            val value = AtomicReference<T>()
            val done = AtomicBoolean()
            val error = AtomicReference<Throwable>();

            launch {
                try {
                    source.collect {
                        if (consumerReady.get()) {
                            value.set(it);
                            consumerReady.set(false);
                            producerReady.resume();
                        }
                    }
                    done.set(true)
                } catch (ex: Throwable) {
                    error.set(ex)
                }
                producerReady.resume()
            }

            while (true) {
                consumerReady.set(true)
                producerReady.await()

                val d = done.get()
                val ex = error.get()
                val v = value.getAndSet(null)

                if (ex != null) {
                    throw ex;
                }
                if (d) {
                    break;
                }

                collector.emit(v)
            }
        }
    }
}