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

    private final static String CIPHER_ASYM = "RSA/ECB/PKCS1Padding";
    private final static String CIPHER_SYM = "AES/ECB/PKCS5Padding";
    private final static String DIGEST_ALGO = "SHA-256";

    // ----------------------------------------------------------------- //
    // Each user will have a different path
    // Will be necessary for proof signature too.
    // ----------------------------------------------------------------- //
   

    private static PrivateKey clientPrivateKey;
    private static PublicKey clientPublicKey;
    private static PublicKey serverPublicKey;
    private static Key symmetricKey;


    // =============================================================================== //
    //                           [CLIENT CONSTRUCTOR]
    // =============================================================================== //
    public Client(String userID, String hostname, int clientPort, 
        PrivateKey clientPrivateKey, PublicKey clientPublicKey,
        PublicKey serverPublicKey, Key symmetricKey) {

        this.userID = userID;
        this.hostname = hostname;
        this.clientPort = clientPort;


        try{
            //----------------------------------------------------- //
            //                      [Read keys]                     //
            //----------------------------------------------------- //
            this.clientPrivateKey = clientPrivateKey;
            this.clientPublicKey = clientPublicKey;
            this.serverPublicKey = serverPublicKey;
            this.symmetricKey = symmetricKey;
            
        }
        catch(Exception e){
            System.err.println("Error while reading the keys!");
        }
    };

    // =============================================================================== //
    //                           [CLIENT CONNECTION CONSTRUCTOR]
    // =============================================================================== //
    private static class Client_connection{
        private final BufferedReader receiver;
        private final PrintWriter sender ;
        private final Socket socket;

        public Client_connection(PrintWriter sender, BufferedReader receiver, Socket socket) {
            this.receiver = receiver;
            this. sender = sender;
            this.socket =socket;
        }
    };

    // =============================================================================== //
    //                      [CLIENT SERVER COMMUNICATION CONSTRUCTOR]
    // =============================================================================== //

    private static class Client_Server_Connection{
        // ----------------------------------------------------------------- //
        // Might have to change later
        // ----------------------------------------------------------------- //

        //private ArrayList<Long> timestampsAlreadyUsed = new ArrayList<Long>();
        private InetAddress serverAddress = null;
        private int serverPort = 8000;


        // ----------------------------------------------------------------- //
        // Constructur should have: serverAddress; serverPort
        // ----------------------------------------------------------------- //
        public Client_Server_Connection() {
            //this.serverPort = serverPort;
            //this.serverAddress = serverAddress;
            try{
                serverAddress = InetAddress.getByName("localhost");
            }
            catch (Exception ex) {  
                System.err.println(ex);  
            } 
            
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
        //                          Keys Generation
        // ----------------------------------------------------------------- //
        AESKeyGenerator aesKeyGenerator = null;
        RSAKeyGenerator rsaKeyGenerator = null;
        Key aesKey  =  null;
        PublicKey rsaUserPubKey = null; PrivateKey rsaUserPrivKey = null; KeyPair clientKeyPair = null;
        PublicKey serverPubKey = null;

        String userSharedKeysDir = "keys/shared/" + port;
        String userPrivateKeyDir = "keys/private/" + port;
        String serverKeyPath = "keys/shared/server/server_pub.key";
        
        try{
            serverPubKey = readPublicKey(serverKeyPath);            
        }
        catch (Exception ex) {  
            System.err.println("Error Reading Server Public Key.\n"); 
            System.err.println("Check if There is a directory: " + serverKeyPath + " .\n"); 
            return;
        }

        try{
            File sharedDir = new File(userSharedKeysDir);
            if (!sharedDir.exists()){
                sharedDir.mkdirs();
            }
            File privDir = new File(userPrivateKeyDir);
            if (!privDir.exists()){
                privDir.mkdirs();
            }
        }

        catch (Exception ex) {  
            System.err.println("Error Creating The Keys Directories For User.\n"); 
            return;
        } 
        try{
            // Asssuming userID is unique.
            String userSharedAESKeyPath = userSharedKeysDir + "/aes.key";
            aesKeyGenerator = new AESKeyGenerator(userSharedAESKeyPath);

            try{
                aesKey = aesKeyGenerator.read(); 
            }
            catch (Exception ex) {  
                System.out.println("Symmetric Key Does Not Exists Yet.\n");
                System.out.println("Generating Symmetric Key.\n");
                aesKeyGenerator.write();  
                aesKey = aesKeyGenerator.read(); 
            } 
        }
        catch (Exception ex) {  
            System.err.println("Error Generating Symmetric Key.\n");
            System.err.println("Ckeck AESKeyGenerator\n");    
            return;
        } 
        
        try{
            // Asssuming userID is unique.
            String userSharedRSAKeyPath = userSharedKeysDir + "/client_pub.key";
            String userPrivateRSAKeyPath = userPrivateKeyDir + "/client_priv.key";
            //String userPrivKeyDir 
            rsaKeyGenerator = new RSAKeyGenerator(userSharedRSAKeyPath, userPrivateRSAKeyPath);

            try{
                clientKeyPair = rsaKeyGenerator.read(); 
                rsaUserPrivKey = clientKeyPair.getPrivate();
                rsaUserPubKey = clientKeyPair.getPublic();
            }
            catch (Exception ex) {  
                System.out.println("Asymmetric Key Pair Does Not Exists Yet.\n");
                System.out.println("Generating Asymmetric Key Pair.\n");
                rsaKeyGenerator.write();  
                clientKeyPair = rsaKeyGenerator.read(); 
                rsaUserPrivKey = clientKeyPair.getPrivate();
                rsaUserPubKey = clientKeyPair.getPublic();
            } 
        }
        catch (Exception ex) {  
            System.err.println("Error Generating Key Pair.\n");
            return;
        } 

        
        // ----------------------------------------------------------------- //
        //                          Client Init
        // ----------------------------------------------------------------- //
        Client client = new Client(userID, hostname, port, 
            rsaUserPrivKey, rsaUserPubKey, serverPubKey, aesKey);

        
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
                    //byte[] proofBytesB64 = proofBytesB64String.getBytes();
                    proofs.add(proofBytesB64String);

                    /*
                    if (responseLocationProof.equals("Proof")) {

                        
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
                        //byte[] proofBytesB64 = proofBytesB64String.getBytes();
                        proofs.add(proofBytesB64String);
                    }*/
                    client.sender.println("bye");
                    client.socket.close();

                } catch (IOException e) {
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

        Client_Server_Connection client_server_connection = new Client_Server_Connection();

        //----------------------------------------------------- //
        //   [Start New Thread Communication With Server ]      //
        //----------------------------------------------------- //
        
        ClientServerCommunication clientServerCommunication = 
        new ClientServerCommunication(userID, clientPort, 
            clientPrivateKey, clientPublicKey, serverPublicKey, symmetricKey,
            client_server_connection.serverAddress, client_server_connection.serverPort,
            "submitLocationReport", proofs
        );
        clientServerCommunication.run();

    }


    // =============================================================================== //
    //                               [READ KEYS FUNCTIONS]
    // =============================================================================== //
    private static byte[] readFile(String path) throws FileNotFoundException, IOException {
        FileInputStream fis = new FileInputStream(path);
        byte[] content = new byte[fis.available()];
        fis.read(content);
        fis.close();
        return content;
    }

    public static Key readSecretKey(String secretKeyPath) throws Exception {
        byte[] encoded = readFile(secretKeyPath);
        SecretKeySpec keySpec = new SecretKeySpec(encoded, "AES");
        return keySpec;
    }

    public static PublicKey readPublicKey(String publicKeyPath) throws Exception {
        //System.out.println("Reading public key from file " + publicKeyPath + " ...");
        byte[] pubEncoded = readFile(publicKeyPath);
        X509EncodedKeySpec pubSpec = new X509EncodedKeySpec(pubEncoded);
        KeyFactory keyFacPub = KeyFactory.getInstance("RSA");
        PublicKey pub = keyFacPub.generatePublic(pubSpec);
        return pub;
    }

    public static PrivateKey readPrivateKey(String privateKeyPath) throws Exception {
        byte[] privEncoded = readFile(privateKeyPath);
        PKCS8EncodedKeySpec privSpec = new PKCS8EncodedKeySpec(privEncoded);
        KeyFactory keyFacPriv = KeyFactory.getInstance("RSA");
        PrivateKey priv = keyFacPriv.generatePrivate(privSpec);
        return priv;
    }


    public static void deleteDirectory(String dir) {

        
        File index = new File(dir);
        String[]entries = index.list();
        for(String s: entries){
            File currentFile = new File(index.getPath(),s);
            currentFile.delete();
        }
        index.delete();
    }
}