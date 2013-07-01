package de.skiptag.roadrunner.inmemory;

import org.json.JSONException;
import org.json.JSONObject;

import de.skiptag.roadrunner.core.DataService;
import de.skiptag.roadrunner.core.DataServiceCreationException;
import de.skiptag.roadrunner.core.DataServiceFactory;
import de.skiptag.roadrunner.core.RuleBasedAuthorizationService;
import de.skiptag.roadrunner.core.authorization.AuthenticationServiceFactory;
import de.skiptag.roadrunner.core.authorization.AuthorizationService;

public class InMemoryServiceFactory implements DataServiceFactory,
	AuthenticationServiceFactory {

    private static InMemoryServiceFactory instance;

    @Override
    public AuthorizationService getAuthorizationService(JSONObject rule) {
	try {
	    return new RuleBasedAuthorizationService(rule);
	} catch (JSONException e) {
	    e.printStackTrace();
	}
	return null;
    }

    @Override
    public DataService getDataService(
	    AuthorizationService authorizationService, String repositoryName)
	    throws DataServiceCreationException {
	return new InMemoryDataService(authorizationService, repositoryName);
    }

    public static InMemoryServiceFactory getInstance() {
	if (instance == null) {
	    instance = new InMemoryServiceFactory();
	}
	return instance;
    }

}
