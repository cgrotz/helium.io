package io.helium.server.distributor;

import com.google.common.collect.Sets;
import io.helium.server.Endpoint;

import java.util.Optional;
import java.util.Set;

/**
 * Created by Christoph Grotz on 01.06.14.
 */
public class Endpoints {
    private static Optional<Endpoints> instance = Optional.empty();

    private Set<Endpoint> endpoints = Sets.newHashSet();

    private Endpoints() {

    }

    public static Endpoints get() {
        if (!instance.isPresent()) {
            instance = Optional.of(new Endpoints());
        }
        return instance.get();
    }

    public void addEndpoint(Endpoint endpoint) {
        endpoints.add(endpoint);
    }

    public void removeEndpoint(Endpoint endpoint) {
        endpoints.remove(endpoint);
    }

    public Set<Endpoint> endpoints() {
        return endpoints;
    }
}
