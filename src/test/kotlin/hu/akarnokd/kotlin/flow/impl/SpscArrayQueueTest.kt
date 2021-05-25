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

package hu.akarnokd.kotlin.flow.impl

import org.junit.Assert.*
import org.junit.Test

class SpscArrayQueueTest {
    @Test
    fun offerPoll()  {
        val q = SpscArrayQueue<Int>(10)
        val a = Array<Any?>(1) { 0 }

        for (i in 0 until 10) {
            assertTrue(q.offer(i))

            assertFalse(q.isEmpty())

            assertTrue(q.poll(a))

            assertTrue(q.isEmpty())

            assertEquals(i, a[0])
        }
    }

    @Test
    fun offerAllPollAll()  {
        val q = SpscArrayQueue<Int>(10)
        val a = Array<Any?>(1) { 0 }

        for (i in 0 until 16) {
            assertTrue(q.offer(i))

            assertFalse(q.isEmpty())
        }

        assertFalse(q.offer(16))
        for (i in 0 until 16) {
            assertFalse(q.isEmpty())

            assertTrue(q.poll(a))

            assertEquals(i, a[0])
        }

        assertTrue(q.isEmpty())

        assertFalse(q.poll(a))
        assertTrue(q.isEmpty())
    }

    @Test
    fun clear() {
        val q = SpscArrayQueue<Int>(16)
        val a = Array<Any?>(1) { 0 }

        for (i in 0 until 10) {
            q.offer(i)
        }

        q.clear()

        assertTrue(q.isEmpty())

        for (i in 0 until 16) {
            assertTrue(q.offer(i))
        }

        assertFalse(q.offer(16))
        for (i in 0 until 16) {
            assertFalse(q.isEmpty())

            assertTrue(q.poll(a))

            assertEquals(i, a[0])
        }

        assertTrue(q.isEmpty())

        assertFalse(q.poll(a))
        assertTrue(q.isEmpty())
    }
}