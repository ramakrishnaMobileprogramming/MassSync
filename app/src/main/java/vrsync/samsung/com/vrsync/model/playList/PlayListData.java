package vrsync.samsung.com.vrsync.model.playList;

import java.util.ArrayList;

import vrsync.samsung.com.vrsync.Utils.ConstantSFValues;

public class PlayListData {

    private static PlayListData playListData = new PlayListData();
    private ArrayList<PlayListInfoModel> playList = new ArrayList<>();
    private int currentClickedPosition;
    private int prevClickedPosition;

    public static PlayListData getInstance() {
        return playListData;
    }

    public ArrayList<PlayListInfoModel> getPlayList() {
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
                    if (currentState.equalsIgnoreCase(ConstantSFValues.PlayListControls.IDLE)) {
                        playList.get(current).setVideoPlayPauseState(ConstantSFValues.PlayListControls.PLAY);
                    } else if (currentState.equalsIgnoreCase(ConstantSFValues.PlayListControls.PLAY)) {
                        playList.get(current).setVideoPlayPauseState(ConstantSFValues.PlayListControls.PAUSE);
                    } else if (currentState.equalsIgnoreCase(ConstantSFValues.PlayListControls.PAUSE)) {
                        playList.get(current).setVideoPlayPauseState(ConstantSFValues.PlayListControls.PLAY);
                    }
                }
                else
                {
                    playList.get(i).setVideoPlayPauseState(ConstantSFValues.PlayListControls.IDLE);
                }
            }
        }
    }

    public void resetListRewindCase(int currentClickedPosition) {
        if(playList.get(currentClickedPosition).getVideoPlayPauseState().equalsIgnoreCase(ConstantSFValues.PlayListControls.PAUSE))
        {

        }
        playList.get(currentClickedPosition).setVideoPlayPauseState(ConstantSFValues.PlayListControls.IDLE);
    }

    public PlayListInfoModel getCurrentModel() {
        return playList.get(currentClickedPosition);
    }

    public void removeAllList() {
        playList.clear();
    }
}