package io.github.konbini.market.util;

import android.os.Build;
import android.util.Base64;

public final class Base64Compat {
    private static final char[] ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();

    private Base64Compat() {}

    public static String encode(byte[] data) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) return Base64.encodeToString(data,
                Base64.NO_WRAP);
        StringBuilder sb = new StringBuilder((data.length + 2) / 3 * 4);
        for (int i = 0; i < data.length; i += 3) {
            int b0 = data[i] & 0xFF;
            int b1 = i + 1 < data.length ? data[i + 1] & 0xFF : 0;
            int b2 = i + 2 < data.length ? data[i + 2] & 0xFF : 0;
            sb.append(ALPHABET[b0 >> 2]);
            sb.append(ALPHABET[((b0 & 3) << 4) | (b1 >> 4)]);
            sb.append(i + 1 < data.length ? ALPHABET[((b1 & 15) << 2) | (b2 >> 6)] : '=');
            sb.append(i + 2 < data.length ? ALPHABET[b2 & 63] : '=');
        }
        return sb.toString();
    }
}