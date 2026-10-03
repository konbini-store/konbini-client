package io.github.konbini.market.ui.tasks;

import static android.view.View.GONE;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import io.github.konbini.market.R;
import io.github.konbini.market.ui.AppDetailActivity;
import io.github.konbini.market.ui.ScreenshotActivity;
import io.github.konbini.market.util.ImageLoader;

@SuppressWarnings("deprecation")
public class LoadScreenshotsAsyncTask extends AsyncTask<Void, Void, ArrayList<String>> {
    private final WeakReference<AppDetailActivity> ref;

    public LoadScreenshotsAsyncTask(AppDetailActivity ref) {
        this.ref = new WeakReference<>(ref);
    }

    @Override
    public ArrayList<String> doInBackground(Void... v) {
        AppDetailActivity activity = ref.get();
        if (activity.isFinishing()) {
            Log.e("AppDetailActivity", "activity is finishing!!");
            return null;
        }
        if (activity.app == null) {
            Log.e("AppDetailActivity", "app is null!!");
            return null;
        }
        if (activity.app.screenshots == null) {
            Log.e("AppDetailActivity", "app.screenshots is null!!");
            return null;
        }
        return activity.app.screenshots;
    }

    @Override
    public void onPostExecute(ArrayList<String> arr) {
        AppDetailActivity activity = ref.get();
        if (arr == null || arr.size() == 0) {
            activity.txtScreensTitle.setVisibility(GONE);
            activity.screensScroll.setVisibility(GONE);
            return;
        }
        activity.txtScreensTitle.setVisibility(View.VISIBLE);
        activity.screensScroll.setVisibility(View.VISIBLE);
        activity.screensContainer.removeAllViews();
        for (int i = 0; i < arr.size(); i++) {
            final String file = arr.get(i);
            if (file == null || file.length() == 0) continue;
            ImageView iv = new ImageView(activity);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(240, 400);
            lp.rightMargin = (int) (10 * activity.getResources().getDisplayMetrics().density);
            iv.setLayoutParams(lp);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            activity.screensContainer.addView(iv);
            ImageLoader.loadScreenshot(activity, file, iv, R.drawable.banner_placeholder);

            iv.setOnClickListener(v -> {
                if (Build.VERSION.SDK_INT < 8) {
                    new AlertDialog.Builder(activity)
                            .setTitle(R.string.screenshots)
                            .setMessage("Open screenshot in browser?")
                            .setPositiveButton("Yes", (dialog, which) -> {
                                try {
                                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(file));
                                    activity.startActivity(intent);
                                } catch (Exception e) {
                                    Toast.makeText(activity, "Unable to open browser", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                } else {
                    Intent intent = new Intent(activity, ScreenshotActivity.class);
                    intent.putExtra("screenshot_url", file);
                    activity.startActivity(intent);
                }
            });
        }
    }
}