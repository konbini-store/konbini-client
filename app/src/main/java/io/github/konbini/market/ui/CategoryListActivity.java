package io.github.konbini.market.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONObject;

import io.github.konbini.market.R;
import io.github.konbini.market.api.AppShort;
import io.github.konbini.market.model.AppItem;
import io.github.konbini.market.net.Api;
import io.github.konbini.market.ui.tasks.LoadDataAsyncTask;
import io.github.konbini.market.util.ImageLoader;
import io.github.konbini.market.util.LocaleHelper;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class CategoryListActivity extends Activity {

    public static class CategoryItem {
        public final String code;
        public final String label;
        public String preview = "";
        public CategoryItem(String code, String label) { this.code = code; this.label = label; }
        public String toString() { return label; }
    }

    private ListView list;
    private TextView titleView;
    private View loadingOverlay;
    public final ArrayList<CategoryItem> items = new ArrayList<CategoryItem>();
    public final ArrayList<AppItem> allApps = new ArrayList<AppItem>();
    public ArrayAdapter<CategoryItem> adapter;
    public boolean isGame;
    private View promoHeader;
    private View promoRoot;
    private ImageView promoIcon;
    private TextView promoText;
    private AppItem promoApp;
    private TextView promoAppName;
    private AsyncTask<Void, Void, Boolean> loadTask;
    private static final Random RAND = new Random();

    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LocaleHelper.applySavedLocale(this);
        setContentView(R.layout.activity_category_list);

        isGame = getIntent().getBooleanExtra("is_game", false);

        titleView = (TextView) findViewById(R.id.txtTitle);
        list = (ListView) findViewById(R.id.list);
        loadingOverlay = findViewById(R.id.loadingOverlay);

        ImageButton btnHome = (ImageButton) findViewById(R.id.btnHome);
        ImageButton btnSearch = (ImageButton) findViewById(R.id.btnSearch);

        if (btnHome != null) {
                        btnHome.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Intent i = new Intent(CategoryListActivity.this, MainActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(i);
                    finish();
                }
            });
        }
        if (btnSearch != null) {
            btnSearch.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { startActivity(new Intent(CategoryListActivity.this, SearchActivity.class)); }
            });
        }

        if (titleView != null) {
            titleView.setText(getString(isGame ? R.string.games1 : R.string.apps1));
            try {
                Typeface tf = Typeface.createFromAsset(getAssets(), "fonts/storopia.ttf");
                titleView.setTypeface(tf);
            } catch (Exception e) { }
        }

        LayoutInflater inf = LayoutInflater.from(this);
        promoHeader = inf.inflate(R.layout.view_promotion_app, list, false);
        promoRoot = promoHeader.findViewById(R.id.promoRoot);
        promoIcon = (ImageView) promoHeader.findViewById(R.id.promoIcon);
        promoText = (TextView) promoHeader.findViewById(R.id.promoText);
        promoAppName = (TextView) promoHeader.findViewById(R.id.appName);
        if (promoRoot != null) {
            promoRoot.setVisibility(View.GONE);
            promoRoot.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (promoApp == null) return;
                    Intent i = new Intent(CategoryListActivity.this, AppDetailActivity.class);
                    i.putExtra("app_id", promoApp.id);
                    startActivity(i);
                }
            });
        }
        if (list != null) list.addHeaderView(promoHeader, null, false);

        adapter = new ArrayAdapter<CategoryItem>(this, R.layout.list_item_category, R.id.text1, items) {
            final class ViewHolder {
                TextView label;
                TextView preview;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = convertView;
                ViewHolder holder;

                if (v == null) {
                    LayoutInflater inflater = LayoutInflater.from(CategoryListActivity.this);
                    v = inflater.inflate(R.layout.list_item_category, parent, false);
                    holder = new ViewHolder();
                    holder.label = (TextView) v.findViewById(R.id.text1);
                    holder.preview = (TextView) v.findViewById(R.id.text2);
                    v.setTag(holder);
                } else {
                    holder = (ViewHolder) v.getTag();
                }

                CategoryItem item = getItem(position);
                if (holder.label != null) {
                    holder.label.setText(item == null ? "" : item.label);
                    holder.label.setTypeface(Typeface.DEFAULT_BOLD);
                }
                if (holder.preview != null) {
                    String preview = item == null || item.preview == null ? "" : item.preview;
                    holder.preview.setText(preview);
                    holder.preview.setVisibility(preview.length() > 0 ? View.VISIBLE : View.GONE);
                }
                return v;
            }
        };
        if (list != null) {
            list.setAdapter(adapter);
            list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    int idx = position - list.getHeaderViewsCount();
                    if (idx < 0 || idx >= items.size()) return;
                    CategoryItem item = items.get(idx);
                    ArrayList<Integer> appIds = new ArrayList<>();
                    for (AppItem app : allApps) {
                        if (item.code.length() == 0 || item.code.equals(app.categoryCode)) {
                            appIds.add(app.id);
                        }
                    }
                    Intent i = new Intent(CategoryListActivity.this, CategoryAppsActivity.class);
                    i.putExtra("is_game", isGame);
                    i.putExtra("type", "category");
                    i.putExtra("query", item.code);
                    i.putExtra("title", item.label);
                    i.putIntegerArrayListExtra("app_ids", appIds);
                    startActivity(i);
                }
            });
        }
        loadData();
    }

    private void loadData() {
        showLoading(true);
        loadTask = new LoadDataAsyncTask(this);
        loadTask.execute();
    }

    @Override
    protected void onDestroy() {
        if (loadTask != null) {
            loadTask.cancel(true);
            loadTask = null;
        }
        super.onDestroy();
    }

    public void bindPromotion() {
        if (promoRoot == null || promoIcon == null || promoText == null) return;
        if (allApps.isEmpty()) {
            promoRoot.setVisibility(View.GONE);
            return;
        }
        promoRoot.setVisibility(View.VISIBLE);
        promoApp = allApps.get(RAND.nextInt(allApps.size()));
        ImageLoader.load(this, Api.iconUrl(this, promoApp.icon), promoIcon, R.drawable.icon_placeholder);
        String text = promoApp.description == null ? promoApp.name : promoApp.description;
        if (text.length() == 0) text = promoApp.name;
        if (text.length() > 90) text = text.substring(0, 90) + "...";
        promoText.setText(text);
        promoAppName.setText(promoApp.name);
    }

    public void showLoading(boolean show) {
        if (loadingOverlay != null) loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
