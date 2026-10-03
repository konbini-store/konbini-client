package io.github.konbini.market.util.tasks;

import static io.github.konbini.market.util.ImageLoader.calcInSampleSize;
import static io.github.konbini.market.util.ImageLoader.memPut;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.widget.ImageView;

import java.io.File;
import java.lang.ref.WeakReference;

import io.github.konbini.market.net.Http;

@SuppressWarnings("deprecation")
public class LoadBannerAsyncTask extends AsyncTask<Void, Void, Bitmap> {
    private final String url;
    private final int reqW;
    private final int reqH;

    private final WeakReference<ImageView> iv;
    private final int placeholderRes;

    public LoadBannerAsyncTask(String url, int reqW, int reqH, final ImageView iv, final int placeholderRes) {
        this.url = url;
        this.reqH = reqH;
        this.reqW = reqW;
        this.iv = new WeakReference<>(iv);
        this.placeholderRes = placeholderRes;
    }

    protected Bitmap doInBackground(Void... v) {
        try {
            byte[] data = Http.getBytes(url);
            if (data == null) return null;

            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, 0, data.length, bounds);

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = calcInSampleSize(bounds, reqW, reqH);
            opts.inPreferredConfig = Bitmap.Config.RGB_565;
            opts.inDither = true;

            return BitmapFactory.decodeByteArray(data, 0, data.length, opts);
        } catch (Throwable e) {
            return null;
        }
    }

    protected void onPostExecute(Bitmap bmp) {
        ImageView imageView = iv.get();
        if (bmp != null) {
            memPut("banner:" + url, bmp);
            Object tag = imageView.getTag();
            if (tag != null && url.equals(tag.toString())) {
                imageView.setImageBitmap(bmp);
            }
        } else {
            imageView.setImageResource(placeholderRes);
        }
    }
}
