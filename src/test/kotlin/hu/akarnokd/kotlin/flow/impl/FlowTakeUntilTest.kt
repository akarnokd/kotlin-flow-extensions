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

import hu.akarnokd.kotlin.flow.assertResult
import hu.akarnokd.kotlin.flow.range
import hu.akarnokd.kotlin.flow.takeUntil
import hu.akarnokd.kotlin.flow.timer
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.util.concurrent.TimeUnit

@FlowPreview
class FlowTakeUntilTest {
    @Test
    fun basic() = runBlocking {

        range(1, 10)
                .map {
                    delay(100)
                    it
                }
                .takeUntil(timer(550, TimeUnit.MILLISECONDS))
                .assertResult(1, 2, 3, 4, 5)

    }

    @Test
    fun untilTakesLonger() = runBlocking {

        range(1, 10)
                .map {
                    delay(50)
                    it
                }
                .takeUntil(timer(1000, TimeUnit.MILLISECONDS))
                .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)

    }
}