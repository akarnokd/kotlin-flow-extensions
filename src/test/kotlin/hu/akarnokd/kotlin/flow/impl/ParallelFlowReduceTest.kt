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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.runBlocking
import org.junit.Test

@FlowPreview
class ParallelFlowReduceTest {

    @Test
    fun basic() = runBlocking {
        withParallels(1) { execs ->
            arrayOf(1, 2, 3, 4, 5)
                    .asFlow()
                    .parallel(execs.size) { execs[it] }
                    .reduce({ 0 }, { a, b -> a + b })
                    .sequential()
                    .assertResult(15)
        }
    }

    @Test
    fun reduceSeq() = runBlocking {
        withParallels(1) { execs ->
            arrayOf(1, 2, 3, 4, 5)
                    .asFlow()
                    .parallel(execs.size) { execs[it] }
                    .reduce { a, b -> a + b }
                    .assertResult(15)
        }
    }

    @Test
    fun reduceSeqEmpty() = runBlocking {
        withParallels(1) { execs ->
            arrayOf<Int>()
                    .asFlow()
                    .parallel(execs.size) { execs[it] }
                    .reduce ( { 0 }) { a, b -> a + b }
                    .sequential()
                    .assertResult(0)
        }
    }

    @Test
    fun reduceSeqEmpy() = runBlocking {
        withParallels(1) { execs ->
            arrayOf<Int>()
                    .asFlow()
                    .parallel(execs.size) { execs[it] }
                    .reduce { a, b -> a + b }
                    .assertResult()
        }
    }

    @Test
    fun reduceSeq2() = runBlocking {
        withParallels(2) { execs ->
            arrayOf(1, 2, 3, 4, 5)
                    .asFlow()
                    .parallel(execs.size) { execs[it] }
                    .reduce { a, b -> a + b }
                    .assertResult(15)
        }
    }

}