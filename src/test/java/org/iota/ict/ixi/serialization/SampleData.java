package org.iota.ict.ixi.serialization;

import org.iota.ict.ixi.TestUtils;
import org.iota.ict.ixi.serialization.model.AllTypesSampleData;
import org.iota.ict.ixi.serialization.model.MetadataFragment;
import org.iota.ict.ixi.serialization.model.SampleSerializableClass;
import org.iota.ict.ixi.serialization.model.StructuredDataFragment;
import org.iota.ict.ixi.serialization.model.md.FieldDescriptor;
import org.iota.ict.ixi.serialization.util.TritsConverter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SampleData {

    public static MetadataFragment classWithOneAsciiField;
    public static MetadataFragment classWith2AsciiFields;
    public static MetadataFragment classWith3Fields;
    public static MetadataFragment classWithAsciiAndList;
    public static MetadataFragment classWithListAndAscii;

    public static StructuredDataFragment simpleDataFragment;

    public static SampleSerializableClass sample;
    public static AllTypesSampleData allTypesSample;

    static {
        classWithOneAsciiField = buildClassWithOneAsciiField();
        classWith2AsciiFields = buildClassWith2AsciiFields();
        classWith3Fields = buildClassWith3Fields();
        classWithAsciiAndList = buildClassWithAsciiAndList();
        classWithListAndAscii = buildClassWithListAndAscii();

        simpleDataFragment = new StructuredDataFragment.Builder()
                .setMetadata(classWithOneAsciiField)
                .setValue(0, "hello", TritsConverter.ASCII)
                .build();

        sample = buildSampleClassInstance();
        allTypesSample = buildAllTypeSampleInstance();
    }
    private static MetadataFragment buildClassWithOneAsciiField(){
        FieldDescriptor descriptor = FieldDescriptor.build(false,48);
        return new MetadataFragment.Builder()
                .appendField(descriptor)
                .build();
    }

    private static MetadataFragment buildClassWith2AsciiFields(){
        FieldDescriptor descriptor = FieldDescriptor.build(false,48);
        return new MetadataFragment.Builder()
                .appendField(descriptor)
                .appendField(descriptor)
                .build();
    }

    private static MetadataFragment buildClassWith3Fields(){
        FieldDescriptor name = FieldDescriptor.build(false,243);
        FieldDescriptor age = FieldDescriptor.build(false,7);
        FieldDescriptor isMale = FieldDescriptor.build(false,1);
        return new MetadataFragment.Builder()
                .appendField(name)
                .appendField(age)
                .appendField(isMale)
                .build();
    }

    private static MetadataFragment buildClassWithAsciiAndList(){
        FieldDescriptor project = FieldDescriptor.build(false,243);
        FieldDescriptor languages = FieldDescriptor.build(true,243);
        return new MetadataFragment.Builder()
                    .appendField(project)
                    .appendField(languages)
                    .build();
    }
    private static MetadataFragment buildClassWithListAndAscii(){
        FieldDescriptor project = FieldDescriptor.build(false,243);
        FieldDescriptor languages = FieldDescriptor.build(true,243);
        return new MetadataFragment.Builder()
                .appendField(languages)
                .appendField(project)
                .build();
    }

    private static SampleSerializableClass buildSampleClassInstance(){
        SampleSerializableClass sample = new SampleSerializableClass();
        sample.isTest = true;
        sample.myLabel = "aLabel";
        sample.aReferenceHash = TestUtils.randomHash();
        List<String> myHashList = new ArrayList<>();
        for(int i=0;i<50;i++){
            myHashList.add(TestUtils.randomHash());
        }
        sample.listOfReferences = myHashList;
        sample.myInteger = 17;
        return sample;
    }

    private static AllTypesSampleData buildAllTypeSampleInstance(){
        AllTypesSampleData sample = new AllTypesSampleData();
        sample.isTest = true;
        sample.myLabel = "aLabel";
        sample.aReferenceHash = TestUtils.randomHash();
        List<String> myHashList = new ArrayList<>();
        for(int i=0;i<50;i++){
            myHashList.add(TestUtils.randomHash());
        }
        sample.listOfReferences = myHashList;
        sample.myInteger = 17;
        sample.myIntegerObject = new Integer("55");
        sample.isTestList = Arrays.asList(true, true, false, false);
        sample.myBigInteger = BigInteger.valueOf(Long.MAX_VALUE);
        sample.myLong = Long.MAX_VALUE;
        sample.myLongObject = new Long(56);
        sample.myDouble = 222.12;
        sample.myFloat = 333.12f;
        sample.myDoubleObject = new Double("1.234567");
        sample.myFloatObject = new Float("1.23456");
        sample.myBigDecimal = new BigDecimal("1.234567891011121314");
        sample.myIntegerList = Arrays.asList(5,6,7,8,9,10);
        sample.myBigIntegerList = Arrays.asList(BigInteger.ONE,BigInteger.ZERO,BigInteger.TEN);
        sample.myLongObjectList = Arrays.asList(1l,2l,3l,4l);
        sample.myFloatObjectList = Arrays.asList(1.1f,2.2f,3.3f,4.4f);
        sample.myDoubleObjectList = Arrays.asList(1.1d,2.2d,3.3d,4.4d);
        sample.myBigDecimalList = Arrays.asList(BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.TEN);
        sample.myLabelList = Arrays.asList("hi","hello","bonjour");
        return sample;
    }
}
