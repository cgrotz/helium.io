/**
 * 
 */
package io.helium;

import org.junit.Before;
import org.mockito.Mockito;

import io.helium.authorization.Authorization;
import io.helium.authorization.rulebased.RuleBasedAuthorization;
import io.helium.event.HeliumEvent;
import io.helium.json.Node;
import io.helium.messaging.HeliumEndpoint;
import io.helium.messaging.HeliumOutboundSocket;
import io.helium.persistence.inmemory.InMemoryPersistence;

/**
 * @author balu
 * 
 */
public class HeliumSenderTest {

	private static final String				BASE_PATH	= "http://localhot:8080";
	private HeliumOutboundSocket	sender;
	private HeliumEndpoint				endpoint;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		this.sender = Mockito.mock(HeliumEndpoint.class);
		this.endpoint = new HeliumEndpoint(BASE_PATH, new Node(), sender, new InMemoryPersistence(
				new RuleBasedAuthorization(Authorization.ALL_ACCESS_RULE), new Helium(BASE_PATH)),
				new RuleBasedAuthorization(new Node()), null);
	}

	// @Test
	// public void test() throws InterruptedException {
	// endpoint.addListener(new Path("/test/test"), "child_added");
	//
	// endpoint.distribute(HeliumEventBuilder.start().type(HeliumEventType.PUSH)
	// .path(BASE_PATH + "/test/test/asdasd").withNode().put("name",
	// "Hans").complete()
	// .build());
	//
	// Mockito.verify(sender)
	// .send("{\"type\":\"child_added\",\"name\":\"asdasd\",\"path\":\"http://localhot:8080/test/test\",\"parent\":\"http://localhot:8080/test\",\"payload\":{\"name\":\"Hans\"},\"hasChildren\":false,\"numChildren\":0,\"priority\":0}\"");
	// }

	private HeliumEvent createHeliumEvent(String content) {
		return new HeliumEvent(content);
	}

}
