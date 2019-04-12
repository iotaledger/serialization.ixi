package org.iota.ict.ixi.serialization.model;

import org.iota.ict.ixi.serialization.model.md.FieldType;
import org.iota.ict.ixi.serialization.util.SerializableField;

import java.util.List;

public class SampleSerializableClass {

    @SerializableField(index=0,tritLength = 1,label = "isTest",fieldType = FieldType.BOOLEAN)
    public boolean isTest;

    @SerializableField(index=1,tritLength = 99,label = "myLabel",fieldType = FieldType.ASCII)
    public String myLabel;

    @SerializableField(index=2,tritLength = 243,label = "aReference",fieldType = FieldType.HASH)
    public String aReferenceHash;

    @SerializableField(index=3,tritLength = 243,label = "aReferenceList",fieldType = FieldType.HASH_LIST)
    public List<String> listOfReferences;

    @SerializableField(index=4,tritLength = 27,label = "myInteger",fieldType = FieldType.INTEGER)
    public int myInteger;
}
