package com.example.cashlog.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DenominationTest {

    @Test
    fun testFaceValuesMatchSpec() {
        assertEquals(50000, Denomination.W50000.faceValue)
        assertEquals(10000, Denomination.W10000.faceValue)
        assertEquals(5000, Denomination.W5000.faceValue)
        assertEquals(1000, Denomination.W1000.faceValue)
        assertEquals(500, Denomination.C500.faceValue)
        assertEquals(100, Denomination.C100.faceValue)
        assertEquals(10, Denomination.C10_OLD.faceValue)
        assertEquals(50, Denomination.C50.faceValue)
        assertEquals(10, Denomination.C10_NEW.faceValue)
    }

    @Test
    fun testLabelsMatchSpec() {
        assertEquals("오만원", Denomination.W50000.label)
        assertEquals("일만원", Denomination.W10000.label)
        assertEquals("오천원", Denomination.W5000.label)
        assertEquals("천원", Denomination.W1000.label)
        assertEquals("오백원", Denomination.C500.label)
        assertEquals("백원", Denomination.C100.label)
        assertEquals("구십", Denomination.C10_OLD.label)
        assertEquals("오십원", Denomination.C50.label)
        assertEquals("신십", Denomination.C10_NEW.label)
    }

    @Test
    fun testNoValueDerivedFromLabels() {
        assertEquals(10, Denomination.C10_OLD.faceValue)
        
        for (denom in Denomination.values()) {
            val labelValue = when (denom.label) {
                "오만원" -> 50000
                "일만원" -> 10000
                "오천원" -> 5000
                "천원" -> 1000
                "오백원" -> 500
                "백원" -> 100
                "구십" -> 10 // NOT 90
                "오십원" -> 50
                "신십" -> 10
                else -> -1
            }
            assertEquals("Logic must match explicit mapping for ${denom.name}", labelValue, denom.faceValue)
        }
    }
}
