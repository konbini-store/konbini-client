package io.github.konbini.market.ui.tasks;

import android.app.ProgressDialog;
import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.HashMap;

import io.github.konbini.market.R;
import io.github.konbini.market.net.Api;
import io.github.konbini.market.net.Http;
import io.github.konbini.market.ui.ProfileActivity;
import io.github.konbini.market.util.ImageLoader;
import io.github.konbini.market.util.Prefs;

@SuppressWarnings("deprecation")
public class LoadAvatarsThenProfileAsyncTask extends AsyncTask<Void, Void, Object[]> {
    private final WeakReference<ProfileActivity> context;
    private final WeakReference<ProgressDialog> pd;

    public LoadAvatarsThenProfileAsyncTask(ProfileActivity context, ProgressDialog pd) {
        this.context = new WeakReference<>(context);
        this.pd = new WeakReference<>(pd);
    }

    protected Object[] doInBackground(Void... v) {
        ProfileActivity context = this.context.get();
        try {
            String avatarsJSONString = Http.getString(Api.avatarsUrl(context));
            if (avatarsJSONString == null) return null;
            JSONArray avatarsJSON = new JSONArray(avatarsJSONString);

            HashMap<String, String> avatars = new HashMap<>(avatarsJSON.length());
            for (int i = 0; i < avatarsJSON.length(); i++) {
                JSONObject avatar = avatarsJSON.getJSONObject(i);
                avatars.put(avatar.optString("name", "Avatar"),
                        avatar.optString("url", "http://konbini.lol/avatars/cat.png"));
            }

            JSONObject localAccount = new JSONObject();
            localAccount.put("username", Prefs.getUsername(context));
            localAccount.put("avatar", Prefs.getAvatar(context));
            localAccount.put("description", "I'm a very cool person because I use Konbini! B)");

            return new Object[] { avatars, localAccount };
        } catch (Exception e) {
            return null;
        }
    }

    protected void onPostExecute(Object[] out) {
        ProfileActivity context = this.context.get();
        ProgressDialog pd = this.pd.get();
        try { pd.dismiss(); } catch (Exception e) {}

        if (out == null) {
            context.msg(context.getString(R.string.error_network));
            return;
        }

        @SuppressWarnings("unchecked")
        final HashMap<String, String> avatars = (HashMap<String, String>) out[0];
        final JSONObject prof = (JSONObject) out[1];

        context.avatars = avatars;
        final String[] avatarNames = avatars.keySet().toArray(new String[0]);

        String username = prof.optString("username", Prefs.getUsername(context));
        String avatarUrl = prof.optString("avatar", "http://konbini.lol/avatars/cat.png");
        String desc = prof.optString("description", "");
        String created = prof.optString("created_at", "");

        context.txtUser.setText(username);

        context.txtCreated.setText("Created: " + created);

        int idx = 0;
        for (int i = 0; i < avatarNames.length; i++) {
            String url = avatars.get(avatarNames[i]);
            if (url != null && (url.equalsIgnoreCase(avatarUrl) || avatarNames[i].equalsIgnoreCase(avatarUrl))) {
                idx = i;
                break;
            }
        }
        if (avatarNames.length > 0) {
            context.selectedAvatarName = avatarNames[idx];
            ImageLoader.load(context, Api.avatarUrl(context, avatars.get(context.selectedAvatarName)),
                    context.imgAvatar, R.drawable.icon_placeholder);
        }
    }
}
