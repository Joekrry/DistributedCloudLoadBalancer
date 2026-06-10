package com.cloudbalancer.test;

import com.cloudbalancer.security.FileEncryptor;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class FileEncryptorTest {

    @Test
    void testEncryptDecryptRoundTrip() throws Exception {
        SecretKey key = FileEncryptor.generateKey();
        byte[] iv = FileEncryptor.generateIV();
        byte[] original = "Hello CloudBalancer".getBytes();

        byte[] encrypted = FileEncryptor.encrypt(original, key, iv);
        byte[] decrypted = FileEncryptor.decrypt(encrypted, key, iv);

        assertArrayEquals(original, decrypted);
        assertFalse(Arrays.equals(original, encrypted));
    }

    @Test
    void testKeyToStringAndBack() throws Exception {
        SecretKey key = FileEncryptor.generateKey();
        String encoded = FileEncryptor.keyToString(key);
        SecretKey restored = FileEncryptor.stringToKey(encoded);

        assertArrayEquals(key.getEncoded(), restored.getEncoded());
    }
}
