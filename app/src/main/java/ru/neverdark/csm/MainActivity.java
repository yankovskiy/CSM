package ru.neverdark.csm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import ru.neverdark.csm.activity.TrainingFinishAcitivty;
import ru.neverdark.csm.components.TrackerService;
import ru.neverdark.csm.data.GPSData;
import ru.neverdark.csm.db.Db;
import ru.neverdark.csm.db.SummaryTable;
import ru.neverdark.csm.fragments.MainFragment;
import ru.neverdark.csm.fragments.TrainingStatsFragment;
import ru.neverdark.csm.utils.Constants;
import ru.neverdark.csm.utils.Utils;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, MainFragment.OnFragmentInteractionListener {

    public static final String TAG = "MainActivity";
    private static final String ACTIVITY_PATH = Constants.PACKAGE_NAME + "." + TAG;
    public static final String ACTIVITY_REQUEST = ACTIVITY_PATH + ".REQUEST";
    public static final String ACTIVITY_STARTED = ACTIVITY_PATH + ".STARTED";
    public static final String TRAINING_DATA = ACTIVITY_PATH + ".DATA";
    public static final String TRAININD_ID = ACTIVITY_PATH + ".TRAINING_ID";
    public static final String TRAINING_FINISH_DATE = ACTIVITY_PATH + ".FINISH_DATE";
    private static final int TRAINING_RESULT_REQUEST = 1;
    private boolean mIsServiceRunning;
    private Intent mServiceIntent;
    private BroadcastReceiver mReceiver;
    private GPSData mData;
    private MainFragment mMainFragment;
    private ActionBarDrawerToggle mToggle;
    private DrawerLayout mDrawer;

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

        mData = new GPSData();

        mServiceIntent = new Intent(this, TrackerService.class);
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.v(TAG, "onReceive: ");
                GPSData data = (GPSData) intent.getSerializableExtra(TrackerService.TRACKER_SERVICE_MESSAGE);
                if (data != null) {
                    updateUI(data);
                }

                /* если сервис был остановлен */
                if (!intent.getBooleanExtra(TrackerService.TRACKER_SERVICE_STARTED, true)) {
                    long recordId = intent.getLongExtra(TrackerService.TRACKER_CURRENT_TRAINING_ID, 0);
                    if (recordId == 0) {
                        Log.v(TAG, "onReceive: incorrect database record id");
                    } else {
                        Log.v(TAG, "onReceive: create new activity with result");
                        startTrainingFinishActivity(recordId);
                    }
                }
            }
        };

        /* Intent будет содержать значение TRACKER_SERVICE_STARTED (true) если мы пришли из уведомления */
        Intent intent = getIntent();
        mIsServiceRunning = intent.getBooleanExtra(TrackerService.TRACKER_SERVICE_STARTED, false);

        /* Если мы пришли из уведомления, то сервис однозначно запущен и дополнительная проверка не нужна */
        if (!mIsServiceRunning) {
            mIsServiceRunning = Utils.isServiceRunning(this, TrackerService.class);
            Log.v(TAG, "onCreate: service started " + mIsServiceRunning);
        }

        mMainFragment = MainFragment.newInstance(mIsServiceRunning);
        getSupportFragmentManager().beginTransaction().add(R.id.main_content_fragment, mMainFragment).commit();

        setButtonsEnabledState();
    }

    private void startTrainingFinishActivity(long id) {
        Intent intent = new Intent(this, TrainingFinishAcitivty.class);
        intent.putExtra(MainActivity.TRAINING_DATA, mData);
        intent.putExtra(MainActivity.TRAININD_ID, id);
        intent.putExtra(MainActivity.TRAINING_FINISH_DATE, System.currentTimeMillis());
        startActivityForResult(intent, TRAINING_RESULT_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == TRAINING_RESULT_REQUEST) {
            if (resultCode == RESULT_CANCELED) {
                long recordId = data.getLongExtra(MainActivity.TRAININD_ID, 0);
                Log.v(TAG, "onActivityResult: canceled");
                Log.v(TAG, "onActivityResult: recordId = " + recordId);
                removeTraining(recordId);
            } else if (resultCode == RESULT_OK) {
                Log.v(TAG, "onActivityResult: ok");
            }
        } else if (requestCode == MainFragment.REQUEST_CHECK_SETTINGS) {
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
                mMainFragment.handleStartStopButton();
            } else if (resultCode == RESULT_CANCELED) {
                Log.v(TAG, "onActivityResult: for service canceled");
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void removeTraining(long recordId) {
        String fileName = Utils.getSnapshotNameById(recordId);
        File file = new File(getFilesDir(), fileName);
        if (file.exists()) {
            file.delete();
        }
        Db.getInstance(this).getSummaryTable().deleteRecord(recordId);
    }

    private void updateUI(GPSData data) {
        mMainFragment.updateUI(data);
        mData.copyFrom(data);
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
                getSupportFragmentManager().beginTransaction().replace(R.id.main_content_fragment, mMainFragment).commit();
            } else if (id == R.id.nav_stats) {
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

    private void setButtonsEnabledState() {
        if (mIsServiceRunning) {
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
        stopService(mServiceIntent);
        mIsServiceRunning = false;
        setButtonsEnabledState();
    }

    @Override
    public void startTrackerService() {
        startService(mServiceIntent);
        mIsServiceRunning = true;
        setButtonsEnabledState();
    }

    @Override
    protected void onStart() {
        super.onStart();

        /* Если произошел запуск / перезапуск активити при запущеннном сервисе */
        if (mIsServiceRunning) {
            GPSData data = new GPSData();
            data.load(this);
            updateUI(data);

            Intent intent = new Intent(ACTIVITY_REQUEST);
            intent.putExtra(ACTIVITY_STARTED, true);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }

        LocalBroadcastManager.getInstance(this).registerReceiver((mReceiver),
                new IntentFilter(TrackerService.TRACKER_SERVICE_REQUEST)
        );

    }

    @Override
    protected void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        Intent intent = new Intent(ACTIVITY_REQUEST);
        intent.putExtra(ACTIVITY_STARTED, false);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        super.onStop();
    }
}
