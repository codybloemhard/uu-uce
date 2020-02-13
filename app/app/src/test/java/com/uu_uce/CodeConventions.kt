package com.uu_uce

import org.junit.Test
import org.junit.Assert.*

class CodeConventions {
    @Test
    fun ccCheck() {
        assertEquals(2, 1 + 1)
    }

    /*
    Prefer functional methods over looping:
    Prefer map,filter,forEach over using a loop.
    */
    @Test
    fun ccFunctionalOverLoop(){
        var numbers = mutableListOf(0,1,2,3)
        //YES
        numbers = numbers.map{n -> n + 1}.toMutableList()
        //NO
        for(i in 0 until numbers.size){
            numbers[i]++
        }
        assertEquals(numbers, listOf(2,3,4,5))
    }
}