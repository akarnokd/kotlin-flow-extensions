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

import hu.akarnokd.kotlin.flow.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

@FlowPreview
class FlowMergeArrayTest {
    @Test
    fun basic() = runBlocking {

        mergeArray(range(1, 5), range(6, 5))
                .assertResultSet(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)

    }

    @Test
    fun oneSource() = runBlocking {

        mergeArray(range(1, 5))
                .assertResultSet(1, 2, 3, 4, 5)

    }

    @Test
    fun noSources() = runBlocking {

        mergeArray<Int>()
                .assertResultSet()

    }

    @Test
    fun manyAsync() = runBlocking {

        val n = 100_000

        val m = mergeArray(
                    range(0, n / 2).startCollectOn(Dispatchers.IO),
                    range(0, n / 2).startCollectOn(Dispatchers.IO)
        )
                .count()

        assertEquals(n, m)

    }

}