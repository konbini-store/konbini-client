package io.github.konbini.market.ui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.konbini.market.R;
import io.github.konbini.market.api.AppShort;
import io.github.konbini.market.model.AppItem;

import io.github.konbini.market.util.ImageLoader;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

public class AppListAdapter extends BaseAdapter {
    private final Context c;
    private final List<AppShort> items;
    private final LayoutInflater inf;
    private final Map<String, Integer> installedPackages = new HashMap<>();

    public AppListAdapter(Context c, List<AppShort> items) {
        this.c = c;
        this.items = items;
        this.inf = LayoutInflater.from(c);
        refreshInstalledPackages();
    }

    @SuppressWarnings("deprecation")
    public void refreshInstalledPackages() {
        installedPackages.clear();
        try {
            PackageManager pm = c.getPackageManager();
            List<PackageInfo> list = pm.getInstalledPackages(0);
            for (int i = 0; i < list.size(); i++) {
                PackageInfo pi = list.get(i);
                if (pi != null) {
                    installedPackages.put(pi.packageName, pi.versionCode);
                }
            }
        } catch (Throwable e) {
            Log.e("AppListAdapter", "Failed to refresh installed packages: ", e);
        }
    }

    public int getInstalledVersionCode(String packageName) {
        Integer v = installedPackages.get(packageName);
        return v == null ? 0 : v;
    }

    public int getCount() { return items.size(); }
    public Object getItem(int position) {
        return items.get(position);
    }
    public long getItemId(int position) { return items.get(position).id; }

    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) v = inf.inflate(R.layout.list_item_app, parent, false);

        ImageView img = v.findViewById(R.id.img);
        TextView title = v.findViewById(R.id.title);
        TextView developer = v.findViewById(R.id.subtitle);
        TextView status = v.findViewById(R.id.txtStatus);
        RatingBar ratingBar = v.findViewById(R.id.ratingBar);

        AppShort a = items.get(position);
        title.setText(AppItem.safe(a.name));
        developer.setText(a.author);
        ratingBar.setRating((float)a.rating);

        // TODO
//        String packageName = AppItem.safe(a.packageName);
//        boolean installed = packageName.length() > 0 && installedPackages.containsKey(packageName);
//        int installedVersionCode = getInstalledVersionCode(packageName);
//        a.installedVersionCode = installedVersionCode;

//        if(true) {
//        if (installed && a. > 0 && installedVersionCode > 0 && a.versionCode > installedVersionCode) {
//            status.setText(c.getString(R.string.updates_available));
//            status.setTextColor(0xfff28c18);
//        } else if (installed) {
//            status.setText(c.getString(R.string.installed));
//            status.setTextColor(0xff303030);
//        } else {
            status.setText(c.getString(R.string.free));
            status.setTextColor(0xff303030);
//        }

        String iconUrl = a.icon == null ? "" : a.icon;
        ImageLoader.load(c, iconUrl, img, R.drawable.icon_placeholder);

        return v;
    }
}
