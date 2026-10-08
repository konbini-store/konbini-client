package io.github.konbini.market.ui.tasks;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.Objects;

import io.github.konbini.market.R;
import io.github.konbini.market.api.Banner;
import io.github.konbini.market.net.Http;
import io.github.konbini.market.ui.MainActivity;
import io.github.konbini.market.util.ImageLoader;

@SuppressWarnings("deprecation")
public final class LoadBannersAsyncTask extends AsyncTask<Void, Void, Banner> {
    private final WeakReference<MainActivity> activityRef;

    public LoadBannersAsyncTask(MainActivity activity) {
        this.activityRef = new WeakReference<>(activity);
    }

    protected Banner doInBackground(Void... params) {
        MainActivity activity = activityRef.get();
        if (activity == null) return null;
        try {
            return activity.api.getRandomBanner(activity);
        } catch (Exception e) {
            return null;
        }
    }

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

    protected void onPostExecute(Banner banner) {
        final MainActivity activity = activityRef.get();
        if (activity == null) return;
        if (banner == null) {
            Log.e("LoadBannersAsyncTask", "Error loading banners: Null response");
            return;
        }
        try {
            String imageUrl = banner.getImageUrl();
            String targetUrl = banner.getTargetUrl();

            ImageLoader.loadBanner(activity, imageUrl, activity.bannerImage, R.drawable.icon_placeholder);

            activity.bannerImage.post(() -> {
                int width = activity.bannerImage.getWidth();
                if (width > 0) {
                    android.view.ViewGroup.LayoutParams params =
                            activity.bannerImage.getLayoutParams();
                    params.height = Math.round(width * (9.0f / 16.0f));
                    activity.bannerImage.setLayoutParams(params);
                }
            });

            activity.bannerImage.setOnClickListener(v -> {
                final Uri uri = Uri.parse(targetUrl);
                if (!Objects.equals(uri.getScheme(), "konbini")) {
                    new AlertDialog.Builder(activity)
                            .setTitle(R.string.warning)
                            .setMessage(R.string.third_party_website_warning)
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                                launchIntent(activity, uri, false);
                            })
                            .setNegativeButton(android.R.string.cancel, null)
                            .show();
                } else {
                    launchIntent(activity, uri, true);
                }
            });
        } catch (Exception e) {
            Log.e("LoadBannersAsyncTask", "Error loading banners: " + e.getMessage());
        }
    }
}