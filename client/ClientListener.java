package client;


import java.io.*;
import java.net.*;
import java.util.Scanner;


public class ClientListener extends Thread {
    private int port;
    private String userID;
    private Socket socket;

    public ClientListener(String userID, int port) {
        this.port = port;
        this.userID=userID;

    }

    public void run() {
        if(addUser(this.userID, this.port)) {
            while (true) {
                try (ServerSocket serverSocket = new ServerSocket(port)) {
                    System.out.println("Client is listening on port " + port);
                    this.socket = serverSocket.accept();
                    System.out.println("client found");

                    InputStream input = this.socket.getInputStream();
                    OutputStream output = this.socket.getOutputStream();
                    BufferedReader receiver = new BufferedReader(new InputStreamReader(input));
                    PrintWriter sender = new PrintWriter(output, true);

                    String message;
                    //message = receiver.readLine();
                    do {
                        message = receiver.readLine();
                        System.out.println("Client:" + message);
                        switch (message) {
                            case "requestLocationProof":
                                if (handleRequestLocationProof(sender)) {
                                    System.out.println("Proof sent");
                                    //submitLocationReport(userId, ep, report, …)
                                    //Specification: user userId submits a location report.
                                }
                                break;
                        }
                    } while (!message.equals("bye"));

                    socket.close();
                } catch (IOException ex) {
                    System.out.println("ClientListener exception: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        }
    }

    //#########################
    //## Secondary functions ##
    //#########################

    public static boolean handleRequestLocationProof(PrintWriter sender){
        try{
            sender.println("Proof");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean addUser(String userID, int port){
        try {//Check if user is already in the file, or if it is with the same port
            boolean newClient = true;
            File portsFile = new File("usersPorts.txt");
            if(portsFile.exists()) {
                Scanner myReader = new Scanner(portsFile);
                while (myReader.hasNextLine()) {
                    String data[] = myReader.nextLine().split(" ");
                    if (data[0].equals(userID) && Integer.parseInt(data[1]) != port) {
                        System.out.println("Error creating user: UserID already exists with port " + data[1]);
                        return false;
                    }
                    if (data[0].equals(userID) && Integer.parseInt(data[1]) == port) {
                        newClient = false;
                    }
                }
                myReader.close();
            }
            if(newClient) {
                FileWriter usersPorts = new FileWriter("usersPorts.txt", true);
                usersPorts.write(userID + " " + port + "\n");
                usersPorts.close();
                System.out.println("Successfully add User.");
                return true;
            }

        } catch (IOException e) {
            System.out.println("ClientListener exception: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
}
