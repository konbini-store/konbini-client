package io.github.konbini.market.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;
import org.json.JSONObject;

import io.github.konbini.market.api.*;

import io.github.konbini.market.R;
import io.github.konbini.market.db.Database;
import io.github.konbini.market.service.DownloadService;
import io.github.konbini.market.ui.tasks.AddReviewCommentAsyncTask;
import io.github.konbini.market.ui.tasks.LoadDetailsAsyncTask;
import io.github.konbini.market.ui.tasks.LoadReviewsAsyncTask;
import io.github.konbini.market.ui.tasks.LoadScreenshotsAsyncTask;
import io.github.konbini.market.ui.tasks.ReportReviewAsyncTask;
import io.github.konbini.market.ui.tasks.SendReactionAsyncTask;
import io.github.konbini.market.ui.tasks.SendReviewAsyncTask;
import io.github.konbini.market.ui.tasks.ShowCommentsAsyncTask;
import io.github.konbini.market.util.ImageLoader;
import io.github.konbini.market.util.LocaleHelper;
import io.github.konbini.market.util.Prefs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import static android.view.View.GONE;

public class AppDetailActivity extends Activity {
    public int appId;

    public ImageView imgIcon;
    public TextView txtName;
    public TextView txtAuthor;
    public TextView txtMeta;
    public TextView txtDesc;
    public TextView txtToggle;
    public TextView txtDownloadsInfo;
    public TextView txtReviewsInfo;
    public TextView txtHeaderRating;
    public TextView txtreviewinfo;
    public RatingBar ratingHeader;
    public RatingBar ratingAddReview;
    private Button btnInstall;
    private Button btnOpen;
    private Button btnUninstall;
    public TextView txtScreensTitle;
    public TextView txtReviewsTitle;
    private TextView txtDownloadProgress;
    public HorizontalScrollView screensScroll;
    public LinearLayout screensContainer;
    private LinearLayout downloadPanel;
    private LinearLayout installButtons;
    private ProgressBar progressDownload;

    private LinearLayout reviewsTabContainer;
    private View detailsScrollView, reviewsScrollView;
    private ListView listVersions;
    private Button btnTabDetails, btnTabVersions, btnTabReviews;
    public final ArrayList<ReviewItem> reviews = new ArrayList<>();
    public ReviewAdapter adapter;

    public String pkgName = "";
    private final String selectedVersion = "";
    public boolean hasOwnReview = false;
    public String currentIconFile = "";

    private View loadingOverlay;
    private TextView txtLoading;

    public App app;
    public Boolean appInitialized = false;

    public boolean descCollapsed = true;

    public Api api;

    private final BroadcastReceiver dlReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int id = intent.getIntExtra("app_id", -1);
            if (id != appId) return;

            int percent = intent.getIntExtra("percent", 0);
            long speed = intent.getLongExtra("speed_bps", 0);
            boolean done = intent.getBooleanExtra("done", false);
            boolean error = intent.getBooleanExtra("error", false);
            boolean cancelled = intent.getBooleanExtra("cancelled", false);
            boolean active = intent.getBooleanExtra("active", false);

