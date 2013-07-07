package de.skiptag.roadrunner.authorization;

public class RoadrunnerNotAuthorizedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RoadrunnerNotAuthorizedException(RoadrunnerOperation op, String path) {
	super(op.toString() + " not allowed on " + path);
    }

}
