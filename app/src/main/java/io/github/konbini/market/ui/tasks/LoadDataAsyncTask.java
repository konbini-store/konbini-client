package io.github.konbini.market.ui.tasks;

import android.os.AsyncTask;
import android.os.Build;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import io.github.konbini.market.R;
import io.github.konbini.market.api.AppShort;
import io.github.konbini.market.model.AppItem;
import io.github.konbini.market.ui.CategoryListActivity;

public class LoadDataAsyncTask extends AsyncTask<Void, Void, Boolean> {
    final ArrayList<CategoryListActivity.CategoryItem> outCats = new ArrayList<>();
    final ArrayList<AppItem> outApps = new ArrayList<>();

    final WeakReference<CategoryListActivity> ref;

    public LoadDataAsyncTask(CategoryListActivity act) {
        this.ref=new WeakReference<>(act);
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        CategoryListActivity act = ref.get();
        try {
            if (isCancelled()) return false;
            io.github.konbini.market.api.Api api = io.github.konbini.market.api.Api.getInstance(act);
            JSONArray arr = api.getCategories(act, act.isGame);
            ArrayList<AppShort> apps = api.getAllApps(act);

            outCats.add(new CategoryListActivity.CategoryItem("", act.getString(act.isGame ? R.string.all_games : R.string.all_apps)));
            HashSet<String> categoryCodes = new HashSet<>();
            for (int i = 0; i < arr.length(); i++) {
                if (isCancelled()) return false;
                JSONObject o = arr.getJSONObject(i);
                String code = o.optString("cat_id", o.optString("id", ""));
                outCats.add(new CategoryListActivity.CategoryItem(code, o.optString("name", "")));
                if (code.length() > 0) categoryCodes.add(code);
            }

            int deviceApi = Build.VERSION.SDK_INT;
            for (int i = 0; i < apps.size(); i++) {
                if (isCancelled()) return false;
                AppShort o = apps.get(i);
                String categoryCode = o.categoryCode == null ? "other_apps" : o.categoryCode;
                if (!categoryCodes.contains(categoryCode)) continue;

                AppItem a = new AppItem();
                a.id = o.id;
                a.name = o.name;
                a.developer = o.author;
                a.icon = o.icon;
                a.api = o.api;
                a.packageName = o.packageName;
                a.isGame = act.isGame;
                a.categoryCode = categoryCode;
                a.categoryLabel = o.categoryLabel;
                a.rating = (float) o.rating;
                a.downloads = o.downloads;
                a.description = o.description;
                if (a.api <= deviceApi) outApps.add(a);
            }

            HashMap<String, ArrayList<String>> previews = new HashMap<>();
            for (int i = 0; i < outApps.size(); i++) {
                if (isCancelled()) return false;
                AppItem a = outApps.get(i);
                String key = a.categoryCode == null ? "" : a.categoryCode;
                ArrayList<String> names = previews.get(key);
                if (names == null) { names = new ArrayList<>(); previews.put(key, names); }
                if (names.size() < 3) names.add(a.name);
            }
            ArrayList<String> allNames = new ArrayList<>();
            for (int i = 0; i < outApps.size() && allNames.size() < 3; i++) allNames.add(outApps.get(i).name);

            for (int i = 0; i < outCats.size(); i++) {
                if (isCancelled()) return false;
                CategoryListActivity.CategoryItem c = outCats.get(i);
                ArrayList<String> names = c.code.length() == 0 ? allNames : previews.get(c.code);
                if (names != null && names.size() > 0) {
                    StringBuilder sb = new StringBuilder();
                    for (int j = 0; j < names.size(); j++) {
                        if (j > 0) sb.append(", ");
                        sb.append(names.get(j));
                    }
                    c.preview = sb.toString();
                } else {
                    c.preview = "";
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    protected void onPostExecute(Boolean ok) {
        CategoryListActivity act = ref.get();
        if (isCancelled()) return;
        if (act.isFinishing() ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1
                        && act.isDestroyed())) {
            return;
        }
        act.showLoading(false);
        if (!ok) {
            Toast.makeText(act, R.string.error_network, Toast.LENGTH_SHORT).show();
            return;
        }
        act.items.clear();
        act.items.addAll(outCats);
        act.allApps.clear();
        act.allApps.addAll(outApps);
        act.adapter.notifyDataSetChanged();
        act.bindPromotion();
    }
}
