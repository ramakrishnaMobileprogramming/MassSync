package vrsync.samsung.com.vrsync.view.fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vrsync.samsung.com.vrsync.R;
import vrsync.samsung.com.vrsync.Utils.ConstantSFValues;
import vrsync.samsung.com.vrsync.database.AppDatabase;
import vrsync.samsung.com.vrsync.listeners.IControllerListener;
import vrsync.samsung.com.vrsync.model.playList.PlayListData;
import vrsync.samsung.com.vrsync.model.playList.PlayListInfoModel;
import vrsync.samsung.com.vrsync.view.activity.DashboardActivity;
import vrsync.samsung.com.vrsync.view.adapters.PlayListAdapter;
import vrsync.samsung.com.vrsync.view.asyncTasks.SavePlaylistTask;

public class PlayListFragment extends BaseFragment implements View.OnClickListener, SavePlaylistTask.Done, SwipeRefreshLayout.OnRefreshListener {

    private PlayListAdapter playListAdapter;
    private AppCompatTextView videoCountTV;
    private ArrayList<PlayListInfoModel> playlist;
    private AppCompatImageView savePlaylistIv;
    private SwipeRefreshLayout swipeLayout;
    private AppCompatImageView crossIV;
    private RecyclerView playListRV;
    private IControllerListener controllerListener;
    private LinearLayoutManager linearLayoutManager;
    private Context mContext;
    private AppCompatImageView thumbnailCurrentIV;
    private AppCompatImageView thumbnailNextIV;

