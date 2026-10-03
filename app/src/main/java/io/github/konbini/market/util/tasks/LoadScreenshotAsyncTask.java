package io.github.konbini.market.util.tasks;

import static io.github.konbini.market.util.ImageLoader.applyScreenshotBitmap;
import static io.github.konbini.market.util.ImageLoader.decodeSampled;
import static io.github.konbini.market.util.ImageLoader.memPut;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;

import io.github.konbini.market.net.Http;

@SuppressWarnings("deprecation")
public class LoadScreenshotAsyncTask extends AsyncTask<Void, Void, Bitmap> {
    private final String url;
    private final int reqW;
    private final int reqH;

    private final WeakReference<ImageView> iv;
    private final WeakReference<Context> context;
    private final int placeholderRes;
    private final File f;

    public LoadScreenshotAsyncTask(Context context, String url, int reqW, int reqH, final File f,
                                   final ImageView iv, final int placeholderRes) {
        this.url = url;
        this.reqH = reqH;
        this.reqW = reqW;
        this.f = f;
        this.context = new WeakReference<>(context);
        this.iv = new WeakReference<>(iv);
        this.placeholderRes = placeholderRes;
    }

    protected Bitmap doInBackground(Void... v) {
        try {
            byte[] data = Http.getBytes(url);
            if (data == null) return null;

            Bitmap bmp = decodeSampled(data, reqW, reqH);
            if (bmp == null) return null;

            try {
                FileOutputStream fos = new FileOutputStream(f);
                fos.write(data);
                fos.close();
            } catch (Throwable e) {
                Log.e("LoadScreenshotAsyncTask", "Failed to write data to FileOutputStream: ", e);
            }

            return bmp;
        } catch (OutOfMemoryError e) {
            return null;
        } catch (Throwable e) {
            return null;
        }
    }

    protected void onPostExecute(Bitmap bmp) {
        Context c = context.get();
        ImageView imageView = iv.get();
        if (bmp != null) {
            memPut(url, bmp);
            Object tag = imageView.getTag();
            if (tag != null && url.equals(tag.toString())) {
                try {
                    applyScreenshotBitmap(c, imageView, bmp);
                } catch (Throwable e) {
                    imageView.setImageResource(placeholderRes);
                }
            }
        } else {
            Object tag = imageView.getTag();
            if (tag != null && url.equals(tag.toString())) {
                imageView.setImageResource(placeholderRes);
            }
        }
    }
}