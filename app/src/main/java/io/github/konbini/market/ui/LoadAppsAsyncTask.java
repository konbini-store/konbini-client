package io.github.konbini.market.ui;

import android.os.AsyncTask;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;

import io.github.konbini.market.R;
import io.github.konbini.market.api.Api;
import io.github.konbini.market.api.AppShort;

public class LoadAppsAsyncTask extends AsyncTask<Void, Void, ArrayList<AppShort>> {
    WeakReference<CategoryAppsActivity> ref;

    String type;
    String query;
    boolean isGame;
    ArrayList<Integer> appIds;
    Api api;

    public LoadAppsAsyncTask(CategoryAppsActivity act, String type, String query, boolean isGame,
                             ArrayList<Integer> appIds, Api api) {
        ref=new WeakReference<>(act);
        this.type = type;
        this.query = query;
        this.isGame = isGame;
        this.appIds = appIds;
        this.api = api;
    }
    protected ArrayList<AppShort> doInBackground(Void... v) {
        CategoryAppsActivity act = ref.get();
        try {
            ArrayList<AppShort> apps;
            switch (type) {
                case "author":
                    apps = api.getAuthorApps(act, query);
                    break;
                case "category":
                    if (appIds == null) {
                        apps = api.getCategoryApps(act, query);
                    } else {
                        ArrayList<AppShort> source = api.getAllApps(act);
                        if (source == null) return null;
                        HashSet<Integer> selectedIds = new HashSet<>(appIds);
                        apps = new ArrayList<>();
                        for (AppShort app : source) {
                            if (selectedIds.contains(app.id)) apps.add(app);
                        }
                    }
                    break;
                default:
                    return null;
            }
            return apps;
        } catch (Exception e) { return null; }
    }
    protected void onPostExecute(ArrayList<AppShort> out) {
        CategoryAppsActivity act = ref.get();
        act.showLoading(false);
        if (out == null) {
            Toast.makeText(act, R.string.error_network, Toast.LENGTH_SHORT).show();
            return;
        }
        act.originalItems.clear();
        act.originalItems.addAll(out);
        act.bindPromotion();
        act.applySort();
        act.updateTabButtons();
    }
}
