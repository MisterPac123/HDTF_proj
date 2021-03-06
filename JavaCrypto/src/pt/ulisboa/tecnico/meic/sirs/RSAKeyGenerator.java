package pt.ulisboa.tecnico.meic.sirs;

import javax.crypto.spec.SecretKeySpec;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;

public class RSAKeyGenerator {

    public static void main(String[] args) throws Exception {

        // check args
        if (args.length != 3) {
            System.err.println("Usage: RSAKeyGenerator [r|w] <priv-key-file> <pub-key-file>");
            return;
        }

        final String mode = args[0];
        final String privkeyPath = args[1];
        final String pubkeyPath = args[2];

        if (mode.toLowerCase().startsWith("w")) {
            System.out.println("Generate and save keys");
            write(privkeyPath);
            write(pubkeyPath);
        } else {
            System.out.println("Load keys");
            read(privkeyPath);
            read(pubkeyPath);            
        }

        System.out.println("Done.");
    }

    public static void write(String keyPath) throws GeneralSecurityException, IOException {
        // get an AES private key
        System.out.println("Generating RSA key ..." );
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(1024);
        KeyPair keys = keyGen.generateKeyPair();
        System.out.println("Finish generating RSA keys");
        
        System.out.println("Private Key:");
        PrivateKey privKey = keys.getPrivate();
        byte[] privKeyEncoded = privKey.getEncoded();
        System.out.println(DataUtils.bytesToHex(privKeyEncoded));
        System.out.println("Public Key:");
        PublicKey pubKey = keys.getPublic();
        byte[] pubKeyEncoded = pubKey.getEncoded();
        System.out.println(DataUtils.bytesToHex(pubKeyEncoded));

        System.out.println("Writing Private key to '" + keyPath + "' ..." );
        try (FileOutputStream privFos = new FileOutputStream(keyPath)) {
            privFos.write(privKeyEncoded);
        }
        System.out.println("Writing Pubic key to '" + keyPath + "' ..." );
        try (FileOutputStream pubFos = new FileOutputStream(keyPath)) {
            pubFos.write(pubKeyEncoded);
        }
    }

    public static Key read(String keyPath) throws IOException {
        System.out.println("Reading key from file " + keyPath + " ...");
        byte[] encoded;
        try (FileInputStream fis = new FileInputStream(keyPath)) {
            encoded = new byte[fis.available()];
            fis.read(encoded);
        }

        return new SecretKeySpec(encoded, "RSA");
    }

}
