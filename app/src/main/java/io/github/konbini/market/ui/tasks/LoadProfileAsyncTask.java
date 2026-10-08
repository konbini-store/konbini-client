package io.github.konbini.market.ui.tasks;

import android.app.ProgressDialog;
import android.os.AsyncTask;

import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.HashMap;

import io.github.konbini.market.R;
import io.github.konbini.market.net.Api;
import io.github.konbini.market.net.Http;
import io.github.konbini.market.ui.UserProfileActivity;
import io.github.konbini.market.util.ImageLoader;
import io.github.konbini.market.util.Prefs;

@SuppressWarnings("deprecation")
public class LoadProfileAsyncTask extends AsyncTask<Void, Void, JSONObject> {
    private final WeakReference<UserProfileActivity> context;
    private final int userId;
    private final ProgressDialog pd;

    public LoadProfileAsyncTask(UserProfileActivity context, int userId, ProgressDialog pd) {
        this.context = new WeakReference<>(context);
        this.userId = userId;
        this.pd = pd;
    }

    protected JSONObject doInBackground(Void... v) {
        UserProfileActivity context = this.context.get();
        try {
            JSONObject localAccount = new JSONObject();
            localAccount.put("username", Prefs.getUsername(context));
            localAccount.put("avatar", Prefs.getAvatar(context));
            localAccount.put("description", "I'm a very cool person because I use Konbini! B)");
            return localAccount;
        } catch (Exception e) {
            return null;
        }
    }

    protected void onPostExecute(JSONObject o) {
        UserProfileActivity context = this.context.get();
        try { pd.dismiss(); } catch (Exception e) {}

        if (o == null) {
            context.txtUser.setText("Network error");
            return;
        }

        String username = o.optString("username", "User");
        String avatar = o.optString("avatar", "default_avatar.png");
        String desc = o.optString("description", "");
        String created = o.optString("created_at", "");

        context.txtUser.setText(username + " (ID: " + userId + ")");
        context.txtDesc.setText(desc.length() > 0 ? desc : "-");
        context.txtCreated.setText(created.length() > 0 ? ("Created: " + created) : "Created: -");

        ImageLoader.load(context, avatar,
                context.imgAvatar, R.drawable.icon_placeholder);
    }
}