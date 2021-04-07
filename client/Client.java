package client;

import java.util.Scanner;

import java.net.*;
import java.io.*;


public class Client {

    private static String userID;
    private static String hostname;

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
                System.out.println("Choose option:\n     1 - Request Location Proof\n     2 - exit");
                text = reader.readLine();

                switch (text) {
                    case "1":
                        requestLocationProof();
                        break;
                    case "changeLocation":
                        //changeLocation(axisX, axisY);

                }
            } while (!text.equals("2"));

        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Bye");
        System.exit(0);
    }


    //#########################
    //## Secondary functions ##
    //#########################

    private static void requestLocationProof() throws IOException {
        int[] ports = getClientConnections();

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

    private static int[] getClientConnections() throws IOException {
        //versao teste!! tem que ir buscar so os users proximos
        int[] ports = new int[20];
        int i = 0;

        File usersPorts = new File("usersPorts.txt");
        Scanner myReader = new Scanner(usersPorts);
        while (myReader.hasNextLine()) {
            String[] data = myReader.nextLine().split(" ");
            if (!data[0].equals(userID)){
                int port = Integer.parseInt(data[1]);
                ports[i] = (port);
                i++;
            }
        }
        myReader.close();
        return ports;
    }
}