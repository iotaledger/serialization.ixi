package org.iota.ict.ixi.serialization.model.md;

import org.iota.ict.ixi.serialization.util.Utils;
import org.iota.ict.utils.Trytes;

import java.math.BigInteger;

@SuppressWarnings("WeakerAccess")
public class FieldDescriptor {

    public static final int FIELD_TYPE_TRIT_LENGTH = 6;
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

    private FieldType type;
    private BigInteger tritSize;
    private String label;

    private final String trytes;

    public static FieldDescriptor withAsciiLabel(FieldType type, long tritSize, String label) {
        return withAsciiLabel(type, BigInteger.valueOf(tritSize), label);
    }

    public static FieldDescriptor withTrytesLabel(FieldType type, long tritSize, String label) {
        return withTrytesLabel(type, BigInteger.valueOf(tritSize), label);
    }

    public static FieldDescriptor withAsciiLabel(FieldType type, BigInteger tritSize, String label) {
        checkInputs(type, tritSize);
        return new FieldDescriptor(
                type,
                tritSize,
                asciiLabelToTrytes(label));
    }

    public static FieldDescriptor withTrytesLabel(FieldType type, BigInteger tritSize, String label){
        checkInputs(type, tritSize);
        return new FieldDescriptor(type, tritSize, label);
    }

    public static FieldDescriptor fromTrytes(String trytes) {
        return new FieldDescriptor(
                FieldType.fromTrytes(trytes.substring(0,FIELD_SIZE_TRYTE_OFFSET)),
                Trytes.toNumber(trytes.substring(FIELD_SIZE_TRYTE_OFFSET, LABEL_TRYTE_OFFSET)),
                trytes.substring(LABEL_TRYTE_OFFSET)
             );
    }

    public FieldType getType() {
        return type;
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

    private FieldDescriptor(FieldType type, BigInteger tritSize, String labelAsTrytes){
        this.type = type;
        this.tritSize = tritSize;
        this.label = labelAsTrytes.length()>FIELD_LABEL_TRYTE_LENGTH ? labelAsTrytes.substring(0,FIELD_LABEL_TRYTE_LENGTH) : labelAsTrytes;

        trytes = type.trytes() +
                Trytes.fromNumber(tritSize, FIELD_SIZE_TRYTE_LENGTH) +
                labelAsTrytes;
    }

    private static void checkInputs(FieldType type, BigInteger size) {
        if(type==null){
            throw new IllegalArgumentException("type cannot be null");
        }
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
        return type.isSingleValue();
    }

    public boolean isInteger() {
        return type.isInteger();
    }

    public boolean isAscii() {
        return type.isAscii();
    }

    public boolean isHash() {
        return type.isHash();
    }

    public boolean isBoolean() {
        return type.isBoolean();
    }

    public boolean isDecimal() {
        return type.isDecimal();
    }
}
