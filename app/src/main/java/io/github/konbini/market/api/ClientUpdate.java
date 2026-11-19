package io.github.konbini.market.api;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by paul on 14/07/26.
 */

public class ClientUpdate {
    public int versionCode;
    public String versionName;
    public String updateUrl;
    public LocalizedString notes;

    public ClientUpdate(JSONObject obj) throws JSONException {
        this.versionCode = obj.getInt("version_code");
        this.versionName = obj.getString("version_name");
        this.updateUrl = obj.getString("update_url");
        if (obj.has("notes")) {
            this.notes = new LocalizedString(obj.optJSONObject("notes"));
            return;
        }
        this.notes = new LocalizedString();
        if (obj.has("notes_en"))
            this.notes.put("en", obj.getString("notes_en"));
        if (obj.has("notes_ru"))
            this.notes.put("ru", obj.getString("notes_ru"));
    }
}
