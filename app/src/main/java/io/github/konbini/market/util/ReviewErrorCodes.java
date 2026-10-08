package io.github.konbini.market.util;

import java.util.HashMap;

public class ReviewErrorCodes {
    static final HashMap<Integer, String> errorCodes = new HashMap<>(){
        {
            put(0, "Network error.");
            put(400, "Invalid request. Try updating the app.");
            put(401, "Unauthorized. Your app is outdated or you got banned.");
            put(402, "This social server requires payment to send reviews.");
            put(403, "You've been banned from posting reviews on this server.");
            put(404, "This social server does not support sending reviews.");
            put(405, "This social server does not support sending reviews.");
            put(429, "You're sending way too many reviews. Please try again later.");
        }
    };

    public static String getErrorDescription(int errorCode) {
        if (errorCode >= 500) return "Internal server error. Please try again later.";
        if (errorCodes.containsKey(errorCode)) return errorCodes.get(errorCode);
        return "Unknown error.";
    }
}
