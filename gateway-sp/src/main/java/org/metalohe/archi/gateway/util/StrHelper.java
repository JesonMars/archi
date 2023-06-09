package org.metalohe.archi.gateway.util;

public class StrHelper {
    public static String EmptyString = "";

    public StrHelper() {
    }

    public static int GetHashcode(String str) {
        int hash1 = 5381;
        int hash2 = hash1;
        int len = str.length();

        for(int i = 0; i < len; ++i) {
            int c = str.charAt(i);
            hash1 = (hash1 << 5) + hash1 ^ c;
            ++i;
            if (i >= len) {
                break;
            }

            c = str.charAt(i);
            hash2 = (hash2 << 5) + hash2 ^ c;
        }

        return hash1 + hash2 * 1566083941;
    }

    public static boolean isEmptyOrNull(String str) {
        return "".equals(str) || str == null;
    }
}