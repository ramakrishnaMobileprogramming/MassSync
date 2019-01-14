package vrsync.samsung.com.vrsync.view.activity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.content.PermissionChecker;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatSeekBar;
import android.support.v7.widget.AppCompatTextView;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Toast;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import vrsync.samsung.com.vrsync.R;
import vrsync.samsung.com.vrsync.Utils.CircleSeekBar;
import vrsync.samsung.com.vrsync.Utils.ConnectedDeviceExt;
import vrsync.samsung.com.vrsync.Utils.ConstantSFValues;
import vrsync.samsung.com.vrsync.Utils.ConstantSValues;
import vrsync.samsung.com.vrsync.Utils.DeviceFetcher;
import vrsync.samsung.com.vrsync.database.AppDatabase;
import vrsync.samsung.com.vrsync.listeners.IControllerListener;
import vrsync.samsung.com.vrsync.model.playList.PlayListData;
import vrsync.samsung.com.vrsync.model.playList.PlayListInfoModel;
import vrsync.samsung.com.vrsync.services.PlaySongService;
import vrsync.samsung.com.vrsync.view.asyncTasks.RetrievePlaylistTask;
import vrsync.samsung.com.vrsync.view.asyncTasks.VideoAsyncTask;
import vrsync.samsung.com.vrsync.view.fragments.DeviceListFragment;
import vrsync.samsung.com.vrsync.view.fragments.NoDeviceConnectedFragment;
import vrsync.samsung.com.vrsync.view.fragments.PlayListFragment;

