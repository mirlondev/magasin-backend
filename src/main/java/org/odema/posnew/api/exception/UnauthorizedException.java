package org.odema.posnew.api.exception;

public class UnauthorizedException extends Exception {
    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException(String resource, String field, Object value) {
        super(String.format("%s Vous ne pouvez pas acceder a cette ressource avec %s : '%s'", resource, field, value));
    }

}
