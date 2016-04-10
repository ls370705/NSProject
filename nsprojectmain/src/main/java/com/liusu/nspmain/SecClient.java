package com.liusu.nspmain;


import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
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
//commit
public class SecClient {
    public static void main(String args[]){
        String hostName = "10.12.20.209";
        int portNumber = 4321;
        Socket clientSocket;
        PrintWriter printWriter;
        BufferedReader bufferedReader;
        File file = new File("D:\\Study\\Term 5\\Computer System Engineering\\NSProjectRelease\\sampleData\\smallFile.txt");
        //File certFile = new File("/Users/liusu/Documents/Liu Su/Term 5/Computer System Engineering/NS Programming Assignment/CASignedPublicKey.crt");
        String message;
        boolean authenticationSuccessful;
        ArrayList<String> fileInput;
        DataInputStream inputStream;
        DataOutputStream outputStream;

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
            //message = bufferedReader.readLine();


            inputStream=new DataInputStream(clientSocket.getInputStream());
            int len=inputStream.readInt();
            byte[] data=new byte[len];
            inputStream.readFully(data);


            printWriter.println("Give me your certificate signed by CA");
            String certificateFile="";
            while (true){
                String line=bufferedReader.readLine();
                certificateFile+=line+"\n";
                if (line.equals("-----END CERTIFICATE-----")){
                    break;
                }
            }
            FileWriter fileWriter=new FileWriter("D:\\Study\\Term 5\\Computer System Engineering\\NSProjectRelease\\Certificate.crt",true);
            fileWriter.write(certificateFile);
            fileWriter.close();
            PublicKey publicKey = getServerPublicKey(new File("D:\\Study\\Term 5\\Computer System Engineering\\NSProjectRelease\\Certificate.crt"));

            if(verifyIdentity(publicKey, data)){
                printWriter.println("Authentication successful, start the transmission");
                authenticationSuccessful = true;
            }else{
                printWriter.println("Bye!");
                authenticationSuccessful = false;
            }

            outputStream=new DataOutputStream(clientSocket.getOutputStream());

            /**
             * Transmission CP-1
             */
            if(authenticationSuccessful){
                fileInput = readFile(file);
                for(String i: fileInput){
                    byte[] encryFile = encryptWithPublicKey(publicKey, i+"\n");
                    outputStream.writeInt(encryFile.length);
                    outputStream.write(encryFile,0,encryFile.length);
                }
                String fin="Transmission Finished";
                byte[] finish=fin.getBytes();
                outputStream.writeInt(finish.length);
                outputStream.write(finish, 0, finish.length);
            }

            /**
             * Transmission CP-2
             */
            if(authenticationSuccessful){
                fileInput = readFile(file);
                SecretKey sessionKey = generateSessionKey();

                //byte[] encrSessionKey=encryptWithPublicKey(publicKey, Base64.getEncoder().encodeToString(sessionKey.getEncoded()));
                byte[] encrSessionKey=encryptWithPublicKey(publicKey,sessionKey.getEncoded());
                outputStream.writeInt(encrSessionKey.length);
                outputStream.write(encrSessionKey,0,encrSessionKey.length);

                String encryptFile="";
                for(String i: fileInput){
                    encryptFile+=i+"\n";
                }
                byte[] encryFile=encryptWithSessionKey(sessionKey, encryptFile);
                outputStream.writeInt(encryFile.length);
                outputStream.write(encryFile,0,encryFile.length);
                //printWriter.println("Transmission Finished");
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
    public static boolean verifyIdentity(PublicKey publicKey, byte[] data) throws Exception{
        //System.out.println("Message: " + message);
        Cipher decryptCipher = Cipher.getInstance("RSA");
        decryptCipher.init(Cipher.DECRYPT_MODE, publicKey);

        String decryptedMessage = new String(decryptCipher.doFinal(data));
        return decryptedMessage.equals("I'm SecStore");
    }
    public static byte[] encryptWithPublicKey(PublicKey publicKey, String message) throws Exception{
        Cipher encryptCipher = Cipher.getInstance("RSA");
        encryptCipher.init(Cipher.ENCRYPT_MODE, publicKey);

        return encryptCipher.doFinal(message.getBytes());
    }

    public static byte[] encryptWithPublicKey(PublicKey publicKey, byte[] message) throws Exception{
        Cipher encryptCipher = Cipher.getInstance("RSA");
        encryptCipher.init(Cipher.ENCRYPT_MODE, publicKey);

        return encryptCipher.doFinal(message);
    }

    public static SecretKey generateSessionKey() throws Exception{

        return KeyGenerator.getInstance("AES").generateKey();
    }
    public static byte[] encryptWithSessionKey(SecretKey secretKey, String message) throws Exception{
        Cipher encryptCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        encryptCipher.init(Cipher.ENCRYPT_MODE, secretKey);

        return encryptCipher.doFinal(message.getBytes());
    }
}
