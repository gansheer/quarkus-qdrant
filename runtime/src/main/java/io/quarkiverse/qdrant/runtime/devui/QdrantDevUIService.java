package io.quarkiverse.qdrant.runtime.devui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import io.quarkiverse.qdrant.runtime.QdrantClient;
import io.quarkiverse.qdrant.runtime.model.CollectionInfo;
import io.quarkiverse.qdrant.runtime.model.ScoredPoint;

public class QdrantDevUIService {

    @Inject
    QdrantClient qdrant;

    public List<Map<String, Object>> listCollections() {
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

    public Map<String, Object> createCollection(String name, Integer vectorSize, String distance) {
        if (name == null || name.isBlank()) {
            return Map.of("error", "Collection name is required");
        }
        if (vectorSize == null || vectorSize < 1) {
            return Map.of("error", "Vector size must be a positive integer");
        }
        try {
            qdrant.createCollection(name).vectorSize(vectorSize).distance(distance).execute();
            return Map.of("message", "Collection \"" + name + "\" created");
        } catch (Exception e) {
            return Map.of("error", "Failed to create collection \"" + name + "\": " + e.getMessage());
        }
    }

    public Map<String, Object> deleteCollection(String name) {
        try {
            qdrant.deleteCollection(name);
            return Map.of("message", "Collection \"" + name + "\" deleted");
        } catch (Exception e) {
            return Map.of("error", "Failed to delete collection \"" + name + "\": " + e.getMessage());
        }
    }

    public Map<String, Object> search(String collection, String vectorCsv, int limit) {
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
            List<ScoredPoint> results = qdrant.search(collection)
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
            return Map.of("data", List.of(),
                    "error", "Search failed: vector dimension may not match the collection's vector size");
        }
    }
}
