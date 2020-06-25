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
            Pair(1.0f, 1.0f),
            Pair(2.0f, 2.0f),
            Pair(2.1f, 0.0f),
            Pair(3.0f, 9.0f)
        ), false)
        assertEquals(aaBoundingBoxContains(
            Pair(1.0f, 1.0f),
            Pair(2.0f, 2.0f),
            Pair(2.0f, 0.0f),
            Pair(3.0f, 9.0f)
        ), true)
    }
}

/* This program has been developed by students from the bachelor Computer
# Science at Utrecht University within the Software Project course. ©️ Copyright
# Utrecht University (Department of Information and Computing Sciences)*/

