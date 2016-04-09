package com.liusu.nspmain;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.Buffer;
import java.util.ArrayList;

/**
 * SecClient.java
 * Created by liusu on 5/4/16.
 */
public class SecClient {
    public static void main(String args[]){
        String hostName = "localhost";
        int portNumber = 4321;
        Socket clientSocket;
        PrintWriter printWriter;
        BufferedReader bufferedReader;
        File file = new File("");
        String message;
        boolean authenticationSuccessful;
        ArrayList<String> fileInput;
        System.out.println("Hello World");
        try{
            /**
             * Establish Connection
             */
            clientSocket = new Socket(hostName, portNumber);
            printWriter = new PrintWriter(clientSocket.getOutputStream(),true);
            bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            /**
             * Authentication
             */
            printWriter.println("Hello SecStore, please prove your identity");
            message = bufferedReader.readLine();
            printWriter.println("Give me your certificate signed by CA");
            getServerPublicKey();
            if(verifyIdentity()){
                printWriter.println("Authentication successful, start the transmission");
                authenticationSuccessful = true;
            }else{
                printWriter.println("Bye!");
                authenticationSuccessful = false;
            }

            /**
             * Transmission CP-1
             */
            if(authenticationSuccessful){
                fileInput = readFile(file);
                for(String i: fileInput){
                    printWriter.println(encryptWithPublicKey());
                }
                printWriter.println("Transmission Finished");
            }

            /**
             * Transmission CP-2
             */
            if(authenticationSuccessful){
                fileInput = readFile(file);
                generateSessionKey();

                printWriter.println(encryptWithPublicKey());

                for(String i: fileInput){
                    printWriter.println(encryptWithSesstionKey());
                }
                printWriter.println("Transmission Finished");
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public static ArrayList<String> readFile(File file){
        return null;
    }

    public static void getServerPublicKey(){}
    public static boolean verifyIdentity(){
        return true;
    }
    public static byte[] encryptWithPublicKey(){
        return null;
    }
    public static void generateSessionKey(){}
    public static byte[] encryptWithSesstionKey(){
        return null;
    }
}
