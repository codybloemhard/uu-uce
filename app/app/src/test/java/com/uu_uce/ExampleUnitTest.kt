package com.uu_uce

import com.uu_uce.mapOverlay.aaBoundingBoxContains
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
        assertEquals(aaBoundingBoxContains(
            Pair(1.0, 1.0),
            Pair(2.0, 2.0),
            Pair(2.1, 0.0),
            Pair(3.0, 9.0)
        ), false)
        assertEquals(aaBoundingBoxContains(
            Pair(1.0, 1.0),
            Pair(2.0, 2.0),
            Pair(2.0, 0.0),
            Pair(3.0, 9.0)
        ), true)
    }
}
