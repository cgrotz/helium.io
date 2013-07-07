package de.skiptag.roadrunner.inmemory;

import java.util.concurrent.ExecutionException;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import de.skiptag.roadrunner.core.authorization.AuthenticationServiceFactory;
import de.skiptag.roadrunner.core.authorization.AuthorizationService;
import de.skiptag.roadrunner.core.authorization.RuleBasedAuthorizationService;
import de.skiptag.roadrunner.core.dataService.DataService;
import de.skiptag.roadrunner.core.dataService.DataServiceCreationException;
import de.skiptag.roadrunner.core.dataService.DataServiceFactory;

public class InMemoryServiceFactory implements DataServiceFactory,
	AuthenticationServiceFactory {

    private static InMemoryServiceFactory instance;
    private AuthorizationService authorizationService;

    CacheLoader<String, DataService> loader = new CacheLoader<String, DataService>() {
	public DataService load(String key) throws Exception {
	    return new InMemoryDataService(authorizationService);
	}
    };
    LoadingCache<String, DataService> cache = CacheBuilder.newBuilder()
	    .build(loader);

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
	this.authorizationService = authorizationService;
	try {
	    return cache.get(repositoryName);
	} catch (ExecutionException e) {
	    throw new DataServiceCreationException(e);
	}
    }

    public static InMemoryServiceFactory getInstance() {
	if (instance == null) {
	    instance = new InMemoryServiceFactory();
	}
	return instance;
    }
}
