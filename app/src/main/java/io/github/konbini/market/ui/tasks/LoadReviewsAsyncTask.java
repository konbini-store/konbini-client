package io.github.konbini.market.ui.tasks;

import static android.view.View.GONE;

import android.os.AsyncTask;
import android.view.View;


import java.lang.ref.WeakReference;
import java.util.ArrayList;

import io.github.konbini.market.R;
import io.github.konbini.market.ui.AppDetailActivity;
import io.github.konbini.market.util.Prefs;

@SuppressWarnings("deprecation")
public class LoadReviewsAsyncTask extends AsyncTask<Void, Void, Object> {
    final WeakReference<AppDetailActivity> ref;
    public LoadReviewsAsyncTask(AppDetailActivity act) {
        ref=new WeakReference<>(act);
    }
    @Override
    protected Object doInBackground(Void... v) {
        AppDetailActivity activity = ref.get();
        try {
            int viewerId = Prefs.getUserId(activity);
            // TODO
//            String s = null;
//            if (s == null)
            return "null response";
//            JSONArray arr = new JSONArray(s);
//            ArrayList<AppDetailActivity.ReviewItem> out = new ArrayList<>();
//            for (int i = 0; i < arr.length(); i++) {
//                out.add(activity.parseReview(arr.getJSONObject(i)));
//            }
//            return out;
        } catch (Exception e) {
            return e.toString();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onPostExecute(Object out) {
        AppDetailActivity activity = ref.get();
        if (out instanceof String) {
            activity.txtReviewsTitle.setText(String.format(activity.getString(R.string.reviews_count2), 0));
            activity.txtReviewsInfo.setText(String.format(activity.getString(R.string.reviews_count), 0));
            activity.hasOwnReview = false;
            activity.ratingAddReview.setVisibility(View.VISIBLE);
            activity.txtreviewinfo.setVisibility(View.VISIBLE);
            return;
        }

        ArrayList<AppDetailActivity.ReviewItem> listOut = (ArrayList<AppDetailActivity.ReviewItem>) out;
        activity.reviews.clear();
        activity.reviews.addAll(listOut);
        activity.hasOwnReview = false;
        int myId = Prefs.getUserId(activity);
        for (int i = 0; i < activity.reviews.size(); i++) {
            if (activity.reviews.get(i).userId == myId && myId > 0) {
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