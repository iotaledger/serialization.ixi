package org.iota.ict.ixi.serialization.model;


import org.iota.ict.ixi.serialization.SampleData;
import org.iota.ict.ixi.serialization.util.TritsConverter;
import org.iota.ict.ixi.serialization.util.UnknownMetadataException;
import org.iota.ict.ixi.serialization.util.Utils;
import org.iota.ict.utils.Trytes;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;

import static org.iota.ict.ixi.serialization.util.Utils.asciiFromTrits;
import static org.junit.jupiter.api.Assertions.*;

public class StructuredDataFragmentTest {


    @Test
    public void singleValueTest() throws UnknownMetadataException {
        MetadataFragment metadataFragment = SampleData.classWithOneAsciiField;
        StructuredDataFragment structuredDataFragment = new StructuredDataFragment.Builder()
                .setMetadata(metadataFragment)
                .setValue(0, "hello", TritsConverter.ASCII)
                .build();
        assertEquals(metadataFragment.hash(),structuredDataFragment.getHeadTransaction().extraDataDigest(), "ExtraDataDigest must metadata hash");
        assertEquals("hello", asciiFromTrits(structuredDataFragment.getValue(0)));
        assertFalse(MetadataFragment.isHead(structuredDataFragment.getHeadTransaction()));
        assertFalse(MetadataFragment.isTail(structuredDataFragment.getHeadTransaction()));
    }

    @Test
    public void twoValueTest() throws UnknownMetadataException {
        MetadataFragment metadataFragment = SampleData.classWith2AsciiFields;
        StructuredDataFragment structuredDataFragment = new StructuredDataFragment.Builder()
                .setMetadata(metadataFragment)
                .setValue(0, "hello", TritsConverter.ASCII)
                .setValue(1, "hi", TritsConverter.ASCII)
                .build();
        assertEquals(metadataFragment.hash(),structuredDataFragment.getHeadTransaction().extraDataDigest(), "ExtraDataDigest must metadata hash");
        assertEquals("hello", asciiFromTrits(structuredDataFragment.getValue(0)));
        assertEquals("hi", asciiFromTrits(structuredDataFragment.getValue(1)));
    }

    @Test
    public void booleanValueTest() throws UnknownMetadataException {
        MetadataFragment metadataFragment = SampleData.classWith3Fields;
        StructuredDataFragment structuredDataFragment = new StructuredDataFragment.Builder()
                .setMetadata(metadataFragment)
                .setValue(0, Trytes.fromAscii("my name"), TritsConverter.TRYTES)
                .setValue(1, Trytes.fromNumber(BigInteger.valueOf(47),2), TritsConverter.TRYTES)
                .setValue(2, true, TritsConverter.BOOLEAN)
                .build();

        assertEquals(metadataFragment.hash(),structuredDataFragment.getHeadTransaction().extraDataDigest(), "ExtraDataDigest must metadata hash");
        assertEquals("my name", structuredDataFragment.getValue(0, TritsConverter.ASCII));
        assertEquals(BigInteger.valueOf(47), structuredDataFragment.getValue(1,TritsConverter.BIG_INTEGER));
        assertTrue(structuredDataFragment.getValue(2, TritsConverter.BOOLEAN));
        assertEquals(1,structuredDataFragment.getValue(2).length);
        assertEquals(1,structuredDataFragment.getValue(2)[0]);
    }

    @Test
    public void listValueTest() throws UnknownMetadataException {
        MetadataFragment metadataFragment = SampleData.classWithAsciiAndList;
        StructuredDataFragment structuredDataFragment = new StructuredDataFragment.Builder()
                .setMetadata(metadataFragment)
                .setValue(0, "Qubic", TritsConverter.ASCII)
                .setValues(1, TritsConverter.ASCII, "Qupla","Abra", "Java")
                .build();

        assertEquals(metadataFragment.hash(),structuredDataFragment.getHeadTransaction().extraDataDigest(), "ExtraDataDigest must metadata hash");
        assertEquals("Qubic", structuredDataFragment.getValue(0, TritsConverter.ASCII));
        List<byte[]> values = structuredDataFragment.getListValue(1);
        assertEquals(3, values.size());
        assertEquals("Qupla", Utils.asciiFromTrits(values.get(0)));
        assertEquals("Abra", Utils.asciiFromTrits(values.get(1)));
        assertEquals("Java", Utils.asciiFromTrits(values.get(2)));

        List<String> valuesAsString = structuredDataFragment.getListValue(1, TritsConverter.ASCII);
        assertEquals(3, valuesAsString.size());
        assertEquals("Qupla", valuesAsString.get(0));
        assertEquals("Abra", valuesAsString.get(1));
        assertEquals("Java", valuesAsString.get(2));

    }

