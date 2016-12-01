import javax.crypto.Cipher;
import javax.crypto.SealedObject;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import java.io.*;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.util.HashMap;

/**
 * Created by udbhav on 29/11/16.
 */
public class FileDecrypter {

    //Method for selecting files.
    public static File getSelectedFile()
    {
        File file = null;
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int selection = fileChooser.showOpenDialog(null);
        if (selection == JFileChooser.APPROVE_OPTION) {
            file = fileChooser.getSelectedFile();
        }
        else if(selection == JFileChooser.CANCEL_OPTION)
        {
            System.out.println("Exiting.");
            System.exit(1);
        }

        return file;

    }

    public static void main(String[] args) {
        try{

            System.out.println("Select File: ");
            File file = getSelectedFile();
            System.out.println("Select private key: ");
            File pvKeyFile = getSelectedFile();

            PrivateKey pvKey = null;
            try {
                ObjectInputStream keyReader = new ObjectInputStream(new FileInputStream(pvKeyFile));
                pvKey = (PrivateKey) keyReader.readObject();
                keyReader.close();
            }catch(ClassCastException ccx){System.out.println("\nInvalid private key! Aborting!");System.exit(2);/*In case an invalid file is selected.*/}

            HashMap<Integer, SealedObject> sealedStuff = null;

            try {
                ObjectInputStream fileReader = new ObjectInputStream(new FileInputStream(file));
                sealedStuff = (HashMap<Integer, SealedObject>) fileReader.readObject();
                fileReader.close();


                //Initialise decryption cipher with private key.
            Cipher deCipher = Cipher.getInstance("RSA");
            deCipher.init(Cipher.DECRYPT_MODE,pvKey);

                //Get AES key bytes.
            SealedObject sealedKey = sealedStuff.get(0);
            byte[] keyBytes = (byte[])sealedKey.getObject(deCipher);
                //Get AES IV bytes.
            SealedObject sealedIV = sealedStuff.get(1);
            byte[] IVBytes = (byte[])sealedIV.getObject(deCipher);

                //Generate key and AES decryption cipher.
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes,"AES");


            Cipher contentCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            contentCipher.init(Cipher.DECRYPT_MODE,keySpec,new IvParameterSpec(IVBytes));

                //Get decrypted name and content.
            SealedObject sealedName = sealedStuff.get(2);
            String fileName = (String)sealedName.getObject(contentCipher);


            SealedObject sealedContent = sealedStuff.get(3);
            byte[] content = (byte[])sealedContent.getObject(contentCipher);
            //Write decrypted data in file.
            FileOutputStream fileOut = new FileOutputStream(fileName);
            fileOut.write(content);
            fileOut.close();

            }catch(ClassCastException ccx){System.out.println("Invalid sealed file! Aborting!");System.exit(3);/*In case an invalid sealed file is selected.*/}

        }catch(IOException iox){System.out.println("IO Error! Exiting!"); iox.printStackTrace();System.exit(4);/*General IO errors.*/}
        catch(GeneralSecurityException gsx){System.out.println("Decryption Error! Aborting!"); System.exit(5);/*In case of wrong key.*/}
        catch(Exception x){System.out.println("Error! Exiting!"); x.printStackTrace();System.exit(6);/*Other errors.*/}
    }
}