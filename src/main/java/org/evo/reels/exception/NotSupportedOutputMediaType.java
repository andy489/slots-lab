package org.evo.reels.exception;

public class NotSupportedOutputMediaType extends RuntimeException {

    public NotSupportedOutputMediaType(String msg) {
        super(msg);
    }

    public NotSupportedOutputMediaType(String msg, Throwable cause) {
        super(msg, cause);
    }

}
