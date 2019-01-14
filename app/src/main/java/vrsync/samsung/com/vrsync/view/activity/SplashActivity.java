package vrsync.samsung.com.vrsync.view.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;

import vrsync.samsung.com.vrsync.R;

public class SplashActivity extends BaseActivity {

    private Context mContext;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        mContext = SplashActivity.this;
        goToNext();
    }

    private void goToNext() {
        int splashLength = 2500;
        new Handler().postDelayed(() -> {
            startActivity(new Intent(mContext, DashboardActivity.class));
            finish();
        }, splashLength);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        goToNext();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        exitFromApp();
    }
}