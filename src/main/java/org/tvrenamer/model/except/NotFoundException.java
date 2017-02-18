package org.tvrenamer.model.except;

public class NotFoundException extends RuntimeException {
    private static final long serialVersionUID = 0L;

    public NotFoundException(String message) {
        super(message);
    }
}
