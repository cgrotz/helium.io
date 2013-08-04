package de.skiptag.roadrunner.spring;

import java.util.ArrayList;

import org.json.Node;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.common.collect.Lists;

import de.skiptag.roadrunner.server.RoadrunnerMessageInbound;
import de.skiptag.roadrunner.server.RoadrunnerWebSocketServlet;

public class RoadrunnerSpringServlet extends RoadrunnerWebSocketServlet {

    private static final long serialVersionUID = 1L;
    private Node auth;

    @Override
    public RoadrunnerMessageInbound createInbound(String servername) {

	auth.put("id", SecurityContextHolder.getContext()
		.getAuthentication()
		.getPrincipal());
	ArrayList<String> roles = Lists.newArrayList();
	for (GrantedAuthority autho : SecurityContextHolder.getContext()
		.getAuthentication()
		.getAuthorities()) {
	    roles.add(autho.getAuthority());
	}
	auth.put("roles", roles.toArray());
	return new RoadrunnerMessageInbound(auth, servername, roadrunner);
    }

}
