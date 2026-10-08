package io.github.konbini.market.ui;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;

import io.github.konbini.market.R;
import io.github.konbini.market.net.Api;
import io.github.konbini.market.net.Http;
import io.github.konbini.market.ui.tasks.LoadAvatarsThenProfileAsyncTask;
import io.github.konbini.market.util.ImageLoader;
import io.github.konbini.market.util.LocaleHelper;
import io.github.konbini.market.util.Prefs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

public class ProfileActivity extends Activity {

    public ImageView imgAvatar;
    public EditText edtDesc;
    public TextView txtUser;
    public TextView txtCreated;
    public HashMap<String, String> avatars;
    public String selectedAvatarName;
    private Button btnSave, btnLogout;

    public int userId;

    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LocaleHelper.applySavedLocale(this);
        setContentView(R.layout.activity_profile);

        userId = Prefs.getUserId(this);
        if (userId <= 0) {
            msg("Login required");
            finish();
            return;
        }

        imgAvatar = (ImageView) findViewById(R.id.imgAvatar);
        txtUser = (TextView) findViewById(R.id.txtUser);
        txtCreated = (TextView) findViewById(R.id.txtCreated);
        btnSave = (Button) findViewById(R.id.btnSave);

        txtUser.setText(Prefs.getUsername(this));

        imgAvatar.setOnClickListener((View v) -> {
            if (avatars == null || avatars.isEmpty()) {
                return;
            }
            final String[] avatarNames = avatars.keySet().toArray(new String[0]);
            new AlertDialog.Builder(ProfileActivity.this)
                    .setTitle("Select avatar")
                    .setItems(avatarNames, (dialog, which) -> {
                        selectedAvatarName = avatarNames[which];
                        String url = avatars.get(selectedAvatarName);
                        ImageLoader.load(ProfileActivity.this, Api.avatarUrl(ProfileActivity.this, url),
                                imgAvatar, R.drawable.icon_placeholder);
                    })
                    .show();
        });

        txtUser.setOnClickListener((View v) -> {
            final EditText input = new EditText(ProfileActivity.this);
            input.setText(txtUser.getText().toString());
            input.setSelection(input.getText().length());

            new AlertDialog.Builder(ProfileActivity.this)
                    .setTitle("Edit username")
                    .setView(input)
                    .setPositiveButton("OK", (dialog, which) -> {
                        String newName = input.getText().toString().trim();
                        if (newName.length() > 0) {
                            txtUser.setText(newName);
                            Prefs.setUsername(ProfileActivity.this, newName);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        btnSave.setOnClickListener(v -> saveProfile());

        loadAvatarsThenProfile();
    }

    private void loadAvatarsThenProfile() {
        final ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage(getString(R.string.loading));
        pd.setCancelable(false);
        pd.show();

        new LoadAvatarsThenProfileAsyncTask(this, pd).execute();
    }

    private void saveProfile() {
        final String avatar = (avatars != null && avatars.containsKey(selectedAvatarName)) ? avatars.get(selectedAvatarName) : selectedAvatarName;
        final String username = txtUser.getText().toString();

        final ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Saving...");
        pd.setCancelable(false);
        pd.show();

        Prefs.setAvatar(this, avatar);
        Prefs.setUsername(this, username);

        try { pd.dismiss(); } catch (Exception e) {}
        msg("Saved");
    }

    public void msg(String s) {
        try {
            new AlertDialog.Builder(this)
                    .setMessage(s)
                    .setPositiveButton("OK", null)
                    .show();
        } catch (Exception e) {}
    }
}
