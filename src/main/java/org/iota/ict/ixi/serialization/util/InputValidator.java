package org.iota.ict.ixi.serialization.util;

public class InputValidator {

    public static boolean isValidHash(String hash) {
        if(hash == null)
            return false;
        return hash.matches("^[A-Z9]{81,81}$");
    }

}
