package org.iota.ict.ixi.serialization.model.md;

import org.iota.ict.ixi.serialization.model.MetadataFragment;
import org.iota.ict.model.Transaction;
import org.iota.ict.utils.Trytes;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

public class FieldDescriptorTest {

    @Test
    public void ensureFieldDescriptorSize(){
        assertEquals(0, FieldDescriptor.FIELD_DESCRIPTOR_LENGTH % 3,  "Field descriptor should be an exact multiple of 3");
    }

    @Test
    public void simpleConstructorTest(){
        String expected = Trytes.padRight("INTA", FieldDescriptor.FIELD_DESCRIPTOR_TRYTE_LENGTH);
        FieldDescriptor fieldDescriptor = FieldDescriptor.withAsciiLabel(FieldType.fromTrytes("INT"),1,null);
        assertEquals(expected, fieldDescriptor.toTrytes());
    }

    @Test
    public void deserializerTest() {
        String src = Trytes.padRight("INTA", FieldDescriptor.FIELD_DESCRIPTOR_LENGTH /3);

        FieldDescriptor fieldDescriptor = FieldDescriptor.fromTrytes(src);
        assertEquals(FieldType.fromTrytes("INT"), fieldDescriptor.getType());
        assertEquals(BigInteger.ONE, fieldDescriptor.getSize());
        assertEquals("", fieldDescriptor.getAsciiLabel());
        assertEquals(src, fieldDescriptor.toTrytes());
    }

    @Test
    public void serializeDeserializerTest() {
        FieldDescriptor fieldDescriptor = FieldDescriptor.withAsciiLabel(FieldType.fromTrytes("ASC"),48,"a simple label");
        String trytes = fieldDescriptor.toTrytes();
        FieldDescriptor fieldDescriptorDeserialized = FieldDescriptor.fromTrytes(trytes);
        assertEquals(fieldDescriptor.getType(), fieldDescriptorDeserialized.getType());
        assertEquals(fieldDescriptor.getSize(), fieldDescriptorDeserialized.getSize());
        assertEquals(fieldDescriptor.getAsciiLabel(), fieldDescriptorDeserialized.getAsciiLabel());
    }

    @Test
    public void labelTruncate() {
        FieldDescriptor fieldDescriptor = FieldDescriptor.withTrytesLabel(FieldType.fromTrytes("ASC"),48,"ABCDEFGHIKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWX");
        String trytes = fieldDescriptor.toTrytes();
        FieldDescriptor fieldDescriptorDeserialized = FieldDescriptor.fromTrytes(trytes);
        assertEquals(fieldDescriptor.getType(), fieldDescriptorDeserialized.getType());
        assertEquals(fieldDescriptor.getSize(), fieldDescriptorDeserialized.getSize());
        assertEquals("ABCDEFGHIKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUV", fieldDescriptorDeserialized.getLabel());
    }
}
