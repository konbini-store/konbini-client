package io.github.konbini.market.ui;

import io.github.konbini.market.R;
import io.github.konbini.market.db.Database;
import io.github.konbini.market.util.LocaleHelper;
import io.github.konbini.market.util.Prefs;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import java.io.DataOutputStream;
import java.util.Locale;

public class SettingsActivity extends Activity {

    private EditText edtServer;
    private Spinner spnLang;
    private EditText socialServer;
    private CheckBox enableAnalytics;
    private CheckBox chkAutoInstallRoot;
    private boolean ignoreRootToggle = false;

    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LocaleHelper.applySavedLocale(this);
        setContentView(R.layout.activity_settings);

        edtServer = findViewById(R.id.edtServer);
        spnLang = findViewById(R.id.spnLang);
        Button btnSave = findViewById(R.id.btnSave);
        socialServer = findViewById(R.id.socialServerUrl);
        enableAnalytics = findViewById(R.id.checkBox);
        chkAutoInstallRoot = findViewById(R.id.chkAutoInstallRoot);
        Button clearCacheButton = findViewById(R.id.clearCacheButton);

        ArrayAdapter<String> a = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                new String[]{getString(R.string.lang_ru), getString(R.string.lang_en), getString(R.string.lang_ja)});
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnLang.setAdapter(a);

        edtServer.setText(Prefs.getServer(this));
        socialServer.setText(Prefs.getSocialServer(this));
        enableAnalytics.setChecked(Boolean.TRUE.equals(Prefs.getAnalyticsConsent(this)));
        String lang = Prefs.getLang(this);
        if (lang == null) {
            String defaultLang = Locale.getDefault().getLanguage();
            if ("ja".equals(defaultLang)) {
                spnLang.setSelection(2);
            } else if ("en".equals(defaultLang)) {
                spnLang.setSelection(1);
            } else {
                spnLang.setSelection(0);
            }
        } else if ("ja".equals(lang)) {
            spnLang.setSelection(2);
        } else if ("en".equals(lang)) {
            spnLang.setSelection(1);
        } else {
            spnLang.setSelection(0);
        }

        if (chkAutoInstallRoot != null) {
            chkAutoInstallRoot.setChecked(Prefs.isAutoInstallRoot(this));
            chkAutoInstallRoot.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (ignoreRootToggle) return;
                if (isChecked) {
                    if (requestRootAccess()) {
                        Prefs.setRootGranted(SettingsActivity.this, true);
                        Prefs.setAutoInstallRoot(SettingsActivity.this, true);
                        Toast.makeText(SettingsActivity.this, getString(R.string.auto_install_root), Toast.LENGTH_SHORT).show();
                    } else {
                        Prefs.setRootGranted(SettingsActivity.this, false);
                        Prefs.setAutoInstallRoot(SettingsActivity.this, false);
                        ignoreRootToggle = true;
                        chkAutoInstallRoot.setChecked(false);
                        ignoreRootToggle = false;
                        Toast.makeText(SettingsActivity.this, "ROOT denied", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Prefs.setAutoInstallRoot(SettingsActivity.this, false);
                }
            });
        }

        btnSave.setOnClickListener(v -> {
            String host = edtServer.getText().toString().trim();
            if (host.length() == 0) host = "http://konbini.lol";
            Prefs.setServer(SettingsActivity.this, host);

            String socialHost = socialServer.getText().toString().trim();
            if (socialHost.length() == 0) socialHost = "http://social.konbini.lol";
            while (socialHost.endsWith("/")) {
                socialHost = socialHost.substring(0, socialHost.length() - 1);
            }
            Prefs.setSocialServer(SettingsActivity.this, socialHost);
            Prefs.setAnalyticsConsent(SettingsActivity.this, enableAnalytics.isChecked());

            String sel;
            int pos = spnLang.getSelectedItemPosition();
            if (pos == 2) {
                sel = "ja";
            } else if (pos == 1) {
                sel = "en";
            } else {
                sel = "ru";
            }
            Prefs.setLang(SettingsActivity.this, sel);

            LocaleHelper.applySavedLocale(SettingsActivity.this);
            Toast.makeText(SettingsActivity.this, R.string.save, Toast.LENGTH_SHORT).show();
            finish();
        });

        clearCacheButton.setOnClickListener(v -> {
            Prefs.clearCache(SettingsActivity.this);
            Database.clearCache(SettingsActivity.this);
            Toast.makeText(SettingsActivity.this, "Cache cleared", Toast.LENGTH_SHORT).show();
        });
    }

    private boolean requestRootAccess() {
        Process p = null;
        DataOutputStream os = null;
        try {
            p = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(p.getOutputStream());
            os.writeBytes("exit\n");
            os.flush();
            int rc = p.waitFor();
            return rc == 0;
        } catch (Exception e) {
            Log.e("requestRootAccess", "Failed to request root access: ", e);
            return false;
        } finally {
            try { if (os != null) os.close(); } catch (Exception e) {
                Log.e("requestRootAccess", "Failed to close DataOutputStream: ", e);
            }
            try { if (p != null) p.destroy(); } catch (Exception e) {
                Log.e("requestRootAccess", "Failed to destroy Process: ", e);
            }
        }
    }
}
