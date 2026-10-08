package io.github.konbini.market.ui;


import io.github.konbini.market.R;
import io.github.konbini.market.util.LocaleHelper;
import io.github.konbini.market.util.Prefs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class LoginActivity extends Activity {

    private EditText edtUser, edtPass;
    private Button btnLogout;
    private TextView txtStatus;

    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LocaleHelper.applySavedLocale(this);
        setContentView(R.layout.activity_login);

//        Toast.makeText(this, R.string.social_features_not_implemented_yet, Toast.LENGTH_LONG).show();
//        this.finish();

        edtUser = findViewById(R.id.edtUser);
        edtPass = findViewById(R.id.edtPass);
        Button btnLogin = findViewById(R.id.btnLogin);
        btnLogout = findViewById(R.id.btnLogout);
        txtStatus = findViewById(R.id.txtStatus);

        refreshUi();

        btnLogin.setOnClickListener(v -> doLogin());

        btnLogout.setOnClickListener(v -> {
            Prefs.logout(LoginActivity.this);
            refreshUi();
            msg("OK");
        });
    }

    void refreshUi() {
        boolean logged = Prefs.isLoggedIn(this);

        btnLogout.setEnabled(logged);

        if (logged) {
            String u = Prefs.getUsername(this);
            txtStatus.setText(String.format(getString(R.string.logged_in_as), (u.length() > 0 ? u : ("ID " + Prefs.getUserId(this)))));
            edtUser.setText(u);
            edtPass.setText("");
        } else {
        	txtStatus.setText(getString(R.string.not_logged_in));
        }
    }

    @SuppressWarnings("deprecation")
    private void doLogin() {
        final String u = edtUser.getText().toString().trim();
        final String p = edtPass.getText().toString();

        if (u.length() == 0 || p.length() == 0) {
        	msg("Enter username and password");
            return;
        }

        final ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage(getString(R.string.loading));
        pd.setCancelable(false);
        pd.show();

        new LoginAsyncTask(this, pd, u, p).execute();
    }

    void msg(String s) {
        try {
            new AlertDialog.Builder(this)
                    .setMessage(s)
                    .setPositiveButton("OK", null)
                    .show();
        } catch (Exception e) {
            Log.e("msg@LoginActivity", "Failed to make a new AlertDialog: ", e);
        }
    }
}
