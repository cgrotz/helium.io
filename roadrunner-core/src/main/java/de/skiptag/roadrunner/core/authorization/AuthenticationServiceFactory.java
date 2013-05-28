package de.skiptag.roadrunner.core.authorization;

public interface AuthenticationServiceFactory {
	public AuthorizationService getAuthorizationService(String repositoryName);
}
