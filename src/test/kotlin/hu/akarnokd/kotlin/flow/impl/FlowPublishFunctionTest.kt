package hu.akarnokd.kotlin.flow.impl

import hu.akarnokd.kotlin.flow.assertResult
import hu.akarnokd.kotlin.flow.publish
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Ignore
import org.junit.Test

@FlowPreview
class FlowPublishFunctionTest {
    @Test
    @Ignore("Doesn't work, not sure how not to deadlock the suspension")
    fun basic() = runBlocking {

        arrayOf(1, 2, 3, 4, 5)
                .asFlow()
                .publish {
                    shared ->

                    shared.filter { it % 2 == 0}
                }
                .assertResult(2, 4)
    }
}