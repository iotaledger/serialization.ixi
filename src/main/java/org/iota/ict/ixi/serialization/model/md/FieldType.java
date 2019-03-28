package org.iota.ict.ixi.serialization.model.md;

import java.util.Objects;

public class FieldType {

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
