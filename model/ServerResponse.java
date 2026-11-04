package model;

import java.io.Serializable;

/* A message the VC Controller server sends back for one submitted request. */
public final class ServerResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String requestId;
    private final RequestStatus status;
    private final String message;

    public ServerResponse(String requestId, RequestStatus status, String message) {
        if (requestId == null || requestId.trim().isEmpty()) {
            throw new IllegalArgumentException("Request ID is required.");
        }
        if (status == null) {
            throw new IllegalArgumentException("Request status is required.");
        }
        this.requestId = requestId.trim();
        this.status = status;
        this.message = message == null ? "" : message.trim();
    }

    /* The server received the request and placed it in the review queue. */
    public static ServerResponse received(String requestId) {
        return new ServerResponse(requestId, RequestStatus.PENDING,
                "Request received and waiting for controller review.");
    }

    public String getRequestId() {
        return requestId;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public boolean isFinal() {
        return status.isFinal();
    }
}
