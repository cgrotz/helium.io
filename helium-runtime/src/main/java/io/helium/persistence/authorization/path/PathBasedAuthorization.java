package io.helium.persistence.authorization.path;

import io.helium.common.Path;
import io.helium.json.Node;
import io.helium.persistence.DataSnapshot;
import io.helium.persistence.Persistence;
import io.helium.persistence.authorization.Authorization;
import io.helium.persistence.authorization.NotAuthorizedException;
import io.helium.persistence.authorization.Operation;
import io.helium.persistence.inmemory.InMemoryDataSnapshot;

import java.util.Optional;

/**
 * Created by Christoph Grotz on 29.05.14.
 */
public class PathBasedAuthorization implements Authorization {

    private final Persistence persistence;

    public PathBasedAuthorization( Persistence persistence ) {
        this.persistence = persistence;
    }

    @Override
    public void authorize(Operation op, Optional<Node> auth, DataSnapshot root, Path path, Object object) throws NotAuthorizedException {
        if(!isAuthorized(op,auth, root, path, object)){
            throw new NotAuthorizedException(op, path);
        }
    }

    @Override
    public boolean isAuthorized(Operation op, Optional<Node> auth, DataSnapshot root, Path path, Object object) {
        Node authentication = auth.orElseGet(() -> Authorization.ANONYMOUS);
        if(path.getFirstElement().equalsIgnoreCase("users")) {
            return authentication.getBoolean("isAdmin", false);
        }
        if(path.getFirstElement().equalsIgnoreCase("rules")) {
            return authentication.getBoolean("isAdmin", false);
        }
        if(authentication.has("permissions")) {
            authentication.getNode("permissions").keys().forEach(key -> {
                Path permission = Path.of(key);
            });
        }
        return true;
    }

    @Override
    public Object filterContent(Optional<Node> auth, Path path, Node root, Object content) {
        if (content instanceof Node) {
            Node org = (Node) content;
            Node node = new Node();
            for (String key : org.keys()) {
                if (isAuthorized(Operation.READ, auth, new InMemoryDataSnapshot(root),
                        path.append(key), new InMemoryDataSnapshot(org.get(key)))) {
                    node.put(key, filterContent(auth, path.append(key), root, org.get(key)));
                }
            }
            return node;
        } else {
            return content;
        }
    }
}
