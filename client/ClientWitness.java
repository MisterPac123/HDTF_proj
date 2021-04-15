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

public class ClientWitness {
    private final static String CIPHER_ASYM = "RSA/ECB/PKCS1Padding";
    private static int witnessPort;
    public ClientWitness(int witnessPort){

        this.witnessPort = witnessPort;

    };




    // =============================================================================== //
    //                           [SIGNATURE GENERATION]
    // =============================================================================== //

    public static String generateSinature(String proof){
        System.out.println("----------------------------------------");
        System.out.println("signProof\n");
        System.out.println(witnessPort);
        System.out.println("I AM A WITNESS AND I WILL SIGN THE PROOF\n");
        System.out.println("----------------------------------------");


        PrivateKey rsaUserPrivKey = null; 


        // ---------------------------------------------------------------- //
        // Keys Are Generated When A client Inits! But is better to Check
        // ---------------------------------------------------------------- //

        String userPrivateKeyDir = "keys/private/" + witnessPort + "/client_priv.key";
        
        
        try{
            rsaUserPrivKey = readPrivateKey(userPrivateKeyDir);            
        }
        catch (Exception ex) {  
            System.err.println("Error Reading Client Private Key.\n"); 
            System.err.println("Check if There is a directory: " + userPrivateKeyDir + " .\n"); 
            System.err.println("Without the Private Key, witness " + witnessPort +
                " can not sign the proof! .\n"); 
            
        }

        //return
        byte[] proofBytes = proof.toString().getBytes();
        String proofBytesB64String = Base64.getEncoder().encodeToString(proofBytes);
        byte[] proofBytesB64Bytes = proofBytesB64String.getBytes();
        
        // Cipher proof with private key
        Cipher sign = null;
        try{
            sign = Cipher.getInstance(CIPHER_ASYM);
            sign.init(Cipher.ENCRYPT_MODE, rsaUserPrivKey);
        }
        catch (Exception ex) {  
            System.err.println("Error Creating Client Signature!\n");  
            System.err.println("Client Priv Key Might Not Be Correct.\n");  
            
        } 

        byte[] signature_bytes = null;
        try{
            signature_bytes = sign.doFinal(proofBytesB64Bytes);
        }
        catch (Exception ex) {  
            System.err.println("Error Converting Client Signature to Bytes.\n");  
            
        } 
        String signatureB64String = Base64.getEncoder().encodeToString(signature_bytes);
        return signatureB64String;
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
    public static PrivateKey readPrivateKey(String privateKeyPath) throws Exception {
        byte[] privEncoded = readFile(privateKeyPath);
        PKCS8EncodedKeySpec privSpec = new PKCS8EncodedKeySpec(privEncoded);
        KeyFactory keyFacPriv = KeyFactory.getInstance("RSA");
        PrivateKey priv = keyFacPriv.generatePrivate(privSpec);
        return priv;
    }

}
