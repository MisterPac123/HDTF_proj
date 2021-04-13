package client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;


public class Map {

    public Map() { }

    public void addPosition(String userID, int epoch, int axisX, int axisY){
        try {
            FileWriter epochFileW = new FileWriter("epoch" + epoch + "Map.txt", true);
            epochFileW.write(userID + " " + axisX + " " + axisY + "\n");
            epochFileW.close();
            System.out.println("Successfully add User.");
        } catch (IOException e) {
            System.out.println("Map exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public int[] getNearbyUsersPorts(String userID, int epoch) {
        String[] ids = new String[20];
        int axisX, axisY, i = 0;
        Scanner myReader;

        try {
            File epochFile = new File("epoch" + epoch + "Map.txt");
            String[] userLocation = userExists(epochFile, userID);
            if(userLocation == null) { System.out.println("User not found in epoch " + epoch +"."); }
            else{
                axisX = Integer.parseInt(userLocation[1]);
                axisY = Integer.parseInt(userLocation[2]);

                myReader = new Scanner(epochFile);
                while (myReader.hasNextLine()) {
                    String[] witnessLocation = myReader.nextLine().split(" ");
                    if (!witnessLocation[0].equals(userID)) {
                        int witness_x = Integer.parseInt(witnessLocation[1]);
                        int witness_y = Integer.parseInt(witnessLocation[2]);
                        double distance = Math.sqrt(Math.pow(witness_x - axisX, 2) + Math.pow(witness_y - axisY, 2));
                        if (distance <= 3) {
                            ids[i] = (witnessLocation[0]);
                            i++;
                        }
                    }
                }
                myReader.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return getPorts(ids);
    }

    public String[] userExists(File file, String userID){
        try {
            Scanner myReader = new Scanner(file);
            while (myReader.hasNextLine()) {
                String[] data = myReader.nextLine().split(" ");
                if (data[0].equals(userID)) {
                    return data;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public int[] getPorts(String[] usersID){
        int[] ports = new int[20];
        int i = 0;

        try {
            File usersPorts = new File("usersPorts.txt");
            Scanner myReader = new Scanner(usersPorts);
            while (myReader.hasNextLine()) {
                String[] data = myReader.nextLine().split(" ");
                for (String user : usersID) {
                    if (data[0].equals(user)) {
                        int port = Integer.parseInt(data[1]);
                        ports[i] = (port);
                        i++;
                    }
                }
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return ports;
    }

}
