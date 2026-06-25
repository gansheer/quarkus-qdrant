package io.quarkiverse.qdrant.runtime.devui;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import io.quarkiverse.qdrant.runtime.QdrantClient;
import io.quarkiverse.qdrant.runtime.QdrantClientName;
import io.quarkiverse.qdrant.runtime.model.CollectionInfo;
import io.quarkiverse.qdrant.runtime.model.ScoredPoint;

public class QdrantDevUIService {

    @Any
    @Inject
    Instance<QdrantClient> clients;

    public List<String> listClients() {
        List<String> names = new ArrayList<>();
        for (Instance.Handle<QdrantClient> handle : clients.handles()) {
            names.add(clientNameOf(handle));
        }
        return names;
    }

    public List<Map<String, Object>> listCollections(String clientName) {
        QdrantClient qdrant = resolve(clientName);
        List<String> names = qdrant.listCollections();
        List<Map<String, Object>> result = new ArrayList<>();
        for (String name : names) {
            try {
                CollectionInfo info = qdrant.getCollectionInfo(name);
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("name", name);
                entry.put("status", info.getStatus());
                entry.put("pointsCount", info.getPointsCount());
                entry.put("vectorSize", info.getVectorSize());
                entry.put("distance", info.getDistance());
                entry.put("indexedVectorsCount", info.getIndexedVectorsCount());
                result.add(entry);
            } catch (Exception e) {
                result.add(Map.of("name", name, "status", "error"));
            }
        }
        return result;
    }

    public Map<String, Object> createCollection(String clientName, String name, Integer vectorSize, String distance) {
        if (name == null || name.isBlank()) {
            return Map.of("error", "Collection name is required");
        }
        if (vectorSize == null || vectorSize < 1) {
            return Map.of("error", "Vector size must be a positive integer");
        }
        try {
            resolve(clientName).createCollection(name).vectorSize(vectorSize).distance(distance).execute();
            return Map.of("message", "Collection \"" + name + "\" created");
        } catch (Exception e) {
            return Map.of("error", "Failed to create collection \"" + name + "\": " + e.getMessage());
        }
    }

    public Map<String, Object> deleteCollection(String clientName, String name) {
        try {
            resolve(clientName).deleteCollection(name);
            return Map.of("message", "Collection \"" + name + "\" deleted");
        } catch (Exception e) {
            return Map.of("error", "Failed to delete collection \"" + name + "\": " + e.getMessage());
        }
    }

    public Map<String, Object> search(String clientName, String collection, String vectorCsv, int limit) {
        if (collection == null || collection.isBlank() || vectorCsv == null || vectorCsv.isBlank()) {
            return Map.of("data", List.of(), "error", "Collection and vector are required");
        }

        float[] vec;
        try {
            String[] parts = vectorCsv.split(",");
            vec = new float[parts.length];
            for (int i = 0; i < parts.length; i++) {
                vec[i] = Float.parseFloat(parts[i].trim());
            }
        } catch (NumberFormatException e) {
            return Map.of("data", List.of(),
                    "error", "Invalid vector format. Enter comma-separated floats, e.g. 0.1, 0.2, 0.3, 0.4");
        }

        try {
            List<ScoredPoint> results = resolve(clientName).search(collection)
                    .vector(vec)
                    .limit(limit > 0 ? limit : 10)
                    .withPayload(true)
                    .execute();

            List<Map<String, Object>> data = new ArrayList<>();
            if (results != null) {
                for (ScoredPoint point : results) {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("id", point.getId());
                    entry.put("score", point.getScore());
                    entry.put("payload", point.getPayload());
                    data.add(entry);
                }
            }
            return Map.of("data", data);
        } catch (Exception e) {
            return Map.of("data", List.of(), "error", "Search failed: " + e.getMessage());
        }
    }

    private QdrantClient resolve(String name) {
        if (name == null || name.equals("default")) {
            return clients.select(Default.Literal.INSTANCE).get();
        }
        return clients.select(QdrantClientName.Literal.of(name)).get();
    }

    private static String clientNameOf(Instance.Handle<QdrantClient> handle) {
        for (Annotation qualifier : handle.getBean().getQualifiers()) {
            if (qualifier instanceof QdrantClientName qcn) {
                return qcn.value();
            }
        }
        return "default";
    }
}
