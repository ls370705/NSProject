package com.liusu.nspmain;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;

import javax.crypto.Cipher;

/**
 * SecStore.java
 * Created by liusu on 5/4/16.
 */
public class SecStore {
    public static void main(String args[]){
        PublicKey publicKey;
        PrivateKey privateKey;
        PrintWriter printWriter;
        BufferedReader bufferedReader;
        String message;
        String contentCP1 = "";
        String contentCP2 = "";
        String privateKeyFileName = "/Users/liusu/Documents/Liu Su/Term 5/Computer System Engineering/NS Programming Assignment/Certificate/privateServer.der";
        String publicKeyFilename = "/Users/liusu/Documents/Liu Su/Term 5/Computer System Engineering/NS Programming Assignment/Certificate/publicServer.der";
        System.out.println("Hello World");
        try{
            /**
             * Establish connection
             */
            ServerSocket serverSocket = new ServerSocket(4321);
            System.out.println("Waiting for client...");
            Socket clientSocket = serverSocket.accept();
            printWriter = new PrintWriter(clientSocket.getOutputStream(),true);
            bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Files.readAllBytes(Paths.get(privateKeyFileName)));
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            privateKey = keyFactory.generatePrivate(keySpec);

            keySpec = new PKCS8EncodedKeySpec(Files.readAllBytes(Paths.get(publicKeyFilename)));
            publicKey = keyFactory.generatePublic(keySpec);
            /**
             * Authentication
             */
            label:
            while (true) {
                message = bufferedReader.readLine();
                switch (message) {
                    case "Hello SecStore, please prove your identity":
                        sendSignedMessage(printWriter, privateKey);
                        break;
                    case "Give me your certificate signed by CA":
                        sendPublicKey(printWriter);
                        break;
                    case "Bye!":
                        printWriter.close();
                        bufferedReader.close();
                        serverSocket.close();
                        clientSocket.close();
                        break;
                    case "Authentication successful, start the transmission":
                        break label;
                    default:
                        printWriter.println("Invalid request, please resend.");
                        break;
                }
            }

            /**
             * File Transmission CP-1
             */
            long cp1StartTime = System.currentTimeMillis();
            while (true){
                message = bufferedReader.readLine();
                if(message.equals("Transmission Finished")) break;
//                contentCP1 += new String(decryptMessage(message.getBytes()));
            }

            long cp1SpentTime = System.currentTimeMillis() - cp1StartTime;
            /**
             * File Transmission CP-2
             */
            long cp2StartTime = System.currentTimeMillis();
            message = bufferedReader.readLine();
            getSessionKey(message.getBytes());

            while (true){
                message = bufferedReader.readLine();
                if(message.equals("Transmission Finished")) break;
                contentCP2 += new String(decryptWithSessionKey(message.getBytes()));
            }
            long cp2SpentTime = System.currentTimeMillis() - cp1StartTime;

            /**
             * Write File
             */
            writeFile(contentCP1);
            writeFile(contentCP2);
            System.out.println("Running time for CP-1: " + cp1SpentTime);
            System.out.println("Running time for CP-2: " + cp2SpentTime);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void sendSignedMessage(PrintWriter printWriter, PrivateKey privateKey) throws Exception{
        Cipher encryptCipher = Cipher.getInstance("RSA");
        encryptCipher.init(Cipher.ENCRYPT_MODE, privateKey);

        printWriter.println(new String(encryptCipher.doFinal("I'm SecStore".getBytes())));
    }
    public static void sendPublicKey(PrintWriter printWriter){}
    public static String decryptMessage(PrivateKey privateKey, String message) throws Exception{
        Cipher decryptCipher = Cipher.getInstance("RSA");
        decryptCipher.init(Cipher.DECRYPT_MODE, privateKey);

        return new String(decryptCipher.doFinal(message.getBytes()));
    }
    public static byte[] getSessionKey(byte[] input){
        return null;
    }

    public static byte[] decryptWithSessionKey(byte[] input){
        return null;
    }
    public static void writeFile(String content){}
}
