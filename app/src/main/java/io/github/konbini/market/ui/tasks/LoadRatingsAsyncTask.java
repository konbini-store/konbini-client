package io.github.konbini.market.ui.tasks;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import io.github.konbini.market.api.Api;
import io.github.konbini.market.db.Database;
import io.github.konbini.market.net.Http;

@SuppressWarnings("deprecation")
public class LoadRatingsAsyncTask extends AsyncTask<Void, Void, Map<String, Double>> {

    public interface OnRatingsLoadedListener {
        void onRatingsLoaded(Map<String, Double> ratings);
    }

    private final WeakReference<Context> contextRef;
    private final OnRatingsLoadedListener listener;

    public LoadRatingsAsyncTask(Context context, OnRatingsLoadedListener listener) {
        this.contextRef = new WeakReference<>(context);
        this.listener = listener;
    }

    @Override
    protected Map<String, Double> doInBackground(Void... params) {
        Context context = contextRef.get();
        if (context == null) return null;
        try {
            String url = Api.getInstance(context).getSocialRatingsUrl(context);
            Log.d("LoadRatingsAsyncTask", "Fetching ratings from: " + url);
            String jsonStr = Http.getString(url);
            if (jsonStr == null) return null;

            JSONObject obj = new JSONObject(jsonStr);
            JSONObject ratingsObj = obj.optJSONObject("ratings");
            if (ratingsObj == null) return null;

            Map<String, Double> map = new HashMap<>();
            Iterator<String> keys = ratingsObj.keys();
            while (keys.hasNext()) {
                String pkg = keys.next();
                JSONObject item = ratingsObj.optJSONObject(pkg);
                if (item != null) {
                    double average = item.optDouble("average", 0.0);
                    map.put(pkg, average);
                }
            }

            Database.updateRatings(context, map);
            Api.getInstance(context).updateMemoryRatings(map);

            return map;
        } catch (Exception e) {
            Log.e("LoadRatingsAsyncTask", "Error fetching ratings: ", e);
            return null;
        }
    }

    @Override
    protected void onPostExecute(Map<String, Double> ratings) {
        if (ratings != null && listener != null) {
            listener.onRatingsLoaded(ratings);
        }
    }
}
