package org.iota.ict.ixi.serialization.util;

public class UnknownMetadataException extends Exception {

    public UnknownMetadataException() {
    }

    public UnknownMetadataException(String message) {
        super(message);
    }

    public UnknownMetadataException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnknownMetadataException(Throwable cause) {
        super(cause);
    }

    public UnknownMetadataException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
