package ru.neverdark.csm;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

import ru.neverdark.csm.components.TrackerService;
import ru.neverdark.csm.fragments.MainFragment;
import ru.neverdark.csm.fragments.TrainingStatsFragment;
import ru.neverdark.csm.utils.Utils;
import ru.neverdark.widgets.Antenna;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, MainFragment.OnFragmentInteractionListener {

    public static final String TAG = "MainActivity";

    private MainFragment mMainFragment;
    private ActionBarDrawerToggle mToggle;
    private DrawerLayout mDrawer;
    private Antenna mAntenna;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        mToggle = new ActionBarDrawerToggle(
                this, mDrawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawer.setDrawerListener(mToggle);
        mToggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        /* Intent будет содержать значение TRACKER_SERVICE_STARTED (true) если мы пришли из уведомления */
        Intent intent = getIntent();
        boolean mIsServiceRunning = intent.getBooleanExtra(TrackerService.TRACKER_SERVICE_STARTED, false);

        /* Если мы пришли из уведомления, то сервис однозначно запущен и дополнительная проверка не нужна */
        if (!mIsServiceRunning) {
            mIsServiceRunning = Utils.isServiceRunning(this, TrackerService.class);
            Log.v(TAG, "onCreate: service started " + mIsServiceRunning);
        }

        mMainFragment = MainFragment.newInstance(mIsServiceRunning);
        getSupportFragmentManager().beginTransaction().add(R.id.main_content_fragment, mMainFragment).commit();

        mAntenna = (Antenna) findViewById(R.id.quality);
        setDrawerDisabledState(mIsServiceRunning);
    }

    @Override
    public void setTitle(CharSequence title) {
        TextView tv = (TextView) findViewById(R.id.title);
        tv.setText(title);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MainFragment.REQUEST_CHECK_SETTINGS) {
            if (resultCode == RESULT_OK) {
                Log.v(TAG, "onActivityResult: result_ok");
                mMainFragment.startGeoClient();
            } else if (resultCode == RESULT_CANCELED) {
                Log.v(TAG, "onActivityResult: result_canceled");
                // TODO отобразить информационное сообщение о необходимости включить GPS
            }
        } else if (requestCode == MainFragment.REQUETST_CHECK_SETTINGS_FOR_SERVICE) {
            if (resultCode == RESULT_OK) {
                Log.v(TAG, "onActivityResult: for servie ok");
                mMainFragment.startGeoClient();
                mMainFragment.checkAccuracyAndRunTracker();
            } else if (resultCode == RESULT_CANCELED) {
                Log.v(TAG, "onActivityResult: for service canceled");
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onBackPressed() {

        if (mDrawer.isDrawerOpen(GravityCompat.START)) {
            mDrawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (!item.isChecked()) {
            Log.v(TAG, "onNavigationItemSelected: ");
            if (id == R.id.nav_training) {
                mAntenna.setVisibility(View.VISIBLE);
                getSupportFragmentManager().beginTransaction().replace(R.id.main_content_fragment, mMainFragment).commit();
            } else if (id == R.id.nav_stats) {
                if (mAntenna.getVisibility() != View.GONE) {
                    mAntenna.setVisibility(View.GONE);
                }
                TrainingStatsFragment fragment = TrainingStatsFragment.newInstance();
                getSupportFragmentManager().beginTransaction().replace(R.id.main_content_fragment, fragment).commit();
            } else if (id == R.id.nav_gallery) {

            } else if (id == R.id.nav_slideshow) {

            } else if (id == R.id.nav_manage) {

            } else if (id == R.id.nav_share) {

            } else if (id == R.id.nav_send) {

            }
        }
        mDrawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void setDrawerDisabledState(boolean isDisabled) {
        if (isDisabled) {
            setTitle("00:00:00");
            mToggle.setDrawerIndicatorEnabled(false);
            mDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        } else {
            setTitle(R.string.app_name);
            mToggle.setDrawerIndicatorEnabled(true);
            mDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        }
    }

    @Override
    public void stopTrackerService() {
        stopService(new Intent(this, TrackerService.class));
        mBound = false;
        setDrawerDisabledState(false);
    }

    @Override
    public void startTrackerService() {
        startService(new Intent(this, TrackerService.class));
        setDrawerDisabledState(true);
    }

    @Override
    public void updateSignal(int signal) {
        mAntenna.setSignal(signal);
    }

    @Override
    public List<LatLng> getLatLngList() {
        if (mBound) {
            return mService.getLatLngList();
        } else {
            return null;
        }
    }

    private boolean mBound;
    private TrackerService mService;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            Log.v(TAG, "onServiceConnected: ");
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            TrackerService.LocalBinder binder = (TrackerService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;

            mMainFragment.onTrackerServiceConnected();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.v(TAG, "onServiceDisconnected: ");
            mBound = false;
        }
    };

    @Override
    public void bindTrackerService() {
        if (!mBound) {
            Intent intent = new Intent(this, TrackerService.class);
            boolean bind = bindService(intent, mConnection, 0);
            Log.v(TAG, "bindTrackerService: " + bind);
        } else {
            Log.v(TAG, "bindTrackerService: already bind");
        }
    }

    @Override
    public void unbindTrackerService() {
        Log.v(TAG, "unbindTrackerService: ");
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }
}
