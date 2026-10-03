package io.github.konbini.market.ui.tasks;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import java.io.InputStream;
import java.lang.ref.WeakReference;

// Source - https://stackoverflow.com/a/10868126
// Posted by Kyle Clegg, modified by community. See post 'Timeline' for change history
// Retrieved 2026-07-08, License - CC BY-SA 4.0

@SuppressWarnings("deprecation")
public class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
    private final WeakReference<ImageView> bmImage;

    public DownloadImageTask(ImageView bmImage) {
        this.bmImage = new WeakReference<>(bmImage);
    }

    protected Bitmap doInBackground(String... urls) {
        String urldisplay = urls[0];
        Bitmap mIcon11 = null;
        try {
            InputStream in = new java.net.URL(urldisplay).openStream();
            mIcon11 = BitmapFactory.decodeStream(in);
        } catch (Exception e) {
            Log.e("DownloadImageTask", "Something went wrong: ", e);
        }
        return mIcon11;
    }

    protected void onPostExecute(Bitmap result) {
        bmImage.get().setImageBitmap(result);
    }
}
