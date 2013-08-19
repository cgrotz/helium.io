package de.skiptag.roadrunner;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.skiptag.roadrunner.disruptor.event.RoadrunnerEvent;
import de.skiptag.roadrunner.disruptor.event.RoadrunnerEventType;
import de.skiptag.roadrunner.disruptor.event.builder.RoadrunnerEventBuilder;
import de.skiptag.roadrunner.disruptor.processor.persistence.PersistenceProcessor;
import de.skiptag.roadrunner.persistence.Path;
import de.skiptag.roadrunner.persistence.inmemory.InMemoryPersistence;

public class PersistenceProcessorTest {
	public static final String BASE_PATH = "http://localhot:8080";
	private InMemoryPersistence persistence;
	private PersistenceProcessor persistenceProcessor;

	@Before
	public void setUp() throws Exception {
		persistence = new InMemoryPersistence(new Roadrunner(BASE_PATH));
		persistenceProcessor = new PersistenceProcessor(persistence);
	}

	@Test
	public void pushTest() {
		RoadrunnerEvent event = RoadrunnerEventBuilder.start().type(RoadrunnerEventType.PUSH)
				.path(BASE_PATH + "/test/test/").withNode().put("msg", "HalloWelt").complete()
				.build();
		persistenceProcessor.onEvent(event, 0, false);
		Path path = new Path("/test/test");
		Assert.assertEquals(persistence.get(path.append("msg")), "HalloWelt");
	}

	@Test
	public void setTest() {
		RoadrunnerEvent event = RoadrunnerEventBuilder.start().type(RoadrunnerEventType.SET)
				.path(BASE_PATH + "/test/test/").withNode().put("msg", "HalloWelt").complete()
				.build();
		persistenceProcessor.onEvent(event, 0, false);
		Path path = new Path("/test/test");
		Assert.assertEquals(persistence.get(path.append("msg")), "HalloWelt");
	}

	@Test
	public void setPriorityTest() {
		RoadrunnerEvent event = RoadrunnerEventBuilder.start()
				.type(RoadrunnerEventType.SETPRIORITY).path(BASE_PATH + "/test/test/")
				.priority("1").build();
		persistenceProcessor.onEvent(event, 0, false);
	}
}
