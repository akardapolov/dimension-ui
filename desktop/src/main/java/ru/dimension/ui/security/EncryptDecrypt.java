package ru.dimension.ui.security;

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
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.exception.PasswordEncryptDecryptException;

@Log4j2
@Singleton
public class EncryptDecrypt {

  private static final String ALGORITHM = "PBEWithMD5AndDES";

  // 8-byte Salt
  private static final byte[] SALT = {
      (byte) 0xA9, (byte) 0x9B, (byte) 0xC8, (byte) 0x32,
      (byte) 0x56, (byte) 0x35, (byte) 0xE3, (byte) 0x03
  };

  // Iteration count
  private static final int ITERATION_COUNT = 19;

  private final String cachedComputerName;
  private volatile boolean fallbackToPlainText = true;

  // Thread-safe cache for cipher instances
  private final ThreadLocal<CipherPair> cipherCache = new ThreadLocal<>();

  // Cache for keys to avoid recreating them
  private final ConcurrentHashMap<String, SecretKey> keyCache = new ConcurrentHashMap<>();

  @Inject
  public EncryptDecrypt() {
    this.cachedComputerName = getComputerName();
    log.info("EncryptDecrypt initialized with computer name: {}", cachedComputerName);
  }

  public String encrypt(String plainText) {
    if (plainText == null) {
      throw new PasswordEncryptDecryptException("Text for encryption could not be null.. " +
                                                    "Check password field of connProfile entity in configuration!");
    }

    // Check if already encrypted (base64 pattern)
    if (isEncrypted(plainText)) {
      log.debug("Text appears to be already encrypted, returning as-is");
      return plainText;
    }

    try {
      return encryptInternal(cachedComputerName, plainText);
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

    // First check if it's actually encrypted
    if (!isEncrypted(encryptedText)) {
      log.debug("Text doesn't appear to be encrypted, returning as plain text");
      return encryptedText; // Return as-is if it's plain text
    }

    try {
      return decryptInternal(cachedComputerName, encryptedText);
    } catch (BadPaddingException e) {
      return handleDecryptionFailure(encryptedText, e);
    } catch (Exception e) {
      log.error("Decryption failed", e);

      if (fallbackToPlainText) {
        log.warn("Decryption error, treating as plain text: {}", e.getMessage());
        return encryptedText;
      }

      throw new PasswordEncryptDecryptException("Decryption failed: " + e.getMessage());
    }
  }

  private String handleDecryptionFailure(String encryptedText, BadPaddingException e) {
    log.warn("Decryption failed with current computer name '{}', attempting fallback strategies",
             cachedComputerName);

    // Try common fallback keys
    String[] fallbackKeys = getFallbackKeys();
    for (String fallbackKey : fallbackKeys) {
      if (fallbackKey == null || fallbackKey.equals(cachedComputerName)) {
        continue;
      }

      try {
        log.debug("Attempting decryption with fallback key: {}", fallbackKey);
        String result = decryptInternal(fallbackKey, encryptedText);
        log.info("Successfully decrypted with fallback key: {}", fallbackKey);

        // Re-encrypt with current key for future use
        String reEncrypted = encryptInternal(cachedComputerName, result);
        log.info("Consider updating the password in configuration with new encryption");

        return result;
      } catch (Exception ex) {
        log.debug("Fallback key '{}' failed", fallbackKey);
      }
    }

    // If all fallback attempts fail and fallback to plain text is enabled
    if (fallbackToPlainText) {
      log.warn("All decryption attempts failed. Treating as plain text password. " +
                   "Consider re-saving the connection to properly encrypt the password.");
      return encryptedText;
    }

    throw new PasswordEncryptDecryptException("Decryption failed: " + e.getMessage() +
                                                  ". Password may have been encrypted on a different machine.");
  }

  private boolean isEncrypted(String text) {
    if (text == null || text.isEmpty()) {
      return false;
    }

    // Check if it's a valid Base64 string and has reasonable length for encrypted data
    try {
      if (text.length() < 12) { // Encrypted passwords are typically longer
        return false;
      }

      // Try to decode as Base64
      Base64.getDecoder().decode(text);

      // Additional check: encrypted strings typically end with = or have specific patterns
      return text.matches("^[A-Za-z0-9+/]+=*$");
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  private String[] getFallbackKeys() {
    // Return possible alternative computer names/hostnames
    return new String[] {
        "localhost",
        "DESKTOP",
        getAlternativeComputerName(),
        getHostnameFromEnv(),
        getComputerNameFromEnv()
    };
  }

  private String getAlternativeComputerName() {
    try {
      return InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      return "unknown";
    }
  }

  private String getHostnameFromEnv() {
    return System.getenv().getOrDefault("HOSTNAME", "hostname");
  }

  private String getComputerNameFromEnv() {
    return System.getenv().getOrDefault("COMPUTERNAME", "computername");
  }

  private String encryptInternal(String secretKey, String plainText)
      throws NoSuchAlgorithmException,
      InvalidKeySpecException,
      NoSuchPaddingException,
      InvalidKeyException,
      InvalidAlgorithmParameterException,
      IllegalBlockSizeException,
      BadPaddingException {

    CipherPair cipherPair = getCipherPair(secretKey);

    synchronized (cipherPair.encryptCipher) {
      byte[] in = plainText.getBytes(StandardCharsets.UTF_8);
      byte[] out = cipherPair.encryptCipher.doFinal(in);
      return Base64.getEncoder().encodeToString(out);
    }
  }

  private String decryptInternal(String secretKey, String encryptedText)
      throws NoSuchAlgorithmException,
      InvalidKeySpecException,
      NoSuchPaddingException,
      InvalidKeyException,
      InvalidAlgorithmParameterException,
      IllegalBlockSizeException,
      BadPaddingException {

    CipherPair cipherPair = getCipherPair(secretKey);

    synchronized (cipherPair.decryptCipher) {
      byte[] enc = Base64.getDecoder().decode(encryptedText);
      byte[] utf8 = cipherPair.decryptCipher.doFinal(enc);
      return new String(utf8, StandardCharsets.UTF_8);
    }
  }

  private CipherPair getCipherPair(String secretKey)
      throws NoSuchAlgorithmException, InvalidKeySpecException,
      NoSuchPaddingException, InvalidKeyException,
      InvalidAlgorithmParameterException {

    CipherPair cipherPair = cipherCache.get();

    if (cipherPair == null || !cipherPair.isForKey(secretKey)) {
      SecretKey key = getOrCreateKey(secretKey);
      AlgorithmParameterSpec paramSpec = new PBEParameterSpec(SALT, ITERATION_COUNT);

      Cipher encryptCipher = Cipher.getInstance(ALGORITHM);
      encryptCipher.init(Cipher.ENCRYPT_MODE, key, paramSpec);

      Cipher decryptCipher = Cipher.getInstance(ALGORITHM);
      decryptCipher.init(Cipher.DECRYPT_MODE, key, paramSpec);

      cipherPair = new CipherPair(secretKey, encryptCipher, decryptCipher);
      cipherCache.set(cipherPair);
    }

    return cipherPair;
  }

  private SecretKey getOrCreateKey(String secretKey)
      throws NoSuchAlgorithmException, InvalidKeySpecException {

    return keyCache.computeIfAbsent(secretKey, k -> {
      try {
        KeySpec keySpec = new PBEKeySpec(k.toCharArray(), SALT, ITERATION_COUNT);
        return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(keySpec);
      } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
        throw new RuntimeException("Failed to create key", e);
      }
    });
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

  public void setFallbackToPlainText(boolean fallbackToPlainText) {
    this.fallbackToPlainText = fallbackToPlainText;
  }

  public String getCurrentComputerName() {
    return cachedComputerName;
  }

  public boolean canDecrypt(String encryptedText) {
    try {
      decrypt(encryptedText);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public String reEncryptWithCurrentKey(String password) {
    try {
      // First decrypt with whatever key works
      String plainText = decrypt(password);
      // Then encrypt with current key
      return encrypt(plainText);
    } catch (Exception e) {
      log.error("Failed to re-encrypt password", e);
      return password;
    }
  }

  /**
   * Inner class to hold a pair of ciphers for a specific key
   */
  private static class CipherPair {
    private final String keyId;
    private final Cipher encryptCipher;
    private final Cipher decryptCipher;

    CipherPair(String keyId, Cipher encryptCipher, Cipher decryptCipher) {
      this.keyId = keyId;
      this.encryptCipher = encryptCipher;
      this.decryptCipher = decryptCipher;
    }

    boolean isForKey(String key) {
      return keyId.equals(key);
    }
  }
}