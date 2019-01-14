package vrsync.samsung.com.vrsync.view.fragments;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import java.util.ArrayList;

import vrsync.samsung.com.vrsync.Utils.ConnectedDeviceExt;
import vrsync.samsung.com.vrsync.listeners.IDeviceListListener;


public class BaseFragment extends Fragment implements IDeviceListListener {

    protected void popUpFragment() {
        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager != null) {
            fragmentManager.popBackStackImmediate();
        }
    }


    //Call Back of Device List
    @Override
    public void getDeviceList(ArrayList<ConnectedDeviceExt> list) {

    }
}