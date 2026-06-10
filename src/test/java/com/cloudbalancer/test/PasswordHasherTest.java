package com.cloudbalancer.test;

import com.cloudbalancer.security.PasswordHasher;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordHasherTest {

    @Test
    void testHashAndVerify() {
        String password = "testPassword123";
        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hashPassword(password, salt);

        assertTrue(PasswordHasher.verifyPassword(password, hash, salt));
        assertFalse(PasswordHasher.verifyPassword("wrongPassword", hash, salt));
    }

    @Test
    void testDifferentSaltsProduceDifferentHashes() {
        String password = "samePassword";
        String salt1 = PasswordHasher.generateSalt();
        String salt2 = PasswordHasher.generateSalt();

        assertNotEquals(
            PasswordHasher.hashPassword(password, salt1),
            PasswordHasher.hashPassword(password, salt2)
        );
    }

    @Test
    void testMinimalPassword() {
        String password = "1";
        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hashPassword(password, salt);
        assertTrue(PasswordHasher.verifyPassword(password, hash, salt));
    }
}