            if (active) showDownloadUi(percent, speed);
            if (done) {
                hideDownloadUi();
                String path = intent.getStringExtra("file_path");
                if (path != null && hasWindowFocus()) openInstaller(path);
            }
            if (error) {
                hideDownloadUi();
                String message = intent.getStringExtra("error_message");
                Toast.makeText(AppDetailActivity.this,
                        "Download failed: " + (message == null ? "Unknown error" : message),
                        Toast.LENGTH_LONG).show();
            }
            if (cancelled) hideDownloadUi();
        }
    };

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LocaleHelper.applySavedLocale(this);
        setContentView(R.layout.activity_app_detail);

        appId = getIntent().getIntExtra("app_id", 0);

        loadingOverlay = findViewById(R.id.loadingOverlay);
        txtLoading = findViewById(R.id.txtLoading);
        LinearLayout detailsContainer = findViewById(R.id.detailsContainer);
        reviewsTabContainer = findViewById(R.id.reviewsTabContainer);
        detailsScrollView = findViewById(R.id.detailsScrollView);
        reviewsScrollView = findViewById(R.id.reviewsScrollView);
        ListView list = findViewById(R.id.listReviews);
        listVersions = findViewById(R.id.listVersions);
        btnTabDetails = findViewById(R.id.btnTabDetails);
        btnTabVersions = findViewById(R.id.btnTabVersions);
        btnTabReviews = findViewById(R.id.btnTabReviews);

        View header = LayoutInflater.from(this).inflate(R.layout.app_detail_header, detailsContainer, false);
        detailsContainer.addView(header);

        imgIcon = findViewById(R.id.imgIcon);
        txtName = findViewById(R.id.txtName);
        txtAuthor = findViewById(R.id.txtAuthor);
        txtHeaderRating = findViewById(R.id.txtHeaderRating);
        ratingHeader = findViewById(R.id.ratingHeader);

        txtDownloadsInfo = header.findViewById(R.id.txtDownloadsInfo);
        txtReviewsInfo = header.findViewById(R.id.txtReviewsInfo);
        txtMeta = header.findViewById(R.id.txtMeta);
        txtDesc = header.findViewById(R.id.txtDesc);
        txtToggle = header.findViewById(R.id.toggleDescriptionTextView);
        txtScreensTitle = header.findViewById(R.id.txtScreensTitle);
        screensScroll = header.findViewById(R.id.screensScroll);
        screensContainer = header.findViewById(R.id.screensContainer);
        txtReviewsTitle = findViewById(R.id.txtReviewsTitle);
        ratingAddReview = findViewById(R.id.ratingAddReview);
        txtreviewinfo = findViewById(R.id.txtreviewinfo);

        btnInstall = findViewById(R.id.btnInstall);
        btnOpen = findViewById(R.id.btnOpen);
        btnUninstall = findViewById(R.id.btnUninstall);
        Button btnCancelDownload = findViewById(R.id.btnCancelDownload);
        txtDownloadProgress = findViewById(R.id.txtDownloadProgress);
        progressDownload = findViewById(R.id.progressDownload);
        downloadPanel = findViewById(R.id.downloadPanel);
        installButtons = findViewById(R.id.installButtons);

        adapter = new ReviewAdapter();
        list.setAdapter(adapter);

        list.setOnItemClickListener((parent, view, position, id) -> {
            if (position < 0 || position >= reviews.size()) return;
            showReviewActionsDialog(reviews.get(position));
        });

        btnInstall.setOnClickListener(v -> {
            AppVersion target = getLatestSupportedVersion();
            if (target != null) {
                startDownload(target.id);
            } else {
                Toast.makeText(this, "Unable to find the latest supported version.",
                        Toast.LENGTH_SHORT).show();
            }
        });
        btnOpen.setOnClickListener(v -> openApp());
        btnUninstall.setOnClickListener(v -> uninstallApp());
        btnCancelDownload.setOnClickListener(v -> {
            Intent i = new Intent(AppDetailActivity.this, DownloadService.class);
            i.setAction(DownloadService.ACTION_CANCEL);
            i.putExtra("app_id", appId);
            startService(i);
        });

        btnTabDetails.setOnClickListener(v -> showTab(0));
        btnTabVersions.setOnClickListener(v -> showTab(1));
        btnTabReviews.setOnClickListener(v -> showTab(2));
        listVersions.setOnItemClickListener((parent, view, position, id) -> {
            AppVersion version = ((AppVersionAdapter) parent.getAdapter()).getItem(position);
            if (version != null) startDownload(version.id);
        });

        ratingAddReview.setOnTouchListener((v, event) -> {
            v.performClick();
            if (event.getAction() != MotionEvent.ACTION_UP) return true;
            if (!Prefs.isLoggedIn(AppDetailActivity.this)) {
                startActivity(new Intent(AppDetailActivity.this, LoginActivity.class));
                return true;
            }
            if (hasOwnReview) return true;
            RatingBar rb = (RatingBar) v;
            float stars = rb.getNumStars() * event.getX() / Math.max(1f, rb.getWidth());
            int rating = (int) Math.ceil(stars);
            if (rating < 1) rating = 1;
            if (rating > 5) rating = 5;
            rb.setRating(rating);
            showAddReviewDialog(rating);
            rb.setRating(0f);
            return true;
        });
