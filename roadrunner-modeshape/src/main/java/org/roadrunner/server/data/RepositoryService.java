package org.roadrunner.server.data;

import java.io.FileNotFoundException;
import java.net.URL;
import java.util.Map;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;

import org.infinispan.schematic.document.ParsingException;
import org.modeshape.common.collection.Problems;
import org.modeshape.jcr.ConfigurationException;
import org.modeshape.jcr.ModeShapeEngine;
import org.modeshape.jcr.RepositoryConfiguration;

import com.google.common.collect.Maps;

/**
 * Created with IntelliJ IDEA. User: Balu Date: 18.02.13 Time: 20:46 To change
 * this template use File | Settings | File Templates.
 */
public class RepositoryService
{
  private static RepositoryService instance;
  private ModeShapeEngine engine;
  private Map<String, Repository> repositories = Maps.newHashMap();

  private RepositoryService()
  {
    engine = new ModeShapeEngine();
    engine.start();
  }

  public static RepositoryService getInstance()
  {
    if (instance == null)
    {
      instance = new RepositoryService();
    }
    return instance;
  }

  public Repository getRepository(String repositoryName) throws ParsingException, ConfigurationException, RepositoryException, FileNotFoundException
  {
    if (repositories.containsKey(repositoryName))
    {
      return repositories.get(repositoryName);
    }

    Repository repository = null;
    if (!engine.getRepositoryNames().contains(repositoryName))
    {
      RepositoryConfiguration config = RepositoryConfiguration.read("{'name' : '"+repositoryName+"','jndiName': null,'workspaces' : {'predefined' : ['otherWorkspace'],'default' : 'default','allowCreation' : true},'security' : {'anonymous' : {'roles' : ['readonly','readwrite','admin'],'useOnFailedLogin' : false},'providers' : [{'name' : 'Rule Based Authorization Provider','classname' : 'org.roadrunner.security.RuleBasedAuthorizationProvider'}]}}");
      // We could change the name of the repository programmatically ...
      config = config.withName(repositoryName);

      // Verify the configuration for the repository ...
      Problems problems = config.validate();
      if (problems.hasErrors())
      {
        throw new RuntimeException();
      }

      // Deploy the repository ...
      repository = engine.deploy(config);
    }
    else
    {
      repository = engine.getRepository(repositoryName);
    }
    repositories.put(repositoryName, repository);
    return repository;
  }
  
  public DataService getDataService(String repositoryName) throws ParsingException, ConfigurationException, RepositoryException, FileNotFoundException
  {
	  Repository commonRepo = getRepository("common");
	  Repository dataRepo = getRepository(repositoryName);
	  return new DataService(commonRepo.login(), dataRepo.login());
  }
}