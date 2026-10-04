package io.github.konbini.market.api;

import android.content.ContentValues;

import org.json.JSONException;
import org.json.JSONObject;

public class Banner {
    private final String imageUrl;
    private final String targetUrl;

    public Banner(JSONObject object) throws JSONException {
        imageUrl = object.getString("image");
        targetUrl = object.optString("target");
    }

    public Banner(String image, String target) {
        imageUrl = image;
        targetUrl = target;
    }

    public String getImageUrl() { return imageUrl; }
    public String getTargetUrl() { return targetUrl; }

    public ContentValues toContentValues() {
        ContentValues cv = new ContentValues();
        cv.put("image", imageUrl);
        cv.put("target", targetUrl);
        return cv;
    }
}
