package com.liusu.nspmain;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
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
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * SecStore.java
 * Created by liusu on 5/4/16.
 */
public class SecStore {
    public static void main(String args[]){
        PrivateKey privateKey;
        SecretKey sessionKey;
        PrintWriter printWriter;
        BufferedReader bufferedReader;
        DataOutputStream dataOutputStream;
        DataInputStream dataInputStream;
        String message;
        byte[] data;
        int byteArrayLength;
        String contentCP1 = "";
        String contentCP2;
        String certFileName = "/Users/liusu/Documents/Liu Su/Term 5/Computer System Engineering/NS Programming Assignment/CASignedPublicKey.crt";
//        String privateKeyFileName = "D:\\Study\\Term 5\\Computer System Engineering\\NSProjectRelease\\privateServer.der";
        String privateKeyFileName = "Users/liusu/Documents/Liu Su/Term 5/Computer System Engineering/NS Programming Assignment/Certificate/privateServer.der";
//        String publicKeyFileName = "D:\\Study\\Term 5\\Computer System Engineering\\NSProjectRelease\\publicServer.der";
        String publicKeyFileName = "Users/liusu/Documents/Liu Su/Term 5/Computer System Engineering/NS Programming Assignment/Certificate/publicServer.der";
        String smallFileNameCP1 = "Users/liusu/Documents/Liu Su/Term 5/Computer System Engineering/NS Programming Assignment/smallFileCP1.txt";
        String smallFileNameCP2 = "Users/liusu/Documents/Liu Su/Term 5/Computer System Engineering/NS Programming Assignment/smallFileCP2.txt";
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
            dataInputStream = new DataInputStream(clientSocket.getInputStream());
            dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());

            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Files.readAllBytes(Paths.get(privateKeyFileName)));
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            privateKey = keyFactory.generatePrivate(keySpec);
            /**
             * Authentication
             */
            label:
            while (true) {
                message = bufferedReader.readLine();
                switch (message) {
                    case "Hello SecStore, please prove your identity":
                        sendSignedMessage(dataOutputStream, privateKey);
                        break;
                    case "Give me your certificate signed by CA":
                        sendPublicKey(printWriter,certFileName);
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
                byteArrayLength = dataInputStream.readInt();
                data = new byte[byteArrayLength];
                dataInputStream.readFully(data,0,byteArrayLength);

                if(message.equals("Transmission Finished")) break;
                contentCP1 += decryptMessage(privateKey, data);
            }

            long cp1SpentTime = System.currentTimeMillis() - cp1StartTime;
            /**
             * File Transmission CP-2
             */
            long cp2StartTime = System.currentTimeMillis();
            byteArrayLength = dataInputStream.readInt();
            data = new byte[byteArrayLength];
            dataInputStream.readFully(data,0,byteArrayLength);
            sessionKey = getSessionKey(privateKey,data);

//            while (true){
                byteArrayLength = dataInputStream.readInt();
                data = new byte[byteArrayLength];
//                if(message.equals("Transmission Finished")) break;
                contentCP2 = decryptWithSessionKey(sessionKey,data);
//            }
            long cp2SpentTime = System.currentTimeMillis() - cp1StartTime;

            /**
             * Write File
             */
            writeFile(contentCP1,smallFileNameCP1);
            writeFile(contentCP2,smallFileNameCP2);
            System.out.println("Running time for CP-1: " + cp1SpentTime);
            System.out.println("Running time for CP-2: " + cp2SpentTime);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void sendSignedMessage(DataOutputStream dataOutputStream, PrivateKey privateKey) throws Exception{
        Cipher encryptCipher = Cipher.getInstance("RSA");
        encryptCipher.init(Cipher.ENCRYPT_MODE, privateKey);
        byte[] message = encryptCipher.doFinal("I'm SecStore".getBytes());

        dataOutputStream.writeInt(message.length);
        dataOutputStream.write(message,0,message.length);
    }
    public static void sendPublicKey(PrintWriter printWriter, String certFileName) throws Exception{
        BufferedReader reader = new BufferedReader(new FileReader(new File(certFileName)));
        String temp;
        String content = "";
        while((temp = reader.readLine()) != null) certFileName += temp;

        printWriter.println(content);
    }
    public static String decryptMessage(PrivateKey privateKey, byte[] message) throws Exception{
        Cipher decryptCipher = Cipher.getInstance("RSA");
        decryptCipher.init(Cipher.DECRYPT_MODE, privateKey);

        return new String(decryptCipher.doFinal(message));
    }
    public static SecretKey getSessionKey(PrivateKey privateKey, byte[] input) throws Exception{
        Cipher decryptCipher = Cipher.getInstance("RSA");
        decryptCipher.init(Cipher.DECRYPT_MODE, privateKey);

        return new SecretKeySpec(input,0,input.length,"RSA");
    }

    public static String decryptWithSessionKey(SecretKey secretKey, byte[] input) throws Exception {
        Cipher decryptCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        decryptCipher.init(Cipher.DECRYPT_MODE,secretKey);

        return new String(decryptCipher.doFinal(input));
    }
    public static void writeFile(String content, String fileName) throws Exception{
        PrintWriter fileWriter = new PrintWriter(fileName,"UTF-8");
        fileWriter.println(content);
    }
}
