package de.skiptag.roadrunner.core.authorization;

import org.json.JSONObject;

public interface AuthenticationServiceFactory {
	public AuthorizationService getAuthorizationService(JSONObject rule);
}
