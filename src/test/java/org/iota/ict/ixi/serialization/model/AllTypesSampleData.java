package org.iota.ict.ixi.serialization.model;

import org.iota.ict.ixi.serialization.model.md.FieldType;
import org.iota.ict.ixi.serialization.util.SerializableField;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

public class AllTypesSampleData {


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

    @SerializableField(index=5,tritLength = 27,label = "myIntegerObject",fieldType = FieldType.INTEGER)
    public Integer myIntegerObject;

    @SerializableField(index=6,tritLength = 54,label = "myLong",fieldType = FieldType.INTEGER)
    public long myLong;

    @SerializableField(index=7,tritLength = 54,label = "myLongObject",fieldType = FieldType.INTEGER)
    public Long myLongObject;

    @SerializableField(index=8,tritLength = 54,label = "myBigInteger",fieldType = FieldType.INTEGER)
    public BigInteger myBigInteger;

    @SerializableField(index=9,tritLength = 27,label = "myFloat",fieldType = FieldType.DECIMAL)
    public float myFloat;

    @SerializableField(index=10,tritLength = 27,label = "myFloatObject",fieldType = FieldType.DECIMAL)
    public Float myFloatObject;

    @SerializableField(index=11,tritLength = 54,label = "myDouble",fieldType = FieldType.DECIMAL)
    public double myDouble;

    @SerializableField(index=12,tritLength = 54,label = "myDoubleObject",fieldType = FieldType.DECIMAL)
    public Double myDoubleObject;

    @SerializableField(index=13,tritLength = 66,label = "myBigDecimal",fieldType = FieldType.DECIMAL)
    public BigDecimal myBigDecimal;

    @SerializableField(index=14,tritLength = 1,label = "isTestList",fieldType = FieldType.BOOLEAN_LIST)
    public List<Boolean> isTestList;

    @SerializableField(index=15,tritLength = 99,label = "myLabelList",fieldType = FieldType.ASCII_LIST)
    public List<String> myLabelList;

    @SerializableField(index=16,tritLength = 27,label = "myIntegerList",fieldType = FieldType.INTEGER_LIST)
    public List<Integer> myIntegerList;

    @SerializableField(index=17,tritLength = 54,label = "myLongObjectList",fieldType = FieldType.INTEGER_LIST)
    public List<Long> myLongObjectList;

    @SerializableField(index=18,tritLength = 54,label = "myBigIntegerList",fieldType = FieldType.INTEGER_LIST)
    public List<BigInteger> myBigIntegerList;

    @SerializableField(index=19,tritLength = 27,label = "myFloatObjectList",fieldType = FieldType.DECIMAL_LIST)
    public List<Float> myFloatObjectList;

    @SerializableField(index=20,tritLength = 54,label = "myDoubleObjectList",fieldType = FieldType.DECIMAL_LIST)
    public List<Double> myDoubleObjectList;

    @SerializableField(index=21,tritLength = 54,label = "myBigDecimalList",fieldType = FieldType.DECIMAL_LIST)
    public List<BigDecimal> myBigDecimalList;

}
