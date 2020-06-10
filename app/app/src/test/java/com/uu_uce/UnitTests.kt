package com.uu_uce

import com.uu_uce.mapOverlay.coordToScreen
import com.uu_uce.mapOverlay.screenToCoord
import com.uu_uce.services.UTMCoordinate
import com.uu_uce.services.degreeToUTM
import com.uu_uce.services.latToUTMLetter
import com.uu_uce.shapefiles.FileReader
import com.uu_uce.shapefiles.p2
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.random.Random
import kotlin.random.nextUInt
import kotlin.random.nextULong

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
        val utm = degreeToUTM(Pair(55.0f,110.0f))
        assertEquals(utm.letter, 'U')
        assertEquals(utm.zone, 49)
        assertEquals(utm.east, 436032.58f, 0.1f)
        assertEquals(utm.north, 6095248.71f, 0.1f)
    }

    @Test
    fun testScreenMapConversion(){
        val coordinate = UTMCoordinate(31, 'N', 313368.0f, 4671833.6f)
        val viewport = Pair(
                p2(308968.83f, 4667733.3f),
                p2(319547.5f, 4682999.6f))

        val screenWidth = 1920
        val screenHeight = 1080

        val screenCoordinate = coordToScreen(coordinate, viewport, screenWidth, screenHeight)
        val mapCoordinate = screenToCoord(screenCoordinate, viewport, screenWidth, screenHeight)
        assertEquals(mapCoordinate.east, coordinate.east, 0.1f)
        assertEquals(mapCoordinate.north, coordinate.north, 0.1f)
    }

    @ExperimentalUnsignedTypes
    @Test
    fun testBinaryReader(){
        val longs = ULongArray(20){ Random.nextULong()}
        val ubytes: MutableList<UByte> = mutableListOf()
        for(long in longs){
            ubytes.add((long shr 56).toUByte())
            ubytes.add((long shr 48).toUByte())
            ubytes.add((long shr 40).toUByte())
            ubytes.add((long shr 32).toUByte())
            ubytes.add((long shr 24).toUByte())
            ubytes.add((long shr 16).toUByte())
            ubytes.add((long shr 8).toUByte())
            ubytes.add((long shr 0).toUByte())
        }
        var reader = FileReader(ubytes.toUByteArray())
        for(long in longs){
            assertEquals(long, reader.readULong())
        }

        val ints = UIntArray(20){ Random.nextUInt()}
        ubytes.clear()
        for(int in ints){
            ubytes.add((int shr 24).toUByte())
            ubytes.add((int shr 16).toUByte())
            ubytes.add((int shr 8).toUByte())
            ubytes.add((int shr 0).toUByte())
        }
        reader = FileReader(ubytes.toUByteArray())
        for(int in ints){
            assertEquals(int, reader.readUInt())
        }

        val shorts = UIntArray(20){ Random.nextUInt()}
        ubytes.clear()
        for(short in shorts){
            ubytes.add((short shr 8).toUByte())
            ubytes.add((short shr 0).toUByte())
        }
        reader = FileReader(ubytes.toUByteArray())
        for(short in shorts){
            assertEquals(short.toUShort(), reader.readUShort())
        }
    }


}
