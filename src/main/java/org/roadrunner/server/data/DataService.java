package org.roadrunner.server.data;

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
public class DataService
{
  private static DataService instance;
  private ModeShapeEngine engine;
  private Map<String, Repository> repositories = Maps.newHashMap();

  private DataService()
  {
    engine = new ModeShapeEngine();
    engine.start();
  }

  public static DataService getInstance()
  {
    if (instance == null)
    {
      instance = new DataService();
    }
    return instance;
  }

  public Repository getRepository(String repositoryName) throws ParsingException, ConfigurationException, RepositoryException
  {
    if (repositories.containsKey(repositoryName))
    {
      return repositories.get(repositoryName);
    }

    Repository repository = null;
    if (!engine.getRepositoryNames().contains(repositoryName))
    {
      URL url = DataService.class.getClassLoader().getResource("roadrunner-repository-config.json");

      RepositoryConfiguration config = RepositoryConfiguration.read(url);
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
}