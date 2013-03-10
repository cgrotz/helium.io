package org.roadrunner.core;

public class DataServiceCreationException extends Exception {
	private static final long serialVersionUID = 1L;

	public DataServiceCreationException() {
		super();
	}

	public DataServiceCreationException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public DataServiceCreationException(String message, Throwable cause) {
		super(message, cause);
	}

	public DataServiceCreationException(String message) {
		super(message);
	}

	public DataServiceCreationException(Throwable cause) {
		super(cause);
	}

}
