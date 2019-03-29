package org.iota.ict.ixi.serialization.model.md;

import org.iota.ict.utils.Trytes;

import java.util.Objects;

public class FieldType {

    public static final FieldType TRITS = new FieldType(Trytes.fromTrits(new byte[]{1,0,0,0,0,0}));  //A9
    public static final FieldType TRITS_LIST = new FieldType(Trytes.fromTrits(new byte[]{-1,0,0,0,0,0})); //Z9
    public static final FieldType INTEGER = new FieldType(Trytes.fromTrits(new byte[]{1,1,0,0,0,0}));  //D9
    public static final FieldType INTEGER_LIST = new FieldType(Trytes.fromTrits(new byte[]{-1,1,0,0,0,0}));  //B9
    public static final FieldType BOOLEAN = new FieldType(Trytes.fromTrits(new byte[]{1,0,1,0,0,0}));  //J9
    public static final FieldType BOOLEAN_LIST = new FieldType(Trytes.fromTrits(new byte[]{-1,0,1,0,0,0}));  //H9
    public static final FieldType FLOAT = new FieldType(Trytes.fromTrits(new byte[]{1,0,0,1,0,0})); //AA
    public static final FieldType FLOAT_LIST = new FieldType(Trytes.fromTrits(new byte[]{-1,0,0,1,0,0})); //ZA
    public static final FieldType HASH = new FieldType(Trytes.fromTrits(new byte[]{1,0,0,0,1,0})); //AC
    public static final FieldType HASH_LIST = new FieldType(Trytes.fromTrits(new byte[]{-1,0,0,0,1,0})); //ZC
    public static final FieldType ASCII = new FieldType(Trytes.fromTrits(new byte[]{1,0,0,0,0,1})); //AI
    public static final FieldType ASCII_LIST = new FieldType(Trytes.fromTrits(new byte[]{-1,0,0,0,0,1})); //ZI

    private String trytes;

    FieldType(String trytes) {
        this.trytes = trytes;
    }

    public static FieldType fromTrytes(String trytes) {
        return new FieldType(trytes);
    }

    public String trytes(){
        return trytes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FieldType fieldType = (FieldType) o;
        return Objects.equals(trytes, fieldType.trytes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(trytes);
    }
}
