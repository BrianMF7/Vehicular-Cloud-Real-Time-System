package controller;

import model.RequestStatus;
import model.ServerResponse;
import model.SubmissionRequest;

import java.io.ObjectInputFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import java.util.function.Consumer;

public class ControllerClient {

    private static final long CONNECTION_WAIT_MILLIS = 10 * 60 * 1000;
    private static final int CONNECTION_TIMEOUT_MILLIS = 1000;
    private static final long RETRY_DELAY_MILLIS = 1000;

    private final String host;
    private final int port;

    public ControllerClient() {
        this("127.0.0.1", ControllerServer.DEFAULT_PORT);
    }

    public ControllerClient(String host, int port) {
        if (host == null || host.trim().isEmpty()) {
            throw new IllegalArgumentException("Server host is required.");
        }

        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Invalid server port.");
        }

        this.host = host.trim();
        this.port = port;
    }

    /**
     * Sends a submission and waits for the final controller decision.
     * The acknowledgement listener is called when the PENDING response arrives.
     */
    public ServerResponse submit(
            SubmissionRequest request,
            Consumer<ServerResponse> acknowledgementListener
    ) throws IOException {

        if (request == null) {
            throw new IllegalArgumentException("Submission request is required.");
        }

        try (Socket socket = connectToController()) {

            // Create the output stream first, matching the server.
            ObjectOutputStream out =
                    new ObjectOutputStream(socket.getOutputStream());
            out.flush();

            ObjectInputStream in =
                    new ObjectInputStream(socket.getInputStream());

            // Only allow the expected server response classes.
            in.setObjectInputFilter(ControllerClient::filterResponse);

            // Send the submission to the controller server.
            out.writeObject(request);
            out.flush();

            // Receive the immediate acknowledgement.
            ServerResponse acknowledgement = readResponse(in);
            validateResponse(request, acknowledgement);

            if (acknowledgement.getStatus() != RequestStatus.PENDING) {
                throw new IOException(
                        "Expected a PENDING acknowledgement but received "
                                + acknowledgement.getStatus()
                );
            }

            // Notify the GUI that the controller received the request.
            if (acknowledgementListener != null) {
                acknowledgementListener.accept(acknowledgement);
            }

            // Wait for the controller to accept or reject the submission.
            ServerResponse decision = readResponse(in);
            validateResponse(request, decision);

            if (!decision.isFinal()) {
                throw new IOException(
                        "The controller did not return a final decision."
                );
            }

            return decision;

        } catch (ClassNotFoundException ex) {
            throw new IOException(
                    "Could not read the server response.",
                    ex
            );
        }
    }

    /**
     * Keeps trying to connect while waiting for the controller to sign in.
     */
    private Socket connectToController() throws IOException {

        long deadline = System.nanoTime()
                + CONNECTION_WAIT_MILLIS * 1_000_000L;

        IOException lastFailure = null;

        while (System.nanoTime() < deadline) {

            Socket socket = new Socket();

            try {
                socket.connect(
                        new InetSocketAddress(host, port),
                        CONNECTION_TIMEOUT_MILLIS
                );

                // The controller server is now available.
                return socket;

            } catch (IOException ex) {

                lastFailure = ex;

                try {
                    socket.close();
                } catch (IOException ignored) {
                    // Continue trying to connect.
                }

                try {
                    Thread.sleep(RETRY_DELAY_MILLIS);

                } catch (InterruptedException interrupted) {

                    Thread.currentThread().interrupt();

                    throw new IOException(
                            "Waiting for the controller was interrupted.",
                            interrupted
                    );
                }
            }
        }

        throw new IOException(
                "The controller has not signed in yet. "
                        + "Keep the application open and try again "
                        + "after the controller starts.",
                lastFailure
        );
    }

    private ServerResponse readResponse(ObjectInputStream in)
            throws IOException, ClassNotFoundException {

        Object response = in.readObject();

        if (!(response instanceof ServerResponse)) {
            throw new IOException(
                    "Received an unexpected response from the server."
            );
        }

        return (ServerResponse) response;
    }

    private void validateResponse(
            SubmissionRequest request,
            ServerResponse response
    ) throws IOException {

        if (!request.getRequestId().equals(response.getRequestId())) {
            throw new IOException(
                    "The server response does not match this submission."
            );
        }
    }

    private static ObjectInputFilter.Status filterResponse(
            ObjectInputFilter.FilterInfo info
    ) {

        // Limit the size and complexity of incoming responses.
        if (info.depth() > 3
                || info.references() > 16
                || info.streamBytes() > 65536
                || info.arrayLength() > 0) {

            return ObjectInputFilter.Status.REJECTED;
        }

        Class<?> type = info.serialClass();

        if (type == null) {
            return ObjectInputFilter.Status.UNDECIDED;
        }

        if (type == ServerResponse.class
                || type == RequestStatus.class
                || type == Enum.class
                || type == String.class) {

            return ObjectInputFilter.Status.ALLOWED;
        }

        return ObjectInputFilter.Status.REJECTED;
    }
}
