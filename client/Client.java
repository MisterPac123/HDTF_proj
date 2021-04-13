package client;

import java.util.Scanner;

import java.net.*;
import java.io.*;
import java.util.StringJoiner;

public class Client {

    private static String userID;
    private static String hostname;

    static final int mapSize = 100;


    private static class Client_connection{
        private final BufferedReader receiver;
        private final PrintWriter sender ;
        private final Socket socket;

        public Client_connection(PrintWriter sender, BufferedReader receiver, Socket socket) {
            this.receiver = receiver;
            this. sender = sender;
            this.socket =socket;
        }
    }


    public static void main(String[] args) {

        //Check arguments
        int port;
        int epoch;
        if (args.length == 3 && args[0] instanceof String && args[1] instanceof String) {
            userID = args[0];
            hostname = args[1];
            try {
                port = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid arguments:(string) id (string) host (int) port");
                return;
            }
        }
        else {
            System.out.println("Invalid arguments:(string) id (string) host (int) port");
            return;
        }

        //Start listener socket
        new ClientListener(userID, port).start();

        //read input command
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String text;
            System.out.println("\nWelcome " + userID + "\n\n");
            do {
                System.out.println("Choose option:\n     1 - Request Location Proof\n     2 - add Location");
                text = reader.readLine();
                String[] command = text.split(" ");
                switch (command[0]) {
                    case "1" -> {
                        epoch = Integer.parseInt(command[1]);
                        requestLocationProof(epoch);
                    }
                    case "2" -> {
                        epoch = Integer.parseInt(command[1]);
                        int axisX = Integer.parseInt(command[2]);
                        int axisY = Integer.parseInt(command[3]);
                        addLocation(epoch, axisX, axisY);
                    }
                }
            } while (!text.equals("3"));

        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Bye");
        System.exit(0);
    }


    //####################
    //## Main functions ##
    //####################

    private static void requestLocationProof(int epoch) throws IOException {
        int[] ports = getClientConnections(epoch);

        for(int p : ports){
            if(p != 0) {
                try {
                    Client_connection client = connectToClient(p);
                    client.sender.println("requestLocationProof");

                    String responseLocationProof = client.receiver.readLine();
                    if (responseLocationProof.equals("Proof")) {
                        System.out.println("user port " + p + " sent a proof");
                    }
                    client.sender.println("bye");
                    client.socket.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void addLocation(int epoch, int axisX, int axisY) throws IOException {
        Map map= new Map();
        map.addPosition(userID, epoch, axisX, axisY);
    }


    //#########################
    //## Secondary functions ##
    //#########################
    private static Client_connection connectToClient(int client_port) throws IOException {
        Client_connection client;
        Socket socket = new Socket(hostname, client_port);

        InputStream input = socket.getInputStream();
        OutputStream output = socket.getOutputStream();
        BufferedReader receiver = new BufferedReader(new InputStreamReader(input));
        PrintWriter sender = new PrintWriter(output, true);

        client = new Client_connection(sender, receiver, socket);

        return client;
    }

    private static int[] getClientConnections(int epoch){
        Map map= new Map();
        return map.getNearbyUsersPorts(userID, epoch);
    }
}