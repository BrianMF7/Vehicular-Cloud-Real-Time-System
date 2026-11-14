
package controller;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import model.*;


// Listens for SubmissionRequest messages from job and vehicle owners.
// Sends an acknowledgement, then waits for Accept/Reject before storing anything.
public class ControllerServer {

    // Avoid port 5000; macOS AirPlay already uses it.
    public static final int DEFAULT_PORT = 5050;

    private final int port;
    private final PendingRequestRegistry registry;
    private final VehicularCloudController controller;
    private ServerSocket serverSocket;
    // False after stop() so the accept loop can exit.
    private volatile boolean running;
    // Port 0 lets tests pick a free port.
    public ControllerServer(
            int port,
            PendingRequestRegistry registry,
            VehicularCloudController controller) {

        if (port < 0) {
            throw new IllegalArgumentException("Port must be 0 or greater.");
        }

        if (registry == null || controller == null) {
            throw new IllegalArgumentException("Registry and controller are required.");
        }

        this.port = port;
        this.registry = registry;
        this.controller = controller;
    }

    // The review screen reads waiting requests from this registry.
    public PendingRequestRegistry getRegistry() {
        return registry;
    }

    // Open the listening socket and start a background thread.
    public synchronized void start() throws IOException {
        if (running) {
            return;
        }
        serverSocket = new ServerSocket(
                port,
                50,
                InetAddress.getByName("127.0.0.1")
        );
        running = true;
        Thread acceptor = new Thread(this::acceptLoop, "vc-accept");
        acceptor.setDaemon(true);
        acceptor.start();
    }

    // The real port after start().
    public int getPort() {
        if (serverSocket == null) {
            throw new IllegalStateException("Server has not started.");
        }
        return serverSocket.getLocalPort();
    }
    // Close the listening socket so accept() unblocks.
    public void stop() throws IOException {
        running = false;
        ServerSocket listening = serverSocket;
        serverSocket = null;
        if (listening != null && !listening.isClosed()) {
            listening.close();
        }
    }
    // Accept stores the data; Reject leaves the file unchanged.
    // The waiting client is notified either way.
    public ServerResponse decide(String requestId, boolean accept) {
        String id = requestId == null ? "" : requestId.trim();
        if (id.isEmpty()) {
            return error("unknown", "Request is not waiting for review.");
        }
        // Take it out of the queue first so the screen cannot decide twice.
        PendingRequestRegistry.PendingRequest pending = registry.take(id);

        if (pending == null) {
            return error(id, "Request is not waiting for review.");
        }
        // Let the controller write the log.
        ServerResponse response = controller.decideRequest(pending.getRequest(), accept);
        // Wake the client worker so it can send the final response.
        pending.complete(response);
        return response;
    }

    // One thread accepts connections; each client gets its own worker thread.
    private void acceptLoop() {
        while (running) {
            try {
                ServerSocket listening = serverSocket;
                if (listening == null || listening.isClosed()) {
                    break;
                }
                Socket socket = listening.accept();
                Thread worker = new Thread(
                        () -> handleClient(socket),
                        "vc-client"
                );
                worker.setDaemon(true);
                worker.start();
            } catch (IOException ex) {
                // stop() closes the socket on purpose.
                if (running) {
                    System.err.println(
                            "Could not accept a client: " + ex.getMessage()
                    );
                }
            }
        }
    }

    // Handles one client submission.
    private void handleClient(Socket socket) {
        String requestId = null;
        PendingRequestRegistry.PendingRequest pending = null;
        try (Socket client = socket;
             ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream())) {
            out.flush();
            ObjectInputStream in = new ObjectInputStream(client.getInputStream());
            in.setObjectInputFilter(ControllerServer::filterSubmission);
            Object raw = in.readObject();
            if (!(raw instanceof SubmissionRequest)) {
                out.writeObject(
                        error("unknown", "Expected a submission request.")
                );
                out.flush();
                return;
            }
            SubmissionRequest request = (SubmissionRequest) raw;
            requestId = request.getRequestId();
            // Add the request to the registry.
            pending = registry.add(request);
            // Acknowledge receipt.
            out.writeObject(ServerResponse.received(requestId));
            out.flush();
            // Watch for the client disconnecting while the request is pending.
            startDisconnectWatcher(in, requestId, pending);
            // Wait for the controller's decision or client cancellation.
            ServerResponse decision = pending.awaitDecision();
            // A null decision means the client disconnected.
            if (decision == null) {
                return;
            }
            // Send the final response.
            out.writeObject(decision);
            out.flush();

        } catch (InterruptedException ex) {

            Thread.currentThread().interrupt();

        } catch (Exception ex) {

            System.err.println("Client handler stopped:");
            ex.printStackTrace();

        } finally {

            // Remove the request if it is still waiting in the registry.
            if (pending != null) {
                boolean removed = registry.cancel(requestId, pending);

                if (removed) {
                    System.out.println(
                            "Removed abandoned job request: " + requestId
                    );
                }
            }
        }
    }

    // Watches for the client closing its connection before a decision.
    private void startDisconnectWatcher(
            ObjectInputStream in,
            String requestId,
            PendingRequestRegistry.PendingRequest pending) {

        Thread watcher = new Thread(() -> {

            try {
                // The client should not send another object after its request.
                // This blocks until the client sends something or disconnects.
                in.readObject();
                System.err.println("Unexpected message from job client: " + requestId);
                registry.cancel(requestId, pending);
            } catch (EOFException ex) {
                // The client closed its connection.
                if (registry.cancel(requestId, pending)) {
                    System.out.println("Cancelled disconnected job request: " + requestId);
                }
            } catch (Exception ex) {
                // A connection or protocol failure occurred.
                if (registry.cancel(requestId, pending)) {
                    System.err.println("Cancelled job request after client connection failure: " + requestId);
                }
            }

        }, "job-client-disconnect-" + requestId);

        watcher.setDaemon(true);
        watcher.start();
    }

    // Shared error response.
    private static ServerResponse error(String requestId, String message) {
        return new ServerResponse(
                requestId,
                RequestStatus.ERROR,
                message
        );
    }

    // Strict deserialization filter for incoming submissions.
    private static ObjectInputFilter.Status filterSubmission(ObjectInputFilter.FilterInfo info) {
        Class<?> type = info.serialClass();
        System.err.println("Filter check: class=" +
                        (type == null ? "null" : type.getName()) +
                        ", depth=" + info.depth() +
                        ", references=" + info.references() +
                        ", bytes=" + info.streamBytes() +
                        ", arrayLength=" + info.arrayLength()
        );
        if (info.depth() > 5 ||
                info.references() > 32 ||
                info.streamBytes() > 65536 ||
                info.arrayLength() > 0) {
            System.err.println("FILTER REJECTED: resource limit");
            return ObjectInputFilter.Status.REJECTED;
        }
        if (type == null) {
            return ObjectInputFilter.Status.UNDECIDED;
        }
        if (type == SubmissionRequest.class ||
                type == RequestType.class ||
                type == Enum.class ||
                type == Job.class ||
                type == Vehicle.class ||
                type == String.class) {
            return ObjectInputFilter.Status.ALLOWED;
        }
        System.err.println("FILTER REJECTED CLASS: " + type.getName());
        return ObjectInputFilter.Status.REJECTED;
    }
}
