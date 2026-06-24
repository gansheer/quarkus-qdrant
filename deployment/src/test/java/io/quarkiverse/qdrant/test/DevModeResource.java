package io.quarkiverse.qdrant.test;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkiverse.qdrant.runtime.QdrantClient;

@Path("/devmode")
public class DevModeResource {

    @Inject
    QdrantClient client;

    @GET
    @Path("/collections")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> listCollections() {
        return client.listCollections();
    }

    @GET
    @Path("/greeting")
    @Produces(MediaType.TEXT_PLAIN)
    public String greeting() {
        return "hello";
    }
}
