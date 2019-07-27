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

package hu.akarnokd.kotlin.flow.impl

import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReferenceArray

/**
 * A Single-Producer Single-Consumer bounded queue.
 */
internal class SpscArrayQueue<T> : AtomicReferenceArray<Any> {

    private val consumerIndex = AtomicLong()

    private val producerIndex = AtomicLong()

    constructor(capacity: Int) : super(nextPowerOf2(capacity)) {
        for (i in 0 until length()) {
            lazySet(i, EMPTY)
        }
    }

    fun offer(value: T) : Boolean {
        val mask = length() - 1
        val pir = producerIndex
        val pi = pir.get()

        val offset = pi.toInt() and mask

        if (get(offset) == EMPTY) {
            lazySet(offset, value)
            pir.lazySet(pi + 1)
            return true
        }
        return false
    }

    /**
     * Poll the next available item into the first slot of the
     * array or return false if no item is available
     */
    fun poll(out: Array<T>) : Boolean {
        val mask = length() - 1
        val cir = consumerIndex
        val ci = cir.get()
        val offset = ci.toInt() and mask

        val v = get(offset)
        if (v == EMPTY) {
            return false
        }
        @Suppress("UNCHECKED_CAST")
        out[0] = v as T
        lazySet(offset, EMPTY)
        cir.lazySet(ci + 1)
        return true
    }

    fun isEmpty() : Boolean = consumerIndex.get() == producerIndex.get()

    fun clear() {
        val mask = length() - 1
        val cir = consumerIndex
        var ci = cir.get()

        while (true) {
            val offset = ci.toInt() and mask

            if (get(offset) == EMPTY) {
                break
            }
            lazySet(offset, EMPTY)

            ci++
        }
        cir.lazySet(ci)
    }

    companion object {
        val EMPTY = Object()
    }
}

fun nextPowerOf2(x: Int) : Int {
    val h = Integer.highestOneBit(x)
    if (h == x) {
        return x
    }
    return h * 2
}