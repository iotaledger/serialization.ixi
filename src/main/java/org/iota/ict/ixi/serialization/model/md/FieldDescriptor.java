package org.iota.ict.ixi.serialization.model.md;

import org.iota.ict.ixi.serialization.util.UnknownFieldTypeException;
import org.iota.ict.utils.Trytes;

import java.lang.reflect.Field;
import java.math.BigInteger;

@SuppressWarnings("WeakerAccess")
public class FieldDescriptor {

    public static final int FIELD_TYPE_LENGTH = 9;
    public static final int FIELD_SIZE_LENGTH = 12;
    public static final int FIELD_LABEL_LENGTH = 141;

    public static final int FIELD_DESCRIPTOR_TRIT_LENGTH = FIELD_TYPE_LENGTH + FIELD_SIZE_LENGTH + FIELD_LABEL_LENGTH;
    public static final int FIELD_DESCRIPTOR_TRYTE_LENGTH = FIELD_DESCRIPTOR_TRIT_LENGTH/3;

    private FIELD_TYPE type;
    private BigInteger size;
    private String label;

    private final String trytes;

    public static FieldDescriptor withAsciiLabel(FIELD_TYPE type, int size, String label) {
        return withAsciiLabel(type, BigInteger.valueOf(size), fit(Trytes.fromAscii(label == null ? "" : removeTrailing9(label)), FIELD_LABEL_LENGTH / 3));
    }

    public static FieldDescriptor withTrytesLabel(FIELD_TYPE type, int size, String label) {
        return withTrytesLabel(type, BigInteger.valueOf(size), label);
    }

    public static FieldDescriptor withAsciiLabel(FIELD_TYPE type, BigInteger size, String label) {
        checkInputs(type, size);
        return new FieldDescriptor(
                type,
                size,
                fit(Trytes.fromAscii(label == null ? "" : removeTrailing9(label)), FIELD_LABEL_LENGTH / 3));
    }

    public static FieldDescriptor withTrytesLabel(FIELD_TYPE type, BigInteger size, String label){
        checkInputs(type, size);
        return new FieldDescriptor(type, size, label);
    }

    public static FieldDescriptor fromTrytes(String trytes) throws UnknownFieldTypeException {
        FIELD_TYPE type = FIELD_TYPE.fromTrytes(trytes.substring(0,FIELD_TYPE_LENGTH/3));
        if(type==null){
            throw new UnknownFieldTypeException("trytes.substring(0,FIELD_TYPE_LENGTH/3) is not a valid field type");
        }
        return new FieldDescriptor(
                type,
                Trytes.toNumber(trytes.substring(FIELD_TYPE_LENGTH/3,(FIELD_TYPE_LENGTH+FIELD_SIZE_LENGTH)/3)),
                trytes.substring((FIELD_TYPE_LENGTH+FIELD_SIZE_LENGTH)/3)
             );
    }

    private FieldDescriptor(FIELD_TYPE type, BigInteger size, String labelAsTrytes){
        this.type = type;
        this.size = size;
        this.label = labelAsTrytes.length()>FIELD_LABEL_LENGTH/3 ? labelAsTrytes.substring(0,FIELD_LABEL_LENGTH/3) : labelAsTrytes;

        trytes = type.trytes() +
                Trytes.fromNumber(size, FIELD_SIZE_LENGTH / 3) +
                labelAsTrytes;
    }

    private static void checkInputs(FIELD_TYPE type, BigInteger size) {
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

    public String toTrytes(){
        return trytes;
    }

    private static String fit(String original, int targetSize){
        if(original==null) original = "";
        if(original.length()<targetSize) return Trytes.padRight(original, targetSize);
        if(original.length()>targetSize) return original.substring(0,targetSize);
        return original;
    }

    private static String removeTrailing9(String s){
        while(s.length()>0 && s.charAt(s.length()-1)=='9'){
            s = s.substring(0,s.length()-1);
        }
        return s;
    }

    public FIELD_TYPE getType() {
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

}
