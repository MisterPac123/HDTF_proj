package server;

import javax.swing.*;
import java.io.*;
import java.net.*;
import java.net.*;

/**
 * This thread is responsible to handle client connection.
 *
 * @author www.codejava.net
 */
public class ServerThrd extends Thread {
    private Socket socket;

    public ServerThrd(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        try {
            InputStream input = socket.getInputStream();
            OutputStream output = socket.getOutputStream();
            BufferedReader receiver = new BufferedReader(new InputStreamReader(input));
            PrintWriter sender = new PrintWriter(output, true);

            String message;

            do {
                message = receiver.readLine();
                System.out.println("Client:" + message);
                switch (message){
                    case "requestLocationProof":
                        System.out.println("received proof request from user");
                        if (handle_requestLocationProof(sender)){
                            //submitLocationReport(userId, ep, report, …)
                            //Specification: user userId submits a location report.re
                        };
                    break;
                }



            } while (!message.equals("bye"));

            socket.close();
        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

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
