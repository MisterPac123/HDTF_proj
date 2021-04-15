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

public class ClientListener extends Thread {
    private int port;
    private String userID;
    private Socket socket;
    

    public ClientListener(String userID, int port) {
        this.port = port;
        this.userID=userID;

    }

    public void run() {
        if(addUser(this.userID, this.port)) {
            while (true) {
                try (ServerSocket serverSocket = new ServerSocket(port)) {
                    System.out.println("Client is listening on port " + port);
                    this.socket = serverSocket.accept();
                    System.out.println("client found");

                    InputStream input = this.socket.getInputStream();
                    OutputStream output = this.socket.getOutputStream();
                    BufferedReader receiver = new BufferedReader(new InputStreamReader(input));
                    PrintWriter sender = new PrintWriter(output, true);

                    String message;
                    //message = receiver.readLine();
                    do {
                        message = receiver.readLine();
                        System.out.println("Client:" + message);
                        switch (message) {
                            case "requestLocationProof":
                                if (handleRequestLocationProof(sender, port)) {
                                    System.out.println("Proof sent");
                                    //submitLocationReport(userId, ep, report, …)
                                    //Specification: user userId submits a location report.
                                }
                                break;
                        }
                    } while (!message.equals("bye"));

                    socket.close();
                } catch (IOException ex) {
                    System.out.println("ClientListener exception: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        }
    }

    //#########################
    //## Secondary functions ##
    //#########################

    public static boolean handleRequestLocationProof(PrintWriter sender, int port){
        try{

            // ------------------------------------------------ //
            //              Send Proof Signed!
            // ------------------------------------------------ //
            // Put Whatever You Want On the String Proof!
            String proof = "Proof";
            String proofSigned = signProof(proof, port);
            sender.println(proofSigned);
            return true;
        } 

        catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean addUser(String userID, int port){
        try {//Check if user is already in the file, or if it is with the same port
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




    // =============================================================================== //
    //                               [WITNESS SIGN PROOF]
    // =============================================================================== //

    public static String signProof(String proof, int port){

        System.out.println("----------------------------------------");
        System.out.println("signProof\n");
        System.out.println(port);
        System.out.println("I AM A WITNESS AND I WILL SIGN THE PROOF\n");
        System.out.println("----------------------------------------");


        ClientWitness clientWitness = new ClientWitness(port);
        String proofSigned = clientWitness.generateSinature(proof);
        return proofSigned;
        
    };

}
