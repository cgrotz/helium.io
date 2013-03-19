package org.roadrunner.core;

import org.roadrunner.core.authorization.AuthorizationService;

public interface DataServiceFactory {
	public DataService getDataService(AuthorizationService authorizationService,String repositoryName) throws DataServiceCreationException;
}
