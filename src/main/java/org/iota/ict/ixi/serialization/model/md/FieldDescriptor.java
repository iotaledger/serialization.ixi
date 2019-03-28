package org.iota.ict.ixi.serialization.model.md;

import org.iota.ict.ixi.serialization.util.InputValidator;
import org.iota.ict.utils.Trytes;

import java.math.BigInteger;

@SuppressWarnings("WeakerAccess")
public class FieldDescriptor {

    public static final int FIELD_TYPE_LENGTH = 9;
    public static final int FIELD_SIZE_LENGTH = 12;
    public static final int FIELD_LABEL_LENGTH = 141;

    public static final int FIELD_TYPE_TRYTE_LENGTH = FIELD_TYPE_LENGTH / 3;
    public static final int FIELD_SIZE_TRYTE_LENGTH = FIELD_SIZE_LENGTH / 3;
    public static final int FIELD_LABEL_TRYTE_LENGTH = FIELD_LABEL_LENGTH / 3;

    public static final int FIELD_SIZE_TRYTE_OFFSET = FIELD_TYPE_TRYTE_LENGTH;
    public static final int LABEL_TRYTE_OFFSET = FIELD_SIZE_TRYTE_OFFSET + FIELD_SIZE_TRYTE_LENGTH;

    public static final int FIELD_DESCRIPTOR_LENGTH = FIELD_TYPE_LENGTH + FIELD_SIZE_LENGTH + FIELD_LABEL_LENGTH;
    public static final int FIELD_DESCRIPTOR_TRYTE_LENGTH = FIELD_DESCRIPTOR_LENGTH /3;

    private static final BigInteger BIG_INT_3 = new BigInteger("3");

    private FieldType type;
    private BigInteger size;
    private String label;

    private final String trytes;

    public static FieldDescriptor withAsciiLabel(FieldType type, long size, String label) {
        return withAsciiLabel(type, BigInteger.valueOf(size), label);
    }

    public static FieldDescriptor withTrytesLabel(FieldType type, long size, String label) {
        return withTrytesLabel(type, BigInteger.valueOf(size), label);
    }

    public static FieldDescriptor withAsciiLabel(FieldType type, BigInteger size, String label) {
        checkInputs(type, size);
        return new FieldDescriptor(
                type,
                size,
                asciiLabelToTrytes(label));
    }

    public static FieldDescriptor withTrytesLabel(FieldType type, BigInteger size, String label){
        checkInputs(type, size);
        return new FieldDescriptor(type, size, label);
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

    public BigInteger getSize() {
        return size;
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
        return size.divide(BIG_INT_3).intValue();
    }

    private FieldDescriptor(FieldType type, BigInteger size, String labelAsTrytes){
        this.type = type;
        this.size = size;
        this.label = labelAsTrytes.length()>FIELD_LABEL_TRYTE_LENGTH ? labelAsTrytes.substring(0,FIELD_LABEL_TRYTE_LENGTH) : labelAsTrytes;

        trytes = type.trytes() +
                Trytes.fromNumber(size, FIELD_SIZE_TRYTE_LENGTH) +
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
        return InputValidator.fit(Trytes.fromAscii(label == null ? "" : label), FIELD_LABEL_TRYTE_LENGTH);
    }


}
