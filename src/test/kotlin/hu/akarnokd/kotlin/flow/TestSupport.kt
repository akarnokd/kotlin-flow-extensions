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

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import org.junit.Assert.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Execute a suspendable block with a single-threaded executor service.
 */
suspend fun withSingle(block: suspend (ExecutorService) -> Unit) {
    val exec = Executors.newSingleThreadExecutor()
    try {
        block(exec)
    } finally {
        exec.shutdownNow()
    }
}

suspend fun withParallels(parallelism: Int, block: suspend (List<CoroutineDispatcher>) -> Unit) {
    val executors = Array(parallelism) { Executors.newSingleThreadExecutor() }

    try {
        block(executors.map { it.asCoroutineDispatcher() })
    } finally {
        executors.forEach { it.shutdownNow() }
    }
}

suspend fun <T> Flow<T>.assertResult(vararg values: T) {
    val list = ArrayList<T>()

    this.collect {
        list.add(it)
    }

    assertEquals(values.asList(), list)
}

suspend fun <T> Flow<T>.assertResultSet(vararg values: T) {
    val set = HashSet<T>()

    this.collect {
        set.add(it)
    }

    assertEquals("Number of values differ", values.size, set.size)

    values.forEach { assertTrue("Missing: " + it, set.contains(it)) }
}

suspend fun <T, E : Throwable> Flow<T>.assertFailure(errorClazz: Class<E>, vararg values: T) {
    val list = ArrayList<T>()

    var error : Throwable? = null
    try {
        this.collect {
            list.add(it)
        }
    } catch (ex: Throwable) {
        error = ex
    }

    assertEquals(values.asList(), list)

    assertNotNull(error)
    assertTrue("" + error, errorClazz.isInstance(error))
}

suspend fun <T, E : Throwable> Flow<T>.assertError(errorClazz: Class<E>) {
    var error : Throwable? = null
    try {
        this.collect {
        }
    } catch (ex: Throwable) {
        error = ex
    }

    assertNotNull(error)
    assertTrue("" + error, errorClazz.isInstance(error))
}
