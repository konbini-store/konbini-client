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

import org.w3c.dom.Text;

import java.util.Locale;

/**
 * Created by paul on 18/07/26.
 */

public class AppVersionAdapter extends BaseAdapter {

    private final Context context;
    private final SparseArray<AppVersion> versions;
    private final LayoutInflater inflater;

    public AppVersionAdapter(Context context, SparseArray<AppVersion> versions) {
        this.context = context;
        this.versions = versions;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return versions != null ? versions.size() : 0;
    }

    @Override
    public AppVersion getItem(int position) {
        return versions.valueAt(position);
    }

    @Override
    public long getItemId(int position) {
        return versions.keyAt(position);
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
            if (version.abis != null && !version.abis.isEmpty()) {
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
