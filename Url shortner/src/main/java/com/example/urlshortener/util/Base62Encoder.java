package com.example.urlshortener.util;

/**
 * Utility class for Base62 encoding/decoding.
 * Encodes long numbers to compact alphanumeric strings (0-9, a-z, A-Z).
 */
public final class Base62Encoder {

    private static final String CHARACTERS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BASE = CHARACTERS.length(); // 62
    private static final int SHORT_CODE_LENGTH = 7;

    private Base62Encoder() {
        // Utility class – no instantiation
    }

    /**
     * Encodes a positive long to a Base62 string.
     *
     * @param value the non-negative long to encode
     * @return Base62 encoded string
     */
    public static String encode(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("Value must be non-negative: " + value);
        }
        if (value == 0) {
            return String.valueOf(CHARACTERS.charAt(0));
        }

        StringBuilder sb = new StringBuilder();
        long v = value;
        while (v > 0) {
            sb.append(CHARACTERS.charAt((int) (v % BASE)));
            v /= BASE;
        }
        return sb.reverse().toString();
    }

    /**
     * Decodes a Base62 string back to a long.
     *
     * @param encoded the Base62 string to decode
     * @return decoded long value
     */
    public static long decode(String encoded) {
        long result = 0;
        for (char c : encoded.toCharArray()) {
            int index = CHARACTERS.indexOf(c);
            if (index < 0) {
                throw new IllegalArgumentException("Invalid Base62 character: " + c);
            }
            result = result * BASE + index;
        }
        return result;
    }

    /**
     * Generates a random Base62 code of fixed length (7 chars).
     *
     * @return a random 7-character Base62 string
     */
    public static String generateRandomCode() {
        long randomLong = (long) (Math.random() * Long.MAX_VALUE);
        String encoded = encode(randomLong);
        // Pad or trim to SHORT_CODE_LENGTH
        if (encoded.length() >= SHORT_CODE_LENGTH) {
            return encoded.substring(0, SHORT_CODE_LENGTH);
        }
        // Pad with leading zeros character
        StringBuilder padded = new StringBuilder();
        for (int i = encoded.length(); i < SHORT_CODE_LENGTH; i++) {
            padded.append(CHARACTERS.charAt(0));
        }
        padded.append(encoded);
        return padded.toString();
    }
}
