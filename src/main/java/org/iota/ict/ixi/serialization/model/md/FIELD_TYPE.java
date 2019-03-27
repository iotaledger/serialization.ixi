package org.iota.ict.ixi.serialization.model.md;

import org.iota.ict.ixi.serialization.util.UnknownFieldTypeException;

public enum FIELD_TYPE {

    INTEGER("INT"), ASCII("ASC"), FLOAT("FLT"), BOOLEAN("BOO"), TRANSACTION_HASH("TXH");

    private String trytes;

    FIELD_TYPE(String trytes) {
        this.trytes = trytes;
    }

    public static FIELD_TYPE fromTrytes(String trytes) throws UnknownFieldTypeException {
        for(FIELD_TYPE type:values()){
            if(type.trytes.equals(trytes))return type;
        }
        throw new UnknownFieldTypeException(trytes+" is not a valid field type");
    }

    public String trytes(){
        return trytes;
    }
}
