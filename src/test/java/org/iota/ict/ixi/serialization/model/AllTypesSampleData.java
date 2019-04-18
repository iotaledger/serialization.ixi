package org.iota.ict.ixi.serialization.model;

import org.iota.ict.ixi.serialization.util.SerializableField;
import org.iota.ict.ixi.serialization.util.TritsConverter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

public class AllTypesSampleData {


    @SerializableField(index=0,tritLength = 1, converter = TritsConverter.BooleanConverter.class)
    public boolean isTest;

    @SerializableField(index=1,tritLength = 99, converter = TritsConverter.AsciiConverter.class)
    public String myLabel;

    @SerializableField(index=2,tritLength = 243, converter = TritsConverter.TrytesConverter.class)
    public String aReferenceHash;

    @SerializableField(index=3,tritLength = 243, converter = TritsConverter.TrytesConverter.class)
    public List<String> listOfReferences;

    @SerializableField(index=4,tritLength = 27, converter = TritsConverter.IntegerConverter.class)
    public int myInteger;

    @SerializableField(index=5,tritLength = 27, converter = TritsConverter.IntegerConverter.class)
    public Integer myIntegerObject;

    @SerializableField(index=6,tritLength = 54, converter = TritsConverter.LongConverter.class)
    public long myLong;

    @SerializableField(index=7,tritLength = 54, converter = TritsConverter.LongConverter.class)
    public Long myLongObject;

    @SerializableField(index=8,tritLength = 54, converter = TritsConverter.BigIntegerConverter.class)
    public BigInteger myBigInteger;

    @SerializableField(index=9,tritLength = 27, converter = TritsConverter.FloatConverter.class)
    public float myFloat;

    @SerializableField(index=10,tritLength = 27, converter = TritsConverter.FloatConverter.class)
    public Float myFloatObject;

    @SerializableField(index=11,tritLength = 54, converter = TritsConverter.DoubleConverter.class)
    public double myDouble;

    @SerializableField(index=12,tritLength = 54, converter = TritsConverter.DoubleConverter.class)
    public Double myDoubleObject;

    @SerializableField(index=13,tritLength = 66, converter = TritsConverter.BigDecimalConverter.class)
    public BigDecimal myBigDecimal;

    @SerializableField(index=14,tritLength = 1, converter = TritsConverter.BooleanConverter.class)
    public List<Boolean> isTestList;

    @SerializableField(index=15,tritLength = 99, converter = TritsConverter.AsciiConverter.class)
    public List<String> myLabelList;

    @SerializableField(index=16,tritLength = 27, converter = TritsConverter.IntegerConverter.class)
    public List<Integer> myIntegerList;

    @SerializableField(index=17,tritLength = 54, converter = TritsConverter.LongConverter.class)
    public List<Long> myLongObjectList;

    @SerializableField(index=18,tritLength = 54, converter = TritsConverter.BigIntegerConverter.class)
    public List<BigInteger> myBigIntegerList;

    @SerializableField(index=19,tritLength = 27, converter = TritsConverter.FloatConverter.class)
    public List<Float> myFloatObjectList;

    @SerializableField(index=20,tritLength = 54, converter = TritsConverter.DoubleConverter.class)
    public List<Double> myDoubleObjectList;

    @SerializableField(index=21,tritLength = 54, converter = TritsConverter.BigDecimalConverter.class)
    public List<BigDecimal> myBigDecimalList;

}
