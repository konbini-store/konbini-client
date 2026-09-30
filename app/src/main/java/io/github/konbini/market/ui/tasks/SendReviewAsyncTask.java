package io.github.konbini.market.ui.tasks;

import android.os.AsyncTask;
import android.widget.Toast;

import org.json.JSONObject;

import java.lang.ref.WeakReference;

import io.github.konbini.market.net.Http;
import io.github.konbini.market.ui.AppDetailActivity;

public class SendReviewAsyncTask extends AsyncTask<Void, Void, String> {
    private final WeakReference<AppDetailActivity> context;
    private final String token;
    private final int uid;
    private final float safeRating;
    private final String text;

    public SendReviewAsyncTask(AppDetailActivity context, String token, int uid, float safeRating,
                               String text) {
        this.context = new WeakReference<>(context);

        this.token = token;
        this.uid = uid;
        this.safeRating = safeRating;
        this.text = text;
    }

    @Override
    protected String doInBackground(Void... v) {
        try {
            // TODO
            if (true) return null;
            AppDetailActivity context = this.context.get();
            String url = ""; //Api.baseUrl(AppDetailActivity.this) + "/api/app/" + appId + "/review";
            JSONObject o = new JSONObject();
            o.put("token", token);
            o.put("user_id", uid);
            o.put("rating", safeRating);
            o.put("comment", text);
            return Http.postJson(context, url, o.toString());
        } catch (Exception e) {
            return null;
        }
    }
    @Override
    protected void onPostExecute(String s) {
        AppDetailActivity context = this.context.get();
        if (s == null) {
            context.msg("Network error");
            return;
        }
        context.loadReviews();
        Toast.makeText(context, context.isRu() ? "Отправлено" : "Sent", Toast.LENGTH_SHORT).show();
    }
}