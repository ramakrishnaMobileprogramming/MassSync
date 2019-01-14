package vrsync.samsung.com.vrsync.database;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import java.util.List;

import vrsync.samsung.com.vrsync.model.playList.PlayListInfoModel;

@Dao
public interface PlaylistDao {

    @Query("SELECT * FROM Playlist ORDER BY id ASC")
    List<PlayListInfoModel> getAll();

    @Query("DELETE FROM Playlist")
    void clearTable();

    @Query("SELECT COUNT(*) FROM Playlist")
    int getCount();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMulti(PlayListInfoModel... datas);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertList(List<PlayListInfoModel> list);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(PlayListInfoModel data);
}