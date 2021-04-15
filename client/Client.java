package client;

import java.util.Scanner;

import java.net.*;
import java.io.*;
import java.util.StringJoiner;

public class Client {

    private static String userID;
    private static String hostname;
    private static String type;

    private static class Client_connection{
        private final BufferedReader receiver;
        private final PrintWriter sender ;
        private final Socket socket;

        public Client_connection(PrintWriter sender, BufferedReader receiver, Socket socket) {
            this.receiver = receiver;
            this.sender = sender;
            this.socket =socket;
        }
    }


    public static void main(String[] args) {

        //Check arguments
        int port;
        int epoch;
        if (args.length == 3 && args[0] != null && args[1] != null) {
            userID = args[0];
            hostname = args[1];
            type = "regular";
            try {
                port = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid arguments:(string) id (string) host (int) port");
                return;
            }
        }
        else if(args.length == 4 && args[4].equals("super")){
            userID = "SU_"+args[0];
            hostname = args[1];
            type = "super";
            try {
                port = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid arguments:(string) id (string) host (int) port super");
                return;
            }
        }
        else {
            System.out.println("Invalid arguments:/n(string) id (string) host (int) port /n or /n (string) id (string) host (int) port super");
            return;
        }

        //Start listener socket
        new ClientListener(userID, port, type).start();

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
                        if(command.length!=2){
                            System.out.println("Wrong arguments. Expected one argument: 1 (int) epoch");
                            continue;
                        }
                        try {
                            epoch = Integer.parseInt(command[1]);
                            requestLocationProof(epoch);
                        }catch (NumberFormatException e) {
                            System.out.println("Wrong arguments. Expected one argument: 1 (int) epoch");
                            continue;
                        }
                        //submitLocationReport(userId, ep, report, …)
                        //Specification: user userId submits a location report.
                        break;
                    }
                    case "2" -> {
                        if(command.length!=4){
                            System.out.println("Wrong arguments. Expected three argument: 2 (int) epoch (int) axisX (int) axisY");
                        }
                        try {
                            epoch = Integer.parseInt(command[1]);
                            int axisX = Integer.parseInt(command[2]);
                            int axisY = Integer.parseInt(command[3]);
                            addLocation(epoch, axisX, axisY);
                        }catch (NumberFormatException e) {
                            System.out.println("Wrong arguments. Expected three argument: 2 (int) epoch (int) axisX (int) axisY");
                            continue;
                        }
                        break;
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