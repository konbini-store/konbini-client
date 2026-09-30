package io.github.konbini.market.ui.tasks;

import android.os.AsyncTask;

import org.json.JSONObject;

import java.lang.ref.WeakReference;

import io.github.konbini.market.ui.AppDetailActivity;

public class ReportReviewAsyncTask extends AsyncTask<Void, Void, String> {
    private final WeakReference<AppDetailActivity> context;
    private final int reviewId;
    private final int uid;

    public ReportReviewAsyncTask(AppDetailActivity context, int reviewId, int uid) {
        this.context = new WeakReference<>(context);
        this.reviewId = reviewId;
        this.uid = uid;
    }

    @Override
    protected String doInBackground(Void... v) {
        try {
            JSONObject o = new JSONObject();
            o.put("user_id", uid);
            // TODO
            return null;
//                    return Http.postJson(AppDetailActivity.this, Api.reviewReportUrl(AppDetailActivity.this, reviewId), o.toString());
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
        context.msg(context.isRu() ? "Отправлено" : "Reported");
    }
}
