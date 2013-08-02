package de.skiptag.roadrunner.authorization.rulebased;

import org.json.Node;

public class AuthenticationWrapper {

    public String id;

    public AuthenticationWrapper(Node auth) {
	if (auth != null && auth.has("id")) {
	    this.id = auth.getString("id");
	}
    }
}