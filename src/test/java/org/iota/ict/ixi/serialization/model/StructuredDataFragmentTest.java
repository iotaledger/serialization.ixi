package org.iota.ict.ixi.serialization.model;


import org.iota.ict.ixi.TestUtils;
import org.iota.ict.ixi.serialization.SampleData;
import org.iota.ict.ixi.serialization.util.UnknownMetadataException;
import org.iota.ict.ixi.serialization.util.Utils;
import org.iota.ict.utils.Trytes;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
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
                .setValue(0, Trytes.fromAscii("hello"))
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
                .setValue(0, Trytes.fromAscii("hello"))
                .setValue(1, Trytes.fromAscii("hi"))
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
                .setValue(0, Trytes.fromAscii("my name"))
                .setValue(1, Trytes.fromNumber(BigInteger.valueOf(47),2))
                .setBooleanValue(2, true)
                .build();

        assertEquals(metadataFragment.hash(),structuredDataFragment.getHeadTransaction().extraDataDigest(), "ExtraDataDigest must metadata hash");
        assertEquals("my name", structuredDataFragment.getAsciiValue(0));
        assertEquals(BigInteger.valueOf(47), structuredDataFragment.getIntegerValue(1));
        assertTrue(structuredDataFragment.getBooleanValue(2));
        assertEquals(1,structuredDataFragment.getValue(2).length);
        assertEquals(1,structuredDataFragment.getValue(2)[0]);
    }

    @Test
    public void listValueTest() throws UnknownMetadataException {
        MetadataFragment metadataFragment = SampleData.classWithAsciiAndList;
        StructuredDataFragment structuredDataFragment = new StructuredDataFragment.Builder()
                .setMetadata(metadataFragment)
                .setValue(0, Trytes.fromAscii("Qubic"))
                .setValues(1, Trytes.fromAscii("Qupla"), Trytes.fromAscii("Abra"), Trytes.fromAscii("Java"))
                .build();

        assertEquals(metadataFragment.hash(),structuredDataFragment.getHeadTransaction().extraDataDigest(), "ExtraDataDigest must metadata hash");
        assertEquals("Qubic", structuredDataFragment.getAsciiValue(0));
        List<byte[]> values = structuredDataFragment.getListValue(1);
        assertEquals(3, values.size());
        assertEquals("Qupla", Utils.asciiFromTrits(values.get(0)));
        assertEquals("Abra", Utils.asciiFromTrits(values.get(1)));
        assertEquals("Java", Utils.asciiFromTrits(values.get(2)));

    }

    @Test
    public void hugeListValueTest() throws UnknownMetadataException {
        int SIZE = 56;
        String[] manyValues = new String[SIZE];
        for(int i=0;i<SIZE;i++){
            manyValues[i]=Trytes.fromAscii("language_"+i);
        }

        MetadataFragment metadataFragment = SampleData.classWithAsciiAndList;
        StructuredDataFragment structuredDataFragment = new StructuredDataFragment.Builder()
                .setMetadata(metadataFragment)
                .setValue(0, Trytes.fromAscii("Qubic"))
                .setValues(1, manyValues)
                .build();

        assertEquals(metadataFragment.hash(),structuredDataFragment.getHeadTransaction().extraDataDigest(), "ExtraDataDigest must metadata hash");
        assertEquals("Qubic", structuredDataFragment.getAsciiValue(0));
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
                .setValues(0, Trytes.fromAscii("Qupla"), Trytes.fromAscii("Abra"), Trytes.fromAscii("Java"))
                .build();

        assertEquals(metadataFragment.hash(),structuredDataFragment.getHeadTransaction().extraDataDigest(), "ExtraDataDigest must metadata hash");
        assertEquals("Qubic", structuredDataFragment.getAsciiValue(1));
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

        assertEquals(true, dataFragment.getBooleanValue(0));
        assertEquals("aLabel", dataFragment.getAsciiValue(1));
        assertEquals(sample.aReferenceHash, dataFragment.getTrytesValue(2));
        List<String> values = dataFragment.getTryteList(3);
        for(int i=0;i<50;i++){
            assertEquals(sample.listOfReferences.get(i),values.get(i));
        }
        assertEquals(17, dataFragment.getIntegerValue(4).intValue());
    }

    @Test
    public void serializeAllTypeClassInstance() throws UnknownMetadataException {
        AllTypesSampleData sample = SampleData.allTypesSample;

        StructuredDataFragment dataFragment = new StructuredDataFragment.Builder().fromInstance(sample).build();

        assertEquals(true, dataFragment.getBooleanValue(0));
        assertEquals("aLabel", dataFragment.getAsciiValue(1));
        assertEquals(sample.aReferenceHash, dataFragment.getTrytesValue(2));
        List<String> values = dataFragment.getTryteList(3);
        for(int i=0;i<50;i++){
            assertEquals(sample.listOfReferences.get(i),values.get(i));
        }
        assertEquals(17, dataFragment.getIntegerValue(4).intValue());
        assertEquals(55, dataFragment.getIntegerValue(5).intValue());
        assertEquals(Long.MAX_VALUE, dataFragment.getIntegerValue(6).longValue());
        assertEquals(56, dataFragment.getIntegerValue(7).intValue());
        assertEquals(Long.MAX_VALUE, dataFragment.getIntegerValue(8).longValue());
        assertEquals(333.12f, dataFragment.getDecimalValue(9).floatValue());
        assertEquals(1.23456f, dataFragment.getDecimalValue(10).floatValue());
        assertEquals(222.12, dataFragment.getDecimalValue(11).doubleValue());
        assertEquals(1.234567, dataFragment.getDecimalValue(12).doubleValue());
        assertEquals(sample.myBigDecimal, dataFragment.getDecimalValue(13));
        assertEquals(sample.isTestList, dataFragment.getBooleanList(14));
        assertEquals(sample.myLabelList, dataFragment.getAsciiList(15));
        assertEquals(sample.myIntegerList, dataFragment.getIntegerList(16));
        assertEquals(sample.myLongObjectList, dataFragment.getLongList(17));
        assertEquals(sample.myBigIntegerList, dataFragment.getBigIntegerList(18));
        assertEquals(sample.myFloatObjectList, dataFragment.getFloatList(19));
        assertEquals(sample.myDoubleObjectList, dataFragment.getDoubleList(20));
        assertEquals(sample.myBigDecimalList, dataFragment.getBigDecimalList(21));
    }

    @Test
    public void serializeAllTypeClassInstanceAllFieldNull() throws UnknownMetadataException {
        AllTypesSampleData sample = new AllTypesSampleData();

        StructuredDataFragment dataFragment = new StructuredDataFragment.Builder().fromInstance(sample).build();

        assertEquals(false, dataFragment.getBooleanValue(0));
        assertEquals("",dataFragment.getAsciiValue(1));
        assertEquals("999999999999999999999999999999999999999999999999999999999999999999999999999999999",dataFragment.getTrytesValue(2));
        assertEquals(Collections.EMPTY_LIST,dataFragment.getTryteList(3));
        assertEquals(0, dataFragment.getIntegerValue(4).intValue());
        assertEquals(0, dataFragment.getIntegerValue(5).intValue());
        assertEquals(0, dataFragment.getIntegerValue(6).longValue());
        assertEquals(0, dataFragment.getIntegerValue(7).intValue());
        assertEquals(BigInteger.ZERO, dataFragment.getIntegerValue(8));
        assertEquals(0, dataFragment.getDecimalValue(9).floatValue());
        assertEquals(0, dataFragment.getDecimalValue(10).floatValue());
        assertEquals(0, dataFragment.getDecimalValue(11).doubleValue());
        assertEquals(0, dataFragment.getDecimalValue(12).doubleValue());
        assertEquals(BigDecimal.ZERO, dataFragment.getDecimalValue(13));
        assertEquals(Collections.EMPTY_LIST, dataFragment.getBooleanList(14));
        assertEquals(Collections.EMPTY_LIST, dataFragment.getAsciiList(15));
        assertEquals(Collections.EMPTY_LIST, dataFragment.getIntegerList(16));
        assertEquals(Collections.EMPTY_LIST, dataFragment.getLongList(17));
        assertEquals(Collections.EMPTY_LIST, dataFragment.getBigIntegerList(18));
        assertEquals(Collections.EMPTY_LIST, dataFragment.getFloatList(19));
        assertEquals(Collections.EMPTY_LIST, dataFragment.getDoubleList(20));
        assertEquals(Collections.EMPTY_LIST, dataFragment.getBigDecimalList(21));
    }
}
