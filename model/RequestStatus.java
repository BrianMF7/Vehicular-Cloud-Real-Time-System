package model;

/* The current result of a request handled by the VC Controller server. */
public enum RequestStatus {
    // The server received the request and has not made a decision yet.
    PENDING,
    ACCEPTED,
    REJECTED,
    ERROR;

    /* Returns true after the server has finished handling the request. */
    public boolean isFinal() {
        return this == ACCEPTED || this == REJECTED || this == ERROR;
    }
}
