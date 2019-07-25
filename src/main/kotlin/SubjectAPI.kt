package hu.akarnokd.kotlin.flow

import kotlinx.coroutines.flow.FlowCollector

/**
 * Base interface for suspendable push signals emit, emitError and complete.
 */
interface SubjectAPI<T> : FlowCollector<T> {

    /**
     * Signal an Throwable to the collector.
     */
    suspend fun emitError(ex: Throwable)

    /**
     * Indicate no further items will be produced.
     */
    suspend fun complete()

    /**
     * Returns true if this subject has collectors waiting for data.
     */
    fun hasCollectors() : Boolean

    /**
     * Returns the number of collectors waiting for data.
     */
    fun collectorCount() : Int
}