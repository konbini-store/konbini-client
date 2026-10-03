package io.github.konbini.market.util;

import java.io.File;
import java.util.LinkedHashMap;

import io.github.konbini.market.util.tasks.LoadImageAsyncTask;
import io.github.konbini.market.util.tasks.LoadBannerAsyncTask;
import io.github.konbini.market.util.tasks.LoadScreenshotAsyncTask;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;

// safer loader for old Android / low RAM devices
public class ImageLoader {

    private static final int MAX_MEM_ITEMS = 40;

    private static final LinkedHashMap<String, Bitmap> mem =
            new LinkedHashMap<>(MAX_MEM_ITEMS, 0.75f, true) {
                protected boolean removeEldestEntry(Entry<String, Bitmap> eldest) {
                    return size() > MAX_MEM_ITEMS;
                }
            };

    private static File iconCacheDir(Context c) {
        File d = new File(c.getCacheDir(), "icons");
        boolean dir_created = d.exists() || d.mkdirs();
        if (!dir_created)
            Log.w("iconCacheDir@IL", "Failed to create directory!");
        return d;
    }

    private static Bitmap memGet(String k) {
        synchronized (mem) { return mem.get(k); }
    }

    public static void memPut(String k, Bitmap b) {
        synchronized (mem) { mem.put(k, b); }
    }

    public static int calcInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        if (reqWidth <= 0) reqWidth = 64;
        if (reqHeight <= 0) reqHeight = 64;

        while ((height / inSampleSize) > reqHeight * 2 || (width / inSampleSize) > reqWidth * 2) {
            inSampleSize *= 2;
        }

        if (inSampleSize < 1) inSampleSize = 1;
        return inSampleSize;
    }

    @SuppressWarnings("deprecation")
    public static Bitmap decodeSampled(byte[] data, int reqWidth, int reqHeight) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, 0, data.length, bounds);

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = calcInSampleSize(bounds, reqWidth, reqHeight);
            opts.inPreferredConfig = Bitmap.Config.RGB_565; // 2 bytes per pixel, less RAM
            opts.inDither = true;

            return BitmapFactory.decodeByteArray(data, 0, data.length, opts);
        } catch (OutOfMemoryError e) {
            try {
                BitmapFactory.Options opts2 = new BitmapFactory.Options();
                opts2.inSampleSize = 8;
                opts2.inPreferredConfig = Bitmap.Config.RGB_565;
                return BitmapFactory.decodeByteArray(data, 0, data.length, opts2);
            } catch (Throwable t) {
                return null;
            }
        } catch (Throwable t) {
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    private static Bitmap decodeSampledFile(String path, int reqWidth, int reqHeight) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, bounds);

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = calcInSampleSize(bounds, reqWidth, reqHeight);
            opts.inPreferredConfig = Bitmap.Config.RGB_565;
            opts.inDither = true;

            return BitmapFactory.decodeFile(path, opts);
        } catch (OutOfMemoryError e) {
            try {
                BitmapFactory.Options opts2 = new BitmapFactory.Options();
                opts2.inSampleSize = 8;
                opts2.inPreferredConfig = Bitmap.Config.RGB_565;
                return BitmapFactory.decodeFile(path, opts2);
            } catch (Throwable t) {
                return null;
            }
        } catch (Throwable t) {
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    public static void load(final Context c, final String url, final ImageView iv, final int placeholderRes) {
        iv.setImageResource(placeholderRes);
        iv.setTag(url);

        if (url == null || url.length() == 0) return;

        Bitmap cached = memGet(url);
        if (cached != null) {
            Object tag = iv.getTag();
            if (tag != null && url.equals(tag.toString())) {
                iv.setImageBitmap(cached);
            }
            return;
        }

        final int reqW = (iv.getLayoutParams() != null && iv.getLayoutParams().width > 0)
                ? iv.getLayoutParams().width : 96;
        final int reqH = (iv.getLayoutParams() != null && iv.getLayoutParams().height > 0)
                ? iv.getLayoutParams().height : 96;

        final String key = Hash.md5(url);
        final File f = new File(iconCacheDir(c), key + ".img");

        if (f.exists()) {
            Bitmap fb = decodeSampledFile(f.getAbsolutePath(), reqW, reqH);
            if (fb != null) {
                memPut(url, fb);
                Object tag = iv.getTag();
                if (tag != null && url.equals(tag.toString())) {
                    iv.setImageBitmap(fb);
                }
                return;
            }
        }

        new LoadImageAsyncTask(url, reqW, reqW, f, iv, placeholderRes).execute();
    }

    @SuppressWarnings("deprecation")
    public static void loadBanner(final Context c, final String url, final ImageView iv, final int placeholderRes) {
        iv.setImageResource(placeholderRes);
        iv.setTag(url);

        if (url == null || url.length() == 0) return;

        Bitmap cached = memGet("banner:" + url);
        if (cached != null) {
            Object tag = iv.getTag();
            if (tag != null && url.equals(tag.toString())) {
                iv.setImageBitmap(cached);
            }
            return;
        }

        final int reqW = 1024;
        final int reqH = 400;

        new LoadBannerAsyncTask(url, reqW, reqH, iv, placeholderRes).execute();
    }

    @SuppressWarnings("deprecation")
    public static void loadScreenshot(final Context c, final String url, final ImageView iv, final int placeholderRes) {
        iv.setImageResource(placeholderRes);
        iv.setTag(url);

        if (url == null || url.length() == 0) return;

        Bitmap cached = memGet(url);
        if (cached != null) {
            Object tag = iv.getTag();
            if (tag != null && url.equals(tag.toString())) {
                applyScreenshotBitmap(c, iv, cached);
            }
            return;
        }

        final int reqW = 1200;
        final int reqH = 1200;

        final String key = Hash.md5(url);
        final File f = new File(iconCacheDir(c), key + ".img");

        if (f.exists()) {
            Bitmap fb = decodeSampledFile(f.getAbsolutePath(), reqW, reqH);
            if (fb != null) {
                memPut(url, fb);
                Object tag = iv.getTag();
                if (tag != null && url.equals(tag.toString())) {
                    applyScreenshotBitmap(c, iv, fb);
                }
                return;
            }
        }

        new LoadScreenshotAsyncTask(c, url, reqW, reqH, f, iv, placeholderRes).execute();
    }

    public static void applyScreenshotBitmap(Context c, ImageView iv, Bitmap bmp) {
        if (bmp == null) return;
        int bmpW = bmp.getWidth();
        int bmpH = bmp.getHeight();

        int targetHeight = 400;
        int targetWidth = 240;
        if (bmpH > 0) {
            targetWidth = (int) (targetHeight * ((float) bmpW / bmpH));
        }

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(targetWidth, targetHeight);
        lp.rightMargin = (int) (10 * c.getResources().getDisplayMetrics().density);
        iv.setLayoutParams(lp);
        iv.setImageBitmap(bmp);
    }
}