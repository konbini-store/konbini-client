package io.github.konbini.market.api;

import android.content.ContentValues;
import android.util.Log;
import android.util.SparseArray;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by paul on 14/07/26.
 */

public class App {
    public int id;
    public String name;
    public String author;
    public String packageId;
    public String icon;
    public ArrayList<String> screenshots = new ArrayList<>();
    public String description;
    public final SparseArray<AppVersion> versions = new SparseArray<>();
    public String categoryId;
    public boolean fullApp;
    public boolean featured;
//    public ArrayList<Review> reviews;

    public App(JSONObject obj) throws JSONException {
        this.id = obj.getInt("id");
        this.name = obj.optString("name");
        this.author = obj.optString("author");
        this.packageId = obj.optString("packageId");
        this.description = obj.optString("description");

        JSONArray versions = obj.optJSONArray("versions");
        if (versions != null) {
            Log.d("App", "App versions: "+ versions);
            for (int i = 0; i < versions.length(); i++) {
                Log.d("App", "App version: "+versions.optJSONObject(i));
                AppVersion version = new AppVersion(versions.optJSONObject(i));
                this.versions.append(version.id, version);
            }
        }

        this.icon = obj.optString("icon");

        JSONArray screenshots = obj.optJSONArray("screenshots");
        if (screenshots != null) {
            for (int i = 0; i < screenshots.length(); i++) {
                this.screenshots.add(screenshots.getString(i));
            }
        }

        this.categoryId = obj.getString("categoryId");
        this.featured = obj.optBoolean("featured");
        this.fullApp = true;
    }

    public App(int id, String name, String author, String packageId, String description,
               String icon, JSONArray screenshots, JSONArray versions, boolean featured) {
        this.id = id;
        this.name = name;
        this.author = author;
        this.packageId = packageId;
        this.description = description;
        this.icon = icon;
        this.fullApp = (screenshots != null && versions != null && description != null);
        this.featured = featured;

        if (screenshots != null) {
            ArrayList<String> screenshots_converted = new ArrayList<>();
            for (int i = 0; i < screenshots.length(); i++) {
                String temp;
                try { temp = screenshots.getString(i); }
                catch (Exception ignored) { continue; }
                screenshots_converted.add(temp);
            }
            this.screenshots = screenshots_converted;
        }

        if (versions != null) {
            for (int i = 0; i < versions.length(); i++) {
                Log.d("App", "App version: "+versions.optJSONObject(i));
                AppVersion version;
                try {
                    version = new AppVersion(versions.optJSONObject(i));
                } catch (JSONException e) {
                    continue;
                }
                this.versions.append(version.id, version);
            }
        }
    }

    public AppVersion getFirstVersion() {
        if (versions.size() == 0) return null;
        AppVersion firstVersion = null;
        for (int j = 0; j < versions.size(); j++) {
            AppVersion version = versions.valueAt(j);
            if (version == null) continue;
            firstVersion = (firstVersion == null || firstVersion.versionCode > version.versionCode)
                    ? version : firstVersion;
        }
        return firstVersion;
    }

    public AppVersion getLastVersion() {
        if (versions.size() == 0) return null;
        AppVersion lastVersion = null;
        for (int j = 0; j < versions.size(); j++) {
            AppVersion version = versions.valueAt(j);
            if (version == null) continue;
            lastVersion = (lastVersion == null || lastVersion.versionCode < version.versionCode)
                    ? version : lastVersion;
        }
        return lastVersion;
    }

    public SparseArray<AppVersion> supportedVersions() {
        Log.d("App", "Versions found:"+ versions.size());
        SparseArray<AppVersion> result = new SparseArray<>();

        for (int j = 0; j < versions.size(); j++) {
            AppVersion version = versions.valueAt(j);
            if (version == null) continue;
            if (version.isSupported()) result.append(version.id, version);
        }

        return result;
    }

    public boolean isSupported() {
        SparseArray<AppVersion> supportedVersions = this.supportedVersions();
        Log.d("App", String.valueOf(supportedVersions.size()));
        return supportedVersions.size() > 0;
    }

    public ContentValues toContentValues() {
        ContentValues cv = new ContentValues();

        cv.put("package_name", this.packageId);
        cv.put("id", this.id);
        cv.put("name", this.name);
        cv.put("author", this.author);
        cv.put("icon", this.icon);
        cv.put("category_code", this.categoryId);
        cv.put("featured", this.featured ? 1 : 0);
        cv.put("full_description", this.description);

        cv.put("screenshots", new JSONArray(this.screenshots).toString());
        cv.put("featured", this.featured ? 1 : 0);

        JSONArray versionsArray = new JSONArray();
        try {
            for (int j = 0; j < this.versions.size(); j++) {
                AppVersion version = this.versions.valueAt(j);
                JSONObject vObj = new JSONObject();
                vObj.put("id", version.id);
                vObj.put("versionCode", version.versionCode);
                vObj.put("versionName", version.versionName);
                vObj.put("minSdk", version.minSdk);
                vObj.put("size", version.size);
                vObj.put("downloadUrl", version.downloadUrl);
                versionsArray.put(vObj);
            }
        } catch (JSONException e) {
            Log.e("toContentValues@App", "Failed to convert versions to JSONArray: ", e);
        }
        cv.put("versions", versionsArray.toString());

        return cv;
    }
}

