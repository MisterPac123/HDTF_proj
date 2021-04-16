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

    // =============================================================================== //
    //                                   [MAIN]
    // =============================================================================== //
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


        if(addUser(userID, port, type)) {
            //Start listener socket

            // ----------------------------------------------------------------- //
            //                          Actions Menu
            // ----------------------------------------------------------------- //
            new ClientListener(userID, port, type).start();

            List<String> proofs = new ArrayList<String>();
            //read input command
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                String text;
                System.out.println("\nWelcome " + userID + "\n\n");
                do {
                    System.out.println("Choose option:\n     1 - Request Location Proof\n     2 - add Location\n     3 - ObtainLocationReport\n     4 - ObtainUsersAtLocation(HA user only)\n     5 - Exit\n");
                    text = reader.readLine();
                    String[] command = text.split(" ");
                    switch (command[0]) {

                        case "1":
                            if(command.length!=2){
                                System.out.println("Wrong arguments. Expected one argument: 1 (int) epoch");
                                continue;
                            }

                            
                            try { 
                                epoch = Integer.parseInt(command[1]);
                                proofs = requestLocationProof(epoch);
                            }
                            catch (NumberFormatException e) {
                                System.out.println("Wrong arguments. Expected one argument: 1 (int) epoch");
                                continue;
                            }

                            // ----------------------------------------------- //
                            //               Submit Location Report
                            // ----------------------------------------------- //
                            
                            if(proofs.size() > 0){
                                submitLocationReport(port, proofs);
                                break;
                            }
                            //submitLocationReport(userId, ep, report, …)
                            //Specification: user userId submits a location report.
                            break;
                        
                        case "2":
                            //TODO: test inputs (axisX and axisY)
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

                        case "3":
                            System.out.println("Por favor insira UserID e epoch separados por um espaço:\n");
                            String text2 = reader.readLine();
                            String[] split = text2.split("\\s+");
                            int ep = Integer.parseInt(split[1]);
                            obtainLocationReport(port, split[0], ep, proofs);
                        break;
                        case "4":
                            System.out.println("Por favor insira posição 'x,y' e epoch separados por um espaço:\n");
                            String text3 = reader.readLine();
                            String[] split2 = text3.split("\\s+");
                            int ep2 = Integer.parseInt(split2[1]);
                            obtainUsersAtLocation(port, split2[0], ep2, proofs);
                        break;
                    }

                } while (!text.equals("5"));

            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Bye");
            System.exit(0);
        }
        else return;
    }


    //####################
    //## Main functions ##
    //####################




    private static List<String> requestLocationProof(int epoch) throws IOException {
        int[] ports = getClientConnections(epoch);
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




    private static void submitLocationReport(int port, List<String> proofs ) throws IOException {

        for(int i = 0; i < proofs.size(); i++) {
            System.out.println(proofs.get(i));
        }

        // ----------------------------------------------------------------- //
        //                  Client Server Comunication Init
        // ----------------------------------------------------------------- //
        String clientRequest = "submitLocationReport " + userID +" 1 " + "(2,2)";

        ClientServerCommunication clientServerCommunication = 
        new ClientServerCommunication(userID, port,
            clientRequest, proofs);

        clientServerCommunication.generateKeys();
        clientServerCommunication.startSecureCommunication();

    }



    private static void obtainLocationReport(int port, String userID2, int epoch, List<String> proofs) throws IOException {


        //----------------------------------------------------- //
        //   [Start New Thread Communication With Server ]      //
        //----------------------------------------------------- //
        //if(userID == "ha")

        String clientRequest = "obtainLocationReport " + userID2 +" "+ epoch;
        ClientServerCommunication clientServerCommunication = 
        new ClientServerCommunication(userID, port,
            clientRequest, proofs);

        clientServerCommunication.generateKeys();
        clientServerCommunication.startSecureCommunication();


    }
    private static void obtainUsersAtLocation(int port, String pos, int epoch, List<String> proofs) throws IOException {

        //----------------------------------------------------- //
        //   [Start New Thread Communication With Server ]      //
        //----------------------------------------------------- //
        //if(userID == "ha")

        String clientRequest = "obtainUsersAtLocation " + pos +" "+ epoch;
        ClientServerCommunication clientServerCommunication = 
        new ClientServerCommunication(userID, port,
            clientRequest, proofs);

        clientServerCommunication.generateKeys();
        clientServerCommunication.startSecureCommunication();

    }


    public static boolean addUser(String userID, int port, String type){
        try {//Check if user is already in the file, or if it is with the same port, or if already exists a super user
            boolean newClient = true;
            File portsFile = new File("usersPorts.txt");
            if(portsFile.exists()) {
                Scanner myReader = new Scanner(portsFile);
                while (myReader.hasNextLine()) {
                    String data[] = myReader.nextLine().split(" ");

                    if (data[0].equals(userID) && Integer.parseInt(data[1]) != port) {
                        System.out.println("Error creating user: UserID already exists with port " + data[1]);
                        return false;
                    }
                    
                    //if (Integer.parseInt(data[1]) == port) {
                    //    System.out.println("Error creating user: Port: " + data[1] + " already reserved for another client.");
                    //    return false;
                    //}

                    if (type.equals("super") && data[0].length() >= 3) {
                        if (data[0].startsWith("SU_")){
                            System.out.println("Error creating user: Super User already exists");
                            return false;
                        }
                    }
                    if (data[0].equals(userID) && Integer.parseInt(data[1]) == port) {
                        newClient = false;
                    }
                }
                myReader.close();
            }
            if(newClient) {
                FileWriter usersPorts = new FileWriter("usersPorts.txt", true);
                usersPorts.write(userID + " " + port + "\n");
                usersPorts.close();
                System.out.println("Successfully add User.");
                return true;
            }

        } catch (IOException e) {
            System.out.println("ClientListener exception: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
}