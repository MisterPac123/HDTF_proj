package server;

import java.io.*;
import java.net.*;


public class Server {

    public static void main(String[] args) {

        if (args.length < 1) {

            System.err.println("Argument(s) missing!");
            System.err.printf("Usage: java %s serverPort", Server.class.getName());
            return;
        }

        
        final int serverPort = Integer.parseInt(args[0]);

        // ----------------------------------------------------------------- //
        // Considering Server is running now on localhost 8000
        // Change Later
        // ----------------------------------------------------------------- //

        //----------------------------------------------------- //
        //   [Start New Thread Communication With Server ]      //
        //----------------------------------------------------- //
        
        new SecureServer(serverPort).start();
        //SecureServer secureServer = new SecureServer(serverPort);
        //secureServer.run();

        /* 
        try (ServerSocket serverSocket = new ServerSocket(port)) {

            System.out.println("Server is listening on port " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected");

                new ServerThrd(socket).start();
            }

        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        */
    }
}