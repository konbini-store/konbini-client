package io.github.konbini.market.ui.tasks;

import static io.github.konbini.market.util.ImageLoader.decodeSampled;
import static io.github.konbini.market.util.ImageLoader.memPut;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.media.Image;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;

import io.github.konbini.market.net.Http;

@SuppressLint("StaticFieldLeak")
@SuppressWarnings("deprecation")
public class LoadImageAsyncTask extends AsyncTask<Void, Void, Bitmap> {
    private final String url;
    private final int reqW;
    private final int reqH;
    private final File f;

    private final WeakReference<ImageView> iv;
    private final int placeholderRes;

    public LoadImageAsyncTask(String url, int reqW, int reqH, File f, final ImageView iv, final int placeholderRes) {
        this.url = url;
        this.reqH = reqH;
        this.reqW = reqW;
        this.f = f;
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
                Log.e("LoadImageAsyncTask", "Failed to write data to FileOutputStream: ", e);
            }

            return bmp;
        } catch (OutOfMemoryError e) {
            return null;
        } catch (Throwable e) {
            return null;
        }
    }

    protected void onPostExecute(Bitmap bmp) {
        ImageView imageView = iv.get();
        if (bmp != null) {
            memPut(url, bmp);
            Object tag = imageView.getTag();
            if (tag != null && url.equals(tag.toString())) {
                try {
                    imageView.setImageBitmap(bmp);
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
