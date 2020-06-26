package com.uu_uce

import org.junit.Test
import org.junit.Assert.*
import java.util.*

@Suppress("ConstantConditionIf", "LiftReturnOrAssignment", "VARIABLE_WITH_REDUNDANT_INITIALIZER",
    "CascadeIf"
)
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
        for(i in 0 until numbers.size)
            numbers[i]++

        assertEquals(numbers, listOf(2,3,4,5))
    }
    /*
    Testing for null is annoying...
    Kotlin has special solutions for that, using nice operators.
    Use them!
     */
    @Test
    fun ccNullOperatorOverChecking(){
        val x: Int? = 3
        val y: Int? = 4
        var z = 0
        //YES
        z += (x ?: 0) * (y ?: 1)
        assertEquals(z, 12)
        //NO
        if(x != null){
            if(y != null)
                z += x * y
            else
                z += x
        }
        assertEquals(z, 24)
    }
    /*
    Nested logic can be very complicated to reason about.
    We handeling null/empty/default values often returning gives
    an program that is easier to reason about.
    It can worth making a function for, making the code more
    modular and testable at the same time!
     */
    @Test
    fun ccReturnOverNest(){
        //YES
        val aa = 9
        val bb = 3
        val cc = 4
        fun testf(a: Int, b: Int, c: Int): Int{
            if(a == b)
                return 0
            if(b > c)
                return 3
            else if(a > c)
                return c
            return a + b + c
        }
        val x = testf(aa, bb, cc)
        assertEquals(4, x)
        //NO
        val y = if(aa == bb) 0
        else{
            if(bb > cc) 3
            else if(aa > cc) cc
            else aa + bb + cc
        }
        assertEquals(x, y)
    }
    /*
    Prefer flow expressions over defining a var and setting it inside the flow element
    right after it. It takes less lines and looks cleaner.
    We also get the bonus we can make it "val" and not "var".
    Imagin x does not need to change.
    When changing a complicated function, the mistake is made quite easily to assign it.
    Now something is not sound anymore.
    With "val" this can't happen: the compiler will scream at you.
    Then you evaluate consciously whether you should stop assigning or whether it should change
    to "var". If it was a "var" already, we might not notice what we are doing.
     */
    @Test
    fun ccFlowExpressionsOverSetting(){
        //YES
        val x = if(false) 4
        else 5
        //NO
        var y = 0
        if(false)
            y = 4
        else
            y = 5
        assertEquals(x, y)
        // x = 0 // error: can't change a value!
        // y = 0 // this can happen, dangerous!
    }

    /*
    Avoid the !!. operator to avoid NPE's
    When dealing with Java libs that return null's, you could be forced to use it.
    Always look if there is an idiomatic Kotlin way of doing it, or having a default value.
     */
    @Test
    fun ccAvoidNPE(){
        //NO
        val q: Queue<Int> = LinkedList()
        q.add(1)
        q.add(2)
        val p = q.peek()!!.toFloat() + 0.1f
        assertEquals(1.1f, p)
        //YES
        val p1 = (q.peek() ?: 0).toFloat() + 0.1f
        assertEquals(1.1f, p1)
        //YES
        val qq: MutableList<Int> = mutableListOf(1, 2)
        val pp = qq.first() + 0.1f
        assertEquals(1.1f, pp)
    }
}
