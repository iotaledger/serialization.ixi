package org.iota.ict.ixi.serialization.model;


import org.iota.ict.ixi.serialization.model.md.FieldDescriptor;
import org.iota.ict.ixi.serialization.model.md.FieldType;
import org.iota.ict.ixi.serialization.util.Utils;
import org.iota.ict.utils.Trytes;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static org.iota.ict.ixi.serialization.util.Utils.asciiFromTrits;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StructuredDataFragmentTest {


    @Test
    public void singleValueTest(){
        MetadataFragment metadataFragment = buildMetaDataFragment();
        StructuredDataFragment.Builder builder = new StructuredDataFragment.Builder();
        builder.setMetadata(metadataFragment);
        builder.setValue(0, Trytes.fromAscii("hello"));
        StructuredDataFragment structuredDataFragment = builder.build();
        assertEquals(metadataFragment.getHeadTransaction().hash,structuredDataFragment.getHeadTransaction().extraDataDigest(), "ExtraDataDigest must metadata hash");
        assertEquals("hello", asciiFromTrits(structuredDataFragment.getValue(0)));
    }

    @Test
    public void twoValueTest(){
        MetadataFragment metadataFragment = buildMetaDataFragment2();
        StructuredDataFragment.Builder builder = new StructuredDataFragment.Builder();
        builder.setMetadata(metadataFragment);
        builder.setValue(0, Trytes.fromAscii("hello"));
        builder.setValue(1, Trytes.fromAscii("hi"));
        StructuredDataFragment structuredDataFragment = builder.build();
        assertEquals(metadataFragment.getHeadTransaction().hash,structuredDataFragment.getHeadTransaction().extraDataDigest(), "ExtraDataDigest must metadata hash");
        assertEquals("hello", asciiFromTrits(structuredDataFragment.getValue(0)));
        assertEquals("hi", asciiFromTrits(structuredDataFragment.getValue(1)));
    }

    @Test
    public void booleanValueTest(){
        MetadataFragment metadataFragment = buildMetadataFragment3();
        StructuredDataFragment.Builder builder = new StructuredDataFragment.Builder();
        builder.setMetadata(metadataFragment);
        builder.setValue(0, Trytes.fromAscii("my name"));
        builder.setValue(1, Trytes.fromNumber(BigInteger.valueOf(47),2));
        builder.setBooleanValue(2, true);
        StructuredDataFragment structuredDataFragment = builder.build();
        assertEquals(metadataFragment.getHeadTransaction().hash,structuredDataFragment.getHeadTransaction().extraDataDigest(), "ExtraDataDigest must metadata hash");
        assertEquals("my name", structuredDataFragment.getAsciiValue(0));
        assertEquals(BigInteger.valueOf(47), structuredDataFragment.getIntegerValue(1));
        assertTrue(structuredDataFragment.getBooleanValue(2));
        assertEquals(1,structuredDataFragment.getValue(2).length);
        assertEquals(1,structuredDataFragment.getValue(2)[0]);
    }

    @Test
    public void listValueTest() {
        MetadataFragment metadataFragment = buildMetadataFragmentWithList();
        StructuredDataFragment.Builder builder = new StructuredDataFragment.Builder();
        builder.setMetadata(metadataFragment);
        builder.setValue(0, Trytes.fromAscii("Qubic"));
        builder.setValues(1, Trytes.fromAscii("Qupla"), Trytes.fromAscii("Abra"), Trytes.fromAscii("Java"));
        StructuredDataFragment structuredDataFragment = builder.build();
        assertEquals("Qubic", structuredDataFragment.getAsciiValue(0));
        List<byte[]> values = structuredDataFragment.getListValue(1);
        assertEquals(3, values.size());
        assertEquals("Qupla", Utils.asciiFromTrits(values.get(0)));
        assertEquals("Abra", Utils.asciiFromTrits(values.get(1)));
        assertEquals("Java", Utils.asciiFromTrits(values.get(2)));

    }

    @Test
    public void hugeListValueTest() {
        MetadataFragment metadataFragment = buildMetadataFragmentWithList();
        StructuredDataFragment.Builder builder = new StructuredDataFragment.Builder();
        builder.setMetadata(metadataFragment);
        builder.setValue(0, Trytes.fromAscii("Qubic"));
        int SIZE = 56;
        String[] manyValues = new String[SIZE];
        for(int i=0;i<SIZE;i++){
            manyValues[i]=Trytes.fromAscii("language_"+i);
        }
        builder.setValues(1, manyValues);
        StructuredDataFragment structuredDataFragment = builder.build();
        assertEquals("Qubic", structuredDataFragment.getAsciiValue(0));
        List<byte[]> values = structuredDataFragment.getListValue(1);
        assertEquals(SIZE, values.size());
        for(int k=0;k<SIZE;k++) {
            assertEquals("language_" + k, Utils.asciiFromTrits(values.get(k)));
        }
    }

    @Test
    public void listValueTest2() {
        MetadataFragment metadataFragment = buildMetadataFragmentWithList2();
        StructuredDataFragment.Builder builder = new StructuredDataFragment.Builder();
        builder.setMetadata(metadataFragment);
        builder.setValue(1, Trytes.fromAscii("Qubic"));
        builder.setValues(0, Trytes.fromAscii("Qupla"), Trytes.fromAscii("Abra"), Trytes.fromAscii("Java"));
        StructuredDataFragment structuredDataFragment = builder.build();
        assertEquals("Qubic", structuredDataFragment.getAsciiValue(1));
        List<byte[]> values = structuredDataFragment.getListValue(0);
        assertEquals(3, values.size());
        assertEquals("Qupla", Utils.asciiFromTrits(values.get(0)));
        assertEquals("Abra", Utils.asciiFromTrits(values.get(1)));
        assertEquals("Java", Utils.asciiFromTrits(values.get(2)));

    }

    private MetadataFragment buildMetaDataFragment(){
        MetadataFragment.Builder builder = new MetadataFragment.Builder();
        FieldDescriptor descriptor = FieldDescriptor.withAsciiLabel(FieldType.fromTrytes("AI"),48,"a simple label");
        builder.appendField(descriptor);
        return builder.build();
    }

    private MetadataFragment buildMetaDataFragment2(){
        MetadataFragment.Builder builder = new MetadataFragment.Builder();
        FieldDescriptor descriptor = FieldDescriptor.withAsciiLabel(FieldType.fromTrytes("AI"),48,"a simple label");
        builder.appendField(descriptor);
        builder.appendField(descriptor);
        return builder.build();
    }

    private MetadataFragment buildMetadataFragment3(){
        FieldDescriptor name = FieldDescriptor.withAsciiLabel(FieldType.ASCII,243,"name");
        FieldDescriptor age = FieldDescriptor.withAsciiLabel(FieldType.INTEGER,7,"age");
        FieldDescriptor isMale = FieldDescriptor.withAsciiLabel(FieldType.BOOLEAN,1,"isMale");
        MetadataFragment.Builder builder = new MetadataFragment.Builder();
        builder.appendField(name);
        builder.appendField(age);
        builder.appendField(isMale);
        return builder.build();
    }

    private MetadataFragment buildMetadataFragmentWithList(){
        FieldDescriptor project = FieldDescriptor.withAsciiLabel(FieldType.ASCII,243,"name");
        FieldDescriptor languages = FieldDescriptor.withAsciiLabel(FieldType.ASCII_LIST,243,"languages");
        MetadataFragment.Builder builder = new MetadataFragment.Builder();
        builder.appendField(project);
        builder.appendField(languages);
        return builder.build();
    }
    private MetadataFragment buildMetadataFragmentWithList2(){
        FieldDescriptor project = FieldDescriptor.withAsciiLabel(FieldType.ASCII,243,"name");
        FieldDescriptor languages = FieldDescriptor.withAsciiLabel(FieldType.ASCII_LIST,243,"languages");
        MetadataFragment.Builder builder = new MetadataFragment.Builder();
        builder.appendField(languages);
        builder.appendField(project);
        return builder.build();
    }
}
