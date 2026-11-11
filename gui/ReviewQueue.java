package gui;

import java.io.IOException;

import controller.ControllerServer;
import controller.PendingRequestRegistry;
import controller.VehicularCloudController;
import model.RequestStatus;
import model.ServerResponse;

// The job form and the controller screen share this queue with the server.
public class ReviewQueue {

    private static final PendingRequestRegistry registry = new PendingRequestRegistry();
      private static ControllerServer server;

    private ReviewQueue() {
    }

    public static PendingRequestRegistry get() {
        return registry;
    }

    //Start the same server the Accept and Reject buttons use.
    public static void start() {
          if (server != null) {
            return;
             }
        try {
            server = new ControllerServer(ControllerServer.DEFAULT_PORT, registry,
                    VehicularCloudController.getInstance());
            server.start();
            System.out.println("Controller server is listening on port " + server.getPort());
        } catch (IOException ex) {
            server = null;
            System.err.println("Controller server did not start: " + ex.getMessage());
        }
          }

    //Accept or reject through the server so a waiting client is notified.
    public static ServerResponse decide(String requestId, boolean accept) {
        if (server == null) {
              String id = requestId == null || requestId.trim().isEmpty() ? "unknown" : requestId.trim();
            return new ServerResponse(id, RequestStatus.ERROR, "Server is not running.");
        }
        return server.decide(requestId, accept);
       }
      }
