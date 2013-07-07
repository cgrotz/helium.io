package de.skiptag.roadrunner.persistence;

public class PersistenceCreationException extends Exception {
	private static final long serialVersionUID = 1L;

	public PersistenceCreationException() {
		super();
	}

	public PersistenceCreationException(String message, Throwable cause) {
		super(message, cause);
	}

	public PersistenceCreationException(String message) {
		super(message);
	}

	public PersistenceCreationException(Throwable cause) {
		super(cause);
	}

}
