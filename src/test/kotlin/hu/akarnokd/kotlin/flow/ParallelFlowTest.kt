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

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.runBlocking
import org.junit.Test

@FlowPreview
class ParallelFlowTest {

    @Test
    fun basic() = runBlocking {
        withParallels(1) { execs ->
            arrayOf(1, 2, 3, 4, 5)
                    .asFlow()
                    .parallel(execs.size) { execs[it] }
                    .sequential()
                    .assertResult(1, 2, 3, 4, 5)
        }
    }

    @Test
    fun basic2() = runBlocking {
        withParallels(2) { execs ->
            arrayOf(1, 2, 3, 4, 5)
                    .asFlow()
                    .parallel(execs.size) { execs[it] }
                    .sequential()
                    .assertResultSet(1, 2, 3, 4, 5)
        }
    }

    @Test
    fun basic4() = runBlocking {
        withParallels(4) { execs ->
            arrayOf(1, 2, 3, 4, 5)
                    .asFlow()
                    .parallel(execs.size) { execs[it] }
                    .sequential()
                    .assertResultSet(1, 2, 3, 4, 5)
        }
    }

    @Test
    fun map() = runBlocking {
        withParallels(1) { execs ->
            arrayOf(1, 2, 3, 4, 5)
                    .asFlow()
                    .parallel(execs.size) { execs[it] }
                    .map { it + 1 }
                    .sequential()
                    .assertResult(2, 3, 4, 5, 6)
        }
    }

    @Test
    fun map2() = runBlocking {
        withParallels(2) { execs ->
            arrayOf(1, 2, 3, 4, 5)
                    .asFlow()
                    .parallel(execs.size) { execs[it] }
                    .map { it + 1 }
                    .sequential()
                    .assertResultSet(2, 3, 4, 5, 6)
        }
    }

    @Test
    fun filter() = runBlocking {
        withParallels(1) { execs ->
            arrayOf(1, 2, 3, 4, 5)
                    .asFlow()
                    .parallel(execs.size) { execs[it] }
                    .filter { it % 2 == 0 }
                    .sequential()
                    .assertResult(2, 4)
        }
    }

    @Test
    fun filter2() = runBlocking {
        withParallels(2) { execs ->
            arrayOf(1, 2, 3, 4, 5)
                    .asFlow()
                    .parallel(execs.size) { execs[it] }
                    .filter { it % 2 == 0}
                    .sequential()
                    .assertResultSet(2, 4)
        }
    }

    @Test
    fun moreParallelismThanValues() = runBlocking {
        withParallels(2) { execs ->
            arrayOf(1)
                    .asFlow()
                    .parallel(execs.size) { execs[it] }
                    .sequential()
                    .assertResult(1)
        }
    }

    @Test
    fun transformEmpty() = runBlocking {
        withParallels(1) { execs ->
            arrayOf(1, 2, 3, 4, 5)
                    .asFlow()
                    .parallel(execs.size) { execs[it] }
                    .transform<Int, Int> {  }
                    .sequential()
                    .assertResult()
        }
    }

    @Test
    fun transformAsMap() = runBlocking {
        withParallels(1) { execs ->
            arrayOf(1, 2, 3, 4, 5)
                    .asFlow()
                    .parallel(execs.size) { execs[it] }
                    .transform<Int, Int> { emit(it + 1) }
                    .sequential()
                    .assertResult(2, 3, 4, 5, 6)
        }
    }

    @Test
    fun transformAsMany() = runBlocking {
        withParallels(1) { execs ->
            arrayOf(1, 2, 3, 4, 5)
                    .asFlow()
                    .parallel(execs.size) { execs[it] }
                    .transform<Int, Int> {
                        emit(it)
                        emit(it + 1)
                    }
                    .sequential()
                    .assertResult(1, 2, 2, 3, 3, 4, 4, 5, 5, 6)
        }
    }
}