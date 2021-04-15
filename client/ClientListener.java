package client;


import java.io.*;
import java.net.*;
import java.util.Scanner;


public class ClientListener extends Thread {
    private int port;
    private String userID;
    private Socket socket;
    private String type;

    public ClientListener(String userID, int port, String type) {
        this.port = port;
        this.userID=userID;
        this.type=type;
    }

    public void run() {
        
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
                do {
                    message = receiver.readLine();
                    System.out.println("Client:" + message);
                    switch (message) {
                        case "requestLocationProof":
                            if (handleRequestLocationProof(sender, port)) {
                                System.out.println("Proof sent");
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
