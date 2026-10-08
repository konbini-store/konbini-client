package io.github.konbini.market.ui.tasks;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.ResponseHandlerInterface;
import com.loopj.android.http.SyncHttpClient;

import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.entity.StringEntity;
import io.github.konbini.market.net.Http;
import io.github.konbini.market.ui.AppDetailActivity;
import io.github.konbini.market.util.Prefs;
import io.github.konbini.market.util.ReviewErrorCodes;

@SuppressWarnings("deprecation")
public class SendReviewAsyncTask extends AsyncTask<Void, Void, SendReviewAsyncTask.ReviewResult> {
    private final WeakReference<AppDetailActivity> context;
    private final String token;
    private final int uid;
    private final float safeRating;
    private final String text;

    public static class ReviewResult {
        public String result;
        public Integer statusCode;
    }

    public SendReviewAsyncTask(AppDetailActivity context, String token, int uid, float safeRating,
                               String text) {
        this.context = new WeakReference<>(context);

        this.token = token;
        this.uid = uid;
        this.safeRating = safeRating;
        this.text = text;
    }

    @Override
    protected ReviewResult doInBackground(Void... v) {
        try {
            // TODO
            AppDetailActivity context = this.context.get();
            JSONObject o = new JSONObject();
            o.put("user", Prefs.getUserKey(context));
            o.put("username", Prefs.getUsername(context));
            o.put("avatar", Prefs.getAvatar(context));
            o.put("rating", safeRating);
            o.put("instance", context.api.getBaseUrl());
            o.put("package_name", context.app.packageId);
            o.put("comment", text);

            final String[] result = new String[] { null };
            final Integer[] _statusCode = new Integer[] { null };

            final SyncHttpClient client = new SyncHttpClient();
            client.post(
                    context,
                    Prefs.getSocialServer(context) + "/api/review",
                    new StringEntity(o.toString(), "UTF-8"),
                    "application/json",
                    new AsyncHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                            try { result[0] = new String(responseBody, "UTF-8"); }
                            catch(UnsupportedEncodingException e) {
                                Log.e("SendReviewAsyncTask", "We don't support UTF-8: ", e);
                                _statusCode[0] = statusCode;
                                result[0] = "Failed to decode response.";
                            }
                            _statusCode[0] = statusCode;
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                            try { result[0] = new String(responseBody, "UTF-8"); }
                            catch(UnsupportedEncodingException e) {
                                Log.e("SendReviewAsyncTask", "We don't support UTF-8: ", e);
                            }
                            _statusCode[0] = statusCode;
                        }
                    }
            );

            ReviewResult rr = new ReviewResult();
            rr.result = result[0];
            rr.statusCode = _statusCode[0];

            Log.i("SendReviewAsyncTask", String.format("Status code: %d, result: %s",
                    rr.statusCode, rr.result));

            return rr;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected void onPostExecute(ReviewResult s) {
        AppDetailActivity context = this.context.get();
        if (s.statusCode == 422) {
            context.msg(s.result);
            return;
        }
        if (s.statusCode != 200) {
            context.msg(ReviewErrorCodes.getErrorDescription(s.statusCode));
            return;
        }
        context.loadReviews();
        Toast.makeText(context, context.isRu() ? "Отправлено" : "Sent", Toast.LENGTH_SHORT).show();
    }
}