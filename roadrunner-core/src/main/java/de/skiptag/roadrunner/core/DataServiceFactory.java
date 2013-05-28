package de.skiptag.roadrunner.core;

import de.skiptag.roadrunner.core.authorization.AuthorizationService;

public interface DataServiceFactory {
	public DataService getDataService(AuthorizationService authorizationService,String repositoryName) throws DataServiceCreationException;
}
