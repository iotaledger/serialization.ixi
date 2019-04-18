package org.iota.ict.ixi.serialization.model;

import org.iota.ict.ixi.serialization.util.SerializableField;
import org.iota.ict.ixi.serialization.util.TritsConverter;

import java.util.List;

public class SampleSerializableClass {

    @SerializableField(index = 0, tritLength = 1, label = "isTest", converter = TritsConverter.BOOLEAN.class)
    public boolean isTest;

    @SerializableField(index = 1, tritLength = 99, label = "myLabel", converter = TritsConverter.ASCII.class)
    public String myLabel;

    @SerializableField(index = 2, tritLength = 243, label = "aReference", converter = TritsConverter.TRYTES.class)
    public String aReferenceHash;

    @SerializableField(index = 3, tritLength = 243, label = "aReferenceList", converter = TritsConverter.TRYTES.class)
    public List<String> listOfReferences;

    @SerializableField(index = 4, tritLength = 27, label = "myInteger", converter = TritsConverter.INTEGER.class)
    public int myInteger;
}
