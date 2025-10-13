package ru.dimension.ui.security;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.Map;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.exception.PasswordEncryptDecryptException;

/**
 * Encryption and Decryption of String data; PBE(Password Based Encryption and Decryption)
 *
 * @author Vikram
 * @link https://stackoverflow.com/a/30591380
 **/
@Log4j2
@Singleton
public class EncryptDecrypt {

  private Cipher ecipher;
  private Cipher dcipher;
  private String algoritm = "PBEWithMD5AndDES";
  // 8-byte Salt
  private byte[] salt = {
      (byte) 0xA9, (byte) 0x9B, (byte) 0xC8, (byte) 0x32,
      (byte) 0x56, (byte) 0x35, (byte) 0xE3, (byte) 0x03
  };
  // Iteration count
  private int iterationCount = 19;

  @Inject
  public EncryptDecrypt() {
  }

  public String encrypt(String plainText) {
    if (plainText == null) {
      throw new PasswordEncryptDecryptException("Text for encryption could not be null.. " +
                                                    "Check password field of connProfile entity in configuration!");
    }

    try {
      return encrypt(getComputerName(), plainText);
    } catch (Exception e) {
      log.error("Encryption failed", e);
      throw new PasswordEncryptDecryptException("Encryption failed: " + e.getMessage());
    }
  }

  public String decrypt(String encryptedText) {
    if (encryptedText == null) {
      throw new PasswordEncryptDecryptException("Text for decryption could not be null.. " +
                                                    "Check password field of connProfile entity in configuration!");
    }

    try {
      return decrypt(getComputerName(), encryptedText);
    } catch (Exception e) {
      log.error("Decryption failed", e);
      throw new PasswordEncryptDecryptException("Decryption failed: " + e.getMessage());
    }
  }

  /**
   * @param secretKey Key used to encrypt data
   * @param plainText Text input to be encrypted
   * @return Returns encrypted text
   * @throws NoSuchAlgorithmException
   * @throws InvalidKeySpecException
   * @throws NoSuchPaddingException
   * @throws InvalidKeyException
   * @throws InvalidAlgorithmParameterException
   * @throws UnsupportedEncodingException
   * @throws IllegalBlockSizeException
   * @throws BadPaddingException
   */
  private String encrypt(String secretKey,
                         String plainText)
      throws NoSuchAlgorithmException,
      InvalidKeySpecException,
      NoSuchPaddingException,
      InvalidKeyException,
      InvalidAlgorithmParameterException,
      UnsupportedEncodingException,
      IllegalBlockSizeException,
      BadPaddingException {
    //Key generation for enc and desc
    KeySpec keySpec = new PBEKeySpec(secretKey.toCharArray(), salt, iterationCount);
    SecretKey key = SecretKeyFactory.getInstance(algoritm).generateSecret(keySpec);
    // Prepare the parameter to the ciphers
    AlgorithmParameterSpec paramSpec = new PBEParameterSpec(salt, iterationCount);

    //Enc process
    ecipher = Cipher.getInstance(key.getAlgorithm());
    ecipher.init(Cipher.ENCRYPT_MODE, key, paramSpec);
    byte[] in = plainText.getBytes(StandardCharsets.UTF_8);
    byte[] out = ecipher.doFinal(in);
    String encStr = new String(Base64.getEncoder().encode(out));
    return encStr;
  }

  /**
   * @param secretKey     Key used to decrypt data
   * @param encryptedText encrypted text input to decrypt
   * @return Returns plain text after decryption
   * @throws NoSuchAlgorithmException
   * @throws InvalidKeySpecException
   * @throws NoSuchPaddingException
   * @throws InvalidKeyException
   * @throws InvalidAlgorithmParameterException
   * @throws UnsupportedEncodingException
   * @throws IllegalBlockSizeException
   * @throws BadPaddingException
   */
  private String decrypt(String secretKey,
                         String encryptedText)
      throws NoSuchAlgorithmException,
      InvalidKeySpecException,
      NoSuchPaddingException,
      InvalidKeyException,
      InvalidAlgorithmParameterException,
      UnsupportedEncodingException,
      IllegalBlockSizeException,
      BadPaddingException,
      IOException {
    //Key generation for enc and desc
    KeySpec keySpec = new PBEKeySpec(secretKey.toCharArray(), salt, iterationCount);
    SecretKey key = SecretKeyFactory.getInstance(algoritm).generateSecret(keySpec);
    // Prepare the parameter to the ciphers
    AlgorithmParameterSpec paramSpec = new PBEParameterSpec(salt, iterationCount);
    //Decryption process; same key will be used for decr
    dcipher = Cipher.getInstance(key.getAlgorithm());
    dcipher.init(Cipher.DECRYPT_MODE, key, paramSpec);
    byte[] enc = Base64.getDecoder().decode(encryptedText);
    byte[] utf8 = dcipher.doFinal(enc);
    String plainStr = new String(utf8, StandardCharsets.UTF_8);
    return plainStr;
  }

  private String getComputerName() {
    try {
      Map<String, String> env = System.getenv();
      if (env.containsKey("COMPUTERNAME")) {
        return env.get("COMPUTERNAME");
      } else if (env.containsKey("HOSTNAME")) {
        return env.get("HOSTNAME");
      } else {
        return InetAddress.getLocalHost().getHostName();
      }
    } catch (UnknownHostException e) {
      log.error("Failed to get computer name", e);
      throw new PasswordEncryptDecryptException("Failed to get computer name: " + e.getMessage());
    }
  }
}