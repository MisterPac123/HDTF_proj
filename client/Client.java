package client;

import java.util.Scanner;

import java.io.*;
import java.util.*;
import java.security.*;
import java.security.spec.*;
import javax.crypto.*;
import javax.crypto.spec.*;

import java.net.*;


import com.google.gson.*;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.security.Key;

public class Client {


    private static String userID;
    private static String hostname;
    private static int clientPort;

    // ----------------------------------------------------------------- //
    // Each user will have a different path
    // Will be necessary for proof signature too.
    // ----------------------------------------------------------------- //
   
    private static PrivateKey clientPrivateKey;
    private static PublicKey clientPublicKey;
    private static PublicKey serverPublicKey;
    private static Key symmetricKey;

    // =============================================================================== //
    //                           [CLIENT CONNECTION CONSTRUCTOR]
    // =============================================================================== //
    private static class Client_connection{
        private final BufferedReader receiver;
        private final PrintWriter sender ;
        private final Socket socket;

        public Client_connection(PrintWriter sender, BufferedReader receiver, Socket socket) {
            this.receiver = receiver;
            this.sender = sender;
            this.socket =socket;
        }
    };


    // =============================================================================== //
    //                                   [MAIN]
    // =============================================================================== //

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

        if(port == 8000){
            System.out.println("Port 8000 is Reserved For Sever. Please run again with another one");
            return;
        }
        
        // ----------------------------------------------------------------- //
        //                       Client Listener Init
        // ----------------------------------------------------------------- //
        //Start listener socket
        new ClientListener(userID, port).start();

        // ----------------------------------------------------------------- //
        //                          Actions Menu
        // ----------------------------------------------------------------- //
        //read input command
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String text;
            System.out.println("\nWelcome " + userID + "\n\n");
            do {
                System.out.println("Choose option:\n     1 - Request Location Proof\n     2 - exit\n");
                text = reader.readLine();

                switch (text) {
                    case "1":
                        List<String> proofs = new ArrayList<String>();
                        proofs = requestLocationProof();
                        
                        if(proofs.size() > 0){
                            submitLocationReport(proofs);
                            break;
                        }
                        
                        break;
                    case "changeLocation":
                        //changeLocation(axisX, axisY);
                        break;

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

    private static List<String> requestLocationProof() throws IOException {
        int[] ports = getClientConnections();
        List<String> proofs = new ArrayList<String>();
        JsonParser parser = new JsonParser();

        for(int p : ports){
            if(p != 0) {
                
                try {
                    Client_connection client = connectToClient(p);

                    // This requestLocationProof is a proof signe by the witness;
                    client.sender.println("requestLocationProof");
                    String responseLocationProof = client.receiver.readLine();

                    // Its not necessary to check what is the response from the witness!
                    // Server will chek that!
                    JsonObject proofJSON = parser.parse​("{}").getAsJsonObject();
                    {
                        JsonObject infoJson = parser.parse​("{}").getAsJsonObject();
                        
                        infoJson.addProperty("witness", p);
                        proofJSON.add("info", infoJson);
                        
                        infoJson.addProperty("responseLocationProof", responseLocationProof);
                        proofJSON.add("info", infoJson);

                    }

                    System.out.println("user port " + p + " sent a proof");

                    // Encode request in base64
                    byte[] proofBytes = proofJSON.toString().getBytes();
                    String proofBytesB64String = Base64.getEncoder().encodeToString(proofBytes);
                    proofs.add(proofBytesB64String);
                    
                    client.sender.println("bye");
                    client.socket.close();

                } 
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if(proofs.size() == 0){
            System.out.println("At Least Two Clients Must be Running!.\n");
                
        }
        return proofs;
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


    private static void submitLocationReport(List<String> proofs ) throws IOException {

        for(int i = 0; i < proofs.size(); i++) {
            System.out.println(proofs.get(i));
        }

        // ----------------------------------------------------------------- //
        //                  Client Server Comunication Init
        // ----------------------------------------------------------------- //
        String clientRequest = "submitLocationReport";

        ClientServerCommunication clientServerCommunication = 
        new ClientServerCommunication(userID, clientPort,
            clientRequest, proofs);

        clientServerCommunication.generateKeys();
        clientServerCommunication.startSecureCommunication();

    }


}