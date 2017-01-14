package ru.neverdark.csm.components;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.Locale;

import ru.neverdark.csm.MainActivity;
import ru.neverdark.csm.R;
import ru.neverdark.csm.data.GPSData;
import ru.neverdark.csm.db.Db;
import ru.neverdark.csm.db.GpslogTable;
import ru.neverdark.csm.db.SummaryTable;
import ru.neverdark.csm.utils.Constants;

public class TrackerService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
    public static final int SERVICE_NOTIFICATION_ID = 1;
    private static final String TAG = "TrackerService";
    private static final String TRACKER_PATH = Constants.PACKAGE_NAME + "." + TAG;
    public static final String TRACKER_SERVICE_MESSAGE = TRACKER_PATH + ".MESSAGE";
    public static final String TRACKER_SERVICE_STARTED = TRACKER_PATH + ".STARTED";
    public static final String TRACKER_SERVICE_REQUEST = TRACKER_PATH + ".REQUEST";
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 5000;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;
    public static final String TRACKER_SERVICE_TIMER_REQUEST = TRACKER_PATH + ".TIMER_REQUEST";
    public static final String TRACKER_SERVICE_TIMER_DATA = TRACKER_PATH + ".TIMER_DATA";
    private boolean mIsActivityStarted;
    private BroadcastReceiver mReceiver;
    private GPSData mData;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private GpslogTable mGpsLog;
    private Location mCurrentLocation;
    private Location mPreviousLocation;
    private long mTempRecordId;
    private long mSumSpeed;
    private int mSegmentCount;
    private Handler mHandler;
    //private long mStartTime;
    public static final String TRACKER_CURRENT_TRAINING_ID = TRACKER_PATH + ".TEMP_TRAINING_ID";
    private Runnable mChronometer;
    private int mTotalTime;

    public TrackerService() {
    }

    private void initChronometer() {
        mHandler = new Handler();
        final long mStartTime = System.currentTimeMillis();
        mChronometer = new Runnable() {
            @Override
            public void run() {
                mTotalTime = (int) (System.currentTimeMillis() - mStartTime) / 1000;
                Intent data = new Intent(TRACKER_SERVICE_TIMER_REQUEST);
                data.putExtra(TRACKER_SERVICE_TIMER_DATA, mTotalTime);
                LocalBroadcastManager.getInstance(TrackerService.this).sendBroadcast(data);

                Log.v(TAG, "run: " + mTotalTime);
                mHandler.postDelayed(this, 1000);
            }
        };

        mHandler.postDelayed(mChronometer, 1000);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mIsActivityStarted = true;
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mIsActivityStarted = intent.getBooleanExtra(MainActivity.ACTIVITY_STARTED, false);
                Log.v(TAG, "onReceive: Activity started = " + mIsActivityStarted);
                if (!mIsActivityStarted && mData != null) {
                    mData.save(TrackerService.this);
                }
            }
        };
    }

    private synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        createLocationRequest();
    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "onStartCommand: ");
        buildGoogleApiClient();

        mGpsLog = Db.getInstance(this).getGpslogTable();
        SummaryTable summaryTable = Db.getInstance(this).getSummaryTable();
        mTempRecordId = summaryTable.createTempRecord();

        mGoogleApiClient.connect();
        mData = new GPSData();

        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, new IntentFilter(MainActivity.ACTIVITY_REQUEST));
        createNotification();

        /* перезапуск в случае смерти */
        return START_STICKY;
    }

    private void createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.putExtra(TRACKER_SERVICE_STARTED, true);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle(getText(R.string.app_name))
                .setContentText(getText(R.string.notification_message))
                .setSmallIcon(R.drawable.ic_notification_training)
                .setContentIntent(pendingIntent)
                .setTicker(getText(R.string.ticker_text))
                .build();

        startForeground(SERVICE_NOTIFICATION_ID, notification);
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy: ");
        // TODO: добавить отвязку от gps
        // https://developer.android.com/training/location/receive-location-updates.html
        mGoogleApiClient.disconnect();
        mHandler.removeCallbacks(mChronometer);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);

        Intent intent = new Intent(TRACKER_SERVICE_REQUEST);
        intent.putExtra(TRACKER_SERVICE_STARTED, false);
        intent.putExtra(TRACKER_CURRENT_TRAINING_ID, mTempRecordId);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        super.onDestroy();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        startLocationUpdates();
    }

    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            initChronometer();

            if (mCurrentLocation == null) {
                mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                if (mCurrentLocation != null) {
                    Log.v(TAG, String.format(Locale.US, "startLocationUpdates: %f,%f", mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()));
                    updateUI();
                    mGpsLog.saveData(mCurrentLocation, mTempRecordId);
                }
            }

            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
        }
    }

    private void updateUI() {
        prepareData();

        /* Если активити доступна пересылаем данные в него, иначе сохраняем в shared-prefs */
        if (mIsActivityStarted) {
            Log.v(TAG, "updateUI: send data to activity");
            Intent intent = new Intent(TRACKER_SERVICE_REQUEST);
            intent.putExtra(TRACKER_SERVICE_MESSAGE, mData);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        } else {
            Log.v(TAG, "updateUI: save data to shared-prefs");
            mData.save(TrackerService.this);
        }
    }

    private void prepareData() {
        mData.accuracy = mCurrentLocation.getAccuracy();
        mData.altitude = (int) mCurrentLocation.getAltitude();
        mData.longitude = (float) mCurrentLocation.getLongitude();
        mData.latitude = (float) mCurrentLocation.getLatitude();
        mData.speed = (int) mCurrentLocation.getSpeed();

        Log.v(TAG, "prepareData: longitude = " + mData.longitude);
        Log.v(TAG, "prepareData: latitude = " + mData.latitude);
        if (mPreviousLocation != null) {
            double distance = mPreviousLocation.distanceTo(mCurrentLocation);
            mData.distance += distance;

            if (mPreviousLocation.getAltitude() < mData.altitude) {
                mData.up_distance += distance;
                mData.up_altitude += mData.altitude - mPreviousLocation.getAltitude();
            } else if (mPreviousLocation.getAltitude() > mData.altitude) {
                mData.down_distance += distance;
                mData.down_altitude += mPreviousLocation.getAltitude() - mData.altitude;
            }

            Log.v(TAG, "prepareData: " + mData.up_altitude);
            Log.v(TAG, "prepareData: " + mData.down_altitude);
        }

        if (mData.speed > mData.max_speed) {
            mData.max_speed = mData.speed;
        }

        mSumSpeed += mData.speed;
        mSegmentCount++;

        mData.average_speed = mSumSpeed / mSegmentCount;

        if (mData.altitude > mData.max_altitude) {
            mData.max_altitude = mData.altitude;
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.v(TAG, "onConnectionSuspended: ");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.v(TAG, "onConnectionFailed: " + connectionResult.getErrorMessage());
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.v(TAG, "onLocationChanged: ");
        if (mCurrentLocation != null) {
            mPreviousLocation = mCurrentLocation;
        }

        mCurrentLocation = location;

        /* Сохраняем в базу и обновляем интерфейс только если местоположение меняется */
        if (mPreviousLocation != null &&
                mPreviousLocation.getLatitude() != mCurrentLocation.getLatitude() &&
                mPreviousLocation.getLongitude() != mCurrentLocation.getLongitude()) {

            mGpsLog.saveData(mCurrentLocation, mTempRecordId);
            updateUI();
        }
    }
}