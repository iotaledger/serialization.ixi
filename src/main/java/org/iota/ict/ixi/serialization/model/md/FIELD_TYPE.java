package org.iota.ict.ixi.serialization.model.md;

public enum FIELD_TYPE {

    INTEGER("INT"), ASCII("ASC"), FLOAT("FLT"), BOOLEAN("BOO"), TRANSACTION_HASH("TXH");

    private String trytes;

    FIELD_TYPE(String trytes) {
        this.trytes = trytes;
    }

    public static FIELD_TYPE fromTrytes(String trytes) {
        for(FIELD_TYPE type:values()){
            if(type.trytes.equals(trytes))return type;
        }
        return null;
    }

    public String trytes(){
        return trytes;
    }
}
