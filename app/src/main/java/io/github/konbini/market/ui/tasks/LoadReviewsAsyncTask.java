package io.github.konbini.market.ui.tasks;

import static android.view.View.GONE;

import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;


import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.SyncHttpClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;
import io.github.konbini.market.R;
import io.github.konbini.market.ui.AppDetailActivity;
import io.github.konbini.market.util.Prefs;

@SuppressWarnings("deprecation")
public class LoadReviewsAsyncTask extends AsyncTask<Void, Void, LoadReviewsAsyncTask.ReviewsResult> {
    final WeakReference<AppDetailActivity> ref;
    public LoadReviewsAsyncTask(AppDetailActivity act) {
        ref=new WeakReference<>(act);
    }

    public static class ReviewsResult {
        public ArrayList<io.github.konbini.market.ui.AppDetailActivity.ReviewItem> result;
        public Integer statusCode;
    }

    @Override
    protected ReviewsResult doInBackground(Void... v) {
        AppDetailActivity activity = ref.get();
        try {
//            int viewerId = Prefs.getUserId(activity);
            // TODO

            final String[] body = new String[] { null };
            final int[] code = new int[] { 0 };

            SyncHttpClient client = new SyncHttpClient();
            client.get(
                    Prefs.getSocialServer(activity) + "/api/reviews" +
                        String.format("?package=%s&instance=%s&offset=0",
                                activity.pkgName, activity.api.getBaseUrl()),
                    new AsyncHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                            Log.d("LoadReviewsAsyncTask", "Got reviews!");
                            try {
                                body[0] = new String(responseBody, "UTF-8");
                            } catch (Exception e) {
                                Log.e("LoadReviewsAsyncTask", "Something went wrong when" +
                                        " decoding server's response: ", e);
                                return;
                            }
                            code[0] = statusCode;
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                            try {
                                body[0] = new String(responseBody, "UTF-8");
                            } catch (Exception e) {
                                Log.e("LoadReviewsAsyncTask", "Something went wrong when" +
                                        " decoding server's response: ", e);
                                return;
                            }
                            code[0] = statusCode;

                            Log.e("LoadReviewsAsyncTask", String.format("Server sent HTTP %d :( body: %s",statusCode,body[0]));
                        }
                    }
            );


            ReviewsResult result = new ReviewsResult();
            result.statusCode = code[0];
            if (code[0] != 200) return result;
            JSONObject obj = new JSONObject(body[0]);
            JSONArray arr = obj.optJSONArray("reviews");
            if (arr == null) arr = new JSONArray();
            ArrayList<AppDetailActivity.ReviewItem> out = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                out.add(activity.parseReview(arr.getJSONObject(i)));
            }
            result.result = out;
            return result;
        } catch (Exception e) {
            Log.e("LoadReviewsAsyncTask", "Something went wrong when" +
                    " getting server's response: ", e);

            ReviewsResult result = new ReviewsResult();
            result.statusCode = 0;
            return result;
        }
    }

    private String getSHA256(String string) {
        MessageDigest digest = null;
        try { digest = MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException e) {
            Log.e("SendReviewAsyncTask", "Apparently SHA-256 doesn't exist...");
            return null;
        }
        byte[] hash;
        try { hash = digest.digest(string.getBytes("UTF-8")); }
        catch (UnsupportedEncodingException e) {
            Log.e("SendReviewAsyncTask", "We don't support UTF-8 somehow, nice.");
            return null;
        }
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    @Override
    protected void onPostExecute(ReviewsResult out) {
        AppDetailActivity activity = ref.get();
        if (out.statusCode != 200) {
            activity.msg("Network error");
            activity.txtReviewsTitle.setText(String.format(activity.getString(R.string.reviews_count2), 0));
            activity.txtReviewsInfo.setText(String.format(activity.getString(R.string.reviews_count), 0));
            activity.hasOwnReview = false;
            activity.ratingAddReview.setVisibility(View.VISIBLE);
            activity.txtreviewinfo.setVisibility(View.VISIBLE);
            return;
        }

        ArrayList<AppDetailActivity.ReviewItem> listOut = out.result;
        activity.reviews.clear();
        activity.reviews.addAll(listOut);
        activity.hasOwnReview = false;
        String myUserId = getSHA256(Prefs.getUserKey(activity));
//        int myId = Prefs.getUserId(activity);
        for (int i = 0; i < activity.reviews.size(); i++) {
            if (activity.reviews.get(i).userId.equals(myUserId) && !TextUtils.isEmpty(myUserId)) {
                activity.hasOwnReview = true;
                break;
            }
        }

        activity.txtReviewsTitle.setText(String.format(activity.getString(R.string.reviews_count2),
                activity.reviews.size()));
        activity.txtReviewsInfo.setText(String.format(activity.getString(R.string.reviews_count),
                activity.reviews.size()));
        activity.ratingAddReview.setVisibility(activity.hasOwnReview ? GONE : View.VISIBLE);
        activity.txtreviewinfo.setVisibility(activity.hasOwnReview ? GONE : View.VISIBLE);
        activity.adapter.notifyDataSetChanged();
    }
}