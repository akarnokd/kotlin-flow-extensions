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

import hu.akarnokd.kotlin.flow.impl.FlowPublishFunction
import hu.akarnokd.kotlin.flow.impl.FlowStartCollectOn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlin.coroutines.coroutineContext

/**
 * Shares a single collector towards the upstream source and multicasts
 * values to any number of consumers which then can produce the output
 * flow of values.
 */
@FlowPreview
fun <T, R> Flow<T>.publish(transform: suspend (Flow<T>) -> Flow<R>) : Flow<R> =
    FlowPublishFunction(this, transform)

fun <T> Flow<T>.startCollectOn(scope: CoroutineScope, dispatcher: CoroutineDispatcher) : Flow<T> =
        FlowStartCollectOn(this, scope, dispatcher)