package io.github.konbini.market.ui;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.lang.ref.WeakReference;

import io.github.konbini.market.R;
import io.github.konbini.market.net.Api;
import io.github.konbini.market.net.Http;
import io.github.konbini.market.util.Prefs;

@SuppressWarnings("deprecation")
public class LoginAsyncTask extends AsyncTask<Void, Void, String> {
    private final WeakReference<LoginActivity> ref;
    private final ProgressDialog pd;

    private final String username;
    private final String password;

    public LoginAsyncTask(LoginActivity context, ProgressDialog pd, String u, String p) {
        this.ref = new WeakReference<>(context);
        this.pd = pd;
        username = u;
        password = p;
    }

    protected String doInBackground(Void... v) {
        try {
            JSONObject o = new JSONObject();
            o.put("username", username);
            o.put("password", password);
            return Http.postJson(Api.loginUrl(ref.get()), o.toString());
        } catch (Exception e) {
            return null;
        }
    }

    protected void onPostExecute(String s) {
        LoginActivity context = ref.get();
        try { pd.dismiss(); } catch (Exception e) {
            Log.e("onPostExecute@LoginAT", "Failed to dismiss ProgressDialog: ", e);
        }

        if (s == null) {
            context.msg(context.getString(R.string.error_network));
            return;
        }

        try {
            JSONObject o = new JSONObject(s);

            if (!o.optBoolean("success", false)) {
                context.msg(o.optString("error", "Login failed"));
                return;
            }

            int id = o.optInt("user_id", 0);
            String name = o.optString("username", "");
            String authKey = o.optString("token", "");

            if (id <= 0) {
                context.msg("Bad response");
                return;
            }

            Prefs.setAuth(context, id, name, authKey);
            context.refreshUi();
            context.msg("OK");
            context.finish();
        } catch (Exception e) {
            context.msg("Bad response: " + s);
        }
    }
}