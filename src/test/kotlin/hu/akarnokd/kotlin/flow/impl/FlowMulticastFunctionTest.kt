package hu.akarnokd.kotlin.flow.impl

import hu.akarnokd.kotlin.flow.assertResult
import hu.akarnokd.kotlin.flow.concatWith
import hu.akarnokd.kotlin.flow.publish
import hu.akarnokd.kotlin.flow.replay
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Ignore
import org.junit.Test
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@FlowPreview
class FlowMulticastFunctionTest {
    @Test
    fun publish() = runBlocking {
        arrayOf(1, 2, 3, 4, 5)
                .asFlow()
                .publish {
                    shared ->
                        shared.filter { it % 2 == 0}
                }
                .assertResult(2, 4)
    }

    @Test
    fun replay() = runBlocking {
        arrayOf(1, 2, 3, 4, 5)
                .asFlow()
                .replay {
                    shared ->
                    shared.filter { it % 2 == 0}
                }
                .assertResult(2, 4)
    }

    @Test
    fun replaySizeBound() = runBlocking {
        arrayOf(1, 2, 3, 4, 5)
                .asFlow()
                .replay(2) {
                    shared ->
                        shared.filter { it % 2 == 0}
                                .concatWith(shared)

                }
                .assertResult(2, 4, 4, 5)
    }

    @Test
    fun replayTimeBound() = runBlocking {
        arrayOf(1, 2, 3, 4, 5)
                .asFlow()
                .replay(1L, TimeUnit.MINUTES) {
                    shared ->
                    shared.filter { it % 2 == 0}
                            .concatWith(shared)

                }
                .assertResult(2, 4, 1, 2, 3, 4, 5)
    }

    @Test
    fun replaySizeAndTimeBound() = runBlocking {
        arrayOf(1, 2, 3, 4, 5)
                .asFlow()
                .replay(2, 1L, TimeUnit.MINUTES) {
                    shared ->
                    shared.filter { it % 2 == 0}
                            .concatWith(shared)

                }
                .assertResult(2, 4, 4, 5)
    }

    @Test
    fun replaySizeAndTimeBoundCustomTime() = runBlocking {
        arrayOf(1, 2, 3, 4, 5)
                .asFlow()
                .replay(2, 1L, TimeUnit.MINUTES, { 0L }) {
                    shared ->
                    shared.filter { it % 2 == 0}
                            .concatWith(shared)

                }
                .assertResult(2, 4, 4, 5)
    }
}