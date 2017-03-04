package org.tvrenamer.model;

public class FailedShow extends Show {

    private final TVRenamerIOException ioe;

    public TVRenamerIOException getException() {
        return ioe;
    }

    public FailedShow(String id, String name, TVRenamerIOException err) {
        super(id, name);
        ioe = err;
    }
}
