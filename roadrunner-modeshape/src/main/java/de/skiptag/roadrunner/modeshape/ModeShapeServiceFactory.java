package de.skiptag.roadrunner.modeshape;

import java.io.FileNotFoundException;
import java.util.Map;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;

import org.infinispan.schematic.document.ParsingException;
import org.json.JSONObject;
import org.modeshape.common.collection.Problems;
import org.modeshape.jcr.ConfigurationException;
import org.modeshape.jcr.ModeShapeEngine;
import org.modeshape.jcr.RepositoryConfiguration;

import com.google.common.collect.Maps;

import de.skiptag.roadrunner.core.DataServiceCreationException;
import de.skiptag.roadrunner.core.DataServiceFactory;
import de.skiptag.roadrunner.core.RuleBasedAuthorizationService;
import de.skiptag.roadrunner.core.authorization.AuthenticationServiceFactory;
import de.skiptag.roadrunner.core.authorization.AuthorizationService;

public class ModeShapeServiceFactory implements DataServiceFactory,
		AuthenticationServiceFactory {

	private ModeShapeEngine engine;
	private Map<String, Repository> repositories = Maps.newHashMap();

	private static final ModeShapeServiceFactory instance = new ModeShapeServiceFactory();

	public static ModeShapeServiceFactory getInstance() {
		instance.start();
		return instance;
	}

	private ModeShapeServiceFactory() {
	}

	public void destroy() {
		engine.shutdown();
	}

	@Override
	public RuleBasedAuthorizationService getAuthorizationService( JSONObject rule) {
		try
		{
			return new RuleBasedAuthorizationService( rule);
		}
		catch(Exception exp)
		{
			exp.printStackTrace();
		}
		return null;
	}

	@Override
	public ModeShapeDataService getDataService(
			AuthorizationService authorizationService, String repositoryName)
			throws DataServiceCreationException {
		try {
			Repository dataRepo = getRepository(repositoryName);
			return new ModeShapeDataService(authorizationService,
					dataRepo.login());
		} catch (Exception exp) {
			throw new DataServiceCreationException(exp);
		}
	}

	private Repository getRepository(String repositoryName)
			throws ParsingException, ConfigurationException,
			RepositoryException, FileNotFoundException {
		if (repositories.containsKey(repositoryName)) {
			return repositories.get(repositoryName);
		}

		Repository repository = null;
		if (!engine.getRepositoryNames().contains(repositoryName)) {
			RepositoryConfiguration config = RepositoryConfiguration
					.read("{'name' : '"
							+ repositoryName
							+ "','jndiName': null,'workspaces' : {'predefined' : ['otherWorkspace'],'default' : 'default','allowCreation' : true},'security' : {'anonymous' : {'roles' : ['readonly','readwrite','admin'],'useOnFailedLogin' : false}}}");
			// We could change the name of the repository programmatically ...
			config = config.withName(repositoryName);

			// Verify the configuration for the repository ...
			Problems problems = config.validate();
			if (problems.hasErrors()) {
				throw new RuntimeException();
			}

			// Deploy the repository ...
			repository = engine.deploy(config);
		} else {
			repository = engine.getRepository(repositoryName);
		}
		repositories.put(repositoryName, repository);
		return repository;
	}

	private void start() {
		engine = new ModeShapeEngine();
		engine.start();
	}
}
