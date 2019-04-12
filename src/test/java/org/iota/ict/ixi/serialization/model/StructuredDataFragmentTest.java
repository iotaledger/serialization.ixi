package org.iota.ict.ixi.serialization.model;


import org.iota.ict.ixi.TestUtils;
import org.iota.ict.ixi.serialization.SampleData;
import org.iota.ict.ixi.serialization.util.UnknownMetadataException;
import org.iota.ict.ixi.serialization.util.Utils;
import org.iota.ict.utils.Trytes;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static org.iota.ict.ixi.serialization.util.Utils.asciiFromTrits;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
