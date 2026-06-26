package io.quarkiverse.qdrant.runtime.devui;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import io.quarkiverse.qdrant.runtime.QdrantClient;

public class QdrantDevUIService {

    @Any
    @Inject
    Instance<QdrantClient> clients;

    public String getDashboardLink() {
        return clients.stream()
                .findFirst()
                .map(c -> c.getBaseUri().toString() + "/dashboard")
                .orElse("");
    }
}
