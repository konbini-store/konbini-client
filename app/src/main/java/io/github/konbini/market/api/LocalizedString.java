package io.github.konbini.market.api;

import android.os.Build;
import android.os.LocaleList;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

public class LocalizedString {
    private final LinkedHashMap<String, String> localizations = new LinkedHashMap<>();
    private static final List<String> INDEPENDENT_LANGUAGES = Arrays.asList("zh", "pt", "es");

    private String normalizeLocale(String locale) {
        if (locale == null) return null;
        return locale.trim().replace("_", "-").toLowerCase(Locale.US);
    }

    private String checkForCloseLocales(String locale) {
        String[] splitted = locale.split("-");
        String langCode = splitted[0];
        String countryCode = splitted.length > 1 ? splitted[1] : null;

        if (countryCode != null &&
                localizations.containsKey(langCode+"-"+countryCode)) return langCode+"-"+countryCode;
        if (localizations.containsKey(langCode)) return langCode;
        if (!INDEPENDENT_LANGUAGES.contains(langCode)) {
            for (String locale2 : localizations.keySet()) {
                if (locale2.startsWith(langCode+"-")) return locale2;
            }
        }
        return null;
    }

    public LocalizedString() {}

    public void put(Locale locale, String string) {
        localizations.put(normalizeLocale(locale.getLanguage()+"-"+locale.getCountry()), string);
    }

    public void put(String locale, String string) {
        localizations.put(normalizeLocale(locale), string);
    }

    public String get(Locale locale) {
        return localizations.get(normalizeLocale(locale.getLanguage()+"-"+locale.getCountry()));
    }

    public String get(String locale) {
        return localizations.get(normalizeLocale(locale));
    }

    public LocalizedString(JSONObject locales) {
        Iterator<String> it = locales.keys();

        while (it.hasNext()) {
            String locale = it.next();
            localizations.put(normalizeLocale(locale), locales.optString(locale, ""));
        }
    }

    public LocalizedString(String json) throws JSONException {
        this(new JSONObject(json));
    }

    public String getLocalizedString() {
        ArrayList<String> locales = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            LocaleList llist = LocaleList.getDefault();
            for (int i = 0; i < llist.size(); i++) {
                Locale g = llist.get(i);
                locales.add(g.getLanguage()+"-"+g.getCountry());
            }
        } else {
            Locale defloc = Locale.getDefault();
            locales.add(defloc.getLanguage()+"-"+defloc.getCountry());
        }

        for (int i = 0; i < locales.size(); i++) {
            String match = checkForCloseLocales(locales.get(i).toLowerCase(Locale.US));
            if (match != null)
                return localizations.get(match);
        }

        if (localizations.containsKey("en")) return localizations.get("en");
        if (localizations.containsKey("en-us")) return localizations.get("en-us");
        if (localizations.containsKey("en-gb")) return localizations.get("en-gb");
        if (localizations.containsKey("en-uk")) return localizations.get("en-uk");  // sometimes used although incorrect
        if (!localizations.isEmpty())
            return localizations.get(localizations.keySet().iterator().next());
        return null;
    }
}
