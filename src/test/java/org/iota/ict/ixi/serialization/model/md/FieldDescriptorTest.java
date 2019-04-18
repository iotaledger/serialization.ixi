package org.iota.ict.ixi.serialization.model.md;

import org.iota.ict.utils.Trytes;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

public class FieldDescriptorTest {

    @Test
    public void ensureFieldDescriptorSize(){
        assertEquals(0, FieldDescriptor.FIELD_DESCRIPTOR_TRIT_LENGTH % 3,  "Field descriptor should be an exact multiple of 3");
    }

    @Test
    public void simpleConstructorTest(){
        String expected = Trytes.padRight("AA", FieldDescriptor.FIELD_DESCRIPTOR_TRYTE_LENGTH);
        FieldDescriptor fieldDescriptor = FieldDescriptor.build(false,1);
        assertEquals(expected, fieldDescriptor.toTrytes());
    }

    @Test
    public void fieldSizeMustStrictlyPositive(){
        assertThrows(IllegalArgumentException.class,
                () -> FieldDescriptor.build(false,BigInteger.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> FieldDescriptor.build(false,null));
        assertThrows(IllegalArgumentException.class,
                () -> FieldDescriptor.build(false,-1));
    }

    @Test
    public void deserializerTest() {
        String src = Trytes.padRight("AA", FieldDescriptor.FIELD_DESCRIPTOR_TRYTE_LENGTH);

        FieldDescriptor fieldDescriptor = FieldDescriptor.fromTrytes(src);
        assertEquals(false, fieldDescriptor.isList());
        assertEquals(BigInteger.ONE, fieldDescriptor.getTritSize());
        assertEquals(src, fieldDescriptor.toTrytes());
    }

    @Test
    public void serializeDeserializerTest() {
        FieldDescriptor fieldDescriptor = FieldDescriptor.build(false,48);
        String trytes = fieldDescriptor.toTrytes();
        FieldDescriptor fieldDescriptorDeserialized = FieldDescriptor.fromTrytes(trytes);
        assertEquals(fieldDescriptor.isList(), fieldDescriptorDeserialized.isList());
        assertEquals(fieldDescriptor.getTritSize(), fieldDescriptorDeserialized.getTritSize());
    }

}
