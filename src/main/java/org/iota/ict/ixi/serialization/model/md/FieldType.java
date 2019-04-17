package org.iota.ict.ixi.serialization.model.md;

import org.iota.ict.ixi.serialization.util.Utils;

import java.util.Objects;

@SuppressWarnings("WeakerAccess")
public class FieldType {

    public static final String TRITS = "A9";        //1,0,0,0,0,0
    public static final String TRITS_LIST = "Z9";   //-1,0,0,0,0,0
    public static final String INTEGER = "D9";      //1,1,0,0,0,0
    public static final String INTEGER_LIST = "B9"; //-1,1,0,0,0,0
    public static final String BOOLEAN = "J9";      //1,0,1,0,0,0
    public static final String BOOLEAN_LIST = "H9"; //-1,0,1,0,0,0
    public static final String DECIMAL = "AA";        //1,0,0,1,0,0
    public static final String DECIMAL_LIST = "ZA";   //-1,0,0,1,0,0
    public static final String ASCII = "AI";        //1,0,0,0,0,1
    public static final String ASCII_LIST = "ZI";   //-1,0,0,0,0,1
    public static final String HASH = "AC";         //1,0,0,0,1,0
    public static final String HASH_LIST = "ZC";    //-1,0,0,0,1,0

    public static final FieldType TYPE_TRITS = new FieldType(TRITS);  //A9
    public static final FieldType TYPE_TRITS_LIST = new FieldType(TRITS_LIST); //Z9
    public static final FieldType TYPE_INTEGER = new FieldType(INTEGER);  //D9
    public static final FieldType TYPE_INTEGER_LIST = new FieldType(INTEGER_LIST);  //B9
    public static final FieldType TYPE_BOOLEAN = new FieldType(BOOLEAN);  //J9
    public static final FieldType TYPE_BOOLEAN_LIST = new FieldType(BOOLEAN_LIST);  //H9
    public static final FieldType TYPE_DECIMAL = new FieldType(DECIMAL); //AA
    public static final FieldType TYPE_DECIMAL_LIST = new FieldType(DECIMAL_LIST); //ZA
    public static final FieldType TYPE_HASH = new FieldType(HASH); //AC
    public static final FieldType TYPE_HASH_LIST = new FieldType(HASH_LIST); //ZC
    public static final FieldType TYPE_ASCII = new FieldType(ASCII); //AI
    public static final FieldType TYPE_ASCII_LIST = new FieldType(ASCII_LIST); //ZI

    public final String trytes;

    FieldType(String trytes) {
        this.trytes = trytes;
    }

    public static FieldType fromTrytes(String trytes) {
        return new FieldType(trytes);
    }

    public final String trytes(){
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

    public boolean isSingleValue() {
        return Utils.toTrits(trytes.charAt(0))[0]==1;
    }
    public boolean isMultipleValue() {
        return Utils.toTrits(trytes.charAt(0))[0]==-1;
    }

    public boolean isInteger() {
        return trytes.equals(INTEGER) || trytes.equals(INTEGER_LIST);
    }

    public boolean isAscii() {
        return trytes.equals(ASCII) || trytes.equals(ASCII_LIST);
    }

    public boolean isHash() {
        return trytes.equals(HASH) || trytes.equals(HASH_LIST);
    }

    public boolean isBoolean() {
        return trytes.equals(BOOLEAN) || trytes.equals(BOOLEAN_LIST);
    }

    public boolean isDecimal() {
        return trytes.equals(DECIMAL) || trytes.equals(DECIMAL_LIST);
    }
}
