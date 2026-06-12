package io.quarkiverse.qdrant.runtime.devui;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import io.quarkiverse.qdrant.runtime.QdrantClient;
import io.smallrye.common.annotation.NonBlocking;

public class QdrantDevUIService {

    @Any
    @Inject
    Instance<QdrantClient> clients;

    @NonBlocking
    public String getDashboardLink() {
        for (Instance.Handle<QdrantClient> handle : clients.handles()) {
            return handle.get().getBaseUri().toString() + "/dashboard";
        }
        return "";
    }
}