//                (View v, MotionEvent event) -> {
//            v.performClick();
//            if (event.getAction() != MotionEvent.ACTION_UP) return true;
//            if (!Prefs.isLoggedIn(AppDetailActivity.this)) {
//                startActivity(new Intent(AppDetailActivity.this, LoginActivity.class));
//                return true;
//            }
//            if (hasOwnReview) return true;
//            RatingBar rb = (RatingBar) v;
//            float stars = rb.getNumStars() * event.getX() / Math.max(1f, rb.getWidth());
//            int rating = (int) Math.ceil(stars);
//            if (rating < 1) rating = 1;
//            if (rating > 5) rating = 5;
//            rb.setRating(rating);
//            showAddReviewDialog(rating);
//            rb.setRating(0f);
//            return true;
//        });

        api = Api.getInstance(AppDetailActivity.this);
        if (!handleIntent(getIntent()) || appId == 0) {
            finish();
            return;
        }

        showLoading(true, getString(R.string.loading));
        showTab(0);
        loadDetails();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private boolean handleIntent(Intent intent) {
        Uri uri = intent.getData();
        if (uri != null && Intent.ACTION_VIEW.equals(intent.getAction())) {
            String packageName = uri.getLastPathSegment();
            if (packageName == null) return false;

            app = Database.getAppByPackage(this, packageName);
            if (app == null) {
                Toast.makeText(this, "App not found", Toast.LENGTH_SHORT).show();
                return false;
            }
            appId = app.id;
        }
        return true;
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onResume() {
        super.onResume();
        Log.d("AppDetailActivity", "onResume is called!");
        IntentFilter filter = new IntentFilter("io.github.konbini.market.DOWNLOAD_PROGRESS");
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(dlReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(dlReceiver, filter);
        }
        restoreDownloadState();
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(dlReceiver);
        } catch (Exception e) {
            Log.e("onPause@AppDetail", "Something went wrong: ", e);
        }
    }

    private SharedPreferences downloadPrefs() {
        return getSharedPreferences("download_state", MODE_PRIVATE);
    }

    public void restoreDownloadState() {
        SharedPreferences p = downloadPrefs();
        if (p.getBoolean("active", false) && p.getInt("app_id", -1) == appId) {
            showDownloadUi(p.getInt("percent", 0), p.getLong("speed_bps", 0));
        } else {
            hideDownloadUi();
        }
    }

    private void showDownloadUi(int percent, long speed) {
        downloadPanel.setVisibility(View.VISIBLE);
        installButtons.setVisibility(GONE);
        progressDownload.setProgress(percent);
        String speedText = speed >= 1024 * 1024
                ? String.format(Locale.US, "%.1f MB/s", speed / 1024f / 1024f)
                : Math.max(1, speed / 1024) + " KB/s";
        txtDownloadProgress.setText(percent + "%  •  " + speedText);
    }

    private void hideDownloadUi() {
        Log.d("AppDetailActivity", "hideDownloadUi is called!");
        downloadPanel.setVisibility(GONE);
        installButtons.setVisibility(View.VISIBLE);
        progressDownload.setProgress(0);
        txtDownloadProgress.setText("0%");
        Log.i("AppDetailActivity", "App is initialized: "+(this.appInitialized.toString()));
        refreshInstalledButtons(this.app);
    }

    private void showTab(int tab) {
        detailsScrollView.setVisibility(tab == 0 ? View.VISIBLE : GONE);
        listVersions.setVisibility(tab == 1 ? View.VISIBLE : GONE);
        reviewsScrollView.setVisibility(tab == 2 ? View.VISIBLE : GONE);

        btnTabDetails.setSelected(tab == 0);
        btnTabVersions.setSelected(tab == 1);
        btnTabReviews.setSelected(tab == 2);
        btnTabDetails.setEnabled(true);
        btnTabVersions.setEnabled(true);
        btnTabReviews.setEnabled(true);
    }

    public void bindVersionsTab() {
        if (app == null || app.versions.size() == 0) {
            listVersions.setAdapter(null);
            return;
        }
        AppVersionAdapter versionsAdapter = new AppVersionAdapter(this, app.versions);
        listVersions.setAdapter(versionsAdapter);
    }

    private AppVersion getLatestSupportedVersion() {
        if (app == null || app.versions.size() == 0) return null;

        AppVersion latest = null;
        for (int i = 0; i < app.versions.size(); i++) {
            AppVersion version = app.versions.valueAt(i);
            if (version == null || !version.isSupported()) continue;
            if (latest == null || version.versionCode > latest.versionCode) {
                latest = version;
            }
        }
        return latest;
    }

    @SuppressWarnings("deprecation")
    private void loadDetails() {
        showLoading(true, getString(R.string.loading));
        new LoadDetailsAsyncTask(this).execute();
    }

    @SuppressWarnings("deprecation")
    public void loadScreenshots() {
        new LoadScreenshotsAsyncTask(this).execute();
    }

    @SuppressWarnings("deprecation")
    public void loadReviews() {
        new LoadReviewsAsyncTask(this).execute();
    }

    public ReviewItem parseReview(JSONObject r) {
        ReviewItem ri = new ReviewItem();
        ri.userId = r.optString("user_id", "");
        ri.username = r.optString("username", "User");
        ri.avatar = r.optString("avatar", "default_avatar.png");
        ri.rating = r.optInt("rating", 0);
        ri.text = r.optString("comment", r.optString("text", ""));
        ri.createdAt = r.optInt("updated_at", 0);
        return ri;
    }

    private void showAddReviewDialog(final int presetRating) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_review, null);
        TextView lbl = dialogView.findViewById(R.id.txtReviewRating);
        final EditText edt = dialogView.findViewById(R.id.edtReviewText);

        lbl.setText(String.format(getString(R.string.rating), presetRating));

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.post_review))
                .setView(dialogView)
                .setPositiveButton(getString(R.string.send_review), (dialog, which) -> {
                    String text = edt.getText().toString().trim();
                    sendReview(text, presetRating);
                })
                .setNegativeButton(getString(R.string.cancel_review), null)
                .show();
    }

    @SuppressWarnings("deprecation")
    private void sendReview(final String text, final int rating) {
        final int uid = Prefs.getUserId(this);
        final String token = Prefs.getAuthKey(this);
        if (uid <= 0) {
            msg("Login required");
            return;
        }
        final int safeRating = Math.max(1, Math.min(5, rating));
        new SendReviewAsyncTask(this, token, uid, safeRating, text).execute();
    }

    @SuppressWarnings("deprecation")
    private void showCommentsDialog(final int reviewId) {
        new ShowCommentsAsyncTask(this, reviewId).execute();
    }

    public void showAddCommentDialog(final int reviewId) {
        final EditText edt = new EditText(this);
        edt.setHint(isRu() ? "Комментарий" : "Comment");
        new AlertDialog.Builder(this)
                .setTitle(isRu() ? "Добавить комментарий" : "Add comment")
                .setView(edt)
                .setPositiveButton(isRu() ? "Отправить" : "Send", (d, w) -> {
                    String text = edt.getText().toString().trim();
                    if (text.length() == 0) return;
                    addReviewComment(reviewId, text);
                })
                .setNegativeButton(isRu() ? "Отмена" : "Cancel", null)
                .show();
    }

    @SuppressWarnings("deprecation")
    private void addReviewComment(final int reviewId, final String text) {
        final int uid = Prefs.getUserId(this);
        if (uid <= 0) {
            msg("Login required");
            return;
        }
        new AddReviewCommentAsyncTask(this, reviewId, text, uid).execute();
    }

    @SuppressWarnings("deprecation")
    private void sendReaction(final int reviewId, final int value) {
        final int uid = Prefs.getUserId(this);
        if (uid <= 0) {
            msg("Login required");
            return;
        }
        new SendReactionAsyncTask(this, reviewId, value, uid).execute();
    }

    @SuppressWarnings("deprecation")
    private void reportReview(final int reviewId) {
        final int uid = Prefs.getUserId(this);
        if (uid <= 0) {
            msg("Login required");
            return;
        }
        new ReportReviewAsyncTask(this, reviewId, uid).execute();
    }

    private void startDownload(final int version_id) {

        // Create a background thread to handle the network operation
        new Thread(() -> {
        try {
            Log.d("AppDetailActivity", "Versions found: "+ app.versions.size());
            Log.d("AppDetailActivity", "Version requested: "+ version_id);
            for (int i = 0; i < app.versions.size(); i++) {
                Log.d("AppDetailActivity", "Version found: "+app.versions.keyAt(i));
            }
            AppVersion version = app.versions.get(version_id, null);

            if (version == null) {
                runOnUiThread(() -> Toast.makeText(AppDetailActivity.this, "Failed to get the app.", Toast.LENGTH_SHORT).show());
                return;
            }

            final String finalUrl = version.downloadUrl;
            runOnUiThread(() -> {
                Intent i = new Intent(AppDetailActivity.this, DownloadService.class);
                i.setAction(DownloadService.ACTION_START);
                i.putExtra("app_id", appId);
                i.putExtra("app_name", txtName == null ? "" : String.valueOf(txtName.getText()));
                i.putExtra("app_package", app == null ? "" : app.packageId);
                i.putExtra("icon", currentIconFile == null ? "" : currentIconFile);
                i.putExtra("url", finalUrl);
                i.putExtra("file_name", "konbini_" + appId +
                        (version.versionName.length() > 0 ? ("_" + selectedVersion) : "") + ".apk");
                startService(i);
                showDownloadUi(0, 0);
            });

        } catch (Exception e) {
            Log.e("AppDetailActivity", "Error fetching download URL", e);
        }
        }).start();
    }

    public void refreshInstalledButtons(App app) {
        String pkgName = (app == null) ? "" : app.packageId;
        boolean installed = pkgName != null && pkgName.length() > 0 && isInstalled(pkgName);
        if (app == null || !app.isSupported()) {
            btnInstall.setVisibility(View.VISIBLE);
            btnOpen.setVisibility(GONE);
            btnUninstall.setVisibility(GONE);
            btnInstall.setEnabled(false);
            btnInstall.setText(getString(R.string.not_compatible));
            return;
        }

        btnInstall.setVisibility(View.VISIBLE);
        btnOpen.setText(getString(R.string.open));
        btnOpen.setVisibility(installed ? View.VISIBLE : GONE);
        btnUninstall.setText(getString(R.string.uninstall));
        btnUninstall.setVisibility(installed ? View.VISIBLE : GONE);
        btnInstall.setText(getString(R.string.download_action));
        btnInstall.setEnabled(true);
    }

    private boolean isInstalled(String packageName) {
        if (packageName == null || packageName.length() == 0) return false;
        try {
            getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void openApp() {
        if (pkgName == null || pkgName.length() == 0) return;
        PackageManager pm = getPackageManager();
        Intent launch = pm.getLaunchIntentForPackage(pkgName);
        if (launch != null) startActivity(launch);
    }

    private void uninstallApp() {
        if (pkgName == null || pkgName.length() == 0) return;
        Intent intent = new Intent(Intent.ACTION_DELETE);
        intent.setData(Uri.parse("package:" + pkgName));
        startActivity(intent);
    }

    private void openInstaller(String path) {
        try {
            File f = new File(path);
            DownloadService.installApk(this, f);
        } catch (Exception e) {
            Log.e("openInstaller@AppDetail", "Failed to open the installer: ", e);
        }
    }

    public void msg(String s) {
        try {
            new AlertDialog.Builder(this).setMessage(s).setPositiveButton("OK", null).show();
        } catch (Exception e) {
            Log.e("msg@AppDetail", "Failed to show message", e);
        }
    }

    public void showLoading(boolean show, String text) {
        if (txtLoading != null && text != null) txtLoading.setText(text);
        if (loadingOverlay != null) loadingOverlay.setVisibility(show ? View.VISIBLE : GONE);
    }

    public boolean isRu() {
        try {
            String lang = Locale.getDefault().getLanguage();
            return lang.toLowerCase().startsWith("ru");
        } catch (Exception e) {
            return false;
        }
    }

    private void showReviewActionsDialog(final ReviewItem r) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_review_actions, null);

        Button btnProfile = dialogView.findViewById(R.id.btnReviewProfile);
        final Button btnLike = dialogView.findViewById(R.id.btnReviewLike);
        final Button btnDislike = dialogView.findViewById(R.id.btnReviewDislike);
        Button btnComments = dialogView.findViewById(R.id.btnReviewComments);
        Button btnReport = dialogView.findViewById(R.id.btnReviewReport);

        btnProfile.setText(getString(R.string.profile_btn));
        btnReport.setText(getString(R.string.report));

        boolean logged = Prefs.isLoggedIn(this);
        if (!logged) {
            btnLike.setEnabled(false);
            btnDislike.setEnabled(false);
            btnReport.setEnabled(false);
        }

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(r.username)
                .setView(dialogView)
                .setNegativeButton(isRu() ? "Закрыть" : "Close", null)
                .create();

        btnProfile.setOnClickListener(v -> {
            dialog.dismiss();
        });
        btnLike.setOnClickListener(v -> {
            dialog.dismiss();
        });
        btnDislike.setOnClickListener(v -> {
            dialog.dismiss();
        });
        btnComments.setOnClickListener(v -> {
            dialog.dismiss();
        });
        btnReport.setOnClickListener(v -> {
            dialog.dismiss();
            // TODO
//            reportReview(r.id);
        });

        dialog.show();
    }

    public static class ReviewItem {
        public String userId = "";
        String username = "User";
        String avatar = "default_avatar.png";
        int rating = 0;
        String text = "";
        int createdAt;
    }

    public class ReviewAdapter extends BaseAdapter {
        @Override
        public int getCount() { return reviews.size(); }
        @Override
        public Object getItem(int position) { return reviews.get(position); }
        @Override
        public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            convertView = convertView == null ?
                    LayoutInflater.from(AppDetailActivity.this)
                            .inflate(R.layout.list_item_review, parent,
                                    false) : convertView;

            ReviewItem r = reviews.get(position);
            ImageView imgUser = convertView.findViewById(R.id.imgUser);
            TextView txtUser = convertView.findViewById(R.id.txtUser);
            TextView txtDate = convertView.findViewById(R.id.txtDate);
            TextView txtMetaLocal = convertView.findViewById(R.id.txtMeta);
            TextView txtText = convertView.findViewById(R.id.txtText);
            RatingBar rb = convertView.findViewById(R.id.ratingBarReview);

            txtUser.setText(r.username);
            txtDate.setText(String.valueOf(r.createdAt));
            txtMetaLocal.setText("");
            txtText.setText(r.text);
            rb.setRating(r.rating);

            //TODO
            ImageLoader.load(AppDetailActivity.this, r.avatar, imgUser, R.drawable.icon_placeholder);
            return convertView;
        }
    }
}
