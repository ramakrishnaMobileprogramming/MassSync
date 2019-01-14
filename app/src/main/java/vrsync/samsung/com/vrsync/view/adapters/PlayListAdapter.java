package vrsync.samsung.com.vrsync.view.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;

import vrsync.samsung.com.vrsync.R;
import vrsync.samsung.com.vrsync.Utils.ConstantSFValues;
import vrsync.samsung.com.vrsync.Utils.ConstantSValues;
import vrsync.samsung.com.vrsync.listeners.IControllerListener;
import vrsync.samsung.com.vrsync.model.playList.PlayListData;
import vrsync.samsung.com.vrsync.model.playList.PlayListInfoModel;

public class PlayListAdapter extends RecyclerView.Adapter<PlayListAdapter.PlayListHolder> {

    private Context mContext;
    private int prePosClicked = 0;
    private IControllerListener controllerListener;
    private ArrayList<PlayListInfoModel> playList;

    public PlayListAdapter(Context context, IControllerListener listener, ArrayList<PlayListInfoModel> playList) {
        this.mContext = context;
        controllerListener = listener;
        this.playList = playList;
    }

    @NonNull
    @Override
    public PlayListHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.playlist_rv_row, viewGroup, false);
        return new PlayListHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull PlayListHolder playListHolder, int i) {
        PlayListInfoModel model = playList.get(i);
        playListHolder.titleTV.setText(model.getVideoTitle());
        playListHolder.channelNameTv.setText(model.getVideoChannelName());
        playListHolder.durationTv.setText(ConstantSValues.getDurationString(Long.parseLong(model.getVideoDuration())));
        playListHolder.playPauseStateIv.setImageResource(getPlayPauseResource(model.getVideoPlayPauseState()));
        setImageThumbNail(model.getVideoThumbNailBitmap(), playListHolder.thumbnailIV);
        playListHolder.playPauseStateIv.setOnClickListener(view -> {
            PlayListData instance = PlayListData.getInstance();

            //Notify Previous and Current Clicked Item
            notifyItemChanged(playListHolder.getAdapterPosition());
            notifyItemChanged(prePosClicked);

            //Set Current & Previous Position in PlayListData Class
            instance.setCurrentClickedPosition(playListHolder.getAdapterPosition());
            instance.setPrevClickedPosition(prePosClicked);

            //Call Back for Dashboard Activity
            controllerListener.playListItemClicked();

            prePosClicked = playListHolder.getAdapterPosition();
        });
    }

    @Override
    public long getItemId(int position) {
        return playList.get(position).getId();
    }

    @Override
    public int getItemCount() {
        return playList.size();
    }

    private int getPlayPauseResource(String value) {
        switch (value) {
            case ConstantSFValues.PlayListControls.IDLE:
                return R.drawable.play_small_btn;

            case ConstantSFValues.PlayListControls.PLAY:
                return R.drawable.pause_small_btn;

            case ConstantSFValues.PlayListControls.PAUSE:
                return R.drawable.play_small_btn;

            default:
                return R.drawable.play_small_btn;
        }
    }

    private void setImageThumbNail(Bitmap thumbNailBitmap, AppCompatImageView thumbNailIV) {
        RequestOptions myOptions = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .format(DecodeFormat.PREFER_RGB_565)
                .centerCrop()
                .dontAnimate()
                .dontTransform()
                .placeholder(R.drawable.giphy)
                .skipMemoryCache(false);

        Glide.with(mContext)
                .load(thumbNailBitmap)
                .apply(myOptions)
                .into(thumbNailIV);
    }

    public void setList(ArrayList<PlayListInfoModel> playList) {
        this.playList = playList;
        notifyDataSetChanged();
    }

    public ArrayList<PlayListInfoModel> getPlayList() {
        return playList;
    }

    class PlayListHolder extends RecyclerView.ViewHolder {
        private AppCompatImageView thumbnailIV;
        private AppCompatTextView titleTV;
        private AppCompatTextView channelNameTv;
        private AppCompatTextView durationTv;
        private AppCompatImageView playPauseStateIv;

        PlayListHolder(View v) {
            super(v);
            thumbnailIV = v.findViewById(R.id.thumbnail_current_iv);
            titleTV = v.findViewById(R.id.title_tv);
            channelNameTv = v.findViewById(R.id.channel_name_tv);
            durationTv = v.findViewById(R.id.duration_tv);
            playPauseStateIv = v.findViewById(R.id.play_pause_state_iv);
        }
    }
}