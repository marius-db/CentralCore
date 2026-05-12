package com.centralcore;

import com.centralcore.dao.UserDAO;
import com.centralcore.util.LicenseValidator;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class UnitTests {

    //helpers internos para construir claves sin depender de python

    private static String buildKey(String email, String expiry) throws Exception {
        Method computeHmac = LicenseValidator.class.getDeclaredMethod("computeHmac", String.class);
        computeHmac.setAccessible(true);
        String hmac = (String) computeHmac.invoke(null, email + "|" + expiry);
        String raw = email + "|" + expiry + "|" + hmac;
        return Base64.getEncoder().encodeToString(raw.getBytes());
    }

    private static String futureDate() {
        return LocalDate.now().plusYears(1).toString();
    }

    private static String pastDate() {
        return LocalDate.now().minusDays(1).toString();
    }

    @Test
    void validate_validKey_returnsTrue() throws Exception {
        //clave bien formada con fecha futura debe pasar
        String key = buildKey("test@centralcore.com", futureDate());
        assertTrue(LicenseValidator.validate(key));
    }

    @Test
    void validate_expiredKey_returnsFalse() throws Exception {
        //clave con fecha pasada debe fallar
        String key = buildKey("test@centralcore.com", pastDate());
        assertFalse(LicenseValidator.validate(key));
    }

    @Test
    void validate_tamperedKey_returnsFalse() throws Exception {
        //modificar el hmac al final invalida la clave
        String key = buildKey("test@centralcore.com", futureDate());
        String tampered = key.substring(0, key.length() - 3) + "XXX";
        assertFalse(LicenseValidator.validate(tampered));
    }

    @Test
    void extractEmail_returnsCorrectEmail() throws Exception {
        //el email embebido en la clave debe recuperarse intacto
        String email = "mack@centralcore.com";
        String key = buildKey(email, futureDate());
        assertEquals(email, LicenseValidator.extractEmail(key));
    }

    @Test
    void hashPassword_producesValidBcryptHash() {
        //el hash generado debe verificar contra la contrasena original
        String hash = UserDAO.hashPassword("miPassword123");
        assertTrue(BCrypt.checkpw("miPassword123", hash));
    }

    @Test
    void hashPassword_sameInputProducesDifferentHashes() {
        //bcrypt incluye salt aleatorio, dos llamadas nunca deben coincidir
        String hash1 = UserDAO.hashPassword("password");
        String hash2 = UserDAO.hashPassword("password");
        assertNotEquals(hash1, hash2);
    }
}