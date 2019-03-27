package org.iota.ict.ixi.serialization.model.md;

import org.iota.ict.ixi.serialization.model.MetadataFragmentBuilder;
import org.iota.ict.ixi.serialization.util.UnknownFieldTypeException;
import org.iota.ict.model.Transaction;
import org.iota.ict.utils.Trytes;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

public class FieldDescriptorTest {

    @Test
    public void ensureFieldDescriptorSize(){
        assertEquals(0, FieldDescriptor.FIELD_DESCRIPTOR_TRIT_LENGTH % 3);
        assertEquals(0, (Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength - MetadataFragmentBuilder.METADATA_LANGUAGE_VERSION.length()) % FieldDescriptor.FIELD_DESCRIPTOR_TRYTE_LENGTH );
    }

    @Test
    public void simpleConstructorTest(){
        String expected = Trytes.padRight("INTA", FieldDescriptor.FIELD_DESCRIPTOR_TRIT_LENGTH/3);
        FieldDescriptor fieldDescriptor = FieldDescriptor.withAsciiLabel(FIELD_TYPE.INTEGER,1,null);
        assertEquals(expected, fieldDescriptor.toTrytes());
    }

    @Test
    public void deserializerTest() throws UnknownFieldTypeException {
        String src = Trytes.padRight("INTA", FieldDescriptor.FIELD_DESCRIPTOR_TRIT_LENGTH/3);

        FieldDescriptor fieldDescriptor = FieldDescriptor.fromTrytes(src);
        assertEquals(FIELD_TYPE.INTEGER, fieldDescriptor.getType());
        assertEquals(BigInteger.ONE, fieldDescriptor.getSize());
        assertEquals("", fieldDescriptor.getAsciiLabel());
        assertEquals(src, fieldDescriptor.toTrytes());
    }

    @Test
    public void serializeDeserializerTest() throws UnknownFieldTypeException {
        FieldDescriptor fieldDescriptor = FieldDescriptor.withAsciiLabel(FIELD_TYPE.ASCII,48,"a simple label");
        String trytes = fieldDescriptor.toTrytes();
        FieldDescriptor fieldDescriptorDeserialized = FieldDescriptor.fromTrytes(trytes);
        assertEquals(fieldDescriptor.getType(), fieldDescriptorDeserialized.getType());
        assertEquals(fieldDescriptor.getSize(), fieldDescriptorDeserialized.getSize());
        assertEquals(fieldDescriptor.getAsciiLabel(), fieldDescriptorDeserialized.getAsciiLabel());
    }

    @Test
    public void labelTruncate() throws UnknownFieldTypeException {
        FieldDescriptor fieldDescriptor = FieldDescriptor.withTrytesLabel(FIELD_TYPE.ASCII,48,"ABCDEFGHIKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWX");
        String trytes = fieldDescriptor.toTrytes();
        FieldDescriptor fieldDescriptorDeserialized = FieldDescriptor.fromTrytes(trytes);
        assertEquals(fieldDescriptor.getType(), fieldDescriptorDeserialized.getType());
        assertEquals(fieldDescriptor.getSize(), fieldDescriptorDeserialized.getSize());
        assertEquals("ABCDEFGHIKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUV", fieldDescriptorDeserialized.getLabel());
    }
}
