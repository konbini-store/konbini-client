package io.github.konbini.market.ui.tasks;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.ref.WeakReference;

import io.github.konbini.market.ui.AppDetailActivity;
import io.github.konbini.market.ui.LoginActivity;
import io.github.konbini.market.util.Prefs;

@SuppressWarnings("deprecation")
public class ShowCommentsAsyncTask extends AsyncTask<Void, Void, Object> {
    private final WeakReference<AppDetailActivity> context;
    private final int reviewId;

    public ShowCommentsAsyncTask(AppDetailActivity context, int reviewId) {
        this.context = new WeakReference<>(context);
        this.reviewId = reviewId;
    }

    @Override
    protected Object doInBackground(Void... v) {
        try {
            // TODO
//            String s = null; //Http.getString(Api.reviewCommentsUrl(AppDetailActivity.this, reviewId));
//            if (s == null)
                return "null response";
//            return new JSONArray(s);
        } catch (Exception e) {
            return e.toString();
        }
    }

    @Override
    protected void onPostExecute(Object out) {
        AppDetailActivity context = this.context.get();
        if (out instanceof String) {
            context.msg("Comments error: " + out);
            return;
        }
        JSONArray arr = (JSONArray) out;
        final String[] items = new String[arr.length()];
        for (int i = 0; i < arr.length(); i++) {
            JSONObject c = arr.optJSONObject(i);
            if (c == null) {
                items[i] = String.valueOf(arr.opt(i));
            } else {
                String u = c.optString("username", "User");
                String t = c.optString("text", "");
                String d = c.optString("created_at", "");
                items[i] = u + ": " + t + (d.length() > 0 ? ("  (" + d + ")") : "");
            }
        }
        new AlertDialog.Builder(context)
                .setTitle(context.isRu() ? "Комментарии" : "Comments")
                .setItems(items, null)
                .setPositiveButton(context.isRu() ? "Добавить" : "Add", (dialog, which) -> {
                    if (!Prefs.isLoggedIn(context)) {
                        context.startActivity(new Intent(context, LoginActivity.class));
                        return;
                    }
                    context.showAddCommentDialog(reviewId);
                })
                .setNegativeButton(context.isRu() ? "Закрыть" : "Close", null)
                .show();
    }
}