package io.github.konbini.market.ui.tasks;

import android.content.Intent;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.view.View;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
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

@SuppressWarnings("deprecation")
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
        ArrayList<String> compatibilityArray = new ArrayList<>();
        if (firstVersion != null) {
            String range = firstVersion.versionName;
            if (lastVersion != null && lastVersion.versionName != null &&
                    !lastVersion.versionName.equals(firstVersion.versionName)) {
                range = firstVersion.versionName + " – " + lastVersion.versionName;
            }
            compatibilityArray.add(activity.getString(R.string.version) + " " + range);
        }
        if (firstVersion != null) {
            compatibilityArray.add("Android " + AndroidVersions.apiToAndroid(firstVersion.minSdk) +
                    " (API " + firstVersion.minSdk + ")");
        }
        compatibilityArray.add(activity.getString(activity.app.isSupported() ?
                R.string.app_compatible : R.string.app_not_compatible));

        activity.txtMeta.setText(TextUtils.join(" • ", compatibilityArray));
        activity.txtMeta.setVisibility(View.VISIBLE);

        activity.txtAuthor.setOnClickListener(v -> {
            Intent intent = new Intent(activity, CategoryAppsActivity.class);
            intent.putExtra("type", "author");
            intent.putExtra("query", dev);
            intent.putExtra("title", String.format(activity.getString(R.string.apps_made_by),
                    o.author));
            activity.startActivity(intent);
        });
        if (desc.length() > 100) {
            activity.txtDesc.setText(shortDesc);
            activity.txtToggle.setVisibility(View.VISIBLE);
            activity.txtToggle.setOnClickListener(v -> {
                activity.descCollapsed = !activity.descCollapsed;
                activity.txtToggle.setText(activity.descCollapsed ? activity.getString(R.string.expand_desc) : activity.getString(R.string.collapse_desc));
                activity.txtDesc.setText(activity.descCollapsed ? shortDesc : desc);
            });
        } else {
            activity.txtDesc.setText(desc);
        }

        activity.txtHeaderRating.setText(String.format(Locale.US, "%.1f ★", avgRating));
        activity.txtHeaderDownloads.setText(String.format(Locale.US, activity.getString(R.string.downloads_count),
                downloads));

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
