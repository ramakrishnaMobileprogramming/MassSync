package vrsync.samsung.com.vrsync.model.playList;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

@Entity(tableName = "Playlist")
public class PlayListInfoModel implements Parcelable {

    @PrimaryKey(autoGenerate = true) private Long id;
    @ColumnInfo(name = "path") private String videoFilePath;
    @ColumnInfo(name = "title") private String videoTitle;
    @ColumnInfo(name = "channel") private String videoChannelName;
    @ColumnInfo(name = "duration") private String videoDuration;
    @ColumnInfo(name = "state") private String videoPlayPauseState;
    @Ignore private Bitmap imageBitmap;
    @ColumnInfo(name = "video_type") private String videoType;
    @ColumnInfo(name = "audio_type") private String audioType;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setVideoFilePath(String videoFilePath) {
        this.videoFilePath = videoFilePath;
    }

    public void setVideoTitle(String videoTitle) {
        this.videoTitle = videoTitle;
    }

    public void setVideoChannelName(String videoChannelName) {
        this.videoChannelName = videoChannelName;
    }

    public void setVideoDuration(String videoDuration) {
        this.videoDuration = videoDuration;
    }

    public void setVideoPlayPauseState(String videoPlayPauseState) {
        this.videoPlayPauseState = videoPlayPauseState;
    }

    public void setImageBitmap(Bitmap imageBitmap) {
        this.imageBitmap = imageBitmap;
    }

    public String getVideoFilePath() {
        return videoFilePath;
    }

    public String getVideoTitle() {
        return videoTitle;
    }

    public String getVideoChannelName() {
        return videoChannelName;
    }

    public String getVideoDuration() {
        return videoDuration;
    }

    public String getVideoPlayPauseState() {
        return videoPlayPauseState;
    }

    public Bitmap getVideoThumbNailBitmap() {
        return imageBitmap;
    }

    public String getVideoType() {
        return videoType;
    }

    public void setVideoType(String videoType) {
        this.videoType = videoType;
    }

    public String getAudioType() {
        return audioType;
    }

    public void setAudioType(String audioType) {
        this.audioType = audioType;
    }

    public Bitmap getImageBitmap() {
        return imageBitmap;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.videoFilePath);
        dest.writeString(this.videoTitle);
        dest.writeString(this.videoChannelName);
        dest.writeString(this.videoDuration);
        dest.writeString(this.videoPlayPauseState);
        dest.writeString(this.videoType);
        dest.writeString(this.audioType);
    }

    public PlayListInfoModel() {
    }

    protected PlayListInfoModel(Parcel in) {
        this.videoFilePath = in.readString();
        this.videoTitle = in.readString();
        this.videoChannelName = in.readString();
        this.videoDuration = in.readString();
        this.videoPlayPauseState = in.readString();
        this.videoType = in.readString();
        this.audioType = in.readString();
    }

    public static final Creator<PlayListInfoModel> CREATOR = new Creator<PlayListInfoModel>() {
        @Override
        public PlayListInfoModel createFromParcel(Parcel source) {
            return new PlayListInfoModel(source);
        }

        @Override
        public PlayListInfoModel[] newArray(int size) {
            return new PlayListInfoModel[size];
        }
    };
}