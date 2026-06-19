package io.quarkiverse.qdrant.runtime;

public class QdrantApiException extends QdrantException {

    private final int statusCode;
    private final String errorDetails;

    public QdrantApiException(String message, int statusCode, String errorDetails) {
        super(message);
        this.statusCode = statusCode;
        this.errorDetails = errorDetails;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getErrorDetails() {
        return errorDetails;
    }
}
