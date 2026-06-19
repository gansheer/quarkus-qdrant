package io.quarkiverse.qdrant.runtime;

public class QdrantException extends RuntimeException {

    public QdrantException(String message) {
        super(message);
    }

    public QdrantException(String message, Throwable cause) {
        super(message, cause);
    }
}
