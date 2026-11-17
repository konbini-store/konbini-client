package io.github.konbini.market.ui;

import static io.github.konbini.market.db.Database.getDatabase;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

import org.json.JSONObject;

import io.github.konbini.market.R;
import io.github.konbini.market.api.AppShort;
import io.github.konbini.market.api.Api;

import io.github.konbini.market.api.Banner;
import io.github.konbini.market.api.ClientUpdate;
import io.github.konbini.market.net.Http;
import io.github.konbini.market.service.UpdateCheckService;
import io.github.konbini.market.ui.tasks.LoadBannersAsyncTask;
import io.github.konbini.market.ui.tasks.LoadRatingsAsyncTask;
import io.github.konbini.market.util.ImageLoader;
import io.github.konbini.market.util.LocaleHelper;
import io.github.konbini.market.util.Prefs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

@SuppressWarnings("deprecations")
public class MainActivity extends BaseActivity {

    private ListView list;
    private AppListAdapter adapter;
    private final ArrayList<AppShort> items = new ArrayList<>();

    private View loadingOverlay;
    public ImageView bannerImage;
    private static boolean showedUpdatePopup = false;
    private static Banner CACHE_BANNER = null;
    private static long BANNER_CACHE_TIME = 0L;

    public Api api;

    private static final ArrayList<AppShort> CACHE_ITEMS = new ArrayList<>();
    private static long CACHE_TIME = 0L;

    private final Handler bannerHandler = new Handler(Looper.getMainLooper());
    private final Runnable bannerRunnable = new Runnable() {
        @Override
        @SuppressWarnings("deprecation")
        public void run() {
            new LoadBannersAsyncTask(MainActivity.this).execute();
            bannerHandler.postDelayed(this, 10000L);
        }
    };

    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LocaleHelper.applySavedLocale(this);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        setContentView(R.layout.activity_main);

        loadingOverlay = findViewById(R.id.loadingOverlay);
        ImageButton logo = findViewById(R.id.logo);
        list = findViewById(R.id.list);
        Button btnApps = findViewById(R.id.btnApps);
        Button btnGames = findViewById(R.id.btnGames);
        Button btnDownloads = findViewById(R.id.btnDownloads);
        ImageButton btnSearch = findViewById(R.id.btnSearch);
        TextView txtMarket = findViewById(R.id.txtMarket);

        View header = getLayoutInflater().inflate(R.layout.main_list_header, list, false);
        bannerImage = header.findViewById(R.id.promoBannerImage);
        list.addHeaderView(header, null, false);

        this.api = Api.getInstance(this);

        try {
            Typeface tf = Typeface.createFromAsset(getAssets(), "fonts/storopia.ttf");
            txtMarket.setTypeface(tf);
        } catch (Exception e) {
            Log.e("onCreate@MainActivity", "Failed to set fonts: ", e);
        }

        adapter = new AppListAdapter(this, items);
        list.setAdapter(adapter);

        list.setOnItemClickListener((parent, view, position, id) -> {
            int idx = position - list.getHeaderViewsCount();
            if (idx < 0 || idx >= items.size()) return;
            AppShort it = items.get(idx);
            Intent i = new Intent(MainActivity.this, AppDetailActivity.class);
            Log.d("MainActivity@134", String.valueOf(it.id));
            i.putExtra("app_id", it.id);
            startActivity(i);
        });

