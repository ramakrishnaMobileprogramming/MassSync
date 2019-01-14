package vrsync.samsung.com.vrsync.view.activity;


import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.Objects;

import vrsync.samsung.com.vrsync.R;
import vrsync.samsung.com.vrsync.Utils.ConnectedDeviceExt;
import vrsync.samsung.com.vrsync.Utils.ConstantSValues;
import vrsync.samsung.com.vrsync.listeners.IDeviceListListener;
import vrsync.samsung.com.vrsync.receivers.NetworkChangeReceiver;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LOCKED;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
import static android.widget.Toast.LENGTH_SHORT;
import static android.widget.Toast.makeText;

public class BaseActivity extends AppCompatActivity implements IDeviceListListener {

    public EventBus bus = EventBus.getDefault();
    private NetworkChangeReceiver myReceiver = new NetworkChangeReceiver();
    private FragmentTransaction fragmentTransaction;
    private FragmentManager fragmentManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Objects.requireNonNull(getSupportActionBar()).hide();
        fragmentManager = getSupportFragmentManager();
        setScreenOrientation();
        registerNetworkReceiver();
    }

    private void registerNetworkReceiver() {
        //register broadcast receiver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            registerReceiver(myReceiver,
                    new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(myReceiver);
    }

    protected void exitFromApp() {
        Intent localIntent = new Intent("android.intent.action.MAIN");
        localIntent.addCategory("android.intent.category.HOME");
        localIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(localIntent);
        finish();
    }

    private void setScreenOrientation() {
        if (getResources().getString(R.string.screen_type).equals("mobile")) {
            setRequestedOrientation(SCREEN_ORIENTATION_PORTRAIT);
            setRequestedOrientation(SCREEN_ORIENTATION_LOCKED);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe
    public void onEvent(String name) {
        if (name.equalsIgnoreCase("Disconnected")) {
            toastMsg("Device is offline!");
        }
    }


    public void onControllerEvent(String name) {

    }

    public void toastMsg(String message) {
        makeText(getApplicationContext(), message, LENGTH_SHORT).show();
    }

    protected void addFragment(int viewId, Fragment fragment, boolean addToBackStack, String tag) {
        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(viewId, fragment, tag);
        if (addToBackStack) {
            fragmentTransaction.addToBackStack(tag);
        }
        fragmentTransaction.commit();
    }

    protected void replaceFragment(int viewId, Fragment fragment, boolean addToBackStack, String tag) {
        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(viewId, fragment, tag);
        if (addToBackStack) {
            fragmentTransaction.addToBackStack(tag);
        }
        fragmentTransaction.commit();
    }

    protected void removeFragment(Fragment fragment) {
        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.remove(fragment);
        fragmentTransaction.commit();
    }

    @Override
    public void getDeviceList(ArrayList<ConnectedDeviceExt> list) {
        ConstantSValues.isDevicesAvailable = !list.isEmpty();
    }
}