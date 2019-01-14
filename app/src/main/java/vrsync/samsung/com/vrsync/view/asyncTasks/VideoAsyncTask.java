package vrsync.samsung.com.vrsync.view.asyncTasks;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import vrsync.samsung.com.vrsync.Utils.ConstantSFValues;
import vrsync.samsung.com.vrsync.model.playList.PlayListInfoModel;

public class VideoAsyncTask extends AsyncTask<Void, Void, ArrayList<PlayListInfoModel>> {

    public interface Result {
        void fromSD(List<PlayListInfoModel> result);
    }

    public VideoAsyncTask(Context context) {
        this.context = new WeakReference<>(context);
        this.consumer = (Result) context;
    }

    private ArrayList<PlayListInfoModel> playList;
    private WeakReference<Context> context;
    private Cursor cursor;
    private Result consumer;

    @Override
    protected void onPreExecute() {
        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Video.VideoColumns.DATA};
        cursor = context.get().getContentResolver().query(uri, projection, MediaStore.Audio.Media.DATA + " like ? ", new String[]{"%/MilkVR%"}, null);
        playList = new ArrayList<>();
    }

    @Override
    protected ArrayList<PlayListInfoModel> doInBackground(Void... voids) {
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String filePath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA));
                playList.add(createFrom(filePath));
            }
        }
        return playList;
    }

    @Override
    protected void onPostExecute(ArrayList<PlayListInfoModel> playList) {
        super.onPostExecute(playList);
        cursor.close();
        consumer.fromSD(playList);
    }

    private PlayListInfoModel createFrom(String path) {
        String title = path.substring(path.lastIndexOf("/") + 1);
        String mimeType = path.substring(path.lastIndexOf("/") + 1);
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(context.get(), Uri.fromFile(new File(path)));
        long duration = Long.parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
        Bitmap imageBitmap = ThumbnailUtils.createVideoThumbnail(path, MediaStore.Images.Thumbnails.FULL_SCREEN_KIND);
        PlayListInfoModel model = new PlayListInfoModel();
        model.setVideoFilePath(path);
        model.setVideoTitle(title);
        model.setVideoChannelName(mimeType);
        model.setVideoDuration(String.valueOf(duration));
        model.setVideoPlayPauseState(ConstantSFValues.PlayListControls.IDLE);
        model.setImageBitmap(imageBitmap);
        return model;
    }
}