        btnSearch.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, SearchActivity.class)));
        btnApps.setOnClickListener(v -> openCategories(false));
        btnGames.setOnClickListener(v -> openCategories(true));
        btnDownloads.setOnClickListener(v -> openDownloads());
        logo.setOnClickListener(v -> {
            try { openOptionsMenu(); } catch (Exception e) {
                Log.e("onCreate@MainActivity", "Failed to open options menu: ", e);
            }
        });

        try {
            int androidLogoRes = R.drawable.market_android_logo;
            ImageView iw = findViewById(R.id.imgAndroidWord);
            if (iw != null && androidLogoRes != 0) iw.setImageResource(androidLogoRes);
        } catch (Exception e) {
            Log.e("onCreate@MainActivity", "Failed to set imgAndroidWord image: ", e);
        }
        getDatabase(this);
        if (!restoreFromCache()) {
            loadTopContent();
        }
        checkClientUpdateIfNeeded();
        getConsentFromUserThenSendAnalytics();
        scheduleUpdateChecks();
    }

    private void scheduleUpdateChecks() {
        try {
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            Intent i = new Intent(this, UpdateCheckService.class);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 23) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }
            PendingIntent pi = PendingIntent.getService(this, 30001, i, flags);
            long interval = 5L * 60L * 1000L;
            long first = System.currentTimeMillis() + 15000L;
            am.setInexactRepeating(AlarmManager.RTC_WAKEUP, first, interval, pi);
        } catch (Exception e) {
            Log.e("scheduleUpdate@Main",
                    "Failed to schedule update check: ", e);
        }
    }

    protected void onResume() {
        super.onResume();
        adapter.refreshInstalledPackages();
        adapter.notifyDataSetChanged();

        bannerHandler.postDelayed(bannerRunnable, 10000L);
    }

    @Override
    protected void onPause() {
        super.onPause();
        bannerHandler.removeCallbacks(bannerRunnable);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem downloads = menu.add(0, 3, 0, getString(R.string.downloads));
        downloads.setIcon(R.drawable.ic_menu_downloads);
        MenuItem profile = menu.add(0, 1, 1, Prefs.isLoggedIn(this) ? getString(R.string.profile) : getString(R.string.login));
        profile.setIcon(R.drawable.ic_menu_account);
        MenuItem settings = menu.add(0, 2, 2, getString(R.string.settings));
        settings.setIcon(R.drawable.ic_menu_settings_custom);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 1) {
            startActivity(new Intent(this, Prefs.isLoggedIn(this) ? ProfileActivity.class : LoginActivity.class));
            return true;
        }
        if (item.getItemId() == 2) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        if (item.getItemId() == 3) {
            openDownloads();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void openCategories(boolean isGame) {
        Intent i = new Intent(this, CategoryListActivity.class);
        i.putExtra("is_game", isGame);
        startActivity(i);
    }

    private void openDownloads() { startActivity(new Intent(this, DownloadsActivity.class)); }

    // Helper method to open the URL safely
    private void launchIntent(Activity activity, Uri uri, boolean isInternal) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            if (isInternal) {
                intent.setPackage(activity.getPackageName());
            }

            activity.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e("LoadBannersAsyncTask", "No app available to handle URL: " + uri, e);
            Toast.makeText(activity, "Failed to open :(", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("LoadBannersAsyncTask", "Failed to open target URL: ", e);
        }
    }

    public void displayBanner(Banner banner) {
        String imageUrl = banner.getImageUrl();
        String targetUrl = banner.getTargetUrl();

        ImageLoader.loadBanner(this, imageUrl, this.bannerImage, R.drawable.icon_placeholder);

        this.bannerImage.post(() -> {
            int width = this.bannerImage.getWidth();
            if (width > 0) {
                android.view.ViewGroup.LayoutParams params =
                        this.bannerImage.getLayoutParams();
                params.height = Math.round(width * (9.0f / 16.0f));
                this.bannerImage.setLayoutParams(params);
            }
        });

        this.bannerImage.setOnClickListener(v -> {
            final Uri uri = Uri.parse(targetUrl);
            if (!Objects.equals(uri.getScheme(), "konbini")) {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.warning)
                        .setMessage(R.string.third_party_website_warning)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            launchIntent(this, uri, false);
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            } else {
                launchIntent(this, uri, true);
            }
        });
    }

    @SuppressWarnings("deprecation")
    private boolean restoreFromCache() {
        if (CACHE_ITEMS.isEmpty()) return false;
        if (System.currentTimeMillis() - CACHE_TIME > 120000L) return false;
        items.clear();
        items.addAll(CACHE_ITEMS);
        adapter.refreshInstalledPackages();
        adapter.notifyDataSetChanged();
        if (CACHE_BANNER != null && System.currentTimeMillis() - BANNER_CACHE_TIME < 10000L) {
            displayBanner(CACHE_BANNER);
        } else {
            new LoadBannersAsyncTask(this).execute();
        }
        new LoadRatingsAsyncTask(this, ratings -> {
            boolean updated = false;
            for (AppShort app : items) {
                if (app.packageName != null && ratings.containsKey(app.packageName)) {
                    Double r = ratings.get(app.packageName);
                    if (r != null) {
                        app.rating = r;
                        updated = true;
                    }
                }
            }
            if (updated) adapter.notifyDataSetChanged();
        }).execute();
        showLoading(false);
        return true;
    }

    @SuppressWarnings("deprecation")
    private void loadTopContent() {
        final ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage(getString(R.string.loading));
        pd.setCancelable(false);
        pd.show();
        new LoadTopContentTask(this, api, pd).execute();
    }

    @SuppressWarnings("deprecation")
    private static class LoadTopContentTask extends AsyncTask<Void, Void, ArrayList<AppShort>> {
        private final WeakReference<MainActivity> activityRef;
        private final Api api;
        private final ProgressDialog pd;

        LoadTopContentTask(MainActivity activity, Api api, ProgressDialog pd) {
            this.activityRef = new WeakReference<>(activity);
            this.api = api;
            this.pd = pd;
        }

        protected void onPreExecute() {
            MainActivity activity = activityRef.get();
            if (activity != null) {
                activity.showLoading(true);
            }
        }

        protected ArrayList<AppShort> doInBackground(Void... v) {
            MainActivity activity = activityRef.get();
            if (activity == null) return new ArrayList<>();
            ArrayList<AppShort> apps = api.getFeaturedApps(activity);
            if (apps != null)
                Log.i("MainActivity", "App size::: "+apps.size());

            return (apps != null) ? apps : new ArrayList<>();
        }

        protected void onPostExecute(ArrayList<AppShort> out) {
            MainActivity activity = activityRef.get();
            if (activity == null) return;
            try {
                if (pd != null) pd.dismiss();
            } catch (Exception e) {
                Log.e("LoadTopContentTask", Objects.requireNonNull(e.getMessage()));
            }
            activity.showLoading(false);
            if (out == null) {
                Toast.makeText(activity, R.string.error_network, Toast.LENGTH_SHORT).show();
                return;
            }
            activity.items.clear();
            activity.items.addAll(out);
            CACHE_ITEMS.clear();
            CACHE_ITEMS.addAll(out);
            CACHE_TIME = System.currentTimeMillis();
            activity.adapter.refreshInstalledPackages();
            activity.adapter.notifyDataSetChanged();
            new LoadBannersAsyncTask(activity).execute();
            new LoadRatingsAsyncTask(activity, ratings -> {
                boolean updated = false;
                for (AppShort app : activity.items) {
                    if (app.packageName != null && ratings.containsKey(app.packageName)) {
                        Double r = ratings.get(app.packageName);
                        if (r != null) {
                            app.rating = r;
                            updated = true;
                        }
                    }
                }
                if (updated) activity.adapter.notifyDataSetChanged();
            }).execute();
        }

        protected void onCancelled(ArrayList<AppShort> out) {
            try {
                if (pd != null) pd.dismiss();
            } catch (Exception e) {
                Log.e("LoadTopContentTask", Objects.requireNonNull(e.getMessage()));
            }
            MainActivity activity = activityRef.get();
            if (activity != null) {
                activity.showLoading(false);
            }
        }
    }

    private void showLoading(boolean show) {
        if (loadingOverlay != null) loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @SuppressWarnings("deprecation")
    private void checkClientUpdateIfNeeded() {
        if (showedUpdatePopup) return;
        new CheckClientUpdateTask(this).execute();
        showedUpdatePopup = true;
    }

    @SuppressWarnings("deprecation")
    private static class CheckClientUpdateTask extends AsyncTask<Void, Void, ClientUpdate> {
        private final WeakReference<MainActivity> activityRef;

        CheckClientUpdateTask(MainActivity activity) {
            this.activityRef = new WeakReference<>(activity);
        }

        protected ClientUpdate doInBackground(Void... params) {
            MainActivity activity = activityRef.get();
            if (activity == null) return null;
            try {
                return activity.api.getLatestVersion();
            } catch (Exception e) { return null; }
        }

        protected void onPostExecute(ClientUpdate o) {
            final MainActivity activity = activityRef.get();
            if (activity == null) return;
            if (o == null) {
                Log.e("clientUpdate", "Error checking for client update: Null response");
                return;
            }
            try {
                int latestCode = o.versionCode;
                String latestName = o.versionName;
                String updateUrl = o.updateUrl;
                String notes = o.notes.getLocalizedString();
                PackageInfo pi = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
                if (latestCode > pi.versionCode && !TextUtils.isEmpty(updateUrl)) {
                    StringBuilder msg = new StringBuilder(activity.getString(R.string.client_update_message));
                    if (!TextUtils.isEmpty(latestName)) {
                        msg.append("\n\n").append(activity.getString(R.string.version)).append(" ").append(latestName);
                    }
                    if (!TextUtils.isEmpty(notes)) {
                        msg.append("\n\n").append(activity.getString(R.string.client_update_note_prefix)).append(" ").append(notes);
                    }
                    final String finalUrl = updateUrl;
                    new AlertDialog.Builder(activity)
                            .setTitle(activity.getString(R.string.client_update_title))
                            .setMessage(msg.toString())
                            .setPositiveButton(activity.getString(R.string.update_now), (dialog, which) -> {
                                try {
                                    activity.startActivity(new Intent(Intent.ACTION_VIEW,
                                            Uri.parse(finalUrl)));
                                } catch (Exception e) {
                                    Log.e("onPostExecute@CCUT",
                                            "Failed to open activity: ", e);
                                }
                            })
                            .setNegativeButton(activity.getString(R.string.later), null)
                            .show();
                }
            } catch (Exception e) {
                Log.e("clientUpdate",
                        "Error checking for client update: " + e.getMessage());
            }
        }
    }

    private void getConsentFromUserThenSendAnalytics() {
        Boolean consent = Prefs.getAnalyticsConsent(this);

        if (consent == null) {
            new AlertDialog.Builder(this)
                    .setTitle("Analytics")
                    .setMessage("This app sends information about your downloads, as well as periodic information about your device, to the social server specified in the settings.\n" +
                            "\n" +
                            "Do you consent to this type of data collection? You can always change your mind later in the settings.")

                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        Prefs.setAnalyticsConsent(MainActivity.this, true);
                        sendAnalyticsIfAllowed();
                    })

                    .setNegativeButton(android.R.string.cancel, (dialog, which) -> Prefs.setAnalyticsConsent(MainActivity.this, false))
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();

        } else if (consent) {
            sendAnalyticsIfAllowed();
        }
    }

    // TODO
    @SuppressWarnings("deprecation")
    private void sendAnalyticsIfAllowed() {
        Boolean consent = Prefs.getAnalyticsConsent(this);
        if (consent == null || !consent) return;

        Log.d("sendAnalytics@Main", "Sending analytics...");

        long now = System.currentTimeMillis();
        if (now - Prefs.getLastAnalyticsSentAt(this) < 12L * 60L * 60L * 1000L) return;
        Prefs.setLastAnalyticsSentAt(this, now);
        new SendAnalyticsTask(this).execute();
    }

    @SuppressWarnings("deprecation")
    private static class SendAnalyticsTask extends AsyncTask<Void, Void, Void> {
        private final WeakReference<MainActivity> activityRef;

        SendAnalyticsTask(MainActivity activity) {
            this.activityRef = new WeakReference<>(activity);
        }

        protected Void doInBackground(Void... params) {
            MainActivity activity = activityRef.get();
            if (activity == null) return null;
            try {
                PackageInfo pi = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
                JSONObject o = new JSONObject();
                o.put("api_level", Build.VERSION.SDK_INT);
                o.put("app_version_code", pi.versionCode);
                o.put("app_version_name", pi.versionName == null ? "<no version name>" : pi.versionName);
                o.put("device_model", Build.MODEL == null ? "<no model>" : Build.MODEL);
                o.put("manufacturer", Build.MANUFACTURER == null ? "<no manufacturer>" : Build.MANUFACTURER);
                o.put("lang", Locale.getDefault().getLanguage());
                Log.d("sendAnalytics@Main", "About to send to "+Prefs.getSocialServer(activity) + "/api/putStats"+"...");
                Log.d("sendAnalytics@Main", "Response: "+Http.postJson(Prefs.getSocialServer(activity) + "/api/putStats", o.toString()));
            } catch (Exception e) {
                Log.e("SendAnalyticsTask", "Something went wrong when trying to send analytics: ", e);
            }
            return null;
        }
    }

    private void showApi25WarningIfNeeded() {
        if (Build.VERSION.SDK_INT >= 25) {
            new AlertDialog.Builder(this)
                    .setTitle("Warning")
                    .setMessage("Konbini can work badly on newer devices.")
                    .setPositiveButton("OK", (dialog, which) -> { })
                    .show();
        }
    }
}
