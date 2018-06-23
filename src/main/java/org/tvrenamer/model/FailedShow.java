package org.tvrenamer.model;

public class FailedShow extends Show {

    private final GenericException ioe;

    public GenericException getException() {
        return ioe;
    }

    public FailedShow(String id, String name, String url, GenericException e) {
        super(id, name, url);
        ioe = e;
    }

    public FailedShow(String id, String name, TVRenamerIOException e) {
        super(id, name, "");
        ioe = new GenericException(e.getMessage());
    }

    public FailedShow(String message) {
        super("", message, "");
        ioe = null;
    }
}
