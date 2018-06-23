package org.tvrenamer.model;

import java.io.IOException;

public class GenericException extends IOException {
    private static final long serialVersionUID = 3028633984566046407L;

    public GenericException(String message) {
        super(message);
    }

    public GenericException(String message, Throwable cause) {
        // The constructor <code>super(message, cause)</code> was introduced in jdk6 so have to do a hack-around for
        // jdk5. For details see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5070673
        super(message);
        initCause(cause);
    }
}
