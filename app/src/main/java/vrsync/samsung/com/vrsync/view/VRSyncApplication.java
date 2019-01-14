package vrsync.samsung.com.vrsync.view;

import android.app.Application;

public class VRSyncApplication extends Application {

    private static VRSyncApplication sInstance;

   /* //PlayList Data
    private ArrayList<PlayListInfoModel> playList = new ArrayList<>();

    //PlayList Current Clicked Position
    private int currentClickedPosition;

    //PlayList Previous Clicked Position
    private int prevClickedPosition;*/

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
    }

    public static VRSyncApplication getInstance() {
        return sInstance;
    }

    /*public ArrayList<PlayListInfoModel> getPlayList() {
        return playList;
    }

    public void setPlayList(ArrayList<PlayListInfoModel> playList) {
        this.playList = playList;
    }

    public int getCurrentClickedPosition() {
        return currentClickedPosition;
    }

    public void setCurrentClickedPosition(int currentClickedPosition) {
        this.currentClickedPosition = currentClickedPosition;
    }

    public int getPrevClickedPosition() {
        return prevClickedPosition;
    }

    public void setPrevClickedPosition(int prevClickedPosition) {
        this.prevClickedPosition = prevClickedPosition;
    }

    public void resetPlayList(int previous, int current) {
        if (!playList.isEmpty()) {
            for(int i=0;i<playList.size();i++)
            {
                if(i==current)
                {
                    String currentState = playList.get(current).getVideoPlayPauseState();
//                    playList.get(previous).setVideoPlayPauseState(ConstantSValues.IDLE);
                    if (currentState.equalsIgnoreCase(ConstantSValues.IDLE)) {
                        playList.get(current).setVideoPlayPauseState(ConstantSValues.PLAY);
                    } else if (currentState.equalsIgnoreCase(ConstantSValues.PLAY)) {
                        playList.get(current).setVideoPlayPauseState(ConstantSValues.PAUSE);
                    } else if (currentState.equalsIgnoreCase(ConstantSValues.PAUSE)) {
                        playList.get(current).setVideoPlayPauseState(ConstantSValues.PLAY);
                    }
                }
                else
                {
                    playList.get(i).setVideoPlayPauseState(ConstantSValues.IDLE);
                }
            }
        }
    }

    public void resetListRewindCase(int currentClickedPosition) {
        *//*if(playList.get(currentClickedPosition).getVideoPlayPauseState().equalsIgnoreCase(ConstantSValues.PAUSE))
        {

        }
        playList.get(currentClickedPosition).setVideoPlayPauseState(ConstantSValues.IDLE);*//*
    }

    public PlayListInfoModel getCurrentModel() {
        return playList.get(currentClickedPosition);
    }

    public void removeAllList() {
        playList.clear();
    }*/
}