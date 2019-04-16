package org.iota.ict.ixi.serialization.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UtilsTest {

    @Test
    public void decimalConversionDoubleTest(){
        byte[] trits = Utils.decimalToTrits(1d,9);
        BigDecimal decimal = Utils.decimalFromTrits(trits);
        assertEquals(1d,decimal.doubleValue());

        trits = Utils.decimalToTrits(2d,9);
        decimal = Utils.decimalFromTrits(trits);
        assertEquals(2d,decimal.doubleValue());

        trits = Utils.decimalToTrits(Double.MAX_VALUE,54);
        decimal = Utils.decimalFromTrits(trits);
        assertEquals(Double.MAX_VALUE,decimal.doubleValue());

        trits = Utils.decimalToTrits(Double.MIN_VALUE,54);
        decimal = Utils.decimalFromTrits(trits);
        assertEquals(Double.MIN_VALUE,decimal.doubleValue());

        trits = Utils.decimalToTrits(1.1d,9);
        decimal = Utils.decimalFromTrits(trits);
        assertEquals(1.1d,decimal.doubleValue());

        trits = Utils.decimalToTrits(2.1d,9);
        decimal = Utils.decimalFromTrits(trits);
        assertEquals(2.1d,decimal.doubleValue());
    }

    @Test
    public void decimalConversionFloatTest(){
        byte[] trits = Utils.decimalToTrits(1f,9);
        BigDecimal decimal = Utils.decimalFromTrits(trits);
        assertEquals(1f,decimal.floatValue());

        trits = Utils.decimalToTrits(2f,9);
        decimal = Utils.decimalFromTrits(trits);
        assertEquals(2f,decimal.floatValue());

        trits = Utils.decimalToTrits(Float.MAX_VALUE,27);
        decimal = Utils.decimalFromTrits(trits);
        assertEquals(Float.MAX_VALUE,decimal.floatValue());

        trits = Utils.decimalToTrits(Float.MIN_VALUE,27);
        decimal = Utils.decimalFromTrits(trits);
        assertEquals(Float.MIN_VALUE,decimal.floatValue());

        trits = Utils.decimalToTrits(1.1f,9);
        decimal = Utils.decimalFromTrits(trits);
        assertEquals(1.1f,decimal.floatValue());

        trits = Utils.decimalToTrits(2.1f,9);
        decimal = Utils.decimalFromTrits(trits);
        assertEquals(2.1f,decimal.floatValue());
    }
}
