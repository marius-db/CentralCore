package com.centralcore.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;

public class LicenseValidator {

    private static final String SECRET = "gobidiba-gooba-goobster"; //debe coincidir con el generador de Python

    public static boolean validate(String licenseKey) {
        try {
            String decoded = new String(Base64.getDecoder().decode(licenseKey));
            String[] parts = decoded.split("\\|"); //[correo, expiración, hmac]
            if (parts.length != 3) return false;

            String email  = parts[0];
            String expiry = parts[1];
            String hmac   = parts[2];

            //verificar fecha de expiración
            if (LocalDate.now().isAfter(LocalDate.parse(expiry))) return false;

            //recalcular HMAC y comparar
            String expected = computeHmac(email + "|" + expiry);
            return expected.equals(hmac);

        } catch (Exception e) {
            return false;
        }
    }

    public static String extractExpiry(String licenseKey) {
        try {
            String decoded = new String(Base64.getDecoder().decode(licenseKey));
            return decoded.split("\\|")[1];
        } catch (Exception e) {
            return "Unknown";
        }
    }

    public static boolean isActive(String licenseKey) {
        try {
            String decoded = new String(Base64.getDecoder().decode(licenseKey));
            LocalDate expiry = LocalDate.parse(decoded.split("\\|")[1]);
            return !LocalDate.now().isAfter(expiry);
        } catch (Exception e) {
            return false;
        }
    }

    private static String computeHmac(String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(
                SECRET.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        ));
        return Base64.getEncoder().encodeToString(
                mac.doFinal(data.getBytes(StandardCharsets.UTF_8))
        );
    }
}