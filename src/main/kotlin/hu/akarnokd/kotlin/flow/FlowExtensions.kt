/*
 * Copyright 2019 David Karnok
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package hu.akarnokd.kotlin.flow

import hu.akarnokd.kotlin.flow.impl.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import java.util.concurrent.TimeUnit

/**
 * Shares a single collector towards the upstream source and multicasts
 * values to any number of consumers which then can produce the output
 * flow of values.
 */
@FlowPreview
fun <T, R> Flow<T>.publish(transform: suspend (Flow<T>) -> Flow<R>) : Flow<R> =
    FlowMulticastFunction(this, { PublishSubject() }, transform)

/**
 * Shares a single collector towards the upstream source and multicasts
 * cached values to any number of consumers which then can produce the output
 * flow of values.
 */
@FlowPreview
fun <T, R> Flow<T>.replay(transform: suspend (Flow<T>) -> Flow<R>) : Flow<R> =
        FlowMulticastFunction(this, { ReplaySubject() }, transform)

/**
 * Shares a single collector towards the upstream source and multicasts
 * up to a given [maxSize] number of cached values to any number of
 * consumers which then can produce the output
 * flow of values.
 */
@FlowPreview
fun <T, R> Flow<T>.replay(maxSize: Int, transform: suspend (Flow<T>) -> Flow<R>) : Flow<R> =
        FlowMulticastFunction(this, { ReplaySubject(maxSize) }, transform)

/**
 * Shares a single collector towards the upstream source and multicasts
 * up to [maxTime] old cached values to any number of
 * consumers which then can produce the output flow of values.
 */
@FlowPreview
fun <T, R> Flow<T>.replay(maxTime: Long, unit: TimeUnit, transform: suspend (Flow<T>) -> Flow<R>) : Flow<R> =
        FlowMulticastFunction(this, { ReplaySubject(maxTime, unit) }, transform)

/**
 * Shares a single collector towards the upstream source and multicasts
 * up to a given [maxSize] number and up to [maxTime] old cached values to any number of
 * consumers which then can produce the output flow of values.
 */
@FlowPreview
fun <T, R> Flow<T>.replay(maxSize: Int, maxTime: Long, unit: TimeUnit, transform: suspend (Flow<T>) -> Flow<R>) : Flow<R> =
        FlowMulticastFunction(this, { ReplaySubject(maxSize, maxTime, unit) }, transform)

/**
 * Shares a single collector towards the upstream source and multicasts
 * up to a given [maxSize] number and up to [maxTime] old cached values to any number of
 * consumers which then can produce the output flow of values.
 */
@FlowPreview
fun <T, R> Flow<T>.replay(maxSize: Int, maxTime: Long, unit: TimeUnit, timeSource: (TimeUnit) -> Long, transform: suspend (Flow<T>) -> Flow<R>) : Flow<R> =
        FlowMulticastFunction(this, { ReplaySubject(maxSize, maxTime, unit, timeSource) }, transform)

/**
 * Stats collecting the upstream on the specified dispatcher.
 */
@FlowPreview
fun <T> Flow<T>.startCollectOn(dispatcher: CoroutineDispatcher) : Flow<T> =
        FlowStartCollectOn(this, dispatcher)

/**
 * Emit values from the upstream followed by values from the other flow.
 */
fun <T> Flow<T>.concatWith(other: Flow<T>) : Flow<T> {
    val source = this
    return flow {
        source.collect {
            emit(it)
        }
        other.collect {
            emit(it)
        }
    }
}

/**
 * Consumes the main source until the other source emits an item or completes.
 */
@FlowPreview
fun <T, U> Flow<T>.takeUntil(other: Flow<U>) : Flow<T> =
        FlowTakeUntil(this, other)

/**
 * Generates a range of ever increasing integer values.
 */
fun range(start: Int, count: Int) : Flow<Int> =
        flow {
            val end = start + count
            for (i in start until end) {
                emit(i)
            }
        }

/**
 * Signal 0L after the given time passed
 */
