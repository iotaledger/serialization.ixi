package org.iota.ict.ixi.serialization.model;

import org.iota.ict.ixi.serialization.util.SerializableField;
import org.iota.ict.ixi.serialization.util.TritsConverter;

import java.util.List;

public class SampleSerializableClass {

    @SerializableField(index = 0, tritLength = 1, converter = TritsConverter.BooleanConverter.class)
    public boolean isTest;

    @SerializableField(index = 1, tritLength = 99, converter = TritsConverter.AsciiConverter.class)
    public String myLabel;

    @SerializableField(index = 2, tritLength = 243, converter = TritsConverter.TrytesConverter.class)
    public String aReferenceHash;

    @SerializableField(index = 3, tritLength = 243, converter = TritsConverter.TrytesConverter.class)
    public List<String> listOfReferences;

    @SerializableField(index = 4, tritLength = 27, converter = TritsConverter.IntegerConverter.class)
    public int myInteger;
}
