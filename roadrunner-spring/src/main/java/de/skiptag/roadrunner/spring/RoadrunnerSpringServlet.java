package de.skiptag.roadrunner.spring;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import de.skiptag.roadrunner.Roadrunner;
import de.skiptag.roadrunner.json.Node;
import de.skiptag.roadrunner.server.RoadrunnerMessageInbound;
import de.skiptag.roadrunner.server.RoadrunnerWebSocketServlet;

public class RoadrunnerSpringServlet extends RoadrunnerWebSocketServlet {

	private static final long serialVersionUID = 1L;

	@Override
	public RoadrunnerMessageInbound createInbound(String servername) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		Node auth = convertAuthenticationToAuthNode(authentication);
		return new RoadrunnerSpringMessageInbound(auth, servername, roadrunner);
	}

	@Override
	protected Roadrunner createRoadrunnerInstance(String basePath, Node rule, File directory,
			Optional<File> snapshotDirectory) throws IOException {
		return super.createRoadrunnerInstance(basePath, rule, directory, snapshotDirectory);
	}

	public static Node convertAuthenticationToAuthNode(Authentication authentication) {
		Node auth = new Node();
		if (authentication != null) {
			auth.put("id", authentication.getPrincipal());
			ArrayList<String> roles = Lists.newArrayList();
			for (GrantedAuthority autho : authentication.getAuthorities()) {
				roles.add(autho.getAuthority());
			}
			auth.put("roles", roles.toArray());
		}
		return auth;
	}

}