    //Constructor of PlayList Fragment
    public PlayListFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        if (getArguments() != null) {
            mContext = getActivity();
            playlist = getArguments().getParcelableArrayList(ConstantSFValues.Playlist.PLAYLIST_KEY);
        }
    }

    //This static method return PlayList fragment
    public static PlayListFragment newInstance(List<PlayListInfoModel> list) {
        PlayListFragment fragment = new PlayListFragment();
        Bundle args = new Bundle();
        args.putParcelableArrayList(ConstantSFValues.Playlist.PLAYLIST_KEY, new ArrayList<>(list));
        fragment.setArguments(args);
        return fragment;
    }

    //Dialog for Saving the changes of Play List
    DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                startSavePlaylistTask();
                break;

            case DialogInterface.BUTTON_NEGATIVE:
                popUpFragment();
                break;

            default:
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_playlist, container, false);
        initializeView(view, savedInstanceState);
        return view;
    }

    private void initializeView(View view, Bundle savedInstanceState) {
        savePlaylistIv = view.findViewById(R.id.save_playlist_iv);
        swipeLayout = view.findViewById(R.id.pull_to_refresh_layout);
        playListRV = view.findViewById(R.id.playList_rv);
        crossIV = view.findViewById(R.id.cross_iv);
        videoCountTV = view.findViewById(R.id.video_count_tv);
        thumbnailCurrentIV = getActivity().findViewById(R.id.thumbnail_current_iv);
        thumbnailNextIV = getActivity().findViewById(R.id.thumbnail_next_iv);
        controllerListener = (IControllerListener) mContext;
        linearLayoutManager = new LinearLayoutManager(mContext);
        setListeners(savedInstanceState);
    }

    private void setListeners(Bundle savedInstanceState) {
        swipeLayout.setOnRefreshListener(this);
        crossIV.setOnClickListener(this);
        savePlaylistIv.setOnClickListener(this);
        setInitialValues(savedInstanceState);
    }

    private void setInitialValues(Bundle savedInstanceState) {
        playListRV.setLayoutManager(linearLayoutManager);
        if (playlist == null && savedInstanceState != null) {
            playlist = savedInstanceState.getParcelableArrayList(ConstantSFValues.Playlist.PLAYLIST_KEY);
        }
        playListAdapter = new PlayListAdapter(mContext, controllerListener, playlist);
        playListAdapter.setHasStableIds(false);
        String videoCount = playlist.size() + " videos";
        videoCountTV.setText(videoCount);
        playListRV.setAdapter(playListAdapter);
        DragHelperCallback dragCallback = new DragHelperCallback();
        ItemTouchHelper helper = new ItemTouchHelper(dragCallback);
        helper.attachToRecyclerView(playListRV);
    }

    @Override
    public void onRefresh() {
        swipeLayout.setRefreshing(true);
        if (mContext != null) {
            ((DashboardActivity) mContext).initSDCardVideoTask();
        }
    }

    public void setRefreshingFalse(List<PlayListInfoModel> newList) {
        swipeLayout.setRefreshing(false);
        playlist = new ArrayList<>(newList);
        playListAdapter.setList(playlist);
        PlayListData.getInstance().setPlayList(playlist);
        String videoCount = playlist.size() + " videos";
        videoCountTV.setText(videoCount);
        setSaveIconHighlight();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(ConstantSFValues.Playlist.PLAYLIST_KEY, playlist);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.cross_iv:
                if (!playlist.equals(PlayListData.getInstance().getPlayList())) {
                    displaySaveDialog();
                } else {
                    popUpFragment();
                }
                break;

            case R.id.save_playlist_iv:
                startSavePlaylistTask();
                break;

            default:
        }
    }

    private void displaySaveDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setMessage(getString(R.string.save_playlist))
                .setPositiveButton(android.R.string.yes, dialogClickListener)
                .setNegativeButton(android.R.string.no, dialogClickListener)
                .show();
    }

    private void startSavePlaylistTask() {
        new SavePlaylistTask(AppDatabase.getInstance(mContext).playlistDao(), this).execute(playlist);
    }

    // Callback received when items are saved to the DB
    @Override
    public void done(List<PlayListInfoModel> newList) {
        PlayListData.getInstance().setPlayList(new ArrayList<>(newList));
        PlayListData.getInstance().setCurrentClickedPosition(getCurrentPlayingPosition(newList));
        /*   PlayListData.getInstance().setPrevClickedPosition(getPreviousPosition());*///Need not
        ((DashboardActivity) mContext).toastMsg(mContext.getString(R.string.playlist_saved));
        savePlaylistIv.setImageDrawable(ContextCompat.getDrawable(mContext, android.R.drawable.ic_menu_save));
        popUpFragment();
        thumbnailCurrentIV.setImageBitmap(newList.get(0).getImageBitmap());
        thumbnailNextIV.setImageBitmap(newList.get(1).getImageBitmap());
    }

    private int getCurrentPlayingPosition(List<PlayListInfoModel> newList) {
        for (int i = 0; i < newList.size(); i++) {
            if(newList.get(i).getVideoPlayPauseState().equalsIgnoreCase(ConstantSFValues.PlayListControls.PLAY) ||newList.get(i).getVideoPlayPauseState().equalsIgnoreCase(ConstantSFValues.PlayListControls.PAUSE))
            {
                return i;
            }
        }
        return 0;
    }

    //Callback from Base Controller from Dashboard Activity
    public void notifyAccToController() {
        playListAdapter.notifyItemChanged(PlayListData.getInstance().getCurrentClickedPosition());
        playListAdapter.notifyItemChanged(PlayListData.getInstance().getPrevClickedPosition());
    }

    private void onItemDrag(int from, int to) {
        PlayListInfoModel fromModel = playlist.get(from);
        PlayListInfoModel toModel = playlist.get(to);
        if (from < to) {
            for (int index = from; index < to; index++) {
                Collections.swap(playlist, index, index + 1);
                fromModel.setId((long) (index + 1));
                toModel.setId((long) index);
            }
        } else {
            for (int index = from; index > to; index--) {
                Collections.swap(playlist, index, index - 1);
                fromModel.setId((long) (index - 1));
                toModel.setId((long) index);
            }
        }
        setSaveIconHighlight();
    }

    private void setSaveIconHighlight() {
        if (!playlist.equals(PlayListData.getInstance().getPlayList())) {
            savePlaylistIv.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.save_highlight));
        } else {
            savePlaylistIv.setImageDrawable(ContextCompat.getDrawable(mContext, android.R.drawable.ic_menu_save));
        }
    }

    private class DragHelperCallback extends ItemTouchHelper.Callback {

        DragHelperCallback() {
        }

        @Override
        public boolean isLongPressDragEnabled() {
            return true;
        }

        @Override
        public boolean isItemViewSwipeEnabled() {
            return false;
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder item, @NonNull RecyclerView.ViewHolder target) {
            onItemDrag(item.getAdapterPosition(), target.getAdapterPosition());
            return true;
        }

        @Override
        public void onMoved(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder item, int fromPos, @NonNull RecyclerView.ViewHolder target, int toPos, int x, int y) {
            super.onMoved(recyclerView, item, fromPos, target, toPos, x, y);
            playListAdapter.notifyItemMoved(fromPos, toPos);
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        }

        @Override
        public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            int dragFlag = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
            int swipeFlag = 0; // no swipe
            return makeMovementFlags(dragFlag, swipeFlag);
        }
    }
}