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
    private static String clientPrivateKeypath;
    private static String clientPublicKeypath;
    private static String serverPublicKeypath;
    private static String symmetricKeyPath;

    private static PrivateKey clientPrivateKey;
    private static PublicKey clientPublicKey;
    private static PublicKey serverPublicKey;
    private static Key symmetricKey;


    public Client(String userID, String hostname, int clientPort) {

        this.userID = userID;
        this.hostname = hostname;
        this.clientPort = clientPort;

        //this.clientPrivateKeypath = "keys/alice.privkey";
        //this.clientPublicKeypath = "keys/alice.pubkey";
        //this.serverPublicKeypath = "keys/bob.pubkey";
        //this.symmetricKeyPath = "keys/secret.key";

        this.clientPrivateKeypath = "keys/client_priv.key";
        this.clientPublicKeypath = "keys/client_pub.key";
        this.serverPublicKeypath = "keys/server_pub.key";
        this.symmetricKeyPath = "keys/aes.key";

        try{
            //----------------------------------------------------- //
            //                      [Read keys]                     //
            //----------------------------------------------------- //
            this.clientPrivateKey = readPrivateKey(clientPrivateKeypath);
            this.clientPublicKey = readPublicKey(clientPublicKeypath);
            this.serverPublicKey = readPublicKey(serverPublicKeypath);
            this.symmetricKey = readSecretKey(symmetricKeyPath);
            
        }
        catch(Exception e){
            System.err.println("Error while reading the keys!");
        }
    };

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
    //                           [CLIENT SERVER COMMUNICATION]
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
        

        int port; String hostname = null;
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


        // ----------------------------------------------------------------- //
        //                          Client Constructor
        // ----------------------------------------------------------------- //
        Client client = new Client(userID, hostname, port);

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
                        boolean proofsOk = requestLocationProof();

                        // ----------------------------------------------------------------- //
                        // User Eventually sends Json with proofs to Server
                        // Use Some Flag To indicate when to comunicate with server
                        // ----------------------------------------------------------------- //
                        if(proofsOk){
                            sendLocationProofToServer(client.userID, client.clientPort);
                        }
                        // ----------------------------------------------------------------- //
                        // ----------------------------------------------------------------- //

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

    // =============================================================================== //
    //                               [CLIENT COMMUNICATIONS]
    // =============================================================================== //
    private static boolean requestLocationProof() throws IOException {
        int[] ports = getClientConnections();
        boolean atLeastOnePort = false;
        for(int p : ports){
            if(p != 0) {
                atLeastOnePort = true;
                try {
                    Client_connection client = connectToClient(p);

                    client.sender.println("requestLocationProof");
                    String responseLocationProof = client.receiver.readLine();

                    if (responseLocationProof.equals("Proof")) {
                        System.out.println("user port " + p + " sent a proof");
                        return true;
                    }

                    client.sender.println("bye");
                    client.socket.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        if(!atLeastOnePort){
            System.out.println("At Least Two Clients Must be Running!.\n");
                
        }
        return false;
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


    // =============================================================================== //
    //                               [SERVER COMMUNICATIONS]
    // =============================================================================== //

    private static boolean sendLocationProofToServer(String userID, int clientPort) throws IOException {
        
        String clientRequestString = "requestLocationProof";
        // ----------------------------------------------------------------- //
        // Considering Server is running now on localhost 8000
        // Change Later
        // ----------------------------------------------------------------- //

        Client_Server_Connection client_server_connection = new Client_Server_Connection();

        //----------------------------------------------------- //
        //   [Start New Thread Communication With Server ]      //
        //----------------------------------------------------- //
        new ClientServerCommunication(userID, clientPort, 
            clientPrivateKey, clientPublicKey, serverPublicKey, symmetricKey,
            client_server_connection.serverAddress, client_server_connection.serverPort,
            clientRequestString
        ).start();
        return true;
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




}