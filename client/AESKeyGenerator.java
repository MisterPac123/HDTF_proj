package client;

import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;

public class AESKeyGenerator {
    private static String keyPath;

    public AESKeyGenerator(String keyPath) throws Exception {

        this.keyPath = keyPath;
        //write(keyPath);

        System.out.println("Done.");
    }

    public void write() throws GeneralSecurityException, IOException {
        // get an AES private key
        System.out.println("Generating AES key ..." );
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128);
        Key key = keyGen.generateKey();
        System.out.println( "Finish generating AES key" );
        byte[] encoded = key.getEncoded();
        System.out.println("Key:");
        System.out.println(DataUtils.bytesToHex(encoded));

        System.out.println("Writing key to '" + this.keyPath + "' ..." );

        FileOutputStream fos = new FileOutputStream(this.keyPath);
        fos.write(encoded);
        fos.close();
    }

    public Key read() throws GeneralSecurityException, IOException {
        System.out.println("Reading key from file " + this.keyPath + " ...");
        FileInputStream fis = new FileInputStream(this.keyPath);
        byte[] encoded = new byte[fis.available()];
        fis.read(encoded);
        fis.close();

        return new SecretKeySpec(encoded, 0, 16, "AES");
    }

}
