package com.apimarketplace.security;

import java.io.ByteArrayOutputStream;

final class Base32 {

    private static final char[] ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();

    private Base32() {}

    static String encode(byte[] data) {
        StringBuilder result = new StringBuilder((data.length + 4) / 5 * 8);
        int buffer = 0;
        int bitsLeft = 0;

        for (byte value : data) {
            buffer <<= 8;
            buffer |= value & 0xFF;
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                int index = (buffer >> (bitsLeft - 5)) & 0x1F;
                bitsLeft -= 5;
                result.append(ALPHABET[index]);
            }
        }

        if (bitsLeft > 0) {
            int index = (buffer << (5 - bitsLeft)) & 0x1F;
            result.append(ALPHABET[index]);
        }

        while (result.length() % 8 != 0) {
            result.append('=');
        }

        return result.toString();
    }

    static byte[] decode(String base32) {
        String normalized = base32.replace("=", "").replace(" ", "").toUpperCase();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        int buffer = 0;
        int bitsLeft = 0;

        for (char c : normalized.toCharArray()) {
            int value = indexOf(c);
            if (value < 0) {
                continue;
            }
            buffer <<= 5;
            buffer |= value & 0x1F;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                output.write((buffer >> (bitsLeft - 8)) & 0xFF);
                bitsLeft -= 8;
            }
        }

        return output.toByteArray();
    }

    private static int indexOf(char value) {
        for (int i = 0; i < ALPHABET.length; i++) {
            if (ALPHABET[i] == value) {
                return i;
            }
        }
        return -1;
    }
}
