package io.helium.authorization;

import io.helium.common.Path;

public class HeliumNotAuthorizedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public HeliumNotAuthorizedException(HeliumOperation op, Path path) {
	super(op.toString() + " not allowed on " + path);
    }

}
