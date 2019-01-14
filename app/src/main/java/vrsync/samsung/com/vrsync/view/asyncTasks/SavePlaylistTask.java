package vrsync.samsung.com.vrsync.view.asyncTasks;

import android.os.AsyncTask;

import java.util.List;

import vrsync.samsung.com.vrsync.database.PlaylistDao;
import vrsync.samsung.com.vrsync.model.playList.PlayListInfoModel;

public class SavePlaylistTask extends AsyncTask<List<PlayListInfoModel>, Void, List<PlayListInfoModel>> {

    public interface Done {
        void done(List<PlayListInfoModel> newList);
    }

    private PlaylistDao playlistDao;
    private Done listener;

    public SavePlaylistTask(PlaylistDao playlistDao, Done listener) {
        this.playlistDao = playlistDao;
        this.listener = listener;
    }

    @Override
    protected List<PlayListInfoModel> doInBackground(List<PlayListInfoModel>... items) {
        playlistDao.clearTable();
        playlistDao.insertList(items[0]);
        return items[0];
    }

    @Override
    protected void onPostExecute(List<PlayListInfoModel> playlistItems) {
        super.onPostExecute(playlistItems);
        listener.done(playlistItems);
    }
}
