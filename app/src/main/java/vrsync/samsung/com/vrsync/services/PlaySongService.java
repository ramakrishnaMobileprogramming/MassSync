package vrsync.samsung.com.vrsync.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.samsung.dallas.networkutils.txrx.PayloadTXManager;

import java.io.File;
import java.io.IOException;

import vrsync.samsung.com.vrsync.Utils.ConstantSFValues;
import vrsync.samsung.com.vrsync.Utils.PayloadUtil;
import vrsync.samsung.com.vrsync.model.playList.PlayListData;

public class PlaySongService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener {

    private Context mContext;
    private MediaPlayer mediaPlayer;
    private PayloadUtil mPayloadUtil;
    private PayloadTXManager mPayloadTXManager;
    private PlayListData playListData;
    private int currentPosition;
    private String payload;

    @Override
    public void onCreate() {
        mContext = PlaySongService.this;
        mPayloadUtil = PayloadUtil.INSTANCE;
        mPayloadTXManager = PayloadTXManager.getInstance();
        playListData = PlayListData.getInstance();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String filePath = null;
        String action = null;
        if (intent != null) {
            if (intent.hasExtra(ConstantSFValues.Playlist.FILE_PATH)) {
                filePath = intent.getStringExtra(ConstantSFValues.Playlist.FILE_PATH);
            }
            if (intent.hasExtra(ConstantSFValues.PlayListControls.SERVICE)) {
                action = intent.getStringExtra(ConstantSFValues.PlayListControls.SERVICE);
            }
            Log.d("Service", "File path: " + filePath);
            assert action != null;
            switch (action) {
                case ConstantSFValues.PlayListControls.IDLE:
                    try {
                        idleSong(filePath);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;

                case ConstantSFValues.PlayListControls.PLAY:
                    pauseSong();
                    break;

                case ConstantSFValues.PlayListControls.PAUSE:
                    playSong();
                    break;

                case ConstantSFValues.PlayListControls.PREVIOUS:
                    try {
                        idleSong(filePath);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;

                case ConstantSFValues.PlayListControls.NEXT:
                    try {
                        idleSong(filePath);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;

                case ConstantSFValues.PlayListControls.RESET:
                    try {
                        resetSong(filePath);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;

                case ConstantSFValues.PlayListControls.SEEK:
                    Long seekValue = 0L;
                    if (intent.hasExtra(ConstantSFValues.PlayListControls.SEEK_VALUE)) {
                        seekValue = intent.getLongExtra(ConstantSFValues.PlayListControls.SEEK_VALUE, 0);
                    }
                    setSongAccToSeek(seekValue);
                    break;

                default:
                    break;
            }
        }
        return START_STICKY;
    }

    private void resetSong(String filePath) throws IOException {
        if (mediaPlayer.isPlaying()) {
            currentPosition = playListData.getCurrentClickedPosition();
            playListData.setPrevClickedPosition(playListData.getCurrentClickedPosition());
            playListData.setCurrentClickedPosition(0);
            /*---Uncomment below 2 Line code to send StopLoad---*/
//            payload = mPayloadUtil.createStop(playListData.getPlayList().get(currentPosition));
//            mPayloadTXManager.sendPayload(payload);
            mediaPlayer.reset();
            idleSong(playListData.getPlayList().get(playListData.getCurrentClickedPosition()).getVideoFilePath());
        }
    }

    private void pauseSong() {
        currentPosition = playListData.getCurrentClickedPosition();
        payload = mPayloadUtil.createPause(playListData.getPlayList().get(currentPosition));
        mPayloadTXManager.sendPayload(payload);
        mediaPlayer.pause();
    }

    private void playSong() {
        currentPosition = playListData.getCurrentClickedPosition();
        payload = mPayloadUtil.createPlay(playListData.getPlayList().get(currentPosition));
        mPayloadTXManager.sendPayload(payload);
        mediaPlayer.start();
    }

    private void idleSong(String filePath) throws IOException {
        Uri uri = Uri.fromFile(new File(filePath));
        currentPosition = playListData.getCurrentClickedPosition();
        payload = mPayloadUtil.createLoad(playListData.getPlayList().get(currentPosition));
        Log.d("Service", "Payload: " + payload);
        mPayloadTXManager.sendPayload(payload);
        if (mediaPlayer != null) {
            mediaPlayer.reset();
        }
        startPlayMusic(uri);
    }

    private void setSongAccToSeek(Long seekValue) {
        mediaPlayer.seekTo(seekValue.intValue());
        payload = mPayloadUtil.createSeekTo(seekValue);
        mPayloadTXManager.sendPayload(payload);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startPlayMusic(Uri uri) throws IOException {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setDataSource(mContext, uri);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.prepareAsync();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        stopSelf();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }
}