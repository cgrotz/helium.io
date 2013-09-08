package de.skiptag.roadrunner.authorization;

import de.skiptag.roadrunner.common.Path;

public class RoadrunnerNotAuthorizedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RoadrunnerNotAuthorizedException(RoadrunnerOperation op, Path path) {
	super(op.toString() + " not allowed on " + path);
    }

}
