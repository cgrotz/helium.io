/**
 * 
 */
package de.skiptag.roadrunner;

import org.json.Node;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import de.skiptag.roadrunner.authorization.rulebased.RuleBasedAuthorization;
import de.skiptag.roadrunner.disruptor.event.RoadrunnerEvent;
import de.skiptag.roadrunner.disruptor.event.RoadrunnerEventType;
import de.skiptag.roadrunner.disruptor.event.builder.RoadrunnerEventBuilder;
import de.skiptag.roadrunner.messaging.RoadrunnerEndpoint;
import de.skiptag.roadrunner.messaging.RoadrunnerResponseSender;
import de.skiptag.roadrunner.persistence.Path;
import de.skiptag.roadrunner.persistence.inmemory.InMemoryPersistence;

/**
 * @author balu
 * 
 */
public class RoadrunnerSenderTest {

	private static final String BASE_PATH = "http://localhot:8080";
	private RoadrunnerResponseSender sender;
	private RoadrunnerEndpoint endpoint;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		this.sender = Mockito.mock(RoadrunnerResponseSender.class);
		this.endpoint = new RoadrunnerEndpoint(BASE_PATH, new Node(), sender,
				new InMemoryPersistence(), new RuleBasedAuthorization(new Node()));
	}

	@Test
	public void test() throws InterruptedException {
		endpoint.addListener(new Path("/test/test"), "child_added");

		endpoint.distribute(RoadrunnerEventBuilder.start()
				.type(RoadrunnerEventType.PUSH)
				.path(BASE_PATH + "/test/test/asdasd")
				.withNode()
				.put("name", "Hans")
				.complete()
				.build());

		Mockito.verify(sender)
				.send("{\"type\":\"child_added\",\"name\":\"asdasd\",\"path\":\"http://localhot:8080/test/test\",\"parent\":\"http://localhot:8080/test\",\"payload\":{\"name\":\"Hans\"},\"hasChildren\":false,\"numChildren\":0,\"priority\":0}\"");
	}

	private RoadrunnerEvent createRoadrunnerEvent(String content) {
		return new RoadrunnerEvent(content);
	}

}
