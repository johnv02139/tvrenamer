package org.tvrenamer.model.except;

public class EpisodeNotFoundException extends NotFoundException {
    private static final long serialVersionUID = 0L;

    public EpisodeNotFoundException(String message) {
        super(message);
    }
}
