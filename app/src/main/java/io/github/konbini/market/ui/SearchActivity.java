package io.github.konbini.market.ui;

import java.util.ArrayList;
import java.util.List;

import io.github.konbini.market.R;
import io.github.konbini.market.api.AppShort;
import io.github.konbini.market.api.Api;
import io.github.konbini.market.ui.tasks.SearchTask;
import io.github.konbini.market.util.LocaleHelper;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class SearchActivity extends Activity {
    private EditText edt;
    private ImageButton btn;
    private ListView list;
    private View loadingOverlay;
    public ArrayList<AppShort> data = new ArrayList<>();
    public AppListAdapter adapter;
    private SearchTask searchTask;

    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LocaleHelper.applySavedLocale(this);
        setContentView(R.layout.activity_search);

        edt = findViewById(R.id.edtQuery);
        btn = findViewById(R.id.btnDoSearch);
        list = findViewById(R.id.list);
        loadingOverlay = findViewById(R.id.loadingOverlay);

        adapter = new AppListAdapter(this, data);
        list.setAdapter(adapter);

        list.setOnItemClickListener((parent, view, position, id) -> {
            AppShort it = data.get(position);
            Intent i = new Intent(SearchActivity.this, AppDetailActivity.class);
            i.putExtra("app_id", it.id);
            startActivity(i);
        });

        btn.setOnClickListener(v -> doSearch());

        edt.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                doSearch();
                return true;
            }
            return false;
        });
    }

    private void doSearch() {
        final String q = edt.getText().toString().trim();
        if (q.length() == 0) return;
        if (searchTask != null) searchTask.cancel(true);
        searchTask = new SearchTask(this, q);
        searchTask.execute();
    }

    public void showLoading(boolean show) {
        if (loadingOverlay != null) loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
