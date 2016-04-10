package com.liusu.nspmain;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

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
        File file = new File("/Users/liusu/Documents/Liu Su/Term 5/Computer System Engineering/NS Programming Assignment/sampleData/smallFile.txt");
        File certFile = new File("/Users/liusu/Documents/Liu Su/Term 5/Computer System Engineering/NS Programming Assignment/CASignedPublicKey.crt");
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
            PublicKey publicKey = getServerPublicKey(certFile);

            if(verifyIdentity(publicKey, message)){
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
                    printWriter.println(encryptWithPublicKey(publicKey, i));
                }
                printWriter.println("Transmission Finished");
            }

            /**
             * Transmission CP-2
             */
            if(authenticationSuccessful){
                fileInput = readFile(file);
                SecretKey sessionKey = generateSessionKey();

                printWriter.println(encryptWithPublicKey(publicKey, Base64.getEncoder().encodeToString(sessionKey.getEncoded())));

                for(String i: fileInput){
                    printWriter.println(encryptWithSessionKey(sessionKey, i));
                }
                printWriter.println("Transmission Finished");
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static ArrayList<String> readFile(File file){
        ArrayList<String> fileIn = new ArrayList<>();
        String temp;
        try {
            BufferedReader fileReader = new BufferedReader(new FileReader(file));
            while((temp = fileReader.readLine()) != null){
                fileIn.add(temp);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fileIn;
    }

    public static PublicKey getServerPublicKey(File cert){
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate CACert = (X509Certificate)cf.generateCertificate(new FileInputStream(cert));
            CACert.checkValidity();

            return CACert.getPublicKey();
        } catch (CertificateException e) {
            System.out.println("Certificate Exception!");
            e.printStackTrace();
            return null;
        } catch (FileNotFoundException e){
            System.out.println("File not found!");
            e.printStackTrace();
            return null;
        }
    }
    public static boolean verifyIdentity(PublicKey publicKey, String message) throws Exception{
        Cipher decryptCipher = Cipher.getInstance("RSA");
        decryptCipher.init(Cipher.DECRYPT_MODE, publicKey);

        String decryptedMessage = new String(decryptCipher.doFinal(message.getBytes()));
        return decryptedMessage.equals("I'm SecStore");
    }
    public static String encryptWithPublicKey(PublicKey publicKey, String message) throws Exception{
        Cipher encryptCipher = Cipher.getInstance("RSA");
        encryptCipher.init(Cipher.ENCRYPT_MODE, publicKey);

        return new String(encryptCipher.doFinal(message.getBytes()));
    }
    public static SecretKey generateSessionKey() throws Exception{

        return KeyGenerator.getInstance("AES").generateKey();
    }
    public static String encryptWithSessionKey(SecretKey secretKey, String message) throws Exception{
        Cipher encryptCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        encryptCipher.init(Cipher.ENCRYPT_MODE, secretKey);

        return new String(encryptCipher.doFinal(message.getBytes()));
    }
}
