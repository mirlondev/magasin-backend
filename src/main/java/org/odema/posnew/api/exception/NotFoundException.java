package org.odema.posnew.api.rest.exception;

public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }

    public NotFoundException(String resource, String field, Object value) {
        super(String.format("%s non trouvé(e) avec %s : '%s'", resource, field, value));
    }
}