public class DashboardActivity extends BaseActivity implements View.OnClickListener,
        IControllerListener, View.OnLongClickListener, SeekBar.OnSeekBarChangeListener,
        VideoAsyncTask.Result, RetrievePlaylistTask.Result {

    private AppCompatTextView connectedDeviceTV;
    private AppCompatTextView playingDeviceTv;
    private AppCompatImageView refreshIV;
    private AppCompatTextView missingFilesTV;
    private AppCompatTextView overheatingDeviceTV;
    private AppCompatTextView lowBatteryTV;
    private AppCompatTextView moreArrowTv;
    private AppCompatImageView thumbnailCurrentIV;
    private AppCompatTextView currentDurationTV;
    private AppCompatSeekBar progressSeekBarSB;
    private AppCompatTextView totalDurationTv;
    private AppCompatImageView previousSongIV;
    private AppCompatImageView rewindSongIV;
    private AppCompatImageView playPauseIV;
    private AppCompatImageView nextSongIV;
    private AppCompatImageView playlistIV;
    // private AppCompatTextView nextSongTitleTV;
    // private AppCompatTextView nextSongChannelNameTV;
    private AppCompatImageView thumbnailNextIV;
    private Context mContext;
    private PlayListData listInfo;
    private Handler seekBarHandler;
    private Long currentProgress;
    private ProgressDialog progressDialog;

    private CircleSeekBar circularSeekBar;
    private Handler circularSeekBarHandler;
    private int circularSeekBarProgress;
    private boolean isOnClickCalled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        initializeView();
    }

    //Initialize the Views
    private void initializeView() {
        initializeDeviceFetcher();
        connectedDeviceTV = findViewById(R.id.connected_device_tv);
        playingDeviceTv = findViewById(R.id.playing_device_tv);
        refreshIV = findViewById(R.id.refresh_iv);
        missingFilesTV = findViewById(R.id.missing_files_tv);
        overheatingDeviceTV = findViewById(R.id.overheating_device_tv);
        lowBatteryTV = findViewById(R.id.low_battery_tv);
        moreArrowTv = findViewById(R.id.more_arrow_tv);
        thumbnailCurrentIV = findViewById(R.id.thumbnail_current_iv);
        currentDurationTV = findViewById(R.id.current_duration_tv);
        progressSeekBarSB = findViewById(R.id.progress_seekbar_sb);
        totalDurationTv = findViewById(R.id.total_duration_tv);
        previousSongIV = findViewById(R.id.previous_song_iv);
        rewindSongIV = findViewById(R.id.rewind_song_iv);
        playPauseIV = findViewById(R.id.play_pause_iv);
        nextSongIV = findViewById(R.id.next_song_iv);
        playlistIV = findViewById(R.id.playlist_iv);
        /*nextSongTitleTV = findViewById(R.id.next_song_title_tv);
        nextSongChannelNameTV = findViewById(R.id.next_song_channel_name_tv);*/
        thumbnailNextIV = findViewById(R.id.thumbnail_next_iv);
        circularSeekBar = findViewById(R.id.circular_progress_bar);
        mContext = DashboardActivity.this;
        seekBarHandler = new Handler();
        circularSeekBarHandler = new Handler();
        progressDialog = new ProgressDialog(mContext);
        listInfo = PlayListData.getInstance();
        setCurrentFragment();
    }

    public void initSDCardVideoTask() {
        new VideoAsyncTask(this).execute();
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCancelable(false);
        progressDialog.setTitle("Fetching Videos from SD card");
        progressDialog.setMessage("Please Wait...");
        progressDialog.show();
    }

    //Initialize Device Fetcher Class to get Connected DeviceList
    private void initializeDeviceFetcher() {
        new DeviceFetcher(this).initializeDeviceCallbacks();
    }

    //Display Fragment According to Device Data List
    private void setCurrentFragment() {
        if (ConstantSValues.isDevicesAvailable) {
            DeviceListFragment fragment = new DeviceListFragment();
            addFragment(R.id.main_fragment, fragment, true, ConstantSFValues.Fragments.DEVICE_LIST_FRAGMENT);
        } else {
            NoDeviceConnectedFragment fragment = new NoDeviceConnectedFragment();
            addFragment(R.id.main_fragment, fragment, true, ConstantSFValues.Fragments.NO_DEVICE_CONNECTED_FRAGMENT);
        }
        setValues();
    }

    //Set Initial Values on UI
    private void setValues() {
        connectedDeviceTV.setText(String.valueOf(getIntent().getIntExtra(ConstantSFValues.Playlist.SIZE, 0)));
        playingDeviceTv.setText("/ 0");
        missingFilesTV.setText("0");
        overheatingDeviceTV.setText("0");
        lowBatteryTV.setText("0");
        progressSeekBarSB.setMax(100);
        currentProgress = 0L;
        totalDurationTv.setText(ConstantSValues.getDurationString(0L));
        circularSeekBarProgress = 0;
        setAlphaValues();
        setListeners();
    }

    //Make Play,Pause,Next & Previous Buttons Lighter
    private void setAlphaValues() {
        float alphaValue;
        if (ConstantSValues.isDevicesAvailable && !PlayListData.getInstance().getPlayList().isEmpty()) {
            previousSongIV.setEnabled(true);
            rewindSongIV.setEnabled(true);
            playPauseIV.setEnabled(true);
            nextSongIV.setEnabled(true);
            progressSeekBarSB.setEnabled(true);
            alphaValue = 1.0f;
        } else {
            previousSongIV.setEnabled(false);
            rewindSongIV.setEnabled(false);
            playPauseIV.setEnabled(false);
            nextSongIV.setEnabled(false);
            progressSeekBarSB.setEnabled(false);
            alphaValue = 0.4f;
        }
        previousSongIV.setAlpha(alphaValue);
        rewindSongIV.setAlpha(alphaValue);
        playPauseIV.setAlpha(alphaValue);
        nextSongIV.setAlpha(alphaValue);
    }

    //Set Listener on All Buttons or Images
    private void setListeners() {
        refreshIV.setOnClickListener(this);
        playlistIV.setOnClickListener(this);
        moreArrowTv.setOnClickListener(this);
        playPauseIV.setOnClickListener(this);

        previousSongIV.setOnLongClickListener(this);
        rewindSongIV.setOnLongClickListener(this);
        nextSongIV.setOnLongClickListener(this);
        playPauseIV.setOnLongClickListener(this);


        progressSeekBarSB.setOnSeekBarChangeListener(this);
        doPlayListWork();
    }

    public void doPlayListWork() {
        if (checkStoragePermission()) {
            createFolderAfterStoragePermission();
        } else {
            checkRW();
        }
    }

    //Create MilkVR Folder if not exist & after that initialize PlayList Fetching Process
    private void createFolderAfterStoragePermission() {
        //Creating directory in internal memory
        File documentsFolder = new File(ConstantSFValues.Playlist.STORAGE_PATH);
        if (!documentsFolder.exists()) {
            documentsFolder.mkdirs();
        }
        // Check DB for saved Playlist
        new RetrievePlaylistTask(AppDatabase.getInstance(this).playlistDao(), this).execute();
    }

    // Check & get Read Write Runtime Permissions
    public void checkRW() {
        Dexter.withActivity(this)
                .withPermissions(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()) {
                            createFolderAfterStoragePermission();
                        } else {
                            toastMsg("This application will not run without storage permission");
                            checkRW();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).withErrorListener(error -> Toast.makeText(getApplicationContext(), "Error occurred! ", Toast.LENGTH_SHORT).show()).check();
    }

    //Check Read & Write Storage Permission
    private boolean checkStoragePermission() {
        int readStoragePermission = PermissionChecker.checkSelfPermission(mContext, Manifest.permission.READ_EXTERNAL_STORAGE);
        int writeStoragePermission = PermissionChecker.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (Build.VERSION.SDK_INT > 22) {
            return checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        } else {
            return readStoragePermission == PermissionChecker.PERMISSION_GRANTED && writeStoragePermission == PermissionChecker.PERMISSION_GRANTED;
        }
    }

    //Call Back of Device List
    @Override
    public void getDeviceList(ArrayList<ConnectedDeviceExt> list) {
        super.getDeviceList(list);
        if (!list.isEmpty()) {
            int size = list.size();
            connectedDeviceTV.setText(String.valueOf(size));
            DeviceListFragment fragment = (DeviceListFragment) getSupportFragmentManager().findFragmentByTag("MY_FRAGMENT");
            if (fragment == null || !fragment.isVisible()) {
                replaceFragment(R.id.main_fragment, new DeviceListFragment(), false, ConstantSFValues.Fragments.DEVICE_LIST_FRAGMENT);
            }
            setAlphaValues();
        } else {
            NoDeviceConnectedFragment fragment = (NoDeviceConnectedFragment) getSupportFragmentManager().findFragmentByTag("MY_FRAGMENT");
            if (fragment == null || !fragment.isVisible()) {
                replaceFragment(R.id.main_fragment, new NoDeviceConnectedFragment(), false, ConstantSFValues.Fragments.NO_DEVICE_CONNECTED_FRAGMENT);
            }
        }
    }

    //Call Back of On Long Press
    @Override
    public boolean onLongClick(View view) {
        switch (view.getId()) {
            case R.id.play_pause_iv:
                if (listInfo != null && !listInfo.getPlayList().isEmpty()) {
                    isOnClickCalled = false;
                    circularSeekBar.removeCallbacks(updateCircularProgress);
                    circularSeekBarHandler.post(updateCircularProgress);
                }
                break;

            case R.id.previous_song_iv:
                playPreviousSong();
                break;

            case R.id.rewind_song_iv:
                resetSong();
                break;

            case R.id.next_song_iv:
                playNextSong();
                break;
            default:
        }
        return true;
    }

    //Call back from On click
    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.refresh_iv:
                break;

            case R.id.playlist_iv:
                if (checkStoragePermission()) {
                    callPlayListFragment();
                }
                break;

            case R.id.more_arrow_tv:
                break;

            case R.id.play_pause_iv:
                if (listInfo != null && !listInfo.getPlayList().isEmpty()) {
                    isOnClickCalled = true;
                    circularSeekBar.removeCallbacks(updateCircularProgress);
                    circularSeekBarHandler.post(updateCircularProgress);
                }
                break;

            default:
        }
    }

    //Play Previous Song
    private void playPreviousSong() {
        if (listInfo != null && !listInfo.getPlayList().isEmpty()) {
            int prePosition = listInfo.getCurrentClickedPosition() - 1;
            if (prePosition >= 0) {
                listInfo.setPrevClickedPosition(listInfo.getCurrentClickedPosition());
                listInfo.setCurrentClickedPosition(prePosition);
                currentProgress = 0L;
                setControllerData(listInfo.getCurrentModel(), ConstantSFValues.PlayListControls.PREVIOUS);
                playSong(listInfo.getCurrentModel(), ConstantSFValues.PlayListControls.PREVIOUS, 0L);
            }
        }
    }

    //Rewind or Reset Current Song
    private void resetSong() {
        if (listInfo != null && !listInfo.getPlayList().isEmpty()) {
            currentProgress = 0L;
            currentDurationTV.setText(ConstantSValues.getDurationString(0L));
            progressSeekBarSB.setProgress(0);
            setControllerData(listInfo.getCurrentModel(), ConstantSFValues.PlayListControls.RESET);
            playSong(listInfo.getCurrentModel(), ConstantSFValues.PlayListControls.RESET, 0L);
        }
    }

    //Play Next Song
    private void playNextSong() {
        if (listInfo != null && !listInfo.getPlayList().isEmpty()) {
            int nextPosition = listInfo.getCurrentClickedPosition() + 1;
            if (nextPosition < listInfo.getPlayList().size()) {
                listInfo.setPrevClickedPosition(listInfo.getCurrentClickedPosition());
                listInfo.setCurrentClickedPosition(nextPosition);
                currentProgress = 0L;
                setControllerData(listInfo.getCurrentModel(), ConstantSFValues.PlayListControls.NEXT);
                playSong(listInfo.getCurrentModel(), ConstantSFValues.PlayListControls.NEXT, 0L);
            }
        }
    }

    //Replace Fragment
    private void callPlayListFragment() {
        Fragment playListFragment = getSupportFragmentManager().findFragmentByTag(ConstantSFValues.Fragments.PLAYLIST_FRAGMENT);
        if (playListFragment == null || !playListFragment.isVisible()) {
            replaceFragment(R.id.main_fragment, PlayListFragment.newInstance(PlayListData.getInstance().getPlayList()), true, ConstantSFValues.Fragments.PLAYLIST_FRAGMENT);
        }
    }

    //Call Back from PlayList Adapter(Long Press)
    @Override
    public void playListItemClicked() {
        PlayListInfoModel model = listInfo.getCurrentModel();
        setControllerData(model, null);
        playSong(model, null, 0L);
    }

    //Set Controller(Bottom Footer) Data
    private void setControllerData(PlayListInfoModel model, String action) {
        thumbnailCurrentIV.setImageBitmap(model.getVideoThumbNailBitmap());
        setNextTitle();
        totalDurationTv.setText(ConstantSValues.getDurationString(Long.parseLong(model.getVideoDuration())));
        if (model.getVideoPlayPauseState().equalsIgnoreCase(ConstantSFValues.PlayListControls.IDLE)) {
            currentProgress = 0L;
            playPauseIV.setImageResource(R.drawable.pause_big_btn);
            seekBarHandler.removeCallbacks(mUpdateTimeTask);
            seekBarHandler.postDelayed(mUpdateTimeTask, ConstantSFValues.Numbers.ONE_MINUTE);
            if (action != null && action.equalsIgnoreCase(ConstantSFValues.PlayListControls.RESET)) {
                playPauseIV.setImageResource(R.drawable.play_big_btn);
                seekBarHandler.removeCallbacks(mUpdateTimeTask);
            }
        } else if (model.getVideoPlayPauseState().equalsIgnoreCase(ConstantSFValues.PlayListControls.PLAY)) {
            if (action != null && action.equalsIgnoreCase(ConstantSFValues.PlayListControls.RESET)) {
                seekBarHandler.removeCallbacks(mUpdateTimeTask);
                currentProgress = 0L;
                seekBarHandler.postDelayed(mUpdateTimeTask, ConstantSFValues.Numbers.ONE_MINUTE);
            } else {
                playPauseIV.setImageResource(R.drawable.play_big_btn);
                seekBarHandler.removeCallbacks(mUpdateTimeTask);
            }

        } else if (model.getVideoPlayPauseState().equalsIgnoreCase(ConstantSFValues.PlayListControls.PAUSE)) {
            if (action != null && action.equalsIgnoreCase(ConstantSFValues.PlayListControls.RESET)) {
                seekBarHandler.removeCallbacks(mUpdateTimeTask);
                currentProgress = 0L;
            } else {
                playPauseIV.setImageResource(R.drawable.pause_big_btn);
                seekBarHandler.postDelayed(mUpdateTimeTask, ConstantSFValues.Numbers.ONE_MINUTE);
            }
        }
    }

    //Set title & channel of next Song
    private void setNextTitle() {
        int position = listInfo.getCurrentClickedPosition();
        if (position < listInfo.getPlayList().size() - 1) {
            // nextSongTitleTV.setText(listInfo.getPlayList().get(position + 1).getVideoTitle());
            // nextSongChannelNameTV.setText(listInfo.getPlayList().get(position + 1).getVideoChannelName())
            thumbnailNextIV.setImageBitmap(listInfo.getPlayList().get(position + 1).getVideoThumbNailBitmap());
        }
        if (position >= listInfo.getPlayList().size() - 1) {
            //nextSongTitleTV.setText(getString(R.string.no_next_song));
            //nextSongChannelNameTV.setText(getString(R.string.no_next_song));
            thumbnailNextIV.setImageResource(R.drawable.video_thumbnail);
        }
    }

    //Play the Song using Service
    private void playSong(PlayListInfoModel model, String action, Long duration) {
        Intent serviceIntent = new Intent(mContext, PlaySongService.class);
        String filePath = model.getVideoFilePath();
        if (action != null) {
            if (action.equalsIgnoreCase(ConstantSFValues.PlayListControls.SEEK)) {
                serviceIntent.putExtra(ConstantSFValues.PlayListControls.SEEK_VALUE, duration);
            }
            serviceIntent.putExtra(ConstantSFValues.PlayListControls.SERVICE, action);
        } else {
            serviceIntent.putExtra(ConstantSFValues.PlayListControls.SERVICE, model.getVideoPlayPauseState());
        }
        serviceIntent.putExtra(ConstantSFValues.Playlist.FILE_PATH, filePath);
        mContext.startService(serviceIntent);
        if (action != null && action.equalsIgnoreCase(ConstantSFValues.PlayListControls.RESET)) {
            listInfo.resetListRewindCase(listInfo.getCurrentClickedPosition());
        } else {
            if (action != null && action.equalsIgnoreCase(ConstantSFValues.PlayListControls.SEEK)) {
                //Do Nothing
            } else {
                listInfo.resetPlayList(listInfo.getPrevClickedPosition(), listInfo.getCurrentClickedPosition());
                syncToFragment();
            }
        }
    }

    //Sync Controller Actions to Fragments
    private void syncToFragment() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.main_fragment);
        if (fragment instanceof PlayListFragment) {
            ((PlayListFragment) fragment).notifyAccToController();
        }
        bus.postSticky(listInfo.getCurrentModel().getVideoPlayPauseState());
    }

    //Call Back of Seek bar Change
    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
    }

    //Call Back of Seek bar Change
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    //Call Back of Seek bar Change
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        Long seekBarTime = (Long.parseLong(listInfo.getCurrentModel().getVideoDuration()) * seekBar.getProgress()) / 100;
        setAudioAccToSeek(seekBarTime);
    }

    // Callback received when loading videos from SD card
    @Override
    public void fromSD(List<PlayListInfoModel> result) {
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.dismiss();

        //TODO Do not Work from Client Side
        //TODO Do not able to remove duplicate Elements
        /*// Filtering for new items only
        result.removeAll(PlayListData.getInstance().getPlayList());
        ArrayList<PlayListInfoModel> newList = new ArrayList<>(result);*/

        if (!result.isEmpty()) {
            PlayListData.getInstance().setPlayList(new ArrayList<>(result));
            onPlaylistLoaded();
        }

        Fragment playlistFragment = getSupportFragmentManager().findFragmentByTag(ConstantSFValues.Fragments.PLAYLIST_FRAGMENT);
        if (playlistFragment != null) {
            ((PlayListFragment) playlistFragment).setRefreshingFalse(result);
        }
    }

    // Callback received when loading videos from DB
    @Override
    public void fromDB(List<PlayListInfoModel> list) {
        if (list.isEmpty()) {
            initSDCardVideoTask();
        } else {
            PlayListData.getInstance().setPlayList(new ArrayList<>(list));
            onPlaylistLoaded();
        }
    }

    public void onPlaylistLoaded() {
        thumbnailCurrentIV.setImageBitmap(listInfo.getPlayList().get(0).getVideoThumbNailBitmap());
        thumbnailNextIV.setImageBitmap(listInfo.getPlayList().get(1).getVideoThumbNailBitmap());
        PlayListData.getInstance().setCurrentClickedPosition(0);
        setAlphaValues();
    }

    //Seek Current Song
    private void setAudioAccToSeek(Long duration) {
        PlayListInfoModel model = listInfo.getCurrentModel();
        currentProgress = duration;
        playSong(model, ConstantSFValues.PlayListControls.SEEK, duration);
    }

    //Handler for handling seek bar progress.
    private Runnable mUpdateTimeTask = new Runnable() {
        public void run() {
            long totalDuration = Long.parseLong(listInfo.getCurrentModel().getVideoDuration());
            currentProgress = currentProgress + ConstantSFValues.Numbers.ONE_MINUTE;
            if (currentProgress >= totalDuration) {
                currentDurationTV.setText(ConstantSValues.getDurationString(0L));
                playNextSong();
            } else {
                currentDurationTV.setText(ConstantSValues.getDurationString(currentProgress));
                int progress = ConstantSValues.convertTimeToProgress(totalDuration, currentProgress);
                progressSeekBarSB.setProgress(progress);
                seekBarHandler.postDelayed(this, ConstantSFValues.Numbers.ONE_MINUTE);
            }
        }
    };

    //Handler for handling circular progress bar on Long Press of Play Pause button.
    private Runnable updateCircularProgress = new Runnable() {
        public void run() {
            int max = 99;
            if (isOnClickCalled) {
                Random r = new Random();
                max = r.nextInt(75) + 25;
            }
            circularSeekBarProgress++;
            if (circularSeekBarProgress > max) {
                circularSeekBarProgress = 0;
                circularSeekBar.setProgressDisplayAndInvalidate(0);
                if (!isOnClickCalled) {
                    PlayListInfoModel model = listInfo.getCurrentModel();
                    setControllerData(model, null);
                    playSong(model, null, 0L);
                }
                circularSeekBarHandler.removeCallbacks(updateCircularProgress);
            } else {
                circularSeekBar.setProgressDisplayAndInvalidate(circularSeekBarProgress);
                circularSeekBarHandler.postDelayed(this, 20);
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        seekBarHandler.removeCallbacks(mUpdateTimeTask);
        AppDatabase.destroyInstance();
    }
}