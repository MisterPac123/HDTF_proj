package client;

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

public class ClientServerCommunication {
//public class ClientServerCommunication{
    private String clientRequestString;

    private int port;
    private String userID;
    private Socket socket;

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

    private ArrayList<Long> timestampsAlreadyUsed = new ArrayList<Long>();
    private InetAddress serverAddress = null;
    private int serverPort = 8000;
  
    public ClientServerCommunication(String userID, int port, 
        PrivateKey clientPrivateKey, PublicKey clientPublicKey, 
        PublicKey serverPublicKey, Key symmetricKey, 
        InetAddress serverAddress, int serverPort,
        String clientRequestString 
        ){

        this.port = port;
        this.userID = userID;

        this.clientPublicKey = clientPublicKey;
        this.clientPrivateKey = clientPrivateKey;
        this.serverPublicKey = serverPublicKey;
        this.symmetricKey = symmetricKey;

        this.serverAddress = serverAddress;
        this.serverPort = serverPort;

        this.clientRequestString = clientRequestString;

    };

    public void run() {


        //====================================================== //
        //   [Create Start Communication Request Message ]       //
        //====================================================== //

        //------------------------------------------------------------- //
        // Client sends a non secure message to server indicating       //
        // its identity. The message does not need to be encrypted or   //
        // authenticated now. Its just for server to know where to      //
        // find client's public key and symmentric key.                 //
        // Lets now consider that a userA says to Server that           //
        // it is UserB.                                                 //
        // While in the secure communication message exchange, userA    //
        // (that says its userB) can NOT proof its identity(false one). //
        // This is because userA does not have access to usersB private //
        // key so it can produce a false Signature.                     //
        // If this happens, Server can NOT confirm user's authenticity  //
        // and imediately knows that something wrong is going on.       //
        //------------------------------------------------------------- //

        System.out.println(
        "=================================================="+ 
        "==================================================\n");
        System.out.println("[Create Start Communication Request Message]\n");

        System.out.println(
        "=================================================="+ 
        "==================================================\n");

        JsonObject startCommunicationRequestJson = createJsonMessage(this.userID, 
            "Hey I am " + this.userID + " and I Want To Start A Connection.", "");
        
        // Encode request in base64
        byte[] startCommunicationRequest_bytes = startCommunicationRequestJson.toString().getBytes();
        String startCommunicationRequest_B64String = Base64.getEncoder().encodeToString(startCommunicationRequest_bytes);
        byte[] startCommunicationRequest_B64 = startCommunicationRequest_B64String.getBytes();


        Socket clientSocket = null;
        try {
            clientSocket = new Socket(this.serverAddress, this.serverPort);
            DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
            //System.out.println("\nByte array printed before sending to server:\n");


            System.out.println("Sending Request");
            outToServer.writeInt(startCommunicationRequest_B64.length); // write length of the message
            outToServer.write(startCommunicationRequest_B64);      
        }

        catch (Exception ex) {  
            System.err.println("Error Creating Server Socket. Check if Server is Running Correctly!\n");  
            System.err.println("Could not Send Message to Server Sucessfully.\n");  
            return;
        } 

        //====================================================== //
        //   [Recevive Start Communication Request Message ]      //
        //====================================================== //

        String startReplyString = null;
        try{
            DataInputStream inStartReplyFromServer =
            new DataInputStream(clientSocket.getInputStream());    

            int startReplyLength = inStartReplyFromServer.readInt(); 
            byte[] inStartReplyFromServer_B64_bytes = new byte[startReplyLength];  

            for(int i = 0; i < inStartReplyFromServer_B64_bytes.length; i++) {
                inStartReplyFromServer_B64_bytes[i] = inStartReplyFromServer.readByte();
            }
            
            //----------------------------------------------------- //
            //              [Decode Request JSON ]                  //
            //----------------------------------------------------- //
            
            byte[] inStartReply_bytes_decoded =  Base64.getMimeDecoder().decode(inStartReplyFromServer_B64_bytes);

            startReplyString = new String(inStartReply_bytes_decoded);
            System.out.println("inStartRequest_string" + startReplyString);
      

        }

        catch (Exception ex) {  
            System.err.println("Error Decoding Client Request Message.\n");
            return;
        } 
        //----------------------------------------------------- //
        //                  [Parse CM JSON ]                    //
        //----------------------------------------------------- //
        JsonParser parser = new JsonParser();
        JsonObject startReplyStringJSON = parser.parse​(startReplyString).getAsJsonObject();
        
        String from = null, to = null, timestampReceived = null, startMessage = null;
        {
            JsonObject infoJson = startReplyStringJSON.getAsJsonObject("info");
            timestampReceived = infoJson.get("timestamp").getAsString();
            from = infoJson.get("from").getAsString();
            to = infoJson.get("to").getAsString();
            startMessage = infoJson.get("message").getAsString();

        }       

        System.out.println(
        "=================================================="+ 
        "==================================================\n");
        System.out.println("[Starting Secure Communication]\n");

        System.out.println(
        "=================================================="+ 
        "==================================================\n");

        
        //====================================================== //
        //              [Create Request Message ]                //
        //====================================================== //

        //----------------------------------------------------- //
        // PASS SOME PARAMETERS IF NECESSARY
        // REQUESTS ONLY VARY ON RESUEST JSON PARAMETERS 
        // NOT ON CRYPTO FUNCTIONS
        // IMPLEMENT A SWITCH HERE!
        //----------------------------------------------------- //

        String proof = "METE AQUI A PROOF!";
        JsonObject requestJson = createJsonMessage(this.userID,this.clientRequestString, proof);
        byte[] clientData = requestJson.toString().getBytes();


        //--------------------------------------------------------- //
        //              [Cipher Data: Confidentiality]              //
        // • Sender (Client):                                       //
        //  – Generate a random symmetric key KM                    //
        //  – Cipher message M with key KM to produce cryptogram CM //
        //  – Cipher key KM with receiver’s public Key  to produce  //
        //  cryptogram CK                                           //
        //  – Send CK and CM                                        //
        //--------------------------------------------------------- //

        
        // Generate a random symmetric key KMf
        // Cipher message M with key KM to produce cryptogram CM 

        Cipher cipher_km = null; byte[] cm_bytes = null;
        try{
            cipher_km = Cipher.getInstance(CIPHER_SYM);
            cipher_km.init(Cipher.ENCRYPT_MODE, this.symmetricKey);
            cm_bytes = cipher_km.doFinal(clientData);               
        }
        catch (Exception ex) {  
            System.err.println("Error Ciphering Client Data With Symmetric Cipher.\n");  
            System.err.println("Generated Symmetric Key Might Not Be Correct.\n");  
            return;
        } 

        Cipher cipher_rpk = null; byte[] ck_bytes = null;
        try{
            cipher_rpk = Cipher.getInstance(CIPHER_ASYM);
            cipher_rpk.init(Cipher.ENCRYPT_MODE, this.serverPublicKey);
            ck_bytes = cipher_rpk.doFinal(this.symmetricKey.getEncoded());
               
        }
        catch (Exception ex) {  
            System.err.println("Error Ciphering Symmetric Cipher With Receivers Public Key .\n"); 
            System.err.println("Server Public Key Might Not Be Correct.\n");  
            return; 
        } 


        // Encode CM in base64
        String cm_String = cm_bytes.toString();
        String cm_B64String = Base64.getEncoder().encodeToString(cm_bytes);
        byte[] cm_B64 = cm_B64String.getBytes();

        // Encode KM in base64
        String ck_String = ck_bytes.toString();
        String ck_B64String = Base64.getEncoder().encodeToString(ck_bytes);
        byte[] ck_B64 = ck_B64String.getBytes();

        // Cipher key KM with receiver’s public Key  to produce 
        // cryptogram CK 



        //--------------------------------------------------------- //
        //      [Cipher Data: Integrity and Authentication ]        //
        // • Sender (Client):                                       //
        //  – Digest message M to produce hash H                    //
        //  – Cipher hash H with private key of Sender              //
        //    to produce signature S                                //
        //  – Send M and S                                          //
        //--------------------------------------------------------- //
        
            
        // Digest message M to produce hash H 
        byte[] digestB64bytes = null; String digestB64String;
        try{

            MessageDigest messageDigest = MessageDigest.getInstance(DIGEST_ALGO);
            messageDigest.update(clientData);
            byte[] digestBytes = messageDigest.digest();

            // Encode digest in base64
            digestB64String = Base64.getEncoder().encodeToString(digestBytes);
            digestB64bytes = digestB64String.getBytes();

        }
        catch (Exception ex) {  
            System.err.println("Error Digesting Message.\n");  
            return;
        } 

        // Cipher hash H with private key of Sender to produce signature S 
        Cipher sign = null;
        try{
            sign = Cipher.getInstance(CIPHER_ASYM);
            sign.init(Cipher.ENCRYPT_MODE, this.clientPrivateKey);
        }
        catch (Exception ex) {  
            System.err.println("Error Creating Client Signature!\n");  
            System.err.println("Client Priv Key Might Not Be Correct.\n");  
            return;
        } 

        byte[] signature_bytes = null;
        try{
            signature_bytes = sign.doFinal(digestB64bytes);
        }
        catch (Exception ex) {  
            System.err.println("Error Converting Client Signature to Bytes.\n");  
            return;
        } 

        
        String signatureB64String = Base64.getEncoder().encodeToString(signature_bytes);


        //------------------------------------------------------------  //
        //          [Create Request Message With CK, CM, Signature]     //
        //------------------------------------------------------------  //

        //JsonParser parser = new JsonParser();
        JsonObject request = parser.parse("{}").getAsJsonObject();
        {
            JsonObject infoJson = parser.parse("{}").getAsJsonObject();

            infoJson.addProperty("CK", ck_B64String);
            request.add("confidentiality", infoJson);

            infoJson.addProperty("CM", cm_B64String);
            request.add("confidentiality", infoJson);

            infoJson.addProperty("Signature", signatureB64String);
            request.add("integraty_and_auth", infoJson);
        }

        // Encode request in base64
        byte[] request_bytes = request.toString().getBytes();
        String request_B64String = Base64.getEncoder().encodeToString(request_bytes);
        byte[] request_B64 = request_B64String.getBytes();

        //----------------------------------------------------- //
        //          [Create Socket and Send Request ]       //
        //----------------------------------------------------- //
    

        System.out.println("Client started.");
        System.out.println("Connection Sucessfully Established With Server." );    

        
        try {
            //clientSocket = new Socket(this.serverAddress, this.serverPort);
            DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
            //System.out.println("\nByte array printed before sending to server:\n");


            System.out.println("Sending Request");
            outToServer.writeInt(request_B64.length); // write length of the message
            outToServer.write(request_B64);      
        }

        catch (Exception ex) {  
            System.err.println("Error Creating Server Socket. Check if Server is Running Correctly!\n");  
            System.err.println("Could not Send Message to Server Sucessfully.\n");  
            return;
        } 


        //====================================================== //
        //             [Receive Response From Server]            //
        //====================================================== //

        
        System.out.println(
        " =================================================\n"
        + " =================================================\n");
        System.out.println("SERVER -> CLIENT \n");
        System.out.println(
        " =================================================\n"
        + " =================================================\n");

        // Answer is now received from Server
        byte[] response_B64_data = null;
        try{
            DataInputStream inFromServer = new DataInputStream(clientSocket.getInputStream());
            int length = inFromServer.readInt(); // read length of incoming message
            System.out.println("Receiving Response from Server... ");
            //System.out.println("length   " + length);
            response_B64_data = new byte[length];
            for(int i = 0; i < response_B64_data.length; i++) {
                response_B64_data[i] = inFromServer.readByte();

                //System.out.println(messageFromServer[i]);//prints received byte array
            }

            System.out.println("Response Received Successfully. ");
        }
        
        catch (Exception ex) {  
            System.err.println("Error Receiving Response From Server!\n");  
            return;
        } 

        //----------------------------------------------------- //
        //              [Decode Request JSON ]                  //
        //----------------------------------------------------- //
        
        byte[] response_bytes =  Base64.getMimeDecoder().decode(response_B64_data);
        String response_string = new String(response_bytes);

        //----------------------------------------------------- //
        //              [Parse request JSON ]                   //
        //----------------------------------------------------- //

        JsonObject response = parser.parse​(response_string).getAsJsonObject();
        String ck = null, cm = null, digest = null, signature=null;
        {
            JsonObject confJson = response.getAsJsonObject("confidentiality");
            ck = confJson.get("CK").getAsString();
            cm = confJson.get("CM").getAsString();
            JsonObject iaJson = response.getAsJsonObject("integraty_and_auth");
            signature = iaJson.get("Signature").getAsString();
        }
        



        ck_bytes =  Base64.getMimeDecoder().decode(ck);
        cm_bytes =  Base64.getMimeDecoder().decode(cm);
        signature_bytes =  Base64.getMimeDecoder().decode(signature);



        //----------------------------------------------------- //
        //  • Receiver (Lab):                                   //
        //  – Decipher CK with its private key to obtain KM     //
        //  – Decipher CM with key KM to recover M              //
        //----------------------------------------------------- //

        // Decipher CK with its private key to obtain KM    
        
        Cipher cipher_asym  = null; byte[] ck_bytes_deciphered = null;
        try{ 
            System.out.println("Deciphering with " + CIPHER_ASYM + "...");
            cipher_asym = Cipher.getInstance(CIPHER_ASYM);
            cipher_asym.init(Cipher.DECRYPT_MODE, clientPrivateKey);
            ck_bytes_deciphered = cipher_asym.doFinal(ck_bytes);
        }
        catch (Exception ex) {  
            System.err.println("Error Deciphering CK Bytes.\n");  
            System.err.println("Client Private Key Might Not Be Correct.\n");  
            return;
        } 
        String ck_bytes_deciphered_string = new String(ck_bytes_deciphered); 


         

        //----------------------------------------------------- //
        //              [Rebuild KM (symmetric key) ]           //
        //----------------------------------------------------- //
        
        SecretKey km_symmetric = new SecretKeySpec(ck_bytes_deciphered, 0, ck_bytes_deciphered.length, "AES"); 

        //  Decipher CM with key KM to recover M    
        byte[] cm_bytes_deciphered  = null;
        try{
            //System.out.println("Deciphering with " + CIPHER_SYM + "...");
            Cipher cipher_sym = Cipher.getInstance(CIPHER_SYM);
            cipher_sym.init(Cipher.DECRYPT_MODE, km_symmetric);
            cm_bytes_deciphered = cipher_sym.doFinal(cm_bytes);

        }
        catch (Exception ex) {  
            System.err.println("Error Deciphering CM Bytes.\n");  
            System.err.println("Generated Symmetric Key Might Not Be Correct.\n");  
            return;
        } 

        String message_response = new String(cm_bytes_deciphered);
        


        
        System.out.println("----------------------------------------");
        System.out.println("Deciphered data Message:" + message_response);
        System.out.println("Confidentiality ensured.");
        System.out.println("----------------------------------------");

        //----------------------------------------------------- //
        //                  [Parse CM JSON ]                    //
        //----------------------------------------------------- //

        JsonObject cm_json = parser.parse​(message_response).getAsJsonObject();
        
        from = null; to = null; timestampReceived = null; String serverMessage = null; proof = null;
        {
            JsonObject infoJson = cm_json.getAsJsonObject("info");
            timestampReceived = infoJson.get("timestamp").getAsString();
            

            from = infoJson.get("from").getAsString();
            to = infoJson.get("to").getAsString();
            serverMessage = infoJson.get("message").getAsString();
            proof = infoJson.get("proof").getAsString();

        }


        System.out.println("-------------------------------------");
        System.out.println("Json Received From Server: ");
        System.out.println("from: " + from);
        System.out.println("to: " + to);
        System.out.println("timestamp: " + timestampReceived);
        System.out.println("message: " + serverMessage);
        System.out.println("-------------------------------------");


        //------------------------------------------------------------- //
        //                  Check freshness of message
        //------------------------------------------------------------- //
        
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        long timestampActual = timestamp.getTime();
        long timestampMessage = Long.parseLong(timestampReceived);

        //For small messages this is almost always false
        if (timestampActual - timestampMessage >= 10000 || timestampsAlreadyUsed.contains(timestampMessage)) {
            System.out.printf("Message replayed\n");
        }

        else{

            timestampsAlreadyUsed.add(timestampMessage);
            System.out.println("----------------------------------------");
            System.out.println("Freshness ensured.");
            System.out.println("----------------------------------------");
            
        }


        //------------------------------------------------------------- //
        // • Receiver (Client):                                         //
        // – Hash received Message (message) and digest it to produce hash H’ //
        // – Decipher S with public key of sender to get H back         //
        // – If recalculated hash H’ is equal to deciphered hash H then //
        // the message was not modified and was sent by the sender      //
        //------------------------------------------------------------- //

        // Hash received Message (message) and digest it to produce hash H’ 
        byte[] hash = null; byte[] hash_prime = null;
        try {
            MessageDigest digest_prime = MessageDigest.getInstance(DIGEST_ALGO);
            digest_prime.update(cm_bytes_deciphered);

            byte[] digest_prime_bytes = digest_prime.digest();

            digestB64String = Base64.getEncoder().encodeToString(digest_prime_bytes);
            hash_prime = digestB64String.getBytes();

            // Decipher S with public key of sender to get H back   
            cipher_asym.init(Cipher.DECRYPT_MODE, serverPublicKey);
            hash = cipher_asym.doFinal(signature_bytes);

        }
        catch (Exception ex) {  
            System.err.println("Error Generating Hash From Received Message.\n");  
            return;
        } 


        


        // If recalculated hash H’ is equal to deciphered hash H then 
        // the message was not modified and was sent by the sender  

        if(Arrays.equals(hash, hash_prime)){
            System.out.println("----------------------------------------");
            System.out.println("Integrity and Authentication ensured.");
            System.out.println("----------------------------------------");
            

        }
        else{
            System.out.println("----------------------------------------");
            System.out.println("Message might be corrupted. Do not trust.");
            System.out.println("-----------------------------------------");

        }
        
        
        //----------------------------------------------------- //
        //              [Close Socket and Reset]                //
        //----------------------------------------------------- //

        try{ 
            System.out.println("----------------------------------------");
            System.out.println("Closing Socket...");
            System.out.println("----------------------------------------");
            clientSocket.close();
        }
        catch (Exception ex) {  
            System.err.println("Error Closing Server Socket Communication!\n");  
            return;
        } 


    }

    //----------------------------------------------------- //
    //             [Function To Create a JSON]              //
    //----------------------------------------------------- //
    private static JsonObject createJsonMessage(String userID, String clientRequestString, String proof){
        
        
        JsonParser parser = new JsonParser();
        JsonObject requestJson = parser.parse​("{}").getAsJsonObject();
        {
            JsonObject infoJson = parser.parse​("{}").getAsJsonObject();
            infoJson.addProperty("from", userID);
            requestJson.add("info", infoJson);
            
            infoJson.addProperty("to", "Server");
            requestJson.add("info", infoJson);

            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            infoJson.addProperty("timestamp", timestamp.getTime());
            requestJson.add("info", infoJson);

            infoJson.addProperty("message", clientRequestString);
            requestJson.add("info", infoJson);

            infoJson.addProperty("proof", clientRequestString);
            requestJson.add("info", infoJson);


        }


        System.out.println("-------------------------------------");
        System.out.println("Request message: " + requestJson);
        System.out.println("-------------------------------------");
        
        return requestJson;
        
    }


}
