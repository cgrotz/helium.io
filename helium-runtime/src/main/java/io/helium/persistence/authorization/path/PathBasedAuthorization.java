package io.helium.persistence.authorization.path;

import io.helium.common.Path;
import io.helium.json.Node;
import io.helium.persistence.DataSnapshot;
import io.helium.persistence.Persistence;
import io.helium.persistence.authorization.Authorization;
import io.helium.persistence.authorization.NotAuthorizedException;
import io.helium.persistence.authorization.Operation;

/**
 * Created by Christoph Grotz on 29.05.14.
 */
public class PathBasedAuthorization implements Authorization {

    private final Persistence persistence;

    public PathBasedAuthorization( Persistence persistence ) {
        this.persistence = persistence;
    }

    @Override
    public void authorize(Operation op, Node auth, DataSnapshot root, Path path, Object object) throws NotAuthorizedException {
        if(!isAuthorized(op,auth, root, path, object)){
            throw new NotAuthorizedException(op, path);
        }
    }

    @Override
    public boolean isAuthorized(Operation op, Node auth, DataSnapshot root, Path path, Object object) {
        auth.getNode("permissions").keys().forEach(key -> {
            Path permission = Path.of(key);

        });
        return true;
    }
}
