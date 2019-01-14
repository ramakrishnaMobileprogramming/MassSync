package vrsync.samsung.com.vrsync.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.HashSet;
import java.util.Set;

public class PrefsUtil {

    private PrefsUtil() {}

    private static SharedPreferences getDefault(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static synchronized Set<String> getStringSet(Context context, String key) {
        return getDefault(context).getStringSet(key, new HashSet<>());
    }

    public static synchronized void putStringSet(Context context, String key, Set<String> set) {
        getDefault(context).edit().putStringSet(key, set).apply();
    }
}
