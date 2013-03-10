package org.roadrunner.core;

public interface DataServiceFactory {
	public DataService getDataService(String repositoryName) throws DataServiceCreationException;
}
