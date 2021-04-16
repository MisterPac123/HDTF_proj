
package server;
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
import java.text.DateFormat;

import java.util.concurrent.TimeUnit;


import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import java.awt.*;



public class SecureServer extends Thread {
//public class SecureServer{

    private static String serverPrivateKeypath = null;
    private static String serverPublicKeypath = null;
    private static String clientPublicKeypath = null;
    private static String symmetricKeyPath = null; 
    
    
    private static PrivateKey serverPrivateKey = null;
    private static PublicKey serverPublicKey = null;
    private static PublicKey clientPublicKey = null;
    private static Key symmetricKey = null;


    private static ArrayList<Long> timestampsAlreadyUsed = new ArrayList<Long>();
    private ArrayList<String> proofs = new ArrayList<String>();

    private static int clientPort = 0;
    // Not Necessary
    //private static InetAddress serverAddress = null;
    private static int serverPort = 8000;



    final String CIPHER_ASYM = "RSA/ECB/PKCS1Padding";
    final String CIPHER_SYM = "AES/ECB/PKCS5Padding";
    final String DIGEST_ALGO = "SHA-256";




    public SecureServer(int serverPort){
        //this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    };
        
        
    public void run() { 
        while (true){
            //====================================================== //
            //====================================================== //
            //   [Create Start Communication Request Message ]       //
            //====================================================== //
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
            System.out.println("Server is Listening on Port: " + this.serverPort);
            System.out.println("Waiting for Connections");

            System.out.println(
            "=================================================="+ 
            "==================================================\n");
            System.out.println("[Start Communication Request Message]\n");
            System.out.println(
            "=================================================="+ 
            "==================================================\n");

            


            ServerSocket serverSocket = null;
            Socket clientSocket = null;

            try{
                serverSocket = new ServerSocket(this.serverPort);           
            }
            catch (Exception ex) {  
                System.err.println("Error Creating Server Socket.\n");
                return;
            } 

            try{
                clientSocket = serverSocket.accept();   
                System.out.println("Connection Sucessfully Established With Client: " 
                + clientSocket);
        
            }
            catch (Exception ex) {  
                System.err.println("Error Creating Client Socket.\n");
                return;
            } 

             
            String startRequestString = null;
            try{
                DataInputStream inStartRequestFromClient =
                new DataInputStream(clientSocket.getInputStream());    

                int startRequestLength = inStartRequestFromClient.readInt(); 
                byte[] inStartRequestFromClient_B64_bytes = new byte[startRequestLength];  

                for(int i = 0; i < inStartRequestFromClient_B64_bytes.length; i++) {
                    inStartRequestFromClient_B64_bytes[i] = inStartRequestFromClient.readByte();
                }
                
                //----------------------------------------------------- //
                //              [Decode Request JSON ]                  //
                //----------------------------------------------------- //
                
                byte[] inStartRequest_bytes_decoded =  Base64.getMimeDecoder().decode(inStartRequestFromClient_B64_bytes);

                startRequestString = new String(inStartRequest_bytes_decoded);

            }

            catch (Exception ex) {  
                System.err.println("Error Decoding Client Request Message.\n");
                return;
            } 
            //----------------------------------------------------- //
            //                  [Parse CM JSON ]                    //
            //----------------------------------------------------- //
            JsonParser parser = new JsonParser();
            JsonObject startRequestStringJSON = parser.parse​(startRequestString).getAsJsonObject();
            
            String from = null, to = null, timestampReceived = null, startMessage = null;
            String userPort = null;
            {
                JsonObject infoJson = startRequestStringJSON.getAsJsonObject("info");
                timestampReceived = infoJson.get("timestamp").getAsString();
                from = infoJson.get("from").getAsString();
                to = infoJson.get("to").getAsString();
                startMessage = infoJson.get("message").getAsString();
                userPort = infoJson.get("userPort").getAsString();

            }           

            //----------------------------------------------------- //
            //                      [Read keys]                     //
            //----------------------------------------------------- //
            
            this.serverPrivateKeypath = "keys/private/server/server_priv.key";
            this.serverPublicKeypath = "keys/shared/server/server_pub.key";

            //this.clientPublicKeypath = "keys/shared/" + from + "/client_pub.key";
            //this.symmetricKeyPath = "keys/shared/" + from + "/aes.key";

            this.clientPublicKeypath = "keys/shared/" + userPort + "/client_pub.key";
            this.symmetricKeyPath = "keys/shared/" + userPort + "/aes.key";

            try{

                this.serverPrivateKey = readPrivateKey(serverPrivateKeypath);
                this.serverPublicKey = readPublicKey(serverPublicKeypath);
                this.clientPublicKey = readPublicKey(clientPublicKeypath);
                this.symmetricKey = readSecretKey(symmetricKeyPath);
            }

            catch(Exception e){
                System.err.println("Error while reading the keys!");
                return;
            }
           

            //----------------------------------------------------- //
            //          [Reply To Client With an ACK]               //
            //----------------------------------------------------- //

            
            String startReply = "Hi! Lets Start a Secure Communication!.";
            JsonObject startCommunicationReplyJson = parser.parse​("{}").getAsJsonObject();
            {
                JsonObject infoJson = parser.parse​("{}").getAsJsonObject();


                infoJson.addProperty("from", "Server");
                startCommunicationReplyJson.add("info", infoJson);
                
                infoJson.addProperty("to", from);
                startCommunicationReplyJson.add("info", infoJson);

                infoJson.addProperty("userPort", userPort);
                startCommunicationReplyJson.add("info", infoJson);

                Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                infoJson.addProperty("timestamp", timestamp.getTime());
                startCommunicationReplyJson.add("info", infoJson);

                infoJson.addProperty("message", startReply);
                startCommunicationReplyJson.add("info", infoJson);


            }

            
            // Encode request in base64
            byte[] startCommunicationReply_bytes = startCommunicationReplyJson.toString().getBytes();
            String startCommunicationReply_B64String = Base64.getEncoder().encodeToString(startCommunicationReply_bytes);
            byte[] startCommunicationReply_B64 = startCommunicationReply_B64String.getBytes();


            
            try {
                DataOutputStream outToClient = new DataOutputStream(clientSocket.getOutputStream());
                outToClient.writeInt(startCommunicationReply_B64.length); // write length of the message
                outToClient.write(startCommunicationReply_B64);      
            }

            catch (Exception ex) {  
                System.err.println("Error Creating Server Socket. Check if Server is Running Correctly!\n");  
                System.err.println("Could not Send Message to Server Sucessfully.\n");  
                return;
            } 


            //====================================================== //
            //====================================================== //
            //          [Starting Secure Communication]              //
            //====================================================== //
            //====================================================== //

            System.out.println(
            "=================================================="+ 
            "==================================================\n");
            System.out.println("[Starting Secure Communication]\n");

            System.out.println("Server Started A Secure Communication with Client: " 
                + from + ":" + userPort + "\n");
            
            System.out.println(
            "=================================================="+ 
            "==================================================\n");



            //--------------------------------------------------------- //
            // [Create TCP Socket And Receive Client Request Message] //
            //--------------------------------------------------------- //


            String request_string = null;
            try{
                
                DataInputStream inFromClient =
                        new DataInputStream(clientSocket.getInputStream());        

                int len = inFromClient.readInt(); 
                byte[] request_B64_data = new byte[len];
                

                for(int i = 0; i < request_B64_data.length; i++) {
                    request_B64_data[i] = inFromClient.readByte();
                }
                //----------------------------------------------------- //
                //              [Decode Request JSON ]                  //
                //----------------------------------------------------- //
                
                byte[] request_bytes_decoded =  Base64.getMimeDecoder().decode(request_B64_data);
                request_string = new String(request_bytes_decoded);
            }
            catch (Exception ex) {  
                System.err.println("Error Decoding Client Request Message.\n");
                return;
            } 

            //----------------------------------------------------- //
            //              [Parse request JSON ]                   //
            //----------------------------------------------------- //

            //JsonParser parser = new JsonParser();

            JsonObject request = parser.parse​(request_string).getAsJsonObject();
            String ck = null, cm = null, digest = null, signature=null;
            {
                JsonObject confJson = request.getAsJsonObject("confidentiality");
                ck = confJson.get("CK").getAsString();
                cm = confJson.get("CM").getAsString();
                JsonObject iaJson = request.getAsJsonObject("integraty_and_auth");
                signature = iaJson.get("Signature").getAsString();
            }
            



            byte[] ck_bytes =  Base64.getMimeDecoder().decode(ck);
            byte[] cm_bytes =  Base64.getMimeDecoder().decode(cm);
            byte[] signature_bytes =  Base64.getMimeDecoder().decode(signature);

            //----------------------------------------------------- //
            //  • Receiver (Server):                                    //
            //  – Decipher CK with its private key to obtain KM     //
            //  – Decipher CM with key KM to recover M              //
            //----------------------------------------------------- //

            // Decipher CK with its private key to obtain KM    
            Cipher cipher_asym  = null; byte[] ck_bytes_deciphered = null;
            try{
                System.out.println("Deciphering with " + CIPHER_ASYM + "...");
                cipher_asym = Cipher.getInstance(CIPHER_ASYM);
                cipher_asym.init(Cipher.DECRYPT_MODE, this.serverPrivateKey);
                ck_bytes_deciphered = cipher_asym.doFinal(ck_bytes);
            
            }

            catch (Exception ex) {  
                System.err.println("Error Deciphering Client CK.\n");
                return;
            } 

            
            //----------------------------------------------------- //
            //              [Rebuild KM (symmetric key) ]           //
            //----------------------------------------------------- //
            SecretKey km_symmetric = new SecretKeySpec(ck_bytes_deciphered, 0, ck_bytes_deciphered.length, "AES"); 

            //  Decipher CM with key KM to recover M    
            Cipher cipher_sym = null; String message = null;
            byte[] cm_bytes_deciphered  = null;
            try{
                System.out.println("Deciphering with " + CIPHER_ASYM + "...");
                cipher_sym = Cipher.getInstance(CIPHER_SYM);
                cipher_sym.init(Cipher.DECRYPT_MODE, km_symmetric);

                
                cm_bytes_deciphered = cipher_sym.doFinal(cm_bytes);
                message = new String(cm_bytes_deciphered);
                
            }

            catch (Exception ex) {  
                System.err.println("Error Deciphering Client CK.\n");
                return;
            } 

            //----------------------------------------------------- //
            //                  [Parse CM JSON ]                    //
            //----------------------------------------------------- //
            // Proofs Are Sent As Strings But Are Arrays Of JSONs.  //
            // There is an handler for Parsing the proofs Received. //
            //------------------------------------------------------//

            JsonObject cm_json = parser.parse​(message).getAsJsonObject();
            
            from = null; userPort = null; to = null; timestampReceived = null; String clientMessage = null; String proofs = null;
            {
                JsonObject infoJson = cm_json.getAsJsonObject("info");

                from = infoJson.get("from").getAsString();
                to = infoJson.get("to").getAsString();
                //userPort = infoJson.get("userPort").getAsString();

                timestampReceived = infoJson.get("timestamp").getAsString();
                clientMessage = infoJson.get("message").getAsString();
                proofs = infoJson.get("proofs").getAsString();

            }
            
            
            //------------------------------------------------------------- //
            //                  Check freshness of message
            //------------------------------------------------------------- //
            
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            long timestampActual = timestamp.getTime();
            long timestampMessage = Long.parseLong(timestampReceived);

            if (timestampActual - timestampMessage >= 2000 || timestampsAlreadyUsed.contains(timestampMessage)) {
                System.out.printf("Message replayed\n");
                continue;
            }

            else{

                timestampsAlreadyUsed.add(timestampMessage);
                System.out.println("----------------------------------------");
                System.out.println("Freshness ensured.");
                System.out.println("----------------------------------------");
                
            }
            

            //------------------------------------------------------------- //
            // • Receiver (Server):                                         //
            // – Hash received Message (message) and digest it to produce hash H’ //
            // – Decipher S with public key of sender to get H back         //
            // – If recalculated hash H’ is equal to deciphered hash H then //
            // the message was not modified and was sent by the sender      //
            //------------------------------------------------------------- //

            // Hash received Message (message) and digest it to produce hash H’ 
            byte[] hash_prime = null;
            try{
                MessageDigest digest_prime = MessageDigest.getInstance(DIGEST_ALGO);
                digest_prime.update(cm_bytes_deciphered);

                byte[] digest_prime_bytes = digest_prime.digest();

                String digestB64String = Base64.getEncoder().encodeToString(digest_prime_bytes);
                hash_prime = digestB64String.getBytes();

        
            }

            catch (Exception ex) {  
                System.err.println("Error Creating A Digest From The Message Received.\n");
                return;
            } 
            byte[] hash = null;
            try{
                // Decipher S with public key of sender to get H back   
                cipher_asym.init(Cipher.DECRYPT_MODE, this.clientPublicKey);
                hash = cipher_asym.doFinal(signature_bytes);     
            }
            catch (Exception ex) {  
                System.err.println("Error Deciphering Client Signature.\n");
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

            //====================================================== //
            //====================================================== //
            //              [Handler The Request ]                    //
            //====================================================== //
            //====================================================== //

            String[] split = clientMessage.split("\\s+");
            
            //ArrayList<ArrayList<Report>> rep = new ArrayList<ArrayList<Report>>();
            
            String epochF = "epoch";
            boolean fresh_report = false;
            String emessage = "";
            JsonObject replyJson = null;


            byte[] serverData = null;

            switch (split[0]){
                case "submitLocationReport":
                    
                    if(handle_submitLocationReport(proofs)){
                        System.out.println("Proofs Processed Sucessfully");
                        replyJson  = submitLocationReportReply(from, to, userPort);

                    }

                    
                    try{
                        String fileName = epochF+split[2]+".txt";
                        File epochFile = new File(fileName);
                        if(epochFile.exists()){
                            fresh_report = check_free(split[2], split[1], split[3]);
                        }
                        if(fresh_report || epochFile.exists()==false){
                            FileWriter database = new FileWriter(fileName, true);
                            System.out.println(clientMessage);
                            database.write(split[1] + " " + split[3] + "\n");
                            database.close();
                            System.out.println("Report submitted!");
                            emessage = "Report submited!";
                        }
                        else{
                            emessage = "Try next epoch!";
                        }
                    }
                    catch (IOException e) {
                            System.out.println("ClientListener exception: " + e.getMessage());
                            e.printStackTrace();
                    }
                    replyJson = createJsonMessage(from, emessage); 
                    serverData = replyJson.toString().getBytes();
                    break;

                case "obtainLocationReport": //obtainLocationReport john epoch
                    
                    String fileName = epochF+split[2]+".txt";
                    File epochFile = new File(fileName);
                    if(epochFile.exists()){

                        emessage = lookforReport(fileName, from, split[1]);
                    }
                    else{
                        emessage = "Location not available";
                    }
                    
                    replyJson = createJsonMessage(from, emessage); 
                    serverData = replyJson.toString().getBytes();

                    break;

                case "obtainUsersAtLocation"://obtainUsersAtLocation position epoch
                    
                    if(from.startsWith("SU_")){
                        String fileName2 = epochF+split[2]+".txt";
                        emessage = lookforUsers(fileName2, split[1]);
                    }
                    else{
                        emessage = "Permission denied!";
                    }
                    replyJson = createJsonMessage(from, emessage); 
                    serverData = replyJson.toString().getBytes();
                    
                break;

                default:
                    System.out.println("----------------------------------------");
                    System.out.println("An Error Occured Communication With The Server.\n");
                    System.out.println("Server Can Not Understand Your Request " + 
                        clientMessage + " \n");
                    System.out.println("----------------------------------------");
                break;

            }



            //====================================================== //
            //====================================================== //
            //              [Reply To Server's Request]              //
            //====================================================== //
            //====================================================== //




            System.out.println("-------------------------------------");
            System.out.println("Response message: " + replyJson);
            System.out.println("-------------------------------------");
            //byte[] serverData = replyJson.toString().getBytes();

            //--------------------------------------------------------- //
            //              [Cipher Data: Confidentiality]              //
            // • Sender (Server):                                   //
            //  – Generate a random symmetric key KM                    //
            //  – Cipher message M with key KM to produce cryptogram CM //
            //  – Cipher key KM with receiver’s public Key  to produce  //
            //  cryptogram CK                                           //
            //  – Send CK and CM                                        //
            //--------------------------------------------------------- //

            Cipher cipher_km = null;
            try{
                // Generate a random symmetric key KM
                cipher_km = Cipher.getInstance(CIPHER_SYM);
                cipher_km.init(Cipher.ENCRYPT_MODE, this.symmetricKey);

            }
            catch (Exception ex) {  
                System.err.println("Error Generate a random symmetric key KM .\n");
                return;
            }
            byte[] server_cm_bytes = null;
            try{
                // Cipher message M with key KM to produce cryptogram CM 
                server_cm_bytes = cipher_km.doFinal(serverData);
            }
            catch (Exception ex) {  
                System.err.println("Error Ciphering message M with key KM to produce cryptogram CM .\n");
                return;
            }
            
            byte[] server_ck_bytes = null;
            try{
                // Cipher key KM with receiver’s public Key  to produce 
                // cryptogram CK 
                Cipher cipher_rpk = Cipher.getInstance(CIPHER_ASYM);
                cipher_rpk.init(Cipher.ENCRYPT_MODE, this.clientPublicKey);
                server_ck_bytes = cipher_rpk.doFinal(this.symmetricKey.getEncoded());
            }

            catch (Exception ex) {  
                System.err.println("Error Ciphering KM with Client Public Key To Produce CM.\n");
                return;
            }

            // Encode CM in base64
            String cm_String = cm_bytes.toString();
            String cm_B64String = Base64.getEncoder().encodeToString(server_cm_bytes);
            byte[] cm_B64 = cm_B64String.getBytes();


            // Encode KM in base64

            String ck_String = ck_bytes.toString();
            String ck_B64String = Base64.getEncoder().encodeToString(server_ck_bytes);
            byte[] ck_B64 = ck_B64String.getBytes();

                    

            //--------------------------------------------------------- //
            //      [Cipher Data: Integrity and Authentication ]        //
            // • Sender (Server):                                   //
            //  – Digest message M to produce hash H                    //
            //  – Cipher hash H with private key of Sender              //
            //    to produce signature S                                //
            //  – Send M and S                                          //
            //--------------------------------------------------------- //
            byte[] digestB64bytes = null;
            try{
                // Digest message M to produce hash H 

                MessageDigest messageDigest = MessageDigest.getInstance(DIGEST_ALGO);
                messageDigest.update(serverData);
                byte[] digestBytes = messageDigest.digest();
                //System.out.println("digestBytes length " + digestBytes.length);

                // Encode digest in base64
                String serverDigestB64String = Base64.getEncoder().encodeToString(digestBytes);
                digestB64bytes = serverDigestB64String.getBytes();
            }   
            catch (Exception ex) {  
                System.err.println("Error Digest message M to produce hash H.\n");
                return;
            }
            String signatureB64String = null;
            try{
                // Cipher hash H with private key of Sender to produce signature S 

                Cipher sign = Cipher.getInstance(CIPHER_ASYM);
                sign.init(Cipher.ENCRYPT_MODE, this.serverPrivateKey);


                byte[] server_signature_bytes = sign.doFinal(digestB64bytes);
                signatureB64String = Base64.getEncoder().encodeToString(server_signature_bytes);            
            }

            catch (Exception ex) {  
                System.err.println("Error Ciphering hash H with private key of Sender to produce signature S .\n");
                return;
            }
                    

            //------------------------------------------------------------  //
            //          [Create Response Message With CK, CM, Signature]    //
            //------------------------------------------------------------  //


            JsonObject response = parser.parse​("{}").getAsJsonObject();
            {
                JsonObject infoJson = parser.parse​("{}").getAsJsonObject();

                infoJson.addProperty("CK", ck_B64String);
                response.add("confidentiality", infoJson);

                infoJson.addProperty("CM", cm_B64String);
                response.add("confidentiality", infoJson);

                infoJson.addProperty("Signature", signatureB64String);
                response.add("integraty_and_auth", infoJson);
            }

            // Encode response in base64

            byte[] response_bytes = response.toString().getBytes();

            String response_B64String = Base64.getEncoder().encodeToString(response_bytes);
            byte[] response_B64 = response_B64String.getBytes();

            

            
            //----------------------------------------------------- //
            //          [Create Socket and Send Response ]      //
            //----------------------------------------------------- //
            
            try{
                DataOutputStream outToClient =
                        new DataOutputStream(clientSocket.getOutputStream()); //send byte array with changes back to the client

                outToClient.writeInt(response_B64.length); // write length of the message
                outToClient.write(response_B64);           // write the message             
            }

            catch (Exception ex) {  
                System.err.println("Error Sending The Reply To Client.\n");
                return;
            } 

        
            
            
            //----------------------------------------------------- //
            //              [Close Sockets and Reset]               //
            //----------------------------------------------------- //

            System.out.println("----------------------------------------");
            System.out.println("Closing Sockets");
            System.out.println("----------------------------------------");
            
            try{
                serverSocket.close();
            }
            catch (Exception ex) {  
                System.err.println("Error Closing Server Socket.\n");
                return;
            } 


            try{
                serverSocket.close();
            }

            catch (Exception ex) {  
                System.err.println("Error Closing Server Socket.\n");
                return;
            } 
        }
        
            
    };






    //----------------------------------------------------- //
    //                  [Request Handlers]                  //
    //----------------------------------------------------- //

    public boolean handle_submitLocationReport(String proofs){

        System.out.println("----------------------------------------");
        System.out.println("Processing Proofs Validity...\n");


        //----------------------------------------------------- //
        //       [Decode B64 String Proofs To Obtain Byte Array]     //
        //----------------------------------------------------- //
        
        byte[] proofBytes =  Base64.getMimeDecoder().decode(proofs);
        String proofArrayString = new String(proofBytes);
        // Remove "[" and "]"
        String sub = proofArrayString.substring( 1, proofArrayString.length() - 1 );
        // The split space is important to properly parse the array!
        String[] proofArray = sub.split(", ");

        ArrayList<String> proofArrayList = new ArrayList<String>(Arrays.asList(proofArray));

        //----------------------------------------------------------------- //
        // The proof Array List its an array of Strings                     //
        // but this strings are actually JSONs!                             //
        // ArrayList proofs = [proofJSON1, proofJSON2, ... , proofJSONn]    //
        //----------------------------------------------------------------- //
        //  proofJSON1 = {                                                  //
        //          "from" : witnessPort,                                   //
        //          "signature" : witnessSignature                          //
        //  }                                                               //
        //----------------------------------------------------------------- //

        //----------------------------------------------------- //
        //              [Decode B64 String JSONs]               //
        //----------------------------------------------------- //
        for(int i = 0; i < proofArrayList.size(); i++) {
            //System.out.println(i + "->" + proofArrayList.get(i));
            byte[] jsonBytes =  Base64.getMimeDecoder().decode(proofArrayList.get(i));
            String jsonBytesString = new String(jsonBytes);
            //System.out.println(jsonBytesString);

            //----------------------------------------------------- //
            //                  [Parse JSON Proof]                    //
            //----------------------------------------------------- //
            String witness = null, signature = null;
            JsonParser parser = new JsonParser();
            JsonObject jsonProof = parser.parse​(jsonBytesString).getAsJsonObject();

            {
                JsonObject infoJson = jsonProof.getAsJsonObject("info");
                witness = infoJson.get("witness").getAsString();
                signature = infoJson.get("responseLocationProof").getAsString();

            }



            // ---------------------------------------------------------------- //
            // Server knows all client public keys
            // ---------------------------------------------------------------- //
            PublicKey witnessPublicKey = null; 

            String witnessPublicKeyPath = "keys/shared/" + witness + "/client_pub.key";
            
            
            try{
                witnessPublicKey = readPublicKey(witnessPublicKeyPath);            
            }
            catch (Exception ex) {  
                System.err.println("Error Reading Witness Public Key.\n"); 
                System.err.println("Check if There is a directory: " + witnessPublicKeyPath + " .\n"); 
                System.err.println("Without the Witness" + witness + 
                    "Public Key, The Server can not Validate The proof.\n"); 
                
            }

            
           
            //byte[] signatureBytes = signature.getBytes();
            byte[] proofDecipheredBytesB64 = null;
            String proofDecipheredStringB64 = null;

            byte[] proofDecipheredBytes = null;
            String proofDecipheredString = null;
            Cipher cipher_asym  = null;

            byte[] signatureBytes =  Base64.getMimeDecoder().decode(signature);
            try{
                // Decipher S with public key of sender to get H back 
                cipher_asym = Cipher.getInstance(CIPHER_ASYM);  
                cipher_asym.init(Cipher.DECRYPT_MODE, witnessPublicKey);

                proofDecipheredBytesB64 = cipher_asym.doFinal(signatureBytes);     
                proofDecipheredStringB64 = new String(proofDecipheredBytesB64);
                
                proofDecipheredBytes =  Base64.getMimeDecoder().decode(proofDecipheredStringB64);
                proofDecipheredString = new String(proofDecipheredBytes);


            
                
            }
            catch (Exception ex) {  
                System.err.println("Error Deciphering Client Signature. Data Tampered.\n");
            }

            System.out.println("-------------------------------------");
            System.out.println("Proof Received");
            System.out.println("witness: " + witness);
            System.out.println("signature: " + signature);
            System.out.println("proof: " + proofDecipheredString);
            System.out.println("proofB64: " + proofDecipheredStringB64);
            System.out.println("-------------------------------------");
                
        }

        return true;
    }
    public JsonObject submitLocationReportReply(String from, String to, String toPort){
        //----------------------------------------------------- //
        //              [Create Response Message ]              //
        // Might be useful to have a funcion to search for a    //
        // file in a database or maybe a test file is be enough //
        //----------------------------------------------------- //
        
        JsonParser parser = new JsonParser();
        
        JsonObject replyJson = parser.parse​("{}").getAsJsonObject();
        {
            JsonObject infoJson = parser.parse​("{}").getAsJsonObject();
            infoJson.addProperty("from", to);
            replyJson.add("info", infoJson);

            //infoJson.addProperty("userPort", toPort);
            //replyJson.add("info", infoJson);

            infoJson.addProperty("to", from);
            replyJson.add("info", infoJson);

            Timestamp current_timestamp = new Timestamp(System.currentTimeMillis());
            infoJson.addProperty("timestamp", current_timestamp.getTime());
            replyJson.add("info", infoJson);

            infoJson.addProperty("message", "Hi!!");
            replyJson.add("info", infoJson);

            //infoJson.addProperty("proof", proof);
            //replyJson.add("info", infoJson);
        }
        return replyJson;
    }


    public static boolean check_free(String epoch, String prover, String pos){
        String fileName = "epoch"+epoch+".txt";
        try{
            File epochFile = new File(fileName);
            Scanner myReader = new Scanner(epochFile);
            while (myReader.hasNextLine()) {
                String data[] = myReader.nextLine().split(" ");
                if (data[0].equals(prover) && data[1] != pos) {
                    return false;
                }
            }
            myReader.close();
        } catch (IOException e) {
        System.out.println("ClientListener exception: " + e.getMessage());
        e.printStackTrace();
        }
        return true;
    }        
    
    public static String lookforReport(String filename, String prover, String user){
        String fileName = filename;
        try{
            File epochFile = new File(fileName);
            Scanner myReader = new Scanner(epochFile);
            while (myReader.hasNextLine()) {
                String data[] = myReader.nextLine().split(" ");
                if (user.equals(prover) || prover.startsWith("SU_")) {
                    return user + " " +data[1];
                }
            }
            myReader.close();
        } catch (IOException e) {
        System.out.println("ClientListener exception: " + e.getMessage());
        e.printStackTrace();
        }
        return "Report not found / Access Denied";
    }
    public static String lookforUsers(String filename, String position){
        String fileName = filename;
        String res = "";
        try{
            File epochFile = new File(fileName);
            Scanner myReader = new Scanner(epochFile);
            while (myReader.hasNextLine()) {
                String data[] = myReader.nextLine().split(" ");
                if (data[1].equals(position)) {
                    res = res + data[0]+ ", ";
                }
            }
            myReader.close();
        } catch (IOException e) {
        System.out.println("ClientListener exception: " + e.getMessage());
        e.printStackTrace();
        }
        return res;
    }


    //----------------------------------------------------- //
    //              [Key Reading Functions]                 //
    //----------------------------------------------------- //
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
        System.out.println("Reading public key from file " + publicKeyPath + " ...");
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

    //----------------------------------------------------- //
    //             [Function To Create a JSON]              //
    //----------------------------------------------------- //
    private static JsonObject createJsonMessage(String userID, String serverMessage){
        
        
        JsonParser parser = new JsonParser();
        JsonObject requestJson = parser.parse​("{}").getAsJsonObject();
        {
            JsonObject infoJson = parser.parse​("{}").getAsJsonObject();
            infoJson.addProperty("from", "Server");
            requestJson.add("info", infoJson);
            
            infoJson.addProperty("to", userID);
            requestJson.add("info", infoJson);

            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            infoJson.addProperty("timestamp", timestamp.getTime());
            requestJson.add("info", infoJson);

            infoJson.addProperty("message", serverMessage);
            requestJson.add("info", infoJson);

            infoJson.addProperty("proof", "");
            requestJson.add("info", infoJson);

        }


        System.out.println("-------------------------------------");
        System.out.println("Request message: " + requestJson);
        System.out.println("-------------------------------------");
        
        return requestJson;
        
    }


};
