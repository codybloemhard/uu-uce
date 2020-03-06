package com.uu_uce

import com.uu_uce.mapOverlay.boundingBoxIntersect
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun test_aabb_inter(){
        assertEquals(boundingBoxIntersect(
            Triple(1.0, 1.0, 0.0),
            Triple(2.0, 2.0, 0.0),
            Triple(2.1, 0.0, 0.0),
            Triple(3.0, 9.0, 0.0)
        ), false)
        assertEquals(boundingBoxIntersect(
            Triple(1.0, 1.0, 0.0),
            Triple(2.0, 2.0, 0.0),
            Triple(2.0, 0.0, 0.0),
            Triple(3.0, 9.0, 0.0)
        ), true)
    }
}
