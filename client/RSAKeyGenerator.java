package client;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class RSAKeyGenerator {

    private static String privateKeyPath;
    private static String publicKeyPath;

    public RSAKeyGenerator(String publicKeyPath, String privateKeyPath) throws Exception {
        this.publicKeyPath = publicKeyPath;
        this.privateKeyPath = privateKeyPath;
    }

    public void write() throws GeneralSecurityException, IOException {
        // get an AES private key
        System.out.println("Generating RSA key ..." );
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(1024);
        KeyPair keys = keyGen.generateKeyPair();
        
        System.out.println("Private Key:");
        PrivateKey privKey = keys.getPrivate();
        byte[] privKeyEncoded = privKey.getEncoded();
        System.out.println(DataUtils.bytesToHex(privKeyEncoded));
        System.out.println("Public Key:");
        PublicKey pubKey = keys.getPublic();
        byte[] pubKeyEncoded = pubKey.getEncoded();
        System.out.println(DataUtils.bytesToHex(pubKeyEncoded));

        System.out.println("Writing Private key to '" + this.privateKeyPath + "' ..." );
        FileOutputStream privFos = new FileOutputStream(this.privateKeyPath);
        privFos.write(privKeyEncoded);
        privFos.close();
        System.out.println("Writing Pubic key to '" + this.publicKeyPath + "' ..." );
        FileOutputStream pubFos = new FileOutputStream(this.publicKeyPath);
        pubFos.write(pubKeyEncoded);
        pubFos.close();        
    }

    public KeyPair read() throws GeneralSecurityException, IOException {
        System.out.println("Reading public key from file " + this.publicKeyPath + " ...");
        FileInputStream pubFis = new FileInputStream(this.publicKeyPath);
        byte[] pubEncoded = new byte[pubFis.available()];
        pubFis.read(pubEncoded);
        pubFis.close();

        X509EncodedKeySpec pubSpec = new X509EncodedKeySpec(pubEncoded);
        KeyFactory keyFacPub = KeyFactory.getInstance("RSA");
        PublicKey pub = keyFacPub.generatePublic(pubSpec);

        System.out.println("Reading private key from file " + this.privateKeyPath + " ...");
        FileInputStream privFis = new FileInputStream(this.privateKeyPath);
        byte[] privEncoded = new byte[privFis.available()];
        privFis.read(privEncoded);
        privFis.close();

        PKCS8EncodedKeySpec privSpec = new PKCS8EncodedKeySpec(privEncoded);
        KeyFactory keyFacPriv = KeyFactory.getInstance("RSA");
        PrivateKey priv = keyFacPriv.generatePrivate(privSpec);

        KeyPair keys = new KeyPair(pub, priv);
        return keys;
    }
}
