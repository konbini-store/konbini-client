package io.github.konbini.market.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Random;

import io.github.konbini.market.R;
import io.github.konbini.market.api.AppShort;
//import io.github.konbini.market.net.Api;
import io.github.konbini.market.api.Api;
import io.github.konbini.market.util.ImageLoader;
import io.github.konbini.market.util.LocaleHelper;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class CategoryAppsActivity extends Activity {
    private ListView list;
    private View loadingOverlay;
    private AppListAdapter adapter;
    private final ArrayList<AppShort> items = new ArrayList<>();
    ArrayList<AppShort> originalItems = new ArrayList<>();
    private View promoRoot;
    private ImageView promoIcon;
    private TextView promoText;
    private TextView appName;
    private Button btnTopFree, btnTopDownloads;
    private AppShort promoApp;
    private boolean sortDownloads = false;

    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LocaleHelper.applySavedLocale(this);
        setContentView(R.layout.activity_category_apps);

        final String title = getIntent().getStringExtra("title");
        final String query = getIntent().getStringExtra("query");
        final String type = getIntent().getStringExtra("type");
        final boolean isGame = getIntent().getBooleanExtra("is_game", false);
        final ArrayList<Integer> appIds = getIntent().getIntegerArrayListExtra("app_ids");

        TextView titleView = findViewById(R.id.txtTitle);
        TextView subtitleView = findViewById(R.id.txtSubtitle);
        list = findViewById(R.id.list);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        btnTopFree = findViewById(R.id.btnTopFree);
        btnTopDownloads = findViewById(R.id.btnTopDownloads);

        if (list == null) {
            Toast.makeText(this, "list not found", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        View promoHeader = LayoutInflater.from(this).inflate(R.layout.view_promotion_app, list, false);
        promoRoot = promoHeader.findViewById(R.id.promoRoot);
        promoIcon = promoHeader.findViewById(R.id.promoIcon);
        promoText = promoHeader.findViewById(R.id.promoText);
        appName = promoHeader.findViewById(R.id.appName);
        list.addHeaderView(promoHeader, null, false);

        try {
            ImageButton btnHome = findViewById(R.id.btnHome);
            if (btnHome != null) {
                btnHome.setOnClickListener(v -> {
                    Intent i = new Intent(CategoryAppsActivity.this, MainActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(i);
                    finish();
                });
            }
            findViewById(R.id.btnSearch).setOnClickListener(v -> startActivity(new Intent(CategoryAppsActivity.this, SearchActivity.class)));
            Typeface tf = Typeface.createFromAsset(getAssets(), "fonts/storopia.ttf");
            if (titleView != null) titleView.setTypeface(tf);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (titleView != null) titleView.setText(getString(isGame ? R.string.games1 : R.string.apps1));
        if (subtitleView != null) subtitleView.setText(title == null || title.length() == 0 ? getString(isGame ? R.string.all_games : R.string.all_apps) : title);

        adapter = new AppListAdapter(this, items);
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, position, id) -> {
            int idx = position - list.getHeaderViewsCount();
            if (idx < 0 || idx >= items.size()) return;
            AppShort it = items.get(idx);
            Intent i = new Intent(CategoryAppsActivity.this, AppDetailActivity.class);
            i.putExtra("app_id", it.id);
            startActivity(i);
        });

        if (btnTopFree != null)
            btnTopFree.setOnClickListener(v -> {
                sortDownloads = false;
                applySort();
                updateTabButtons();
            });

        if (btnTopDownloads != null)
            btnTopDownloads.setOnClickListener(v -> {
                sortDownloads = true;
                applySort();
                updateTabButtons();
            });

        if (promoRoot != null)
            promoRoot.setOnClickListener(v -> {
                if (promoApp == null) return;
                Intent i = new Intent(CategoryAppsActivity.this, AppDetailActivity.class);
                i.putExtra("app_id", promoApp.id);
                startActivity(i);
            });

        updateTabButtons();
        loadApps(type, query, isGame, appIds);
    }

    protected void onResume() {
        super.onResume();
        if (adapter != null) {
            adapter.refreshInstalledPackages();
            adapter.notifyDataSetChanged();
        }
    }

    private void loadApps(final String type, final String query, final boolean isGame, final ArrayList<Integer> appIds) {
        showLoading(true);
        new LoadAppsAsyncTask(this, type, query,
                isGame, appIds, Api.getInstance(CategoryAppsActivity.this)).execute();
    }

    void applySort() {
        items.clear();
        items.addAll(originalItems);
        if (sortDownloads) {
            Collections.sort(items, (a, b) -> b.downloads - a.downloads);
        } else {
            Collections.sort(items, (a, b) -> {
                int r = Float.compare((float)b.rating, (float)a.rating);
                if (r != 0) return r;
                return a.name.compareToIgnoreCase(b.name);
            });
        }
        adapter.refreshInstalledPackages();
        adapter.notifyDataSetChanged();
    }

    void bindPromotion() {
        if (promoRoot == null || promoIcon == null || promoText == null) return;
        if (originalItems.isEmpty()) {
            promoRoot.setVisibility(View.GONE);
            return;
        }
        promoRoot.setVisibility(View.VISIBLE);
        promoApp = originalItems.get(new Random().nextInt(originalItems.size()));
        ImageLoader.load(this, promoApp.icon, promoIcon, R.drawable.icon_placeholder);
        String text = promoApp.description;
        if (text.length() == 0) text = promoApp.name;
        if (text.length() > 90) text = text.substring(0, 90) + "...";
        promoText.setText(text);
        appName.setText(promoApp.name);
    }

    void updateTabButtons() {
        if (btnTopFree != null) {
            btnTopFree.setCompoundDrawablePadding(6);
            btnTopFree.setCompoundDrawablesWithIntrinsicBounds(sortDownloads ? R.drawable.btn_strip_mark_off : R.drawable.btn_strip_mark_on, 0, 0, 0);
        }
        if (btnTopDownloads != null) {
            btnTopDownloads.setCompoundDrawablePadding(6);
            btnTopDownloads.setCompoundDrawablesWithIntrinsicBounds(sortDownloads ? R.drawable.btn_strip_mark_on : R.drawable.btn_strip_mark_off, 0, 0, 0);
        }
    }

    void showLoading(boolean show) {
        if (loadingOverlay != null) loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
