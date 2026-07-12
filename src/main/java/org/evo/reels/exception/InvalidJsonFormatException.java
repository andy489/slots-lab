package org.evo.reels.exception;

import java.io.IOException;

public class InvalidJsonFormatException extends IOException {
    public InvalidJsonFormatException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
