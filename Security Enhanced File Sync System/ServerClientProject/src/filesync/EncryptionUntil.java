package filesync;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import org.apache.commons.codec.binary.Base64;



import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;


/**
 * @author JavaDigest
 * 
 */
public class EncryptionUntil {

  /**
   * String to hold name of the encryption algorithm.
   */
  public static final String ALGORITHM = "RSA";

  /**
   * String to hold the name of the private key file.
   */
  public static final String PRIVATE_KEY_FILE = "private.key";

  /**
   * String to hold name of the public key file.
   */
  public static final String PUBLIC_KEY_FILE = "public.key";

  /**
   * Generate key which contains a pair of private and public key using 1024
   * bytes. Store the set of keys in Prvate.key and Public.key files.
   * 
   * @throws NoSuchAlgorithmException
   * @throws IOException
   * @throws FileNotFoundException
   */
  public static void generateKey() {
    try {
      final KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALGORITHM);
      keyGen.initialize(1024);
      final KeyPair key = keyGen.generateKeyPair();

      File privateKeyFile = new File(PRIVATE_KEY_FILE);
      File publicKeyFile = new File(PUBLIC_KEY_FILE);

      // Create files to store public and private key
      if (privateKeyFile.getParentFile() != null) {
        privateKeyFile.getParentFile().mkdirs();
      }
      privateKeyFile.createNewFile();

      if (publicKeyFile.getParentFile() != null) {
        publicKeyFile.getParentFile().mkdirs();
      }
      publicKeyFile.createNewFile();

      // Saving the Public key in a file
      ObjectOutputStream publicKeyOS = new ObjectOutputStream(
          new FileOutputStream(publicKeyFile));
      publicKeyOS.writeObject(key.getPublic());
      publicKeyOS.close();

      // Saving the Private key in a file
      ObjectOutputStream privateKeyOS = new ObjectOutputStream(
          new FileOutputStream(privateKeyFile));
      privateKeyOS.writeObject(key.getPrivate());
      privateKeyOS.close();
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  /**
   * The method checks if the pair of public and private key has been generated.
   * 
   * @return flag indicating if the pair of keys were generated.
   */
  public static boolean areKeysPresent() {

    File privateKey = new File(PRIVATE_KEY_FILE);
    File publicKey = new File(PUBLIC_KEY_FILE);

    if (privateKey.exists() && publicKey.exists()) {
      return true;
    }
    return false;
  }

  /**
   * Encrypt the plain text using public key.
   * 
   * @param text
   *          : original plain text
   * @param key
   *          :The public key
   * @return Encrypted text
   * @throws java.lang.Exception
   */
  public static byte[] encrypt(String text, PublicKey key) {
    byte[] cipherText = null;
    try {
      // get an RSA cipher object and print the provider
      final Cipher cipher = Cipher.getInstance(ALGORITHM);
      // encrypt the plain text using the public key
      cipher.init(Cipher.ENCRYPT_MODE, key);
      cipherText = cipher.doFinal(text.getBytes());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return cipherText;
  }

  /**
   * Decrypt text using private key.
   * 
   * @param text
   *          :encrypted text
   * @param key
   *          :The private key
   * @return plain text
   * @throws java.lang.Exception
   */
  public static String decrypt(byte[] text, PrivateKey key) {
    byte[] dectyptedText = null;
    try {
      // get an RSA cipher object and print the provider
      final Cipher cipher = Cipher.getInstance(ALGORITHM);

      // decrypt the text using the private key
      cipher.init(Cipher.DECRYPT_MODE, key);
      dectyptedText = cipher.doFinal(text);

    } catch (Exception ex) {
      ex.printStackTrace();
    }

    return new String(dectyptedText);
  }

  /**
   * Convert String to Public Key
   */
  public static PublicKey StringToPublicKey(String StrPublicKey)
  {
	    byte[] ServerPublicKeyBytes = Base64.decodeBase64(StrPublicKey);
	    KeyFactory keyFactory;
	    PublicKey publicKey;
		try {
			keyFactory = KeyFactory.getInstance(ALGORITHM);
			EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(ServerPublicKeyBytes);
				try {
					publicKey = keyFactory.generatePublic(publicKeySpec);
					return publicKey;
					
				} catch (InvalidKeySpecException e) {					
					e.printStackTrace();
					return null;
				}			
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}
		
  }
  
  public static SecretKey generateSessionKey() {
	    KeyGenerator keyGen;
		try {
			keyGen = KeyGenerator.getInstance("AES");
			keyGen.init(128);
		    SecretKey secretKey = keyGen.generateKey();
		    return secretKey;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}      
}

public static byte[] encryptBySessionKey(String text, SecretKey SessionKey) {
	    byte[] cipherText = null;
	    try {
	      final Cipher cipher = Cipher.getInstance("AES");
	      cipher.init(Cipher.ENCRYPT_MODE, SessionKey);
	      cipherText = cipher.doFinal(text.getBytes());
	    } catch (Exception e) {
	      e.printStackTrace();
	    }
	    return cipherText;
	  }

public static String decryptBySessionKey(byte[] text, SecretKey SessionKey) {
	    byte[] dectyptedText = null;
	    try {
	      final Cipher cipher = Cipher.getInstance("AES");
	      cipher.init(Cipher.DECRYPT_MODE, SessionKey);
	      dectyptedText = cipher.doFinal(text);
	    } catch (Exception ex) {
	      ex.printStackTrace();
	    }
	    return new String(dectyptedText);
	  }



//*******************************************************************************************
private static final int IV_LENGTH=16;

public static void encryptFile(InputStream in, OutputStream out, String password) throws Exception{

	SecureRandom r = new SecureRandom();
	byte[] iv = new byte[IV_LENGTH];
	r.nextBytes(iv);
	out.write(iv); //write IV as a prefix
	out.flush();
	//System.out.println(">>>>>>>>written"+Arrays.toString(iv));

	Cipher cipher = Cipher.getInstance("AES/CFB8/NoPadding"); //"DES/ECB/PKCS5Padding";"AES/CBC/PKCS5Padding"
	SecretKeySpec keySpec = new SecretKeySpec(password.getBytes(), "AES");
	IvParameterSpec ivSpec = new IvParameterSpec(iv);
	cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);    	

	out = new CipherOutputStream(out, cipher);
	byte[] buf = new byte[1024];
	int numRead = 0;
	while ((numRead = in.read(buf)) >= 0) {
		out.write(buf, 0, numRead);
	}
	out.close();
}


public static void decryptFile(InputStream in, OutputStream out, String password) throws Exception{

	byte[] iv = new byte[IV_LENGTH];
	in.read(iv);
	//System.out.println(">>>>>>>>red"+Arrays.toString(iv));

	Cipher cipher = Cipher.getInstance("AES/CFB8/NoPadding"); //"DES/ECB/PKCS5Padding";"AES/CBC/PKCS5Padding"
	SecretKeySpec keySpec = new SecretKeySpec(password.getBytes(), "AES");
	IvParameterSpec ivSpec = new IvParameterSpec(iv);
	cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

	in = new CipherInputStream(in, cipher);
	byte[] buf = new byte[1024];
	int numRead = 0;
	while ((numRead = in.read(buf)) >= 0) {
		out.write(buf, 0, numRead);
	}
	out.close();
}



public static void copy(int mode, String inputFile, String outputFile, String password) throws Exception {

	BufferedInputStream is = new BufferedInputStream(new FileInputStream(inputFile));
	BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(outputFile));
	if(mode==Cipher.ENCRYPT_MODE){
		encryptFile(is, os, password);
	}
	else if(mode==Cipher.DECRYPT_MODE){
		decryptFile(is, os, password);
	}
	else throw new Exception("unknown mode");
	is.close();
	os.close();
}

//*******************************************************************************************













}