    @Test
    public void hugeListValueTest() throws UnknownMetadataException {
        int SIZE = 56;
        String[] manyValues = new String[SIZE];
        for(int i=0;i<SIZE;i++){
            manyValues[i]="language_"+i;
        }

        MetadataFragment metadataFragment = SampleData.classWithAsciiAndList;
        StructuredDataFragment structuredDataFragment = new StructuredDataFragment.Builder()
                .setMetadata(metadataFragment)
                .setValue(0, Trytes.fromAscii("Qubic"))
                .setValues(1, TritsConverter.ASCII, manyValues)
                .build();

        assertEquals(metadataFragment.hash(),structuredDataFragment.getHeadTransaction().extraDataDigest(), "ExtraDataDigest must metadata hash");
        assertEquals("Qubic", structuredDataFragment.getValue(0, TritsConverter.ASCII));
        List<byte[]> values = structuredDataFragment.getListValue(1);
        assertEquals(SIZE, values.size());
        for(int k=0;k<SIZE;k++) {
            assertEquals("language_" + k, Utils.asciiFromTrits(values.get(k)));
        }
    }

    @Test
    public void listValueTest2() throws UnknownMetadataException {
        MetadataFragment metadataFragment = SampleData.classWithListAndAscii;
        StructuredDataFragment structuredDataFragment = new StructuredDataFragment.Builder()
                .setMetadata(metadataFragment)
                .setValue(1, Trytes.fromAscii("Qubic"))
                .setValues(0, TritsConverter.ASCII, "Qupla","Abra", "Java")
                .build();

        assertEquals(metadataFragment.hash(),structuredDataFragment.getHeadTransaction().extraDataDigest(), "ExtraDataDigest must metadata hash");
        assertEquals("Qubic", structuredDataFragment.getValue(1, TritsConverter.ASCII));
        List<byte[]> values = structuredDataFragment.getListValue(0);
        assertEquals(3, values.size());
        assertEquals("Qupla", Utils.asciiFromTrits(values.get(0)));
        assertEquals("Abra", Utils.asciiFromTrits(values.get(1)));
        assertEquals("Java", Utils.asciiFromTrits(values.get(2)));

    }

    @Test
    public void serializeClassInstance() throws UnknownMetadataException {
        SampleSerializableClass sample = SampleData.sample;

        StructuredDataFragment dataFragment = new StructuredDataFragment.Builder().fromInstance(sample).build();

        assertEquals(true, dataFragment.getValue(0, TritsConverter.BOOLEAN));
        assertEquals("aLabel", dataFragment.getValue(1, TritsConverter.ASCII));
        assertEquals(sample.aReferenceHash, dataFragment.getValue(2, TritsConverter.TRYTES));
        List<String> values = dataFragment.getListValue(3,TritsConverter.TRYTES);
        for(int i=0;i<50;i++){
            assertEquals(sample.listOfReferences.get(i),values.get(i));
        }
        assertEquals(17, dataFragment.getValue(4, TritsConverter.INTEGER));
    }

