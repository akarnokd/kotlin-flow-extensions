/*
 * Copyright 2019-2020 David Karnok
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

import org.junit.Test
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.*
import org.junit.Assert.assertTrue
import java.io.IOException
import java.lang.IllegalStateException
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.collections.ArrayList
import kotlin.test.assertEquals
import kotlin.test.fail

@FlowPreview
class UnicastSubjectTest {

    @Test
    fun offlineBasic() = runBlocking {
        val us = UnicastSubject<Int>()

        for (i in 1..5) {
            us.emit(i)
        }
        us.complete()

        val list = ArrayList<Int>()

        us.collect { list.add(it) }

        assertEquals(arrayListOf(1, 2, 3, 4, 5), list)
    }

    @Test
    fun offlineError() = runBlocking {
        val us = UnicastSubject<Int>()

        for (i in 1..5) {
            us.emit(i)
        }
        us.emitError(IOException())

        val list = ArrayList<Int>()

        try {
            us.collect { list.add(it) }
            fail("Should have thrown")
        } catch (e: IOException) {
            // expected
        }

        assertEquals(arrayListOf(1, 2, 3, 4, 5), list)
    }


    @Test
    fun offlineOneCollector() = runBlocking {
        val us = UnicastSubject<Int>()

        for (i in 1..5) {
            us.emit(i)
        }
        us.complete()

        val list = ArrayList<Int>()

        us.collect { list.add(it) }

        try {
            us.collect { list.add(it) }
            fail("Should have thrown")
        } catch (e: IllegalStateException) {
            // expected
        }

        assertEquals(arrayListOf(1, 2, 3, 4, 5), list)
    }

    @Test
    fun offlineTake() = runBlocking {
        val us = UnicastSubject<Int>()

        for (i in 1..5) {
            us.emit(i)
        }
        us.complete()

        val list = ArrayList<Int>()

        us.take(3).collect { list.add(it) }

        assertEquals(arrayListOf(1, 2, 3), list)

        assertTrue(us.collectorCancelled())
    }

    @Test
    fun onlineBasic() = runBlocking {
        withSingle {
            val us = UnicastSubject<Int>()

            launch(it.asCoroutineDispatcher()) {
                while (!us.hasCollectors()) {
                    delay(1)
                }
                for (i in 1..5) {
                    us.emit(i)
                }
                us.complete()
            }

            val list = ArrayList<Int>()

            us.collect { list.add(it) }

            assertEquals(arrayListOf(1, 2, 3, 4, 5), list)
        }
    }

    @Test
    fun onlineLong() = runBlocking {
        withSingle {
            val us = UnicastSubject<Int>()

            launch(it.asCoroutineDispatcher()) {
                while (!us.hasCollectors()) {
                    delay(1)
                }
                for (i in 1..500000) {
                    us.emit(i)
                }
                us.complete()
            }

            val list = Array(1) { 0 }

            us.collect { list[0]++ }

            assertEquals(500000, list[0])
        }
    }

    @Test
    fun onlineTakeLong() = runBlocking {
        withSingle {
            val us = UnicastSubject<Int>()

            launch(it.asCoroutineDispatcher()) {
                while (!us.hasCollectors()) {
                    delay(1)
                }
                for (i in 1..500000) {
                    us.emit(i)
                }
                us.complete()
            }

            val list = Array(1) { 0 }

            us.take(250000).collect { list[0]++ }

            assertEquals(250000, list[0])
        }
    }
}