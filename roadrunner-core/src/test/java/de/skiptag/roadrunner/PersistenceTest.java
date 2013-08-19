package de.skiptag.roadrunner;

import org.json.Node;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.skiptag.roadrunner.disruptor.event.changelog.ChangeLog;
import de.skiptag.roadrunner.persistence.Path;
import de.skiptag.roadrunner.persistence.inmemory.InMemoryPersistence;

public class PersistenceTest {

	private InMemoryPersistence persistence;

	@Before
	public void setUp() throws Exception {
		persistence = new InMemoryPersistence(new Roadrunner(PersistenceProcessorTest.BASE_PATH));
	}

	@Test
	public void setSimpleValueTest() {
		Path path = new Path("/test/test");
		Assert.assertNotNull(persistence.get(path));
		Assert.assertNotNull(persistence.getNode(path));
		persistence.applyNewValue(new ChangeLog(), path.append("msg"), 1, "HalloWelt");
		Assert.assertEquals(persistence.get(path.append("msg")), "HalloWelt");
	}

	@Test
	public void setNodeValueTest() {
		Path path = new Path("/test/test");
		Assert.assertNotNull(persistence.get(path));
		Assert.assertNotNull(persistence.getNode(path));
		persistence.applyNewValue(new ChangeLog(), path, 2, new Node().put("msg", "HalloWelt"));
		Assert.assertEquals(persistence.get(path.append("msg")), "HalloWelt");
		persistence.applyNewValue(new ChangeLog(), path.append("msg"), 1, "HalloWelt2");
		Assert.assertEquals(persistence.get(path.append("msg")), "HalloWelt2");
	}
}
