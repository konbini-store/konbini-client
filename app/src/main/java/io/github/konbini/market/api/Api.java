package io.github.konbini.market.api;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import com.loopj.android.http.*;

import cz.msebera.android.httpclient.Header;
import io.github.konbini.market.db.Database;
import io.github.konbini.market.ui.ServerMetadata;
import io.github.konbini.market.util.Prefs;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by paul on 14/07/26.
 */

public class Api {
    private static final String default_base_url = "http://konbini.lol";
    private static final long CACHE_TTL_MS = 5 * 60 * 1000L;
    private String base_url = default_base_url;
    private ArrayList<AppShort> memoryApps;
    private long memoryAppsAt;
    private boolean attemptedFeaturedRefresh = false;


    private static Api instance;

    private final SyncHttpClient client = new SyncHttpClient();

    private ServerMetadata serverMetadata;

    private Api(Context context, String base_url) {
        if (base_url != null)
            this.base_url = base_url;

        if (!this.base_url.startsWith("http://") && !this.base_url.startsWith("https://")) {
            this.base_url = "http://"+this.base_url;
        }

        fetchServerMetadata(context);
    }

    @SuppressWarnings("CharsetObjectCanBeUsed")
    private void fetchServerMetadata(Context context) {
        AsyncHttpClient client = new AsyncHttpClient();
        String url = String.format(Locale.ENGLISH, "%s/api/meta.json", this.base_url);
        Log.d("fetchServerMetadata@Api", "Metadata check");
        Log.d("fetchServerMetadata@Api", url);

        client.get(url, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                Log.i("fetchServerMetadata@Api", String.format(Locale.ENGLISH, "Got %d status code, yay!", statusCode));
                try {
                    String result = new String(responseBody, "UTF-8");
                    Log.d("fetchServerMetadata@Api", "meta.json onSuccess: " + result);
                    serverMetadata = new ServerMetadata(new JSONObject(result));
                    Log.d("fetchServerMetadata@Api", "Server last updated: " + serverMetadata.getLastUpdated());

                    long lastUpdated = Prefs.getServerLastUpdated(context);
                    if (lastUpdated != serverMetadata.getLastUpdated()) {
                        Log.d("fetchServerMetadata@Api", "fetchServerMetadata: outdated cache!");
                        Prefs.setServerLastUpdated(context, serverMetadata.getLastUpdated());
                        Prefs.clearCache(context);
                    }
                } catch (Exception e) {
                    Log.e("fetchServerMetadata@Api", "Something went wrong", e);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Log.e("Api", String.format(Locale.ENGLISH, "Got %d status code... :( (line 199)", statusCode));
                Log.w("Api", "Failed to fetch server metadata");
            }
        });
    }

    static synchronized Api getInstance(Context context, String base_url) {
        if (instance == null) {
            instance = new Api(context, base_url);
        }
        return instance;
    }

    public static synchronized Api getInstance(Context context) {
        String url = Prefs.getServer(context);
        return getInstance(context, TextUtils.isEmpty(url) ? default_base_url : url);
    }

    public String getBaseUrl() {
        return this.base_url;
    }

    // Get featured apps
    // Isn't exactly great on memory...
    public ArrayList<AppShort> getFeaturedApps(Context context) {
        Log.d("Api", "getFeaturedApps called");
        ArrayList<AppShort> featured = Database.getFeaturedApps(context);
        if ((featured == null || featured.isEmpty()) && !attemptedFeaturedRefresh) {
            attemptedFeaturedRefresh = true;
            Log.d("Api", "No featured apps found in cache. Clearing stale cache and re-fetching once...");
            Database.clearCache(context);
            memoryApps = null;
            getAllApps(context);
            featured = Database.getFeaturedApps(context);
        }
        Log.d("Api", "Database.getFeaturedApps returned size: " + (featured != null ? featured.size() : "null"));
        return featured;
    }

    @SuppressWarnings("CharsetObjectCanBeUsed")
    public ArrayList<AppShort> getAllApps(Context context) {
        final String url = base_url + "/api/apps.json";
        final ArrayList<AppShort> apps = new ArrayList<>();
        final boolean[] success = {false};
        if (memoryApps != null && System.currentTimeMillis() - memoryAppsAt <= CACHE_TTL_MS) {
            return new ArrayList<>(memoryApps);
        }

        if (Database.hasApps(context)) {
            apps.addAll(Database.getAllApps(context));
            rememberApps(apps);
            Log.d("getAllApps@Api", "Using cached response");
            return apps;
        }

        Log.d("getAllApps@Api", "No cache found.");

        client.get(url, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                Log.i("Api", String.format(Locale.ENGLISH, "Got %d status code, yay!", statusCode));
                String result;
                try {
                    result = new String(responseBody, "UTF-8");
                    Log.d("Api", "onSuccess: "+result);
                    success[0] = parseApps(result, apps);
                    if (success[0]) {
                        rememberApps(apps);
                        Database.saveApps(context, apps);
                    }
                } catch (Exception e) {
                    Log.e("getAllApps@Api", "Something went wrong: ", e);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Log.e("Api", String.format(Locale.ENGLISH, "Got %d status code... :(", statusCode));
            }
        });

        return success[0] ? apps : null;
    }

    public ArrayList<AppShort> searchApps(Context context, String query) {
        if (!Database.hasApps(context)) getAllApps(context);
        return Database.searchApps(context, query);
    }

    private void rememberApps(ArrayList<AppShort> apps) {
        memoryApps = new ArrayList<>(apps);
        memoryAppsAt = System.currentTimeMillis();
    }

    private boolean parseApps(String result, ArrayList<AppShort> apps) {
        try {
            JSONArray array = new JSONArray(result);
            for (int i = 0; i < array.length(); i++) {
                AppShort app = new AppShort(array.getJSONObject(i));
                if (app.isSupported()) apps.add(app);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @SuppressWarnings("CharsetObjectCanBeUsed")
    public JSONArray getCategories(Context context, boolean isGame) {
        final String url = base_url + (isGame
                ? "/api/categories/games.json"
                : "/api/categories/apps.json");

        String cached = Prefs.readCache(context, url);
        if (cached != null) {
            try {
                Log.d("getCategories@Api", "Using cached response");
                return new JSONArray(cached);
            } catch (Exception e) {
                Log.w("Api", "Ignoring invalid cached categories response", e);
            }
        }

        Log.d("getCategories@Api", "No cache found.");

        final JSONArray[] categories = new JSONArray[1];
        client.get(url, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                try {
                    String result = new String(responseBody, "UTF-8");
                    categories[0] = new JSONArray(result);
                    Prefs.writeCache(context, url, result);
                } catch (Exception e) {
                    Log.e("Api", "Failed to parse categories response", e);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Log.e("Api", "Failed to fetch categories", error);
            }
        });
        return categories[0];
    }

    public ArrayList<AppShort> getAuthorApps(Context context, String author) {
        if (author == null || author.length() == 0) return new ArrayList<>();
        return Database.getAppsByAuthor(context, author);
    }

    public ArrayList<AppShort> getCategoryApps(Context context, String category) {
        if (category == null || category.length() == 0) {
            Log.e("getCategoryApps@Api", "Category is null or empty, returning all apps");
            return this.getAllApps(context);
        }
        return Database.getAppsByCategory(context, category);
    }

    @SuppressWarnings("CharsetObjectCanBeUsed")
    public App getApp(Context context, final int app_id) {
        final String url = String.format(Locale.ENGLISH, "%s/api/apps/%d.json", base_url, app_id);
        Log.d("Api", "line 178");
        Log.d("Api", url);
        final App[] app = new App[1];
        final boolean[] success = {false};

        if (Database.hasApp(context, app_id)) {
            try {
                app[0] = Database.getAppById(context, app_id);
                if (app[0] != null && app[0].fullApp)
                    return app[0];
                else
                    Log.w("Api", "Skipping short app");
            } catch (Exception e) {
                Log.w("Api", "Ignoring invalid cached app response", e);
            }
        }

        Log.w("Api", "No cache found.");

        client.get(url, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                Log.i("Api", String.format(Locale.ENGLISH, "Got %d status code, yay!", statusCode));
                String result;
                try {
                    result = new String(responseBody, "UTF-8");
                    Log.d("Api", "onSuccess: "+result);
                    Prefs.writeCache(context, url, result);
                    app[0] = new App(new JSONObject(result));
                    Database.saveFullApp(context, app[0]);
                    success[0] = true;
                } catch (Exception e) {
                    Log.e("getApp@Api", "Something went wrong: ", e);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Log.e("Api", String.format(Locale.ENGLISH, "Got %d status code... :( (line 199)", statusCode));
            }
        });
        Log.d("Api", "getApp: " + (app[0] == null ? "null" : app[0].versions.size()));
        return success[0] ? app[0] : null;
    }
}
