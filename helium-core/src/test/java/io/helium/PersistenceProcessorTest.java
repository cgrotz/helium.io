package io.helium;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.helium.authorization.Authorization;
import io.helium.authorization.rulebased.RuleBasedAuthorization;
import io.helium.common.Path;
import io.helium.disruptor.processor.persistence.PersistenceProcessor;
import io.helium.event.HeliumEvent;
import io.helium.event.HeliumEventType;
import io.helium.event.builder.HeliumEventBuilder;
import io.helium.persistence.inmemory.InMemoryPersistence;

public class PersistenceProcessorTest {
	public static final String BASE_PATH = "http://localhot:8080";
	private InMemoryPersistence persistence;
	private PersistenceProcessor persistenceProcessor;

	@Before
	public void setUp() throws Exception {
		persistence = new InMemoryPersistence(new RuleBasedAuthorization(
				Authorization.ALL_ACCESS_RULE), new Helium(BASE_PATH));
		persistenceProcessor = new PersistenceProcessor(persistence);
	}

	@Test
	public void pushTest() {
		HeliumEvent event = HeliumEventBuilder.start().type(HeliumEventType.PUSH)
				.path(BASE_PATH + "/test/test/").withNode().put("msg", "HalloWelt").complete()
				.build();
		persistenceProcessor.onEvent(event, 0, false);
		Path path = new Path("/test/test");
		Assert.assertEquals(persistence.get(path.append("msg")), "HalloWelt");
	}

	@Test
	public void setTest() {
		HeliumEvent event = HeliumEventBuilder.start().type(HeliumEventType.SET)
				.path(BASE_PATH + "/test/test/").withNode().put("msg", "HalloWelt").complete()
				.build();
		persistenceProcessor.onEvent(event, 0, false);
		Path path = new Path("/test/test");
		Assert.assertEquals(persistence.get(path.append("msg")), "HalloWelt");
	}

	@Test
	public void setPriorityTest() {
		HeliumEvent event = HeliumEventBuilder.start()
				.type(HeliumEventType.SETPRIORITY).path(BASE_PATH + "/test/test/")
				.priority("1").build();
		persistenceProcessor.onEvent(event, 0, false);
	}
}
