package vrsync.samsung.com.vrsync.view.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatTextView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import vrsync.samsung.com.vrsync.R;

public class NoDeviceConnectedFragment extends BaseFragment implements View.OnClickListener {

    private AppCompatTextView learn2ConnectTV;
    private Context mContext;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_no_device_connected, container, false);
        initializeView(view);
        return view;
    }

    private void initializeView(View view) {
        learn2ConnectTV = view.findViewById(R.id.learn2connect_tv);
        mContext = getActivity();
        setListeners();
    }

    private void setListeners() {
        learn2ConnectTV.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.learn2connect_tv:
                Toast.makeText(mContext, "Working on It..", Toast.LENGTH_SHORT).show();
                break;
            default:
        }
    }
}