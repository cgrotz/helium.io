package org.roadrunner.core.authorization;

public interface AuthenticationServiceFactory {
	public AuthorizationService getAuthorizationService(String repositoryName);
}
