package vrsync.samsung.com.vrsync.model.playList;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import vrsync.samsung.com.vrsync.Utils.ConstantSFValues;
import vrsync.samsung.com.vrsync.view.VRSyncApplication;

public enum PlaylistManager {
    INSTANCE(VRSyncApplication.getInstance());

    private final Application mApplication;
    private final Gson mGson;
    private final List<PlayListInfoModel> mVideos;

    PlaylistManager(Application application) {
        mApplication = application;
        mGson = new Gson();
        mVideos = new ArrayList<>();
        final Set<String> set = PreferenceManager.getDefaultSharedPreferences(application)
                .getStringSet(ConstantSFValues.Preferences.PLAYLIST_ITEMS, null);
        if (set != null) {
            for (String s : set) {
                final PlayListInfoModel model = mGson.fromJson(s, PlayListInfoModel.class);
                mVideos.add(model);
            }
            Collections.sort(mVideos, (lhs, rhs) -> Long.compare(Long.parseLong(lhs.getVideoDuration()), Long.parseLong(rhs.getVideoDuration())));
        }
        Log.d("PlaylistManager", "PlaylistManager: size:" + mVideos.size());
    }

    public List<PlayListInfoModel> getVideos() {
        return mVideos;
    }

    public void addVideos(List<PlayListInfoModel> videos) {
        Log.d("PlaylistManager", "addVideos: count " + (videos != null ? videos.size() : -1));
        mVideos.addAll(videos);
    }

    public void clear() {
        Log.d("PlaylistManager", "clear: ");
        mVideos.clear();
    }

    public void save() {
        final int size = mVideos.size();
        Log.d("PlaylistManager", "save: count:" + size);
        final SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(mApplication).edit();
        if (size == 0) {
            editor.remove(ConstantSFValues.Preferences.PLAYLIST_ITEMS);
        } else {
            final Set<String> set = new HashSet<>();
            for (PlayListInfoModel v : mVideos) {
                final String s = mGson.toJson(v);
                set.add(s);
            }
            editor.putStringSet(ConstantSFValues.Preferences.PLAYLIST_ITEMS, set);
        }
        editor.apply();
    }
}