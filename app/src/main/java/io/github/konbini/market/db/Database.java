package io.github.konbini.market.db;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.github.konbini.market.api.App;
import io.github.konbini.market.api.AppShort;
import io.github.konbini.market.api.Banner;

public class Database {
    public static SQLiteDatabase getDatabase(Context context) {
        SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(new File(context.getCacheDir(),
                "cache.db"), null);
        db.execSQL("CREATE TABLE IF NOT EXISTS apps (\n" +
                "    package_name TEXT PRIMARY KEY,\n" +
                "    id INTEGER,\n" +
                "    name TEXT,\n" +
                "    api INTEGER,\n" +
                "    category_code TEXT,\n" +
                "    category_label TEXT,\n" +
                "    icon TEXT,\n" +
                "    author TEXT,\n" +
                "    short_description TEXT,\n" +
                "    full_description TEXT,\n" +
                "    downloads INTEGER,\n" +
                "    screenshots TEXT,\n" +
                "    versions TEXT,\n" +
                "    abis TEXT,\n" +
                "    rating REAL,\n" +
                "    cached_at INTEGER,\n" +
                "    featured INTEGER\n" +
                ");");

        db.execSQL("CREATE TABLE IF NOT EXISTS banners (\n" +
                "    image TEXT,\n" +
                "    target TEXT\n" +
                ");");

        return db;
    }

    /// Saves a given list of Banners into the cache database.
    @SuppressWarnings("TryFinallyCanBeTryWithResources")
    public static void saveBanners(Context context, List<Banner> banners) {
        Log.d("Database", "saveBanners count: " + (banners != null ? banners.size() : 0));
        SQLiteDatabase db = getDatabase(context);
        try {
            db.beginTransaction();
            try {
                assert banners != null;
                for (Banner banner : banners) {
                    db.replace("banners", null, banner.toContentValues());
                    Log.d("Database", "Saved banner: " + banner.getImageUrl());
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        } finally {
            db.close();
        }
    }

    @SuppressWarnings("TryFinallyCanBeTryWithResources")
    public static Banner getRandomBanner(Context context) {
        SQLiteDatabase db = getDatabase(context);
        try {
            Cursor cursor = db.query("banners", null, null,
                    null, null, null, "RANDOM()", "1");
            try {
                if (cursor.moveToFirst()) {
                    int image_index = cursor.getColumnIndex("image");
                    int target_index = cursor.getColumnIndex("target");
                    return new Banner(
                            cursor.getString(image_index),
                            cursor.getString(target_index)
                    );
                }
            } finally {
                cursor.close();
            }
        } catch (Exception e) {
            Log.e("getRandomBanner@DB", "Something went wrong when getting a random banner: ", e);
        } finally {
            db.close();
        }
        return null;
    }

    /// Saves a given list of 'short apps' into the cache database.
    /// Can be used with Api.getAllApps to save all apps into the cache
    @SuppressWarnings("TryFinallyCanBeTryWithResources")
    public static void saveApps(Context context, List<AppShort> apps) {
        Log.d("Database", "saveApps count: " + (apps != null ? apps.size() : 0));
        SQLiteDatabase db = getDatabase(context);
        try {
            db.beginTransaction();
            try {
                assert apps != null;
                for (AppShort as : apps) {
                    ContentValues cv = as.toContentValues();
                    cv.put("cached_at", System.currentTimeMillis());
                    db.replace("apps", null, cv);
                    Log.d("Database", "Saved app: " + as.packageName + ", featured: " + as.featured);
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        } finally {
            db.close();
        }
    }

    public static void saveFullApp(Context context, App app) {
        SQLiteDatabase db = null;
        try {
            AppShort base = getShortAppById(context, app.id);
            db = SQLiteDatabase.openOrCreateDatabase(new File(context.getCacheDir(),
                    "cache.db"), null);
            db.beginTransaction();
            ContentValues cv = new ContentValues();
            if (base != null) {
                cv.putAll(base.toContentValues());
            } else {
                Log.w("saveFullApp@Database", String.format("Can't find app #%d!", app.id));
            }

            cv.putAll(app.toContentValues());

            db.replace("apps", null, cv);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e("saveFullApp@Database", "Something went wrong when saving a full app: ", e);
        }
        if (db == null) return;
        db.endTransaction();
        db.close();
    }

    public static App getAppByPackage(Context context, String packageName) {
        return getAppBySelection(context, "package_name = ?", new String[]{packageName});
    }

    public static App getAppById(Context context, int id) {
        return getAppBySelection(context, "id = ?", new String[]{String.valueOf(id)});
    }

    @SuppressLint("Range")
    @SuppressWarnings("TryFinallyCanBeTryWithResources")
    public static App getAppBySelection(Context context, String selection, String[] selectionArgs) {
        SQLiteDatabase db = getDatabase(context);
        try {
            Cursor cursor = db.query("apps", null, selection,
                    selectionArgs, null, null, null, "1");
            try {
                if (cursor.moveToFirst()) {
                    String versionsStr = cursor.getString(cursor.getColumnIndex("versions"));
                    JSONArray versions = null;
                    if (versionsStr != null) versions = new JSONArray(versionsStr);

                    String screenshotsStr = cursor.getString(cursor.getColumnIndex("screenshots"));
                    JSONArray screenshots = null;
                    if (screenshotsStr == null) {
                        screenshots = new JSONArray();
                    }
                    App app = new App(
                            cursor.getInt(cursor.getColumnIndex("id")),
                            cursor.getString(cursor.getColumnIndex("name")),
                            cursor.getString(cursor.getColumnIndex("author")),
                            cursor.getString(cursor.getColumnIndex("package_name")),
                            cursor.getString(cursor.getColumnIndex("full_description")),
                            cursor.getString(cursor.getColumnIndex("icon")),
                            screenshots,
                            versions,
                            cursor.getInt(cursor.getColumnIndex("featured")) != 0
                    );
                    app.fullApp = versionsStr != null && screenshotsStr != null;
                    return app;
                }
            } finally {
                cursor.close();
            }
        } catch (Exception e) {
            Log.e("getAppByPackage@DB", "Something went wrong when getting an app by package: ", e);
        } finally {
            db.close();
        }
        return null;
    }

    @SuppressLint("Range")
    @SuppressWarnings("TryFinallyCanBeTryWithResources")
    public static AppShort getShortAppById(Context context, int appId) {
        SQLiteDatabase db = getDatabase(context);
        try {
            Cursor cursor = db.query("apps", null, "id = ?",
                    new String[]{String.valueOf(appId)}, null, null, null);
            try {
                if (cursor.moveToFirst()) {
                    String abisStr = cursor.getString(cursor.getColumnIndex("abis"));
                    ArrayList<String> abis = new ArrayList<>();
                    if (abisStr != null && !TextUtils.isEmpty(abisStr))
                        abis = new ArrayList<>(Arrays.asList(abisStr.split(",")));
                    return new AppShort(
                            cursor.getInt(cursor.getColumnIndex("id")),
                            cursor.getString(cursor.getColumnIndex("name")),
                            cursor.getInt(cursor.getColumnIndex("api")),
                            cursor.getString(cursor.getColumnIndex("category_code")),
                            cursor.getString(cursor.getColumnIndex("category_label")),
                            cursor.getString(cursor.getColumnIndex("icon")),
                            abis,
                            cursor.getString(cursor.getColumnIndex("short_description")),
                            cursor.getString(cursor.getColumnIndex("author")),
                            cursor.getString(cursor.getColumnIndex("package_name")),
                            cursor.getInt(cursor.getColumnIndex("featured")) != 0
                    );
                }
            } finally {
                cursor.close();
            }
        } catch (Exception e) {
            Log.e("getAppById@Database", "Something went wrong when getting an app by ID: ", e);
        } finally {
            db.close();
        }
        return null;
    }

    public static ArrayList<AppShort> searchApps(Context context, String query) {
        String selection = "name LIKE ? OR package_name LIKE ?";
        String[] selectionArgs = new String[]{"%" + query + "%", "%" + query + "%"};
        return getAppsBySelection(context, selection, selectionArgs);
    }

    public static ArrayList<AppShort> getFeaturedApps(Context context) {
        String selection = "featured != 0";
        Log.d("Database", "getFeaturedApps called");
        ArrayList<AppShort> result = getAppsBySelection(context, selection, null);
        ArrayList<AppShort> allApps = getAllApps(context);

        Log.e("Database", result != null ? result.toString() : "null");
        return result;
    }

    public static ArrayList<AppShort> getAppsByAuthor(Context context, String author) {
        String selection = "author = ?";
        String[] selectionArgs = new String[]{author};
        return getAppsBySelection(context, selection, selectionArgs);
    }

    public static ArrayList<AppShort> getAllApps(Context context) {
        return getAppsBySelection(context, null, null);
    }

    public static ArrayList<AppShort> getAppsByCategory(Context context, String categoryId) {
        return getAppsBySelection(context, "category_code = ?", new String[]{categoryId});
    }

    public static ArrayList<AppShort> getAppsBySelection(Context context, String selection,
                                                         String[] selectionArgs) {
        return getAppsBySelection(context, selection, selectionArgs, "id ASC");
    }

    @SuppressLint("Range")
    @SuppressWarnings("TryFinallyCanBeTryWithResources")
    public static ArrayList<AppShort> getAppsBySelection(Context context, String selection,
                                                         String[] selectionArgs, String orderBy) {
        ArrayList<AppShort> results = new ArrayList<>();
        SQLiteDatabase db = getDatabase(context);
        try {
            Cursor cursor = db.query("apps", null, selection, selectionArgs, null, null, orderBy);
            try {
                Log.d("Database", "getAppsBySelection selection: " + selection + ", count: " + cursor.getCount());
                if (!cursor.moveToFirst()) {
                    Log.d("Database", "getAppsBySelection: moveToFirst false (0 rows found)");
                    return null;
                }

                while (!cursor.isAfterLast()) {
                    String abis_string = cursor.getString(cursor.getColumnIndex("abis"));
                    String[] abis_array = abis_string != null ? abis_string.split(",") : new String[0];
                    ArrayList<String> abis = new ArrayList<>(Arrays.asList(abis_array));

                    String description = cursor.getString(cursor.getColumnIndex("short_description"));
                    if (description == null) {
                        description = cursor.getString(cursor.getColumnIndex("full_description"));
                    }
                    if (description == null) {
                        description = "No description provided.";
                    }

                    results.add(new AppShort(
                            cursor.getInt(cursor.getColumnIndex("id")),
                            cursor.getString(cursor.getColumnIndex("name")),
                            cursor.getInt(cursor.getColumnIndex("api")),
                            cursor.getString(cursor.getColumnIndex("category_code")),
                            cursor.getString(cursor.getColumnIndex("category_label")),
                            cursor.getString(cursor.getColumnIndex("icon")),
                            abis,
                            description,
                            cursor.getString(cursor.getColumnIndex("author")),
                            cursor.getString(cursor.getColumnIndex("package_name")),
                            cursor.getInt(cursor.getColumnIndex("featured")) != 0
                    ));
                    cursor.moveToNext();
                }
            } finally {
                cursor.close();
            }
        } catch (Exception e) {
            Log.e("getAppsBySelection@DB", "Something went wrong when getting apps by selection: ", e);
            Log.e("getAppsBySelection@DB", String.format("Failed selection: \"%s\" with arguments %s",
                    selection, TextUtils.join(",", selectionArgs)));
            return null;
        } finally {
            db.close();
        }

        return results;
    }

    @SuppressWarnings("TryFinallyCanBeTryWithResources")
    public static boolean hasApps(Context context) {
        SQLiteDatabase db = getDatabase(context);
        try {
            Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM apps", null);
            try {
                if (cursor.moveToFirst()) {
                    return cursor.getInt(0) > 0;
                }
            } finally {
                cursor.close();
            }
        } catch (Exception e) {
            Log.e("hasApps@DB", "Something went wrong when checking if there any apps available: ", e);
        } finally {
            db.close();
        }
        return false;
    }

    @SuppressWarnings("TryFinallyCanBeTryWithResources")
    public static boolean hasApp(Context context, String packageName) {
        SQLiteDatabase db = getDatabase(context);
        try {
            Cursor cursor = db.rawQuery("SELECT 1 FROM apps WHERE package_name = ?", new String[]{packageName});
            try {
                return cursor.moveToFirst();
            } finally {
                cursor.close();
            }
        } catch (Exception e) {
            Log.e("hasApps@DB", "Something went wrong when checking if an app is available by package: ", e);
        } finally {
            db.close();
        }
        return false;
    }

    @SuppressWarnings("TryFinallyCanBeTryWithResources")
    public static boolean hasApp(Context context, int appId) {
        SQLiteDatabase db = getDatabase(context);
        try {
            Cursor cursor = db.rawQuery("SELECT 1 FROM apps WHERE id = ?", new String[]{String.valueOf(appId)});
            try {
                return cursor.moveToFirst();
            } finally {
                cursor.close();
            }
        } catch (Exception e) {
            Log.e("hasApps@DB", "Something went wrong when checking if an app is available by ID: ", e);
        } finally {
            db.close();
        }
        return false;
    }

    public static boolean clearCache(Context context) {
        File dbFile = new File(context.getCacheDir(), "cache.db");
        return !dbFile.exists() || dbFile.delete();
    }
}
