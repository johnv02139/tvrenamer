package org.tvrenamer.model.except;

import java.io.IOException;

public class TVRenamerIOException extends IOException {

    public TVRenamerIOException(String message) {
        super(message);
    }

    public TVRenamerIOException(String message, Throwable cause) {
        super(message, cause);
    }
}
