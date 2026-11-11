package controller;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import model.RequestStatus;
import model.ServerResponse;
import model.SubmissionRequest;

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

    // port 0 lets tests pick a free port. The registry and controller must already exist.
    public ControllerServer(int port, PendingRequestRegistry registry,
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

    // Open the listening socket and start a background thread that accepts clients.
    public synchronized void start() throws IOException {
        if (running) {
            return;
        }
        serverSocket = new ServerSocket(port);
        running = true;
        Thread acceptor = new Thread(this::acceptLoop, "vc-accept");
        acceptor.setDaemon(true);
        acceptor.start();
    }

    // The real port after start(). Needed when the constructor used port 0.
    public int getPort() {
        if (serverSocket == null) {
            throw new IllegalStateException("Server has not started.");
        }
        return serverSocket.getLocalPort();
    }

    // Close the listening socket so accept() unblocks and the loop can finish.
    public void stop() throws IOException {
        running = false;
        ServerSocket listening = serverSocket;
        serverSocket = null;
        if (listening != null && !listening.isClosed()) {
            listening.close();
        }
    }

    // Accept stores the data; Reject leaves the file unchanged. The waiting client is notified either way.
    public ServerResponse decide(String requestId, boolean accept) {
        String id = requestId == null ? "" : requestId.trim();
        if (id.isEmpty()) {
            return error("unknown", "Request is not waiting for review.");
        }
        // Take it out of the queue first so the screen cannot decide on it twice.
        PendingRequestRegistry.PendingRequest pending = registry.take(id);
        if (pending == null) {
            return error(id, "Request is not waiting for review.");
        }
        // Let the controller write the log so job start/end times stay in one place.
        ServerResponse response = controller.decideRequest(pending.getRequest(), accept);
        // Wake the client worker so it can send this final response.
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
                Thread worker = new Thread(() -> handleClient(socket), "vc-client");
                worker.setDaemon(true);
                worker.start();
            } catch (IOException ex) {
                // stop() closes the socket on purpose, so only log unexpected failures.
                if (running) {
                    System.err.println("Could not accept a client: " + ex.getMessage());
                }
            }
        }
    }

    // Protocol: client writes SubmissionRequest, reads the pending ack, then one final response.
    // Both sides must create ObjectOutputStream before ObjectInputStream.
    private void handleClient(Socket socket) {
        try (Socket client = socket;
             ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream())) {
            // Flush the stream header before waiting for the client's header.
            out.flush();
            ObjectInputStream in = new ObjectInputStream(client.getInputStream());
            Object raw = in.readObject();
            if (!(raw instanceof SubmissionRequest)) {
                out.writeObject(error("unknown", "Expected a submission request."));
                out.flush();
                return;
            }
            SubmissionRequest request = (SubmissionRequest) raw;
            // Queue it, ack it, then block until Accept or Reject.
            PendingRequestRegistry.PendingRequest pending = registry.add(request);
            out.writeObject(ServerResponse.received(request.getRequestId()));
            out.flush();
            out.writeObject(pending.awaitDecision());
            out.flush();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (Exception ex) {
            System.err.println("Client handler stopped: " + ex.getMessage());
        }
    }

    // Shared error response used when the request cannot be reviewed or stored.
    private static ServerResponse error(String requestId, String message) {
        return new ServerResponse(requestId, RequestStatus.ERROR, message);
    }
}
