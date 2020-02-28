package com.uu_uce

import com.uu_uce.services.degreeToUTM
import com.uu_uce.services.latToUTMLetter
import org.junit.Assert.assertEquals
import org.junit.Test

class UnitTests {
    private fun uglyLatToLetter(lat: Double): Char{
        when {
            lat < -72 ->    return 'C'
            lat < -64 ->    return 'D'
            lat < -56 ->    return 'E'
            lat < -48 ->    return 'F'
            lat < -40 ->    return 'G'
            lat < -32 ->    return 'H'
            lat < -24 ->    return 'J'
            lat < -16 ->    return 'K'
            lat < -8 ->     return 'L'
            lat < 0 ->      return 'M'
            lat < 8 ->      return 'N'
            lat < 16 ->     return 'P'
            lat < 24 ->     return 'Q'
            lat < 32 ->     return 'R'
            lat < 40 ->     return 'S'
            lat < 48 ->     return 'T'
            lat < 56 ->     return 'U'
            lat < 64 ->     return 'V'
            lat < 72 ->     return 'W'
            else ->         return 'X'
        }
    }
    @Test
    fun testLatToUTMLetter() {
        for(x in -100..100){
            val l = x.toDouble()
            assertEquals(latToUTMLetter(l), uglyLatToLetter(l))
        }
    }
    @Test
    fun testDegreeToUTM(){
        val utm = degreeToUTM(Pair(55.0,110.0))
        assertEquals(utm.letter, 'U')
        assertEquals(utm.zone, 49)
        assertEquals(utm.east, 436032.58, 0.1)
        assertEquals(utm.north, 6095248.71, 0.1)
    }
}
