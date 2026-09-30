package io.github.konbini.market.ui.tasks;

import android.content.Intent;
import android.os.AsyncTask;
import android.view.View;

import java.lang.ref.WeakReference;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.github.konbini.market.R;
import io.github.konbini.market.api.Api;
import io.github.konbini.market.api.App;
import io.github.konbini.market.api.AppVersion;
import io.github.konbini.market.ui.AppDetailActivity;
import io.github.konbini.market.ui.CategoryAppsActivity;
import io.github.konbini.market.util.AndroidVersions;
import io.github.konbini.market.util.ImageLoader;

public class LoadDetailsAsyncTask extends AsyncTask<Void, Void, App> {
    private final WeakReference<AppDetailActivity> context;

    public LoadDetailsAsyncTask(AppDetailActivity context) {
        this.context = new WeakReference<>(context);
    }

    @Override
    protected App doInBackground(Void... v) {
        AppDetailActivity activity = context.get();
        Api api = activity.api;
        Logger logger = Logger.getLogger(activity.getPackageName());
        try {
            return api.getApp(activity, activity.appId);
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage());
            activity.appInitialized = true;
            return null;
        }
    }

    @Override
    protected void onPostExecute(App o) {
        AppDetailActivity activity = context.get();
        activity.showLoading(false, null);
        if (o == null) {
            activity.msg(activity.getString(R.string.error_network));
            return;
        }

        activity.app = o;
        String name = o.name;
        final String dev = o.author;
        final String desc = o.description == null ? "" : o.description;
        final String shortDesc = desc.length() > 100 ? desc.substring(0, 100) + "..." : desc;
        String icon = o.icon;
        activity.currentIconFile = icon;

        int downloads = 0;
        int reviewCount = 0;
        float avgRating = 0;
        activity.pkgName = o.packageId;

        activity.txtName.setText(name);
        activity.txtAuthor.setText(dev);
        AppVersion firstVersion = activity.app.getFirstVersion();
        AppVersion lastVersion = activity.app.getLastVersion();
        String compatibility = "";
        if (firstVersion != null) {
            String range = firstVersion.versionName;
            if (lastVersion != null && lastVersion.versionName != null && !lastVersion.versionName.equals(firstVersion.versionName)) {
                range = firstVersion.versionName + " – " + lastVersion.versionName;
            }
            compatibility += activity.getString(R.string.version) + " " + range;
        }
        if (firstVersion != null) {
            compatibility += " • Android " + AndroidVersions.apiToAndroid(firstVersion.minSdk) + " (API " + firstVersion.minSdk + ")";
        }
        compatibility += activity.app.isSupported() ? " • Compatible" : " • Not compatible";
        activity.txtMeta.setText(compatibility);
        activity.txtMeta.setVisibility(View.VISIBLE);

        activity.txtAuthor.setOnClickListener(v -> {
            Intent intent = new Intent(activity, CategoryAppsActivity.class);
            intent.putExtra("type", "author");
            intent.putExtra("query", dev);
            intent.putExtra("title", "by "+dev);
            activity.startActivity(intent);
        });
        if (desc.length() > 100) {
            activity.txtDesc.setText(shortDesc);
            activity.txtToggle.setVisibility(View.VISIBLE);
            activity.txtToggle.setOnClickListener(v -> {
                activity.descCollapsed = !activity.descCollapsed;
                activity.txtToggle.setText(activity.descCollapsed ? R.string.expand_desc : R.string.collapse_desc);
                activity.txtDesc.setText(activity.descCollapsed ? shortDesc : desc);
            });
        } else {
            activity.txtDesc.setText(desc);
        }
        activity.txtDownloadsInfo.setText(downloads + " " + activity.getString(R.string.downloads_count));
        activity.txtReviewsInfo.setText(reviewCount + " " + activity.getString(R.string.reviews_count));
        activity.txtHeaderRating.setText(String.format(Locale.US, "%.1f", avgRating));
        activity.ratingHeader.setRating(avgRating);
        activity.txtReviewsTitle.setText(activity.getString(R.string.reviews) + " (" + reviewCount + ")");

        if (icon != null && icon.length() > 0) {
            ImageLoader.load(activity, icon, activity.imgIcon, R.drawable.icon_placeholder);
        } else {
            activity.imgIcon.setImageResource(R.drawable.icon_placeholder);
        }

        activity.refreshInstalledButtons(activity.app);
        activity.restoreDownloadState();
        activity.bindVersionsTab();

        activity.loadScreenshots();
        activity.loadReviews();
    }
}
