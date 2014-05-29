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

    }

    @Override
    public boolean isAuthorized(Operation op, Node auth, DataSnapshot root, Path path, Object object) {
        return false;
    }
}