fun timer(timeout: Long, unit: TimeUnit) : Flow<Long> =
        flow {
            delay(unit.toMillis(timeout))
            emit(0L)
        }

/**
 * Groups the upstream values into their own Flows keyed by the value returned
 * by the [keySelector] function.
 */
@FlowPreview
fun <T, K> Flow<T>.groupBy(keySelector: suspend (T) -> K) : Flow<GroupedFlow<K, T>> =
        FlowGroupBy(this, keySelector, { it })

/**
 * Groups the mapped upstream values into their own Flows keyed by the value returned
 * by the [keySelector] function.
 */
@FlowPreview
fun <T, K, V> Flow<T>.groupBy(keySelector: suspend (T) -> K, valueSelector: suspend (T) -> V) : Flow<GroupedFlow<K, V>> =
        FlowGroupBy(this, keySelector, valueSelector)

/**
 * Collects all items of the upstream into a list.
 */
fun <T> Flow<T>.toList() : Flow<List<T>> {
    val self = this
    return flow {
        val list = ArrayList<T>()
        self.collect {
            list.add(it)
        }
        emit(list)
    }
}

/**
 * Drops items from the upstream when the downstream is not ready to receive them.
 */
@FlowPreview
fun <T> Flow<T>.onBackpressurureDrop() : Flow<T> = FlowOnBackpressureDrop(this)

/**
 * Maps items from the upstream to [Flow] and relays its items while dropping upstream items
 * until the current inner [Flow] completes.
 */
@FlowPreview
fun <T, R> Flow<T>.flatMapDrop(mapper: suspend (T) -> Flow<R>) : Flow<R> = FlowFlatMapDrop(this, mapper)

// -----------------------------------------------------------------------------------------
// Parallel Extensions
// -----------------------------------------------------------------------------------------

/**
 * Consumes the upstream and dispatches individual items to a parrallel rail
 * of the parallel flow for further consumption.
 */
fun <T> Flow<T>.parallel(parallelism: Int, runOn: (Int) -> CoroutineDispatcher) : ParallelFlow<T> =
    FlowParallel(this, parallelism, runOn)

/**
 * Consumes the parallel upstream and turns it into a sequential flow again.
 */
@FlowPreview
fun <T> ParallelFlow<T>.sequential() : Flow<T> =
        FlowSequential(this)

/**
 * Maps the values of the upstream in parallel.
 */
fun <T, R> ParallelFlow<T>.map(mapper: suspend (T) -> R) : ParallelFlow<R> =
        FlowParallelMap(this, mapper)

/**
 * Filters the values of the upstream in parallel.
 */
fun <T> ParallelFlow<T>.filter(predicate: suspend (T) -> Boolean) : ParallelFlow<T> =
        FlowParallelFilter(this, predicate)

/**
 * Transform each upstream item into zero or more emits for the downstream
 * in parallel.
 */
fun <T, R> ParallelFlow<T>.transform(callback: suspend FlowCollector<R>.(T) -> Unit) : ParallelFlow<R> =
        FlowParallelTransform(this, callback)


/**
 * Maps the upstream value on each rail onto a Flow and emits their values
 * in order on the same rail.
 */
@ExperimentalCoroutinesApi
fun <T, R> ParallelFlow<T>.concatMap(mapper: suspend (T) -> Flow<R>) : ParallelFlow<R> =
        FlowParallelTransform(this) {
            emitAll(mapper(it))
        }

/**
 * Reduces the source items into a single value on each rail
 * and emits those.
 */
fun <T, R> ParallelFlow<T>.reduce(seed: suspend () -> R, combine: suspend (R, T) -> R) : ParallelFlow<R> =
        FlowParallelReduce(this, seed, combine)

/**
 * Reduce the values within the parallel rails and
 * then reduce the rails to a single result value.
 */
@FlowPreview
fun <T> ParallelFlow<T>.reduce(combine: suspend (T, T) -> T) : Flow<T> =
        FlowParallelReduceSequential(this, combine)