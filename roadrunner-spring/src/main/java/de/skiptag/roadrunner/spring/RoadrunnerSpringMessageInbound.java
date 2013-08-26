package de.skiptag.roadrunner.spring;

import java.io.IOException;
import java.nio.CharBuffer;

import org.json.Node;
import org.springframework.security.core.context.SecurityContextHolder;

import de.skiptag.roadrunner.Roadrunner;
import de.skiptag.roadrunner.server.RoadrunnerMessageInbound;

public class RoadrunnerSpringMessageInbound extends RoadrunnerMessageInbound {

	public RoadrunnerSpringMessageInbound(Node auth, String basePath, Roadrunner roadrunner) {
		super(auth, basePath, roadrunner);
	}

	@Override
	protected void onTextMessage(CharBuffer message) throws IOException {

		setAuth(RoadrunnerSpringServlet.convertAuthenticationToAuthNode(SecurityContextHolder
				.getContext().getAuthentication()));
		super.onTextMessage(message);
	}

}
