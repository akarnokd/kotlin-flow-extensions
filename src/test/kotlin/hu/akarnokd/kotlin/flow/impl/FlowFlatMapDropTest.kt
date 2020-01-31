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

import hu.akarnokd.kotlin.flow.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.junit.Ignore
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
@FlowPreview
class FlowFlatMapDropTest {

    @Test
    fun basic() = runBlocking {
        range(1, 10)
        .map {
            delay(100)
            it
        }
        .flatMapDrop {
            range(it * 100, 5)
                    .map {
                        delay(30)
                        it
                    }
        }
        .assertResult(
                100, 101, 102, 103, 104,
                300, 301, 302, 303, 304,
                500, 501, 502, 503, 504,
                700, 701, 702, 703, 704,
                900, 901, 902, 903, 904
        )
    }

    @Test
    fun take() = runBlocking {
        val input = AtomicInteger()

        range(1, 10)
        .map {
            delay(100)
            it
        }
        .flatMapDrop {
            input.set(it)
            range(it * 100, 5)
                    .map {
                        delay(30)
                        it
                    }
        }
        .take(7)
        .assertResult(
                100, 101, 102, 103, 104,
                300, 301
        )

        assertEquals(3, input.get())
    }

}