package io.helium.persistence.authorization.chained;

import com.google.common.base.Strings;
import io.helium.common.Path;
import io.helium.json.HashMapBackedNode;
import io.helium.json.Node;
import io.helium.persistence.authorization.Authorization;
import io.helium.persistence.authorization.NotAuthorizedException;
import io.helium.persistence.authorization.Operation;
import io.helium.persistence.inmemory.InMemoryDataSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Created by Christoph Grotz on 29.05.14.
 */
public class ChainedAuthorization implements Authorization {
    private static final Logger LOG = LoggerFactory.getLogger(ChainedAuthorization.class);

    private final Authorization[] authorizations;

    public ChainedAuthorization(Authorization ... authorizations) {
        this.authorizations = authorizations;
    }

    @Override
    public void authorize(Operation op, Optional<Node> auth, Path path, Object object) throws NotAuthorizedException {
        if(! isAuthorized(op, auth, path, object)){
            throw new NotAuthorizedException(op, path);
        }
    }

    @Override
    public boolean isAuthorized(Operation op, Optional<Node> auth, Path path, Object object) {
        for( Authorization authorization : authorizations) {
            if(authorization.isAuthorized(op, auth, path, object)){
                LOG.info("Authorized access("+op+") to "+path+" for "+auth);
                return true;
            }
        }
        return false;
    }

    @Override
    public Object filterContent(Optional<Node> auth, Path path, Object content) {
        if (content instanceof Node) {
            Node org = (Node) content;
            Node node = new HashMapBackedNode();
            for (String key : org.keys()) {
                if(!Strings.isNullOrEmpty(key)) {
                    if (isAuthorized(Operation.READ, auth,
                            path.append(key), new InMemoryDataSnapshot(org.get(key)))) {
                        node.put(key, filterContent(auth, path.append(key), org.get(key)));
                    }
                }
            }
            return node;
        } else {
            if (isAuthorized(Operation.READ, auth,
                    path, new InMemoryDataSnapshot(content))) {
                return content;
            }
        }
        return null;
    }
}
