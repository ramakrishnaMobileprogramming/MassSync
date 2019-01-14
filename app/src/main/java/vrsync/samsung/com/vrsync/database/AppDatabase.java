package vrsync.samsung.com.vrsync.database;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;

import vrsync.samsung.com.vrsync.model.playList.PlayListInfoModel;

@Database(entities = {PlayListInfoModel.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;

    public static AppDatabase getInstance(Context context) {
        synchronized (AppDatabase.class) {
            if (instance == null) {
                instance = Room.databaseBuilder(context, AppDatabase.class, "appDB.db").build();
            }
            return instance;
        }
    }

    public static void destroyInstance() {
        instance = null;
    }

    public abstract PlaylistDao playlistDao();
}