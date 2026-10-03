package io.github.konbini.market.ui.tasks;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.lang.ref.WeakReference;

import io.github.konbini.market.net.Http;
import io.github.konbini.market.ui.ScreenshotActivity;

@SuppressWarnings("deprecation")
public class LoadScreenshotFullscreenAsyncTask extends AsyncTask<Void, Void, Bitmap> {
    private final String url;
    private final WeakReference<ScreenshotActivity> reference;
    private final int reqH;
    private final int reqW;

    public LoadScreenshotFullscreenAsyncTask(ScreenshotActivity context, String url, int reqH, int reqW) {
        this.reference = new WeakReference<>(context);
        this.url = url;
        this.reqH = reqH;
        this.reqW = reqW;
    }

    protected Bitmap doInBackground(Void... v) {
        try {
            byte[] data = Http.getBytes(url);
            if (data == null) {
                Log.e("loadScrenshotFullscreen", "Failed to get bytes from URL: " + url);
                return null;
            }

            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, 0, data.length, bounds);

            BitmapFactory.Options opts = new BitmapFactory.Options();
            int height = bounds.outHeight;
            int width = bounds.outWidth;
            int inSampleSize = 1;
            while ((height / inSampleSize) > reqH * 2 || (width / inSampleSize) > reqW * 2) {
                inSampleSize *= 2;
            }
            if (inSampleSize < 1) inSampleSize = 1;
            opts.inSampleSize = inSampleSize;
            opts.inPreferredConfig = Bitmap.Config.RGB_565;
            opts.inDither = true;

            Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length, opts);
            if (bmp == null) {
                Log.e("loadScrenshotFullscreen", "Failed to decode bitmap from byte array for URL: " + url);
            }
            return bmp;
        } catch (Throwable e) {
            Log.e("loadScrenshotFullscreen", "Failed to load screenshot: ", e);
            return null;
        }
    }

    protected void onPostExecute(Bitmap bmp) {
        ScreenshotActivity context = this.reference.get();
        if (context == null || context.isFinishing()) return;
        context.progressBar.setVisibility(View.GONE);
        if (bmp != null) {
            context.gestureImageView.setImageBitmap(bmp);
        } else {
            Toast.makeText(context, "Failed to load screenshot", Toast.LENGTH_SHORT).show();
        }
    }
}
