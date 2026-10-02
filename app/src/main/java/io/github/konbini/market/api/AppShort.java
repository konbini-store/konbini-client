package io.github.konbini.market.api;

import android.os.Build;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Created by paul on 14/07/26.
 */

public class AppShort {
    public int id = 0;
    public String name;
    public int api = 0;
    public boolean featured = false;
    public String categoryCode = "";
    public String categoryLabel = "";
    public String icon = "";
    public String author = "";
    public String description = "";
    public boolean is_game = false;
    public ArrayList<String> abis = new ArrayList<>();
    public int downloads = 0;
    public double rating = 0;
    public String packageName = "";

    public AppShort(JSONObject obj) throws Exception {
        try {
            this.id = obj.getInt("id");
            this.name = obj.optString("name");
            this.api = obj.optInt("api");
            this.categoryCode = obj.optString("categoryCode", "other_apps");
            this.categoryLabel = obj.optString("categoryLabel", "Other apps");
            this.icon = obj.optString("icon", "");

            JSONArray abis_json = obj.optJSONArray("abis");
            for (int i = 0; i < Objects.requireNonNull(abis_json).length(); i++) {
                abis.add(abis_json.optString(i));
            }

            this.description = obj.optString("description", "No description provided.");
            this.is_game = obj.optBoolean("isGame", false);
            this.author = obj.optString("author", "Unknown");
            this.downloads = obj.optInt("downloads", 0);
            this.rating = obj.optDouble("rating", 0.0);
            this.packageName = obj.getString("packageName");

            Object featuredObj = obj.opt("featured");
            if (featuredObj instanceof Boolean) {
                this.featured = (Boolean) featuredObj;
            } else if (featuredObj instanceof Number) {
                this.featured = ((Number) featuredObj).intValue() != 0;
            } else if (featuredObj instanceof String) {
                String s = (String) featuredObj;
                this.featured = Boolean.parseBoolean(s) || "1".equals(s);
            } else {
                this.featured = obj.optBoolean("featured", false);
            }
            Log.d("AppShort", "App " + this.packageName + " featuredObj: " + featuredObj + " -> parsed featured: " + this.featured);
        } catch (Exception e) {
            Log.e("AppShort", e.toString());
            throw e;
        }
    }

    public AppShort(int id, String name, int api, String categoryCode, String categoryLabel,
                    String icon, ArrayList<String> abis, String description, String author,
                    String packageName, boolean featured) {
        this.id = id;
        this.name = name;
        this.api = api;
        this.categoryCode = categoryCode;
        this.categoryLabel = categoryLabel;
        this.icon = icon;
        this.abis = abis;
        this.description = description;
        this.author = author;
        this.packageName = packageName;
        this.featured = featured;
    }

    boolean isSupported() {
        if (this.api > Build.VERSION.SDK_INT) {
            Log.e("AppVersion", "REJECTED on minSdk -> App requires API: " + this.api + ", Device is API: " + Build.VERSION.SDK_INT);
            return false;
        }

        if (this.abis.isEmpty()) return true; // noarch apks that don't have any libraries

        List<String> abis = Arrays.asList((Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                ? Build.SUPPORTED_ABIS : new String[]{Build.CPU_ABI,
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) ? Build.CPU_ABI2 : "none"});

        boolean hasMatchingAbi = !Collections.disjoint(this.abis, abis);

        if (!hasMatchingAbi) {
            Log.e("AppVersion", "REJECTED on ABI mismatch -> App ABIs: " + this.abis + " | Device ABIs: " + abis);
        }

        return hasMatchingAbi;
    }
}
