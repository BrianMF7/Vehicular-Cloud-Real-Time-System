package network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.function.Consumer;

import controller.ControllerServer;
import model.RequestStatus;
import model.ServerResponse;
import model.SubmissionRequest;

/** Sends one submission to the controller and receives its acknowledgement and decision. */
public final class ClientNetworking {
    private final String host;
    private final int port;

    public ClientNetworking(String host, int port) {
        if (host == null || host.trim().isEmpty() || port < 1 || port > 65535) {
            throw new IllegalArgumentException("A server host and valid port are required.");
        }
        this.host = host.trim();
        this.port = port;
    }

    public static ClientNetworking local() {
        return new ClientNetworking("127.0.0.1", ControllerServer.DEFAULT_PORT);
    }

    /** Call this from a background thread because the final decision may take time. */
    public ServerResponse submit(SubmissionRequest request, Consumer<ServerResponse> onResponse)
            throws IOException {
        if (request == null || onResponse == null) {
            throw new IllegalArgumentException("A request and response handler are required.");
        }
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 3000);
            try (ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream())) {
                output.flush();
                ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
                output.writeObject(request);
                output.flush();

                ServerResponse first = readResponse(input, request.getRequestId());
                if (first.isFinal()) {
                    onResponse.accept(first);
                    return first;
                }
                if (first.getStatus() != RequestStatus.PENDING) {
                    throw new IOException("Unexpected server acknowledgement.");
                }
                onResponse.accept(first);

                ServerResponse decision = readResponse(input, request.getRequestId());
                if (!decision.isFinal()) {
                    throw new IOException("The server did not send a final decision.");
                }
                onResponse.accept(decision);
                return decision;
            } catch (ClassNotFoundException ex) {
                throw new IOException("The server sent an unknown response type.", ex);
            }
        }
    }

    private static ServerResponse readResponse(ObjectInputStream input, String requestId)
            throws IOException, ClassNotFoundException {
        Object raw = input.readObject();
        if (!(raw instanceof ServerResponse)) {
            throw new IOException("The server sent an invalid response.");
        }
        ServerResponse response = (ServerResponse) raw;
        if (!requestId.equals(response.getRequestId())) {
            throw new IOException("The server response belongs to another request.");
        }
        return response;
    }
}
