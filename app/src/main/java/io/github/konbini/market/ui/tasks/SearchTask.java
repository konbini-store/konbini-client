package io.github.konbini.market.ui.tasks;

import android.os.AsyncTask;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import io.github.konbini.market.R;
import io.github.konbini.market.api.Api;
import io.github.konbini.market.api.AppShort;
import io.github.konbini.market.ui.SearchActivity;

public class SearchTask extends AsyncTask<Void, Void, Object> {
    private final String q;
    private String url;
    private final WeakReference<SearchActivity> ref;

    public SearchTask(SearchActivity act, String q) {
        this.ref = new WeakReference<>(act);
        this.q = q;
    }

    protected void onPreExecute() {
        SearchActivity context = this.ref.get();
        context.showLoading(true);
    }

    protected Object doInBackground(Void... v) {
        SearchActivity context = this.ref.get();
        try {
            ArrayList<AppShort> out = Api.getInstance(context)
                    .searchApps(context, q);
            return out == null ? "Unable to load app catalog" : out;
        } catch (Exception e) {
            return "URL=" + url + "\n" + e;
        }
    }

    @SuppressWarnings("unchecked")
    protected void onPostExecute(Object out) {
        SearchActivity context = this.ref.get();
        context.showLoading(false);
        if (out instanceof String) {
            Toast.makeText(context, "Search error: " + out, Toast.LENGTH_LONG).show();
            return;
        }
        List<AppShort> listOut = (List<AppShort>) out;
        context.data.clear();
        context.data.addAll(listOut);
        context.adapter.refreshInstalledPackages();
        context.adapter.notifyDataSetChanged();
        if (listOut.size() == 0)
            Toast.makeText(context, R.string.nothing_found, Toast.LENGTH_SHORT).show();
    }

    protected void onCancelled() {
        SearchActivity context = this.ref.get();
        context.showLoading(false);
    }
}