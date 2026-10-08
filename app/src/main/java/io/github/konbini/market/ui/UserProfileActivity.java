package io.github.konbini.market.ui;

import org.json.JSONObject;

import io.github.konbini.market.R;
import io.github.konbini.market.net.Api;
import io.github.konbini.market.net.Http;
import io.github.konbini.market.ui.tasks.LoadProfileAsyncTask;
import io.github.konbini.market.util.ImageLoader;
import io.github.konbini.market.util.LocaleHelper;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

public class UserProfileActivity extends Activity {

    private int userId;

    public ImageView imgAvatar;
    public TextView txtUser;
    public TextView txtCreated;
    public TextView txtDesc;

    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LocaleHelper.applySavedLocale(this);
        setContentView(R.layout.activity_user_profile);

        userId = getIntent().getIntExtra("user_id", 0);

        imgAvatar = findViewById(R.id.imgAvatar);
        txtUser = findViewById(R.id.txtUser);
        txtCreated = findViewById(R.id.txtCreated);
        txtDesc = findViewById(R.id.txtDesc);

        loadProfile();
    }

    @SuppressWarnings("deprecation")
    private void loadProfile() {
        final ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage(getString(R.string.loading));
        pd.setCancelable(false);
        pd.show();

        new LoadProfileAsyncTask(this, userId, pd).execute();
    }
}