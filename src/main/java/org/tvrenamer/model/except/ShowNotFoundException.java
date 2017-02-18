package org.tvrenamer.model.except;

public class ShowNotFoundException extends NotFoundException {
    private static final long serialVersionUID = 0L;

    public ShowNotFoundException(String message) {
        super(message);
    }
}
