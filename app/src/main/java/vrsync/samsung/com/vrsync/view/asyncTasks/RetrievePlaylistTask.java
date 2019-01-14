package vrsync.samsung.com.vrsync.view.asyncTasks;

import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.List;

import vrsync.samsung.com.vrsync.database.PlaylistDao;
import vrsync.samsung.com.vrsync.model.playList.PlayListInfoModel;

public class RetrievePlaylistTask extends AsyncTask<Void, Void, List<PlayListInfoModel>> {

    public interface Result {
        void fromDB(List<PlayListInfoModel> list);
    }

    private PlaylistDao playlistDao;
    private Result listener;

    public RetrievePlaylistTask(PlaylistDao playlistDao, Result listener) {
        this.playlistDao = playlistDao;
        this.listener = listener;
    }

    @Override
    protected List<PlayListInfoModel> doInBackground(Void... voids) {
        ArrayList<PlayListInfoModel> list = new ArrayList<>(playlistDao.getAll());
        for (PlayListInfoModel item : list) {
            item.setImageBitmap(ThumbnailUtils.createVideoThumbnail(item.getVideoFilePath(), MediaStore.Images.Thumbnails.FULL_SCREEN_KIND));
        }
        return list;
    }

    @Override
    protected void onPostExecute(List<PlayListInfoModel> list) {
        super.onPostExecute(list);
        listener.fromDB(list);
    }
}
