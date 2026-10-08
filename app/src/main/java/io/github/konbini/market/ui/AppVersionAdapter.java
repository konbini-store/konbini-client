package io.github.konbini.market.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import io.github.konbini.market.R;
import io.github.konbini.market.api.AppVersion;
import io.github.konbini.market.util.AndroidVersions;

import android.content.Context;
import android.util.SparseArray;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Created by paul on 18/07/26.
 */

public class AppVersionAdapter extends BaseAdapter {

    private final List<AppVersion> versions = new ArrayList<>();
    private final LayoutInflater inflater;

    public AppVersionAdapter(Context context, SparseArray<AppVersion> sparseVersions) {
        this.inflater = LayoutInflater.from(context);
        if (sparseVersions != null) {
            for (int i = 0; i < sparseVersions.size(); i++) {
                AppVersion v = sparseVersions.valueAt(i);
                if (v != null) {
                    versions.add(v);
                }
            }
            Collections.sort(versions, (v1, v2) -> Integer.compare(v2.versionCode, v1.versionCode));
        }
    }

    @Override
    public int getCount() {
        return versions.size();
    }

    @Override
    public AppVersion getItem(int position) {
        return versions.get(position);
    }

    @Override
    public long getItemId(int position) {
        AppVersion v = getItem(position);
        return v != null ? v.id : position;
    }

    private static String formatBytes(long bytes) {
        if (bytes < 0) return "0 B";
        if (bytes < 1024) return bytes + " B";

        // Calculates unit magnitude using leading zeros (0 = B, 1 = KB, 2 = MB, etc.)
        int unitIndex = (63 - Long.numberOfLeadingZeros(bytes)) / 10;

        double size = (double) bytes / (1L << (unitIndex * 10));
        char unitPrefix = " KMGTPE".charAt(unitIndex);

        return String.format(Locale.ENGLISH, "%.1f %cB", size, unitPrefix);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.app_version, parent, false);
            holder = new ViewHolder();
            holder.versionText = (TextView) convertView.findViewById(R.id.version_text);
            holder.supportedAndroid = (TextView) convertView.findViewById(R.id.supported_android);
            holder.supportedAbis = (TextView) convertView.findViewById(R.id.supported_abis);
            holder.appSize = (TextView) convertView.findViewById(R.id.appSizeTextView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        AppVersion version = getItem(position);
        if (version != null) {
            // Build ABI comma-separated list string
            String abisList = "noarch";
            if (!version.abis.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (int j = 0; j < version.abis.size(); j++) {
                    sb.append(version.abis.get(j));
                    if (j < version.abis.size() - 1) {
                        sb.append(", ");
                    }
                }
                abisList = sb.toString();
            }

            // Bind values directly to the elements cached in ViewHolder
            holder.versionText.setText(String.format(Locale.ENGLISH, "%s (%d)",
                    version.versionName, version.versionCode));

            holder.supportedAndroid.setText(String.format(Locale.ENGLISH,
                    "Android %s (API %d)",
                    AndroidVersions.apiToAndroid(version.minSdk),
                    version.minSdk));

            holder.supportedAbis.setText(String.format(Locale.ENGLISH,
                    "ABIs: %s", abisList));

            holder.appSize.setText(formatBytes(version.size));
        }

        return convertView;
    }

    // Static ViewHolder class to optimize layout lookups during scroll
    private static class ViewHolder {
        TextView versionText;
        TextView supportedAndroid;
        TextView supportedAbis;
        TextView appSize;
    }
}
