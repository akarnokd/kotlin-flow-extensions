package hu.akarnokd.kotlin.flow.impl

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.collect
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

@FlowPreview
class FlowAmbIterable<T>(private val sources: Iterable<Flow<T>>) : Flow<T> {
    @InternalCoroutinesApi
    override suspend fun collect(collector: FlowCollector<T>) {
        val winner = AtomicInteger()
        val jobs = ConcurrentHashMap<Job, Int>()
        coroutineScope {
            var i = 1
            for (source in sources) {
                val idx = i
                val job = launch {
                    source.collect {
                        val w = winner.get()
                        if (w == idx) {
                            collector.emit(it)
                        } else if (w == 0 && winner.compareAndSet(0, idx)) {
                            for (j in jobs.entries) {
                                if (j.value != idx) {
                                    j.key.cancel()
                                }
                            }

                            collector.emit(it)
                        } else {
                            throw CancellationException()
                        }
                    }
                }

                jobs[job] = i
                val w = winner.get()
                if (w != 0 && w != i) {
                    jobs.remove(job)
                    job.cancel()
                    break
                }

                i++
            }
        }
    }
}