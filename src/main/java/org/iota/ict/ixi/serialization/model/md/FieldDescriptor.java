package org.iota.ict.ixi.serialization.model.md;

import org.iota.ict.ixi.serialization.util.SerializableField;
import org.iota.ict.utils.Trytes;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public class FieldDescriptor {

    public static final int FIELD_TYPE_TRIT_LENGTH = 3;
    public static final int FIELD_SIZE_TRIT_LENGTH = 12;

    public static final int FIELD_TYPE_TRYTE_LENGTH = FIELD_TYPE_TRIT_LENGTH / 3;
    public static final int FIELD_SIZE_TRYTE_LENGTH = FIELD_SIZE_TRIT_LENGTH / 3;

    public static final int FIELD_SIZE_TRYTE_OFFSET = FIELD_TYPE_TRYTE_LENGTH;
    public static final int LABEL_TRYTE_OFFSET = FIELD_SIZE_TRYTE_OFFSET + FIELD_SIZE_TRYTE_LENGTH;

    public static final int FIELD_DESCRIPTOR_TRIT_LENGTH = FIELD_TYPE_TRIT_LENGTH + FIELD_SIZE_TRIT_LENGTH;
    public static final int FIELD_DESCRIPTOR_TRYTE_LENGTH = FIELD_DESCRIPTOR_TRIT_LENGTH /3;

    private boolean isList;
    private BigInteger tritSize;

    private final String trytes;

    static {
        StringBuilder sb = new StringBuilder();
        for(int i=0;i<FIELD_DESCRIPTOR_TRYTE_LENGTH;i++)sb.append("9");
        NULL_DESCRIPTOR_TRYTES = sb.toString();
    }
    public static final String NULL_DESCRIPTOR_TRYTES;

    public static FieldDescriptor build(boolean isList, long tritSize) {
        return build(isList, BigInteger.valueOf(tritSize));
    }

    public static FieldDescriptor build(boolean isList, BigInteger tritSize) {
        checkInputs(tritSize);
        return new FieldDescriptor(
                isList,
                tritSize);
    }

    public static FieldDescriptor fromField(Field field){
        SerializableField annotation = field.getAnnotation(SerializableField.class);
        return FieldDescriptor.build(field.getType().isAssignableFrom(List.class),annotation.tritLength());
    }

    public static FieldDescriptor fromTrytes(String trytes) {
        return new FieldDescriptor(
                Trytes.toTrits(trytes.substring(0,1))[0]==-1,
                Trytes.toNumber(trytes.substring(FIELD_SIZE_TRYTE_OFFSET, LABEL_TRYTE_OFFSET))
             );
    }

    public boolean isList() {
        return isList;
    }

    public BigInteger getTritSize() {
        return tritSize;
    }

    public String toTrytes(){
        return trytes;
    }

    private FieldDescriptor(boolean isList, BigInteger tritSize){
        this.isList = isList;
        this.tritSize = tritSize;

        trytes = (isList?"Z":"A") +
                Trytes.fromNumber(tritSize, FIELD_SIZE_TRYTE_LENGTH);
    }

    private static void checkInputs(BigInteger size) {
        if(size==null){
            throw new IllegalArgumentException("size cannot be null");
        }
        if(size.longValue()<=0){
            throw new IllegalArgumentException("size cannot be < 1");
        }
    }

    public boolean isSingleValue() {
        return !isList;
    }

}
