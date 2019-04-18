package org.iota.ict.ixi.serialization.model.md;

import org.iota.ict.ixi.serialization.util.Utils;
import org.iota.ict.utils.Trytes;

import java.math.BigInteger;

@SuppressWarnings("WeakerAccess")
public class FieldDescriptor {

    public static final int FIELD_TYPE_TRIT_LENGTH = 3;
    public static final int FIELD_SIZE_TRIT_LENGTH = 12;
    public static final int FIELD_LABEL_TRIT_LENGTH = 144;

    public static final int FIELD_TYPE_TRYTE_LENGTH = FIELD_TYPE_TRIT_LENGTH / 3;
    public static final int FIELD_SIZE_TRYTE_LENGTH = FIELD_SIZE_TRIT_LENGTH / 3;
    public static final int FIELD_LABEL_TRYTE_LENGTH = FIELD_LABEL_TRIT_LENGTH / 3;

    public static final int FIELD_SIZE_TRYTE_OFFSET = FIELD_TYPE_TRYTE_LENGTH;
    public static final int LABEL_TRYTE_OFFSET = FIELD_SIZE_TRYTE_OFFSET + FIELD_SIZE_TRYTE_LENGTH;

    public static final int FIELD_DESCRIPTOR_TRIT_LENGTH = FIELD_TYPE_TRIT_LENGTH + FIELD_SIZE_TRIT_LENGTH + FIELD_LABEL_TRIT_LENGTH;
    public static final int FIELD_DESCRIPTOR_TRYTE_LENGTH = FIELD_DESCRIPTOR_TRIT_LENGTH /3;

    private static final BigInteger BIG_INT_3 = new BigInteger("3");

    private boolean isList;
    private BigInteger tritSize;
    private String label;

    private final String trytes;

    static {
        StringBuilder sb = new StringBuilder();
        for(int i=0;i<FIELD_DESCRIPTOR_TRYTE_LENGTH;i++)sb.append("9");
        NULL_DESCRIPTOR_TRYTES = sb.toString();
    }
    public static final String NULL_DESCRIPTOR_TRYTES;

    public static FieldDescriptor withAsciiLabel(boolean isList, long tritSize, String label) {
        return withAsciiLabel(isList, BigInteger.valueOf(tritSize), label);
    }

    public static FieldDescriptor withTrytesLabel(boolean isList, long tritSize, String label) {
        return withTrytesLabel(isList, BigInteger.valueOf(tritSize), label);
    }

    public static FieldDescriptor withAsciiLabel(boolean isList, BigInteger tritSize, String label) {
        checkInputs(isList, tritSize);
        return new FieldDescriptor(
                isList,
                tritSize,
                asciiLabelToTrytes(label));
    }

    public static FieldDescriptor withTrytesLabel(boolean isList, BigInteger tritSize, String label){
        checkInputs(isList, tritSize);
        return new FieldDescriptor(isList, tritSize, label);
    }

    public static FieldDescriptor fromTrytes(String trytes) {
        return new FieldDescriptor(
                Trytes.toTrits(trytes.substring(0,1))[0]==-1,
                Trytes.toNumber(trytes.substring(FIELD_SIZE_TRYTE_OFFSET, LABEL_TRYTE_OFFSET)),
                trytes.substring(LABEL_TRYTE_OFFSET)
             );
    }

    public boolean isList() {
        return isList;
    }

    public BigInteger getTritSize() {
        return tritSize;
    }

    public String getLabel() {
        return label;
    }

    public String getAsciiLabel() {
        return Trytes.toAscii(label).trim();
    }

    public String toTrytes(){
        return trytes;
    }

    public int getTryteSize() {
        return tritSize.divide(BIG_INT_3).intValue();
    }

    private FieldDescriptor(boolean isList, BigInteger tritSize, String labelAsTrytes){
        this.isList = isList;
        this.tritSize = tritSize;
        this.label = labelAsTrytes.length()>FIELD_LABEL_TRYTE_LENGTH ? labelAsTrytes.substring(0,FIELD_LABEL_TRYTE_LENGTH) : labelAsTrytes;

        trytes = (isList?"Z":"A") +
                Trytes.fromNumber(tritSize, FIELD_SIZE_TRYTE_LENGTH) +
                labelAsTrytes;
    }

    private static void checkInputs(boolean isList, BigInteger size) {
        if(size==null){
            throw new IllegalArgumentException("size cannot be null");
        }
        if(size.longValue()==0){
            throw new IllegalArgumentException("size cannot be 0");
        }
    }

    private static String asciiLabelToTrytes(String label) {
        return Utils.fit(Trytes.fromAscii(label == null ? "" : label), FIELD_LABEL_TRYTE_LENGTH);
    }

    public boolean isSingleValue() {
        return !isList;
    }

//    public boolean isInteger() {
//        return type.isInteger();
//    }
//
//    public boolean isAscii() {
//        return type.isAscii();
//    }
//
//    public boolean isHash() {
//        return type.isHash();
//    }
//
//    public boolean isBoolean() {
//        return type.isBoolean();
//    }
//
//    public boolean isDecimal() {
//        return type.isDecimal();
//    }
}
