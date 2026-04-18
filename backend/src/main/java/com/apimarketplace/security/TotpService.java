package com.apimarketplace.security;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class TotpService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int SECRET_BYTES = 20;
    private static final int TIME_STEP_SECONDS = 30;
    private static final int CODE_DIGITS = 6;
    private static final int WINDOW = 1;

    public String generateSecret() {
        byte[] randomBytes = new byte[SECRET_BYTES];
        RANDOM.nextBytes(randomBytes);
        return Base32.encode(randomBytes);
    }

    public String buildOtpAuthUrl(String issuer, String accountName, String secret) {
        String label = urlEncode(issuer + ":" + accountName);
        String encodedIssuer = urlEncode(issuer);
        return "otpauth://totp/" + label
            + "?secret=" + secret
            + "&issuer=" + encodedIssuer
            + "&algorithm=SHA1&digits=" + CODE_DIGITS
            + "&period=" + TIME_STEP_SECONDS;
    }

    public List<String> generateBackupCodes() {
        List<String> codes = new ArrayList<>(10);
        for (int i = 0; i < 10; i++) {
            StringBuilder code = new StringBuilder(8);
            for (int j = 0; j < 8; j++) {
                int pick = RANDOM.nextInt(36);
                code.append((char) (pick < 10 ? ('0' + pick) : ('A' + (pick - 10))));
            }
            codes.add(code.toString());
        }
        return codes;
    }

    public boolean verifyCode(String secret, String code) {
        if (secret == null || code == null || !code.matches("\\d{6}")) {
            return false;
        }

        byte[] key = Base32.decode(secret);
        long timeStep = Instant.now().getEpochSecond() / TIME_STEP_SECONDS;

        for (long candidate = timeStep - WINDOW; candidate <= timeStep + WINDOW; candidate++) {
            if (generateTotp(key, candidate).equals(code)) {
                return true;
            }
        }

        return false;
    }

    private String generateTotp(byte[] key, long timeStep) {
        try {
            byte[] counter = new byte[8];
            long value = timeStep;
            for (int i = 7; i >= 0; i--) {
                counter[i] = (byte) (value & 0xFF);
                value >>= 8;
            }

            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(counter);
            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                | ((hash[offset + 1] & 0xFF) << 16)
                | ((hash[offset + 2] & 0xFF) << 8)
                | (hash[offset + 3] & 0xFF);
            int otp = binary % 1_000_000;
            return String.format("%06d", otp);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Unable to generate TOTP", ex);
        }
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
