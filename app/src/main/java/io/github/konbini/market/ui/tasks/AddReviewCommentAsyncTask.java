package io.github.konbini.market.ui.tasks;

import android.os.AsyncTask;
import android.os.storage.operations.sources.AppDataFileSource;

import org.json.JSONObject;

import java.lang.ref.WeakReference;

import io.github.konbini.market.ui.AppDetailActivity;

public class AddReviewCommentAsyncTask extends AsyncTask<Void, Void, String> {
    private final WeakReference<AppDetailActivity> context;
    private final int reviewId;
    private final String text;
    private final int uid;

    public AddReviewCommentAsyncTask(AppDetailActivity context, int reviewId, String text, int uid) {
        this.context = new WeakReference<>(context);
        this.reviewId = reviewId;
        this.text = text;
        this.uid = uid;
    }

    @Override
    protected String doInBackground(Void... v) {
        try {
            JSONObject o = new JSONObject();
            o.put("user_id", uid);
            o.put("text", text);
            // TODO
            return null; //Http.postJson(AppDetailActivity.this, Api.reviewAddCommentUrl(AppDetailActivity.this, reviewId), o.toString());
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
    }
}