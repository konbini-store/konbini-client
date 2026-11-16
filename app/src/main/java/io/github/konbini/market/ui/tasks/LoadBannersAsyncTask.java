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

    protected void onPostExecute(Banner banner) {
        final MainActivity activity = activityRef.get();
        if (activity == null) return;
        if (banner == null) {
            Log.e("LoadBannersAsyncTask", "Error loading banners: Null response");
            return;
        }
        try {
            activity.displayBanner(banner);
        } catch (Exception e) {
            Log.e("LoadBannersAsyncTask", "Error loading banners: " + e.getMessage());
        }
    }
}