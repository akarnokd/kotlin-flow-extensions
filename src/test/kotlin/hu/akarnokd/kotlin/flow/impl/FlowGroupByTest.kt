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

@ExperimentalCoroutinesApi
@FlowPreview
class FlowGroupByTest {

    @Test
    fun basic() = runBlocking {
        range(1, 10)
                .groupBy { it % 2 }
                .flatMapMerge { it.toList() }
                .assertResultSet(listOf(1, 3, 5, 7, 9), listOf(2, 4, 6, 8, 10))
    }

    @Test
    fun basicValueSelector() = runBlocking {
        range(1, 10)
                .groupBy({ it % 2 }) { it + 1}
                .flatMapMerge { it.toList() }
                .assertResultSet(listOf(2, 4, 6, 8, 10), listOf(3, 5, 7, 9, 11))
    }

    @Test
    fun oneOfEach() = runBlocking {
        range(1, 10)
                .groupBy { it % 2 }
                .flatMapMerge { it.take(1) }
                .assertResultSet(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    }

    @Test
    fun maxGroups() = runBlocking {
        range(1, 10)
                .groupBy { it % 3 }
                .take(2)
                .flatMapMerge { it.toList() }
                .assertResultSet(listOf(1, 4, 7, 10), listOf(2, 5, 8))
    }

    @Test
    @Ignore("Hangs for some reason")
    fun takeItems() = runBlocking {
        range(1, 10)
                .groupBy { it % 2 }
                .flatMapMerge { it }
                .take(2)
                .assertResultSet(1, 2)
    }

    @Test
    fun takeGroupsAndItems() = runBlocking {
        range(1, 10)
                .groupBy { it % 3 }
                .take(2)
                .flatMapMerge { it }
                .take(2)
                .assertResultSet(1, 2)
    }

}