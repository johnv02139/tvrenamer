package org.tvrenamer.model;

import org.tvrenamer.model.except.TVRenamerIOException;

public class UnresolvedShow extends Series {

    private final TVRenamerIOException ioe;

    public TVRenamerIOException getException() {
        return ioe;
    }

    public UnresolvedShow(String name, TVRenamerIOException err) {
        super("", name);
        ioe = err;
    }

    public UnresolvedShow(String name) {
        this(name, null);
    }
}
