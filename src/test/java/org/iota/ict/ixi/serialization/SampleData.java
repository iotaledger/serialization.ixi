package org.iota.ict.ixi.serialization;

import org.iota.ict.ixi.TestUtils;
import org.iota.ict.ixi.serialization.model.MetadataFragment;
import org.iota.ict.ixi.serialization.model.SampleSerializableClass;
import org.iota.ict.ixi.serialization.model.StructuredDataFragment;
import org.iota.ict.ixi.serialization.model.md.FieldDescriptor;
import org.iota.ict.ixi.serialization.model.md.FieldType;
import org.iota.ict.utils.Trytes;

import java.util.ArrayList;
import java.util.List;

public class SampleData {

    public static MetadataFragment classWithOneAsciiField;
    public static MetadataFragment classWith2AsciiFields;
    public static MetadataFragment classWith3Fields;
    public static MetadataFragment classWithAsciiAndList;
    public static MetadataFragment classWithListAndAscii;

    public static StructuredDataFragment simpleDataFragment;

    public static SampleSerializableClass sample;

    static {
        classWithOneAsciiField = buildClassWithOneAsciiField();
        classWith2AsciiFields = buildClassWith2AsciiFields();
        classWith3Fields = buildClassWith3Fields();
        classWithAsciiAndList = buildClassWithAsciiAndList();
        classWithListAndAscii = buildClassWithListAndAscii();

        simpleDataFragment = new StructuredDataFragment.Builder()
                .setMetadata(classWithOneAsciiField)
                .setValue(0, Trytes.fromAscii("hello"))
                .build();

        sample = buildSampleClassInstance();
    }
    private static MetadataFragment buildClassWithOneAsciiField(){
        FieldDescriptor descriptor = FieldDescriptor.withAsciiLabel(FieldType.fromTrytes("AI"),48,"a simple label");
        return new MetadataFragment.Builder()
                .appendField(descriptor)
                .build();
    }

    private static MetadataFragment buildClassWith2AsciiFields(){
        FieldDescriptor descriptor = FieldDescriptor.withAsciiLabel(FieldType.fromTrytes("AI"),48,"a simple label");
        return new MetadataFragment.Builder()
                .appendField(descriptor)
                .appendField(descriptor)
                .build();
    }

    private static MetadataFragment buildClassWith3Fields(){
        FieldDescriptor name = FieldDescriptor.withAsciiLabel(FieldType.TYPE_ASCII,243,"name");
        FieldDescriptor age = FieldDescriptor.withAsciiLabel(FieldType.TYPE_INTEGER,7,"age");
        FieldDescriptor isMale = FieldDescriptor.withAsciiLabel(FieldType.TYPE_BOOLEAN,1,"isMale");
        return new MetadataFragment.Builder()
                .appendField(name)
                .appendField(age)
                .appendField(isMale)
                .build();
    }

    private static MetadataFragment buildClassWithAsciiAndList(){
        FieldDescriptor project = FieldDescriptor.withAsciiLabel(FieldType.TYPE_ASCII,243,"name");
        FieldDescriptor languages = FieldDescriptor.withAsciiLabel(FieldType.TYPE_ASCII_LIST,243,"languages");
        return new MetadataFragment.Builder()
                    .appendField(project)
                    .appendField(languages)
                    .build();
    }
    private static MetadataFragment buildClassWithListAndAscii(){
        FieldDescriptor project = FieldDescriptor.withAsciiLabel(FieldType.TYPE_ASCII,243,"name");
        FieldDescriptor languages = FieldDescriptor.withAsciiLabel(FieldType.TYPE_ASCII_LIST,243,"languages");
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
}