    @Test
    public void serializeAllTypeClassInstance() throws UnknownMetadataException {
        AllTypesSampleData sample = SampleData.allTypesSample;

        StructuredDataFragment dataFragment = new StructuredDataFragment.Builder().fromInstance(sample).build();

        assertEquals(true, dataFragment.getValue(0, TritsConverter.BOOLEAN));
        assertEquals("aLabel", dataFragment.getValue(1, TritsConverter.ASCII));
        assertEquals(sample.aReferenceHash, dataFragment.getValue(2, TritsConverter.TRYTES));
        List<String> values = dataFragment.getListValue(3, TritsConverter.TRYTES);
        for(int i=0;i<50;i++){
            assertEquals(sample.listOfReferences.get(i),values.get(i));
        }
        assertEquals(17, dataFragment.getValue(4, TritsConverter.INTEGER).intValue());
        assertEquals(55, dataFragment.getValue(5, TritsConverter.INTEGER).intValue());
        assertEquals(Long.MAX_VALUE, dataFragment.getValue(6, TritsConverter.LONG).longValue());
        assertEquals(56, dataFragment.getValue(7, TritsConverter.INTEGER).intValue());
        assertEquals(Long.MAX_VALUE, dataFragment.getValue(8, TritsConverter.LONG).longValue());
        assertEquals(333.12f, dataFragment.getValue(9, TritsConverter.FLOAT));
        assertEquals(1.23456f, dataFragment.getValue(10, TritsConverter.FLOAT).floatValue());
        assertEquals(222.12, dataFragment.getValue(11, TritsConverter.DOUBLE).doubleValue());
        assertEquals(1.234567, dataFragment.getValue(12, TritsConverter.DOUBLE).doubleValue());
        assertEquals(sample.myBigDecimal, dataFragment.getValue(13, TritsConverter.BIG_DECIMAL));
        assertEquals(sample.isTestList, dataFragment.getListValue(14, TritsConverter.BOOLEAN));
        assertEquals(sample.myLabelList, dataFragment.getListValue(15, TritsConverter.ASCII));
        assertEquals(sample.myIntegerList, dataFragment.getListValue(16, TritsConverter.INTEGER));
        assertEquals(sample.myLongObjectList, dataFragment.getListValue(17, TritsConverter.LONG));
        assertEquals(sample.myBigIntegerList, dataFragment.getListValue(18, TritsConverter.BIG_INTEGER));
        assertEquals(sample.myFloatObjectList, dataFragment.getListValue(19, TritsConverter.FLOAT));
        assertEquals(sample.myDoubleObjectList, dataFragment.getListValue(20, TritsConverter.DOUBLE));
        assertEquals(sample.myBigDecimalList, dataFragment.getListValue(21, TritsConverter.BIG_DECIMAL));
    }

    @Test
    public void serializeAllTypeClassInstanceAllFieldNull() throws UnknownMetadataException {
        AllTypesSampleData sample = new AllTypesSampleData();

        StructuredDataFragment dataFragment = new StructuredDataFragment.Builder().fromInstance(sample).build();

        assertEquals(false, dataFragment.getValue(0,TritsConverter.BOOLEAN));
        assertEquals("",dataFragment.getValue(1,TritsConverter.ASCII));
        assertEquals("999999999999999999999999999999999999999999999999999999999999999999999999999999999",dataFragment.getValue(2,TritsConverter.TRYTES));
        assertEquals(Collections.EMPTY_LIST,dataFragment.getListValue(3,TritsConverter.TRYTES));
        assertEquals(0, dataFragment.getValue(4,TritsConverter.INTEGER).intValue());
        assertEquals(0, dataFragment.getValue(5,TritsConverter.INTEGER).intValue());
        assertEquals(0, dataFragment.getValue(6,TritsConverter.LONG).longValue());
        assertEquals(0, dataFragment.getValue(7,TritsConverter.INTEGER).intValue());
        assertEquals(BigInteger.ZERO, dataFragment.getValue(8,TritsConverter.BIG_INTEGER));
        assertEquals(0, dataFragment.getValue(9,TritsConverter.FLOAT).floatValue());
        assertEquals(0, dataFragment.getValue(10,TritsConverter.FLOAT).floatValue());
        assertEquals(0, dataFragment.getValue(11,TritsConverter.DOUBLE).doubleValue());
        assertEquals(0, dataFragment.getValue(12,TritsConverter.DOUBLE).doubleValue());
        assertEquals(BigDecimal.ZERO, dataFragment.getValue(13,TritsConverter.BIG_DECIMAL));
        assertEquals(Collections.EMPTY_LIST, dataFragment.getListValue(14,TritsConverter.BOOLEAN));
        assertEquals(Collections.EMPTY_LIST, dataFragment.getListValue(15,TritsConverter.ASCII));
        assertEquals(Collections.EMPTY_LIST, dataFragment.getListValue(16,TritsConverter.INTEGER));
        assertEquals(Collections.EMPTY_LIST, dataFragment.getListValue(17,TritsConverter.LONG));
        assertEquals(Collections.EMPTY_LIST, dataFragment.getListValue(18,TritsConverter.BIG_INTEGER));
        assertEquals(Collections.EMPTY_LIST, dataFragment.getListValue(19,TritsConverter.FLOAT));
        assertEquals(Collections.EMPTY_LIST, dataFragment.getListValue(20,TritsConverter.DOUBLE));
        assertEquals(Collections.EMPTY_LIST, dataFragment.getListValue(21,TritsConverter.BIG_DECIMAL));
    }
}
