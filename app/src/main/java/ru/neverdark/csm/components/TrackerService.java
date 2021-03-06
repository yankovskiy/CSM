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
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ru.neverdark.csm.MainActivity;
import ru.neverdark.csm.R;
import ru.neverdark.csm.data.GPSData;
import ru.neverdark.csm.db.Db;
import ru.neverdark.csm.db.GpslogTable;
import ru.neverdark.csm.db.SummaryTable;
import ru.neverdark.csm.fragments.MainFragment;
import ru.neverdark.csm.utils.Constants;
import ru.neverdark.csm.utils.Settings;

public class TrackerService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
    public static final int SERVICE_NOTIFICATION_ID = 1;
    private static final String TAG = "TrackerService";
    private static final String TRACKER_PATH = Constants.PACKAGE_NAME + "." + TAG;
    public static final String TRACKER_SERVICE_GPSDATA = TRACKER_PATH + ".GPSDATA";
    public static final String TRACKER_SERVICE_STARTED = TRACKER_PATH + ".STARTED";
    public static final String TRACKER_SERVICE_REQUEST = TRACKER_PATH + ".SERVICE_REQUEST";
    public static final String TRACKER_SERVICE_TIMER_REQUEST = TRACKER_PATH + ".TIMER_REQUEST";
    public static final String TRACKER_SERVICE_ACTIVITY_TIME = TRACKER_PATH + ".ACTIVITY_TIME";
    public static final String TRACKER_SERVICE_TIMER_DATA = TRACKER_PATH + ".TIMER_DATA";
    public static final String TRACKER_CURRENT_TRAINING_ID = TRACKER_PATH + ".TEMP_TRAINING_ID";
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 2000;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;
    public static final String TRACKER_START_DATE = TRACKER_PATH + ".START_DATE";
    private final IBinder mBinder = new LocalBinder();
    private final List<LatLng> mLatLngLst = new ArrayList<>();
    private boolean mIsGUIRunning;
    private BroadcastReceiver mReceiver;
    private GPSData mData;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private GpslogTable mGpsLog;
    private Location mCurrentLocation;
    private Location mPreviousLocation;
    private long mTempRecordId;
    private Handler mHandler;
    private Runnable mChronometer;
    private long mStartTime;
    private boolean mIsAutopauseEnabled;
    private boolean mIsTrainingPaused;

    public TrackerService() {
    }

    private void initChronometer() {
        mHandler = new Handler();
        mStartTime = System.currentTimeMillis();
        mChronometer = new Runnable() {
            @Override
            public void run() {
                int totalTime;
                int activityTime;

                long currentTime = System.currentTimeMillis();

                totalTime = (int) (currentTime - mStartTime);
                activityTime = (int) (mData.plain_time + mData.descend_time + mData.ascend_time);

                Intent data = new Intent(TRACKER_SERVICE_TIMER_REQUEST);
                data.putExtra(TRACKER_SERVICE_TIMER_DATA, totalTime);
                data.putExtra(TRACKER_SERVICE_ACTIVITY_TIME, activityTime);
                LocalBroadcastManager.getInstance(TrackerService.this).sendBroadcast(data);

                if (mIsAutopauseEnabled && mIsTrainingPaused) {
                    // для корректного расчета средних скоростей меняем дату последней точки
                    mCurrentLocation.setTime(currentTime);
                }

                Log.v(TAG, "run: " + totalTime);
                mHandler.postDelayed(this, 1000);
            }
        };

        mHandler.postDelayed(mChronometer, 1000);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return true;    // for rebind
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mIsGUIRunning = true;
        /* Receiver поймает данные, когда приложение(GUI) остановлено или запущено */
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mIsGUIRunning = intent.getBooleanExtra(MainFragment.APP_RUNNING, false);
                Log.v(TAG, "onReceive: Activity started = " + mIsGUIRunning);
                if (!mIsGUIRunning && mData != null) {
                    mData.save(TrackerService.this);
                }
            }
        };

        mIsAutopauseEnabled = Settings.getInstance(this).isAutopauseEnabled();
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

        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, new IntentFilter(MainFragment.SERVICE_REQUEST));
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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (location == null) {
                mData.speed = 0;
                notifyUI();
            } else {
                saveAndNotify(location);
            }
        }

        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        mGoogleApiClient.disconnect();
        mHandler.removeCallbacks(mChronometer);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);

        Intent intent = new Intent(TRACKER_SERVICE_REQUEST);
        intent.putExtra(TRACKER_SERVICE_STARTED, false);
        intent.putExtra(TRACKER_CURRENT_TRAINING_ID, mTempRecordId);
        intent.putExtra(TRACKER_START_DATE, mStartTime);
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
                    mGpsLog.saveData(mCurrentLocation, 0, mTempRecordId);
                    mLatLngLst.add(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()));
                    prepareDataAndNotifyUI();
                }
            }

            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
        }
    }

    private void prepareDataAndNotifyUI() {
        prepareData();
        notifyUI();
    }

    /**
     * Уведомляет UI о новых данных или сохраняет их на диск
     * В случае, если пользовательский интерфейс доступен - отсылается уведомление о новых данных с координатами
     * В противном случае данные сохраняются на диск, чтобы при перезапуске приложение сразу отобразило последние данные не дожидаясь изменений в сервисе
     */
    private void notifyUI() {
    /* Если активити доступна пересылаем данные в него, иначе сохраняем в shared-prefs */
        if (mIsGUIRunning) {
            Log.v(TAG, "notifyUI: send data to activity");
            Intent intent = new Intent(TRACKER_SERVICE_REQUEST);
            intent.putExtra(TRACKER_SERVICE_GPSDATA, mData);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        } else {
            Log.v(TAG, "notifyUI: save data to shared-prefs");
            mData.save(TrackerService.this);
        }
    }

    private void prepareData() {
        mData.accuracy = mCurrentLocation.getAccuracy();
        mData.altitude = mCurrentLocation.getAltitude();
        mData.longitude = mCurrentLocation.getLongitude();
        mData.latitude = mCurrentLocation.getLatitude();
        mData.speed = mCurrentLocation.getSpeed();

        Log.v(TAG, "prepareData: longitude = " + mData.longitude);
        Log.v(TAG, "prepareData: latitude = " + mData.latitude);
        float seconds = 0;

        if (mPreviousLocation != null) {
            float distance = mPreviousLocation.distanceTo(mCurrentLocation);
            mData.distance += distance;

            if (mPreviousLocation.getAltitude() < mData.altitude) {
                mData.up_distance += distance;
                mData.up_altitude += mData.altitude - mPreviousLocation.getAltitude();
                mData.ascend_time += (mCurrentLocation.getTime() - mPreviousLocation.getTime());
                seconds = mData.ascend_time / 1000;
                mData.ascend_average_speed = mData.up_distance / seconds;

                if (mData.speed > mData.ascend_max_speed) {
                    mData.ascend_max_speed = mData.speed;
                }
            } else if (mPreviousLocation.getAltitude() > mData.altitude) {
                mData.down_distance += distance;
                mData.down_altitude += mPreviousLocation.getAltitude() - mData.altitude;
                mData.descend_time += (mCurrentLocation.getTime() - mPreviousLocation.getTime());
                seconds = mData.descend_time / 1000;
                mData.descend_average_speed = mData.down_distance / seconds;

                if (mData.speed > mData.descend_max_speed) {
                    mData.descend_max_speed = mData.speed;
                }
            } else {
                float plainDistance = mData.distance - mData.down_distance - mData.up_distance;
                mData.plain_time += (mCurrentLocation.getTime() - mPreviousLocation.getTime());
                seconds = mData.plain_time / 1000;
                mData.plain_average_speed = plainDistance / seconds;

                if (mData.speed > mData.plain_max_speed) {
                    mData.plain_max_speed = mData.speed;
                }
            }

            Log.v(TAG, "prepareData: " + mData.up_altitude);
            Log.v(TAG, "prepareData: " + mData.down_altitude);
        }

        if (mData.speed > mData.max_speed) {
            mData.max_speed = mData.speed;
        }

        seconds = (mData.ascend_time + mData.descend_time + mData.plain_time) / 1000;
        if (seconds == 0) {
            seconds = 1;
        }

        mData.average_speed = mData.distance / seconds;

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
        saveAndNotify(location);

    }

    /**
     * Сохраняет данные о местоположении и уведомляет UI только если местоположение изменилось и
     * при этом зафиксирована ненулевая скорость
     *
     * @param location объект содержащий информацию об определенном местоположении
     */
    private void saveAndNotify(Location location) {
        // Логируем и обновляем интерфейс только если есть скорость
        if (location.getSpeed() != 0.0 || Constants.SAVE_POINTS_WITHOUT_SPEED) {
            if (mCurrentLocation != null) {
                mPreviousLocation = mCurrentLocation;
            }

            mCurrentLocation = location;

            /* Сохраняем в базу и обновляем интерфейс только если местоположение меняется */
            if (mPreviousLocation != null &&
                    mPreviousLocation.getLatitude() != mCurrentLocation.getLatitude() &&
                    mPreviousLocation.getLongitude() != mCurrentLocation.getLongitude()) {

                if (mIsAutopauseEnabled) {
                    mIsTrainingPaused = false;
                }

                double distance = mPreviousLocation.distanceTo(mCurrentLocation);
                mGpsLog.saveData(mCurrentLocation, distance, mTempRecordId);
                mLatLngLst.add(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()));
                prepareDataAndNotifyUI();
            } else if (mIsAutopauseEnabled) {
                mIsTrainingPaused = true;
            }
        } else {
            // отсылка нулевой скорости без сохранения данных в базу
            if (mIsAutopauseEnabled) {
                mIsTrainingPaused = true;
            }
            mData.speed = 0;
            notifyUI();
        }
    }

    public List<LatLng> getLatLngList() {
        return mLatLngLst;
    }

    public class LocalBinder extends Binder {
        public TrackerService getService() {
            // Return this instance of LocalService so clients can call public methods
            return TrackerService.this;
        }
    }

}
