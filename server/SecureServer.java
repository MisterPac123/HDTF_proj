
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



public class SecureServer{


	private static ArrayList<Long> timestampsAlreadyUsed = new ArrayList<Long>();

	public static void main(String[] args) throws IOException, Exception{
		//-------------------------------------------------------------- //
		//mvn compile exec:java -Dmainclass=pt.tecnico.SecureServer
		// -Dexec.args="8000 keys/bob.privkey keys/bob.pubkey keys/alice.pubkey keys/secret.key"
		//-------------------------------------------------------------- //

		if (args.length < 5) {

			System.err.println("Argument(s) missing!");
			System.err.printf("Usage: java %s serverPort %s serverPrivateKey %s serverPublicKey %s clientPublicKey %s", SecureServer.class.getName());
			return;
		}

		int clientPort = 0;
		InetAddress serverAddress = null;

		final int serverPort = Integer.parseInt(args[0]);
		final String serverPrivateKeypath = args[1];
		final String serverPublicKeypath = args[2];
		final String clientPublicKeypath = args[3];
		final String symmetricKeyPath = args[4];

		final String CIPHER_ASYM = "RSA/ECB/PKCS1Padding";
		final String CIPHER_SYM = "AES/ECB/PKCS5Padding";
		final String DIGEST_ALGO = "SHA-256";

		
		
       	PrivateKey serverPrivateKey = null;
        PublicKey serverPublicKey = null;
        PublicKey clientPublicKey = null;
        Key symmetricKey = null;



	 	//----------------------------------------------------- //
		// 						[Read keys]						//
		//----------------------------------------------------- //
		try{

	       	serverPrivateKey = readPrivateKey(serverPrivateKeypath);
	        serverPublicKey = readPublicKey(serverPublicKeypath);
	        clientPublicKey = readPublicKey(clientPublicKeypath);
	        symmetricKey = readSecretKey(symmetricKeyPath);
	    }

	  	catch(Exception e){
	    	System.err.println("Error while reading the keys!");
	    }



 		//----------------------------------------------------- //
		// 		    	[Receive Bytes of Data]		     	//
		//----------------------------------------------------- //
		// Wait for client packets 
		
		
		
		// May have to fix this with a Queue of concurrent client requests
		while (true){

			


	 		//--------------------------------------------------------- //
			// [Create TCP Socket And Receive Hospital Request Message] //
			//--------------------------------------------------------- //

			System.out.println("Server is Listening on Port:\n"
			+ " =================================================\n"
			+ " =================================================\n");

			System.out.println("Server is Listening on Port: " + serverPort);
            System.out.println("Waiting for Connections");
            ServerSocket serverSocket = new ServerSocket(serverPort);
            Socket clientSocket = serverSocket.accept();
            System.out.println("Connection Sucessfully Established With Client: " 
            	+ clientSocket);

            
            DataInputStream inFromClient =
                    new DataInputStream(clientSocket.getInputStream());        

            int length = inFromClient.readInt(); 
            byte[]request_B64_data = new byte[length];
            for(int i = 0; i < request_B64_data.length; i++) {
                request_B64_data[i] = inFromClient.readByte();
            }
            System.out.println("inFromClient" + inFromClient.toString());


			
			//----------------------------------------------------- //
			//				[Decode Request JSON ] 					//
			//----------------------------------------------------- //
			
			byte[] request_bytes_decoded =  Base64.getMimeDecoder().decode(request_B64_data);
			String request_string = new String(request_bytes_decoded);

	 		//----------------------------------------------------- //
			// 		    	[Parse request JSON ]	    			//
			//----------------------------------------------------- //

			JsonParser parser = new JsonParser();

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
			//	• Receiver (Server):									//
			//	– Decipher CK with its private key to obtain KM		//
			//	– Decipher CM with key KM to recover M 				//
			//----------------------------------------------------- //

			// Decipher CK with its private key to obtain KM	
			
			
	    	System.out.println("Deciphering with " + CIPHER_ASYM + "...");
	    	Cipher cipher_asym = Cipher.getInstance(CIPHER_ASYM);
	    	cipher_asym.init(Cipher.DECRYPT_MODE, serverPrivateKey);

	    	
	    	byte[] ck_bytes_deciphered = cipher_asym.doFinal(ck_bytes);
	    	
	    	String ck_bytes_deciphered_string = new String(ck_bytes_deciphered);
	    	
			//----------------------------------------------------- //
			// 		    	[Rebuild KM (symmetric key) ]	    	//
			//----------------------------------------------------- //
			SecretKey km_symmetric = new SecretKeySpec(ck_bytes_deciphered, 0, ck_bytes_deciphered.length, "AES"); 

			//  Decipher CM with key KM to recover M 	
			
			
	    	System.out.println("Deciphering with " + CIPHER_ASYM + "...");
	    	Cipher cipher_sym = Cipher.getInstance(CIPHER_SYM);
	    	cipher_sym.init(Cipher.DECRYPT_MODE, km_symmetric);

	    	
	    	byte[] cm_bytes_deciphered = cipher_sym.doFinal(cm_bytes);
	    	String message = new String(cm_bytes_deciphered);
	    	
	    	System.out.println("----------------------------------------");
	    	System.out.println("Deciphered data Message:" + message);
			System.out.println("Confidentiality ensured.");
	    	System.out.println("----------------------------------------");


	 		//----------------------------------------------------- //
			// 		    		[Parse CM JSON ]	    			//
			//----------------------------------------------------- //

			JsonObject cm_json = parser.parse​(message).getAsJsonObject();
			
			String from = null, to = null, timestampReceived = null, clientMessage = null;
			{
				JsonObject infoJson = cm_json.getAsJsonObject("info");

				from = infoJson.get("from").getAsString();
				to = infoJson.get("to").getAsString();

				timestampReceived = infoJson.get("timestamp").getAsString();
				clientMessage = infoJson.get("message").getAsString();

			}
			
			System.out.println("-------------------------------------");
        	System.out.println("Json Received From Client: ");
        	System.out.println("from: " + from);
        	System.out.println("to: " + to);
        	System.out.println("timestamp: " + timestampReceived);
        	System.out.println("message: " + clientMessage);
        	System.out.println("-------------------------------------");
			
	    	//------------------------------------------------------------- //
	    	//					Check freshness of message
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
			// • Receiver (Server):											//
			// – Hash received Message (message) and digest it to produce hash H’ //
			// – Decipher S with public key of sender to get H back			//
			// – If recalculated hash H’ is equal to deciphered hash H then //
			// the message was not modified and was sent by the sender 		//
	    	//------------------------------------------------------------- //

	        // Hash received Message (message) and digest it to produce hash H’ 
	    	
	        MessageDigest digest_prime = MessageDigest.getInstance(DIGEST_ALGO);
	        digest_prime.update(cm_bytes_deciphered);

	        byte[] digest_prime_bytes = digest_prime.digest();

	        String digestB64String = Base64.getEncoder().encodeToString(digest_prime_bytes);
	        byte[] hash_prime = digestB64String.getBytes();

	        // Decipher S with public key of sender to get H back	
	    	cipher_asym.init(Cipher.DECRYPT_MODE, clientPublicKey);
	    	byte[] hash = cipher_asym.doFinal(signature_bytes);
	    	


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
	    	
	    	
            switch (clientMessage){
                case "requestLocationProof":
                    System.out.println("received proof request from user");
                    //if (handle_requestLocationProof(sender)){
                        //submitLocationReport(userId, ep, report, …)
                        //Specification: user userId submits a location report.re
                    //};
                case "Hello!!":
                	System.out.println("----------------------------------------");
                	System.out.println("received Hello from user");
                	System.out.println("----------------------------------------");
                break;
            }

	    	//----------------------------------------------------- //
			// 		    		[Send Message Back]			     	//
			//----------------------------------------------------- //
	    	
	    	System.out.println(
			" =================================================\n"
			+ " =================================================\n");
	    	System.out.println("SERVER -> CLIENT \n");
	    	System.out.println(
			" =================================================\n"
			+ " =================================================\n");


	    	//----------------------------------------------------- //
			//				[Create Response Message ] 				//
			// Might be useful to have a funcion to search for a 	//
			// file in a database or maybe a test file is be enough //
			//----------------------------------------------------- //
	    	


			JsonObject replyJson = parser.parse​("{}").getAsJsonObject();
			{
				JsonObject infoJson = parser.parse​("{}").getAsJsonObject();
				infoJson.addProperty("from", "Server");
				replyJson.add("info", infoJson);
				
				infoJson.addProperty("to", "Client");
				replyJson.add("info", infoJson);

				Timestamp current_timestamp = new Timestamp(System.currentTimeMillis());
            	infoJson.addProperty("timestamp", current_timestamp.getTime());
            	replyJson.add("info", infoJson);

            	infoJson.addProperty("message", "Hi!!");
            	replyJson.add("info", infoJson);

			}

			System.out.println("-------------------------------------");
			System.out.println("Response message: " + replyJson);
			System.out.println("-------------------------------------");
			byte[] serverData = replyJson.toString().getBytes();

			//--------------------------------------------------------- //
			// 				[Cipher Data: Confidentiality]				//
			// • Sender (Server):									//
			// 	– Generate a random symmetric key KM					//
			// 	– Cipher message M with key KM to produce cryptogram CM //
			// 	– Cipher key KM with receiver’s public Key  to produce  //
			//	cryptogram CK 											//
			//	– Send CK and CM 										//
			//--------------------------------------------------------- //




			// Generate a random symmetric key KM
	        // Cipher message M with key KM to produce cryptogram CM 

	        Cipher cipher_km = Cipher.getInstance(CIPHER_SYM);
	        cipher_km.init(Cipher.ENCRYPT_MODE, symmetricKey);
	        byte[] server_cm_bytes = cipher_km.doFinal(serverData);
	        System.out.println("---------------------------------------");
	        //System.out.println("Cipher message serverData with key KM to produce cryptogram CM");
	        //System.out.println("serverData length " + serverData.length);
	        //System.out.println("cm_bytes length " + server_cm_bytes.length);
	        //System.out.println("---------------------------------------");

	        // Cipher key KM with receiver’s public Key  to produce 
	        // cryptogram CK 

	        Cipher cipher_rpk = Cipher.getInstance(CIPHER_ASYM);
	        cipher_rpk.init(Cipher.ENCRYPT_MODE, clientPublicKey);
	        byte[] server_ck_bytes = cipher_rpk.doFinal(symmetricKey.getEncoded());
	        //System.out.println("---------------------------------------");
	        //System.out.println("Cipher key KM with receiver’s public Key  to produce cryptogram CK ");
	        //System.out.println("server_ck_bytes length " + server_ck_bytes.length);
	        //System.out.println("---------------------------------------");
	        

	        // Encode CM in base64
	        String cm_String = cm_bytes.toString();
	        String cm_B64String = Base64.getEncoder().encodeToString(server_cm_bytes);
	        byte[] cm_B64 = cm_B64String.getBytes();

	    	//System.out.println("---------------------------------------");
	        //System.out.println("Encode CM in base64 ");
	        //System.out.println("cm_B64 length " + cm_B64.length);
	        //System.out.println("---------------------------------------");

	        // Encode KM in base64

	        String ck_String = ck_bytes.toString();
	        String ck_B64String = Base64.getEncoder().encodeToString(server_ck_bytes);
	        byte[] ck_B64 = ck_B64String.getBytes();

	        //System.out.println("---------------------------------------");
	        //System.out.println("Encode KM in base64 ");
	        //System.out.println("ck_B64 length " + ck_B64.length);
	        //System.out.println("---------------------------------------");
	        

	    	

			//--------------------------------------------------------- //
			// 		[Cipher Data: Integrity and Authentication ]		//
			// • Sender (Server):									//
			// 	– Digest message M to produce hash H 					//
			// 	– Cipher hash H with private key of Sender 				//
			// 	  to produce signature S 							 	//
			//	– Send M and S											//
			//--------------------------------------------------------- //
			
				
			// Digest message M to produce hash H 
	        //System.out.println("---------------------------------------");
	        //System.out.println("Digest message M to produce hash H ");
	        MessageDigest messageDigest = MessageDigest.getInstance(DIGEST_ALGO);
	        messageDigest.update(serverData);
	        byte[] digestBytes = messageDigest.digest();
	        //System.out.println("digestBytes length " + digestBytes.length);

			// Encode digest in base64
	        String serverDigestB64String = Base64.getEncoder().encodeToString(digestBytes);
	        byte[] digestB64bytes = serverDigestB64String.getBytes();
	        //System.out.println("digestB64bytes length " + digestB64bytes.length);
	        //System.out.println("---------------------------------------");

			// Cipher hash H with private key of Sender to produce signature S 

	        Cipher sign = Cipher.getInstance(CIPHER_ASYM);
	        sign.init(Cipher.ENCRYPT_MODE, serverPrivateKey);


	        byte[] server_signature_bytes = sign.doFinal(digestB64bytes);
	        String signatureB64String = Base64.getEncoder().encodeToString(server_signature_bytes);
	        //System.out.println("---------------------------------------");
	        //System.out.println("server_signature_bytes length " + server_signature_bytes.length);
	        //System.out.println("server_signature_B64bytes length " + signatureB64String.getBytes().length);
			//System.out.println("---------------------------------------");			

        	//------------------------------------------------------------ 	//
			//			[Create Response Message With CK, CM, Signature]	//
			//------------------------------------------------------------ 	//


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
			//System.out.println("response_bytes length " + response_bytes.length);

			String response_B64String = Base64.getEncoder().encodeToString(response_bytes);
	        byte[] response_B64 = response_B64String.getBytes();
	        //System.out.println("response_B64 length " + response_B64.length);

	        

	        
	        //----------------------------------------------------- //
			//			[Create Socket and Send Response ] 		//
			//----------------------------------------------------- //
			


			DataOutputStream outToClient =
                    new DataOutputStream(clientSocket.getOutputStream()); //send byte array with changes back to the client

            outToClient.writeInt(response_B64.length); // write length of the message
            outToClient.write(response_B64);           // write the message

            System.out.println("Test Response Sent To Client.");
		

			
	        //----------------------------------------------------- //
			//				[Close Sockets and Reset] 				//
			//----------------------------------------------------- //

			System.out.println("----------------------------------------");
    		System.out.println("Closing Sockets");
    		System.out.println("----------------------------------------");
			
			
			serverSocket.close();
			clientSocket.close();
        


	    } //while
		    
	    
		
		
		
	} //main



    //----------------------------------------------------- //
	//				[Key Reading Functions] 				//
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
	//					[Request Handlers] 					//
	//----------------------------------------------------- //

    public static boolean handle_requestLocationProof(PrintWriter sender){
        try{
            sender.println("Proof");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }


}
