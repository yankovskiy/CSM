package ru.neverdark.csm.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatDialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.File;
import java.util.List;
import java.util.Locale;

import ru.neverdark.csm.R;
import ru.neverdark.csm.activity.TrainingFinishAcitivty;
import ru.neverdark.csm.components.Compass;
import ru.neverdark.csm.components.GeoClient;
import ru.neverdark.csm.components.TrackerService;
import ru.neverdark.csm.data.GPSData;
import ru.neverdark.csm.db.Db;
import ru.neverdark.csm.utils.Constants;
import ru.neverdark.csm.utils.Settings;
import ru.neverdark.csm.utils.Utils;
import ru.neverdark.widgets.Antenna;

public class MainFragment extends Fragment implements OnMapReadyCallback, GeoClient.OnGeoClientListener, ConfirmDialog.NoticeDialogListener {
    public static final int COMPASS = 0;
    public static final int MAP = 1;
    public static final int REQUEST_CHECK_SETTINGS = 3;
    public static final int TRAINING_RESULT_REQUEST = 1;
    public static final int REQUETST_CHECK_SETTINGS_FOR_SERVICE = 4;
    private static final String TAG = "MainFragment";
    private static final String FRAGMENT_PATH = Constants.PACKAGE_NAME + "." + TAG;
    public static final String APP_RUNNING = FRAGMENT_PATH + ".APP_RUNNING";
    public static final String SERVICE_REQUEST = FRAGMENT_PATH + ".SERVICE_REQUEST";
    public static final String TRAINING_FINISH_DATE = FRAGMENT_PATH + ".FINISH_DATE";
    public static final String TRAININD_ID = FRAGMENT_PATH + ".TRAINING_ID";
    public static final String TRAINING_DATA = FRAGMENT_PATH + ".DATA";
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_ACCESS_FINE_LOCATION_FROM_MAP = 2;
    private TextView mLatitudeTv;
    private TextView mLongitudeTv;
    private TextView mAltitudeTv;
    private TextView mSpeedTv;
    private TextView mDistanceTv;
    private TextView mAverageSpeedTv;
    private Compass mCompass;
    private OnFragmentInteractionListener mCallback;
    private MapView mMap;
    private View mCompassGroup;
    private View mFirstRow;
    private View mSecondRow;
    private MenuItem mCompassMenuItem;
    private MenuItem mMapMenuItem;
    private GoogleMap mGoogleMap;
    private FloatingActionButton mStartStopTrainingButton;
    private boolean mIsServiceRunning;
    private GeoClient mGeoClient;
    private BroadcastReceiver mTimerReseiver;
    private int mSignal;
    private BroadcastReceiver mLocationReceiver;
    private GPSData mData;
    private boolean mSavedState;
    private boolean mDelayShowDialog;
    private Polyline mPolyline;
    private boolean mDelayedUpdateMap;

    private int mCompleteJob;
    private static final int COMPLETE_MAP_LOAD = 1;
    private static final int COMPLETE_SERVICE_BIND = 2;
    private static final int COMPLETE_ALL = COMPLETE_MAP_LOAD | COMPLETE_SERVICE_BIND;

    private synchronized void completeJob(int job) {
        if (mCompleteJob != COMPLETE_ALL) {
            mCompleteJob |= job;

            if (mCompleteJob == COMPLETE_ALL) {
                loadTrackPointsFromService();
                mDelayedUpdateMap = false;
            }
        }
    }

    public MainFragment() {
        // Required empty public constructor
    }

    public static MainFragment newInstance(boolean isServiceRunning) {
        MainFragment fragment = new MainFragment();
        fragment.mIsServiceRunning = isServiceRunning;
        return fragment;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        mSavedState = true;
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MainFragment.TRAINING_RESULT_REQUEST) {
            if (resultCode == Activity.RESULT_CANCELED) {
                long recordId = data.getLongExtra(MainFragment.TRAININD_ID, 0);
                Log.v(TAG, "onActivityResult: canceled");
                Log.v(TAG, "onActivityResult: recordId = " + recordId);
                removeTraining(recordId);
            } else if (resultCode == Activity.RESULT_OK) {
                Log.v(TAG, "onActivityResult: ok");
            }

            mPolyline.remove();
            mPolyline = null;

        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void removeTraining(long recordId) {
        String fileName = Utils.getSnapshotNameById(recordId);
        File file = new File(getContext().getFilesDir(), fileName);
        if (file.exists()) {
            file.delete();
        }
        Db.getInstance(getContext()).getSummaryTable().deleteRecord(recordId);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate: ");
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate: " + mIsServiceRunning);
        mCompass = new Compass(getContext());
        mGeoClient = new GeoClient(getContext(), this);
        mData = new GPSData();

        // если сервис запущен и перезапущен UI нам нужно обновить масштаб на карте
        mDelayedUpdateMap = mIsServiceRunning;

        setHasOptionsMenu(true);
        createReseivers();
    }

    private void createReseivers() {
        mTimerReseiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                int time = intent.getIntExtra(TrackerService.TRACKER_SERVICE_TIMER_DATA, 0);
                int seconds = time % 60;
                int minutes = (time / 60) % 60;
                int hours = time / (60 * 60);
                String timeStr = String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds);
                getActivity().setTitle(timeStr);
            }
        };

        mLocationReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.v(TAG, "onReceive: ");
                GPSData data = (GPSData) intent.getSerializableExtra(TrackerService.TRACKER_SERVICE_GPSDATA);
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
    }

    private void startTrainingFinishActivity(long id) {
        Intent intent = new Intent(getContext(), TrainingFinishAcitivty.class);
        intent.putExtra(MainFragment.TRAINING_DATA, mData);
        intent.putExtra(MainFragment.TRAININD_ID, id);
        intent.putExtra(MainFragment.TRAINING_FINISH_DATE, System.currentTimeMillis());
        startActivityForResult(intent, MainFragment.TRAINING_RESULT_REQUEST);
    }

    private void setLayoutVisible(View layoutView, boolean visible) {
        layoutView.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                break;
            case R.id.action_show_compass:
                showMapCompass(MainFragment.COMPASS);
                break;
            case R.id.action_show_map:
                showMapCompass(MainFragment.MAP);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Log.v(TAG, "onCreateOptionsMenu: ");
        inflater.inflate(R.menu.main, menu);
        mCompassMenuItem = menu.findItem(R.id.action_show_compass);
        mMapMenuItem = menu.findItem(R.id.action_show_map);
        showMapCompass(MAP);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.v(TAG, "onCreateView: ");
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        mLatitudeTv = (TextView) view.findViewById(R.id.latitude);
        mLongitudeTv = (TextView) view.findViewById(R.id.longitude);
        mAltitudeTv = (TextView) view.findViewById(R.id.altitude);
        mSpeedTv = (TextView) view.findViewById(R.id.speed);
        mDistanceTv = (TextView) view.findViewById(R.id.distance);
        mAverageSpeedTv = (TextView) view.findViewById(R.id.average_speed);
        mMap = (MapView) view.findViewById(R.id.mapView);
        mCompass.setArrowView(view.findViewById(R.id.compass_internal));
        mCompassGroup = view.findViewById(R.id.compass_group);
        mStartStopTrainingButton = (FloatingActionButton) view.findViewById(R.id.start_stop_training_button);
        mFirstRow = view.findViewById(R.id.first_row);
        mSecondRow = view.findViewById(R.id.second_row);

        mStartStopTrainingButton.setOnClickListener(new ButtonsClickListener());
        mMap.onCreate(null);

        mMap.getMapAsync(this);
        return view;
    }

    /**
     * Изменяет иконку кнопки запуска / остановки сервиса
     *
     * @param stopImage true если иконку нужно изменить на значок stop
     *                  false если нужно изменить иконку на значок start
     */
    private void updateImageOnButton(boolean stopImage) {
        Log.v(TAG, "updateImageOnButton: " + stopImage);
        if (stopImage) {
            mStartStopTrainingButton.setImageResource(R.drawable.fab_stop_training);
        } else {
            mStartStopTrainingButton.setImageResource(R.drawable.fab_start_training);
        }
    }

    /**
     * Останавливает и запускает TrackerService
     * В случае успешного остановки или запуска меняет иконку на соответсвующей кнопке
     *
     * @param start true если сервис нужно запустить
     *              false если сервис нужно остановить
     */
    public void startStopService(boolean start) {
        Log.v(TAG, "startStopService: " + start);
        if (start) {
            startTrackerService();
        } else {
            stopTrackerService();
        }
        updateImageOnButton(mIsServiceRunning);
    }

    @Override
    public void onAttach(Context context) {
        Log.v(TAG, "onAttach: ");
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mCallback = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        Log.v(TAG, "onDetach: ");
        super.onDetach();
        mCallback = null;
    }

    public void updateUI(GPSData data) {
        Log.v(TAG, "updateUI: ");
        String latitude = String.format(Locale.US, "%f", data.latitude);
        String longitude = String.format(Locale.US, "%f", data.longitude);
        String altitude = String.format(Locale.US, "%d", Math.round(data.altitude));
        String speed = String.format(Locale.US, "%.2f", data.speed * 3.6);
        String distance = String.format(Locale.US, "%d km %d m", Math.round(data.distance) / 1000, Math.round(data.distance) % 1000);
        String average_speed = String.format(Locale.US, "%.2f", data.average_speed * 3.6);

        Log.v(TAG, "updateUI: latitude = " + latitude);
        Log.v(TAG, "updateUI: longitude = " + longitude);

        mLatitudeTv.setText(latitude);
        mLongitudeTv.setText(longitude);
        mAltitudeTv.setText(altitude);
        mSpeedTv.setText(speed);
        mDistanceTv.setText(distance);
        mAverageSpeedTv.setText(average_speed);

        updateSignalWidget(data.accuracy);
        LatLng latLng = new LatLng(data.latitude, data.longitude);

        // обновляем камеру только если не отложенное обновление карты
        if (!mDelayedUpdateMap) {
            loadTrackPointsFromService();
            updateCamera(latLng);
        }

        mData.copyFrom(data);
    }

    /**
     * Проверяет наличие прав и в случае необходимости запрашивает их
     *
     * @param permission  права доступа для проверки и запроса разрешения
     * @param requestCode код запроса прав доступа для обработки onRequestPermissionsResult
     * @return true если права доступа есть
     */
    private boolean checkAndRequirePermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(getContext(), permission) == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            requestPermissions(new String[]{permission}, requestCode);
            return false;
        }
    }

    private void loadTrackPointsFromService() {
        Log.v(TAG, "loadTrackPointsFromService: ");
        List<LatLng> list = mCallback.getLatLngList();
        if (list != null) {
            Log.v(TAG, "loadTrackPointsFromService: not null");
            if (mPolyline != null) {
                mPolyline.setPoints(list);
            } else {
                PolylineOptions options = new PolylineOptions();
                options.color(Color.RED).geodesic(true).width(4).zIndex(10);
                options.addAll(list);
                mPolyline = mGoogleMap.addPolyline(options);
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.v(TAG, "onMapReady: ");
        mGoogleMap = googleMap;
        mGoogleMap.setMapType(Settings.getInstance(getContext()).loadMapType());
        if (mDelayedUpdateMap) {
            LatLng latLng = new LatLng(mData.latitude, mData.longitude);
            float zoom = Settings.getInstance(getContext()).loadMapZoom();
            Log.v(TAG, "onMapReady: loaded zoom = " + zoom);
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));

            completeJob(COMPLETE_MAP_LOAD);
        }

        enableMyLocationButton();
    }

    /**
     * Срабатывает, когда трекер забинден
     */
    public void onTrackerServiceConnected() {
        Log.v(TAG, "onTrackerServiceConnected: ");
        if (mDelayedUpdateMap) {
            completeJob(COMPLETE_SERVICE_BIND);
        }
    }

    private void enableMyLocationButton() {
        Log.v(TAG, "enableMyLocationButton: ");
        if (checkAndRequirePermission(Manifest.permission.ACCESS_FINE_LOCATION, PERMISSION_REQUEST_ACCESS_FINE_LOCATION_FROM_MAP)) {
            mGoogleMap.setMyLocationEnabled(true);
            Log.v(TAG, "enableMyLocationButton: " + mIsServiceRunning);
        }
    }

    private void showMapCompass(int componentID) {
        Log.v(TAG, "showMapCompass: " + componentID);
        if (componentID == COMPASS) {
            mCompass.start();
        } else {
            mCompass.stop();
        }

        setLayoutVisible(mMap, componentID == MAP);
        setLayoutVisible(mCompassGroup, componentID == COMPASS);
        mMapMenuItem.setVisible(componentID == COMPASS);
        mCompassMenuItem.setVisible(componentID == MAP);

        controlRowsVisible(componentID);
    }

    private void controlRowsVisible(int componentID) {
        if (mIsServiceRunning) {
            setLayoutVisible(mFirstRow, true);
            setLayoutVisible(mSecondRow, true);
        } else {
            if (componentID == MAP) {
                setLayoutVisible(mFirstRow, false);
                setLayoutVisible(mSecondRow, false);
            } else if (componentID == COMPASS) {
                setLayoutVisible(mFirstRow, true);
                setLayoutVisible(mSecondRow, false);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    enableMyLocationButton();
                    startStopService(true);
                }
                break;
            case PERMISSION_REQUEST_ACCESS_FINE_LOCATION_FROM_MAP:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    enableMyLocationButton();
                    if (!mIsServiceRunning && !mGeoClient.isStarted()) {
                        mGeoClient.getLocationSettings(new LocationSettingsListener());
                    }
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                break;
        }
    }

    @Override
    public void onResume() {
        Log.v(TAG, "onResume: ");
        super.onResume();
        mMap.onResume();

        if (mMapMenuItem != null && mMapMenuItem.isVisible() && !mCompass.isStarted()) {
            mCompass.start();
        }

        /* workaround для проблемы #1 */
        if (mDelayShowDialog) {
            mDelayShowDialog = false;
            showLowSignalConfirmDialog();
        }

        mSavedState = false;
    }

    @Override
    public void onStop() {
        Log.v(TAG, "onStop: ");
        Log.v(TAG, "onStop: save zoom " + mGoogleMap.getCameraPosition().zoom);
        Settings.getInstance(getContext()).saveMapType(mGoogleMap.getMapType());
        Settings.getInstance(getContext()).saveMapZoom(mGoogleMap.getCameraPosition().zoom);

        mMap.onStop();

        if (mMapMenuItem.isVisible()) {
            mCompass.stop();
        }

        mGeoClient.stop();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mTimerReseiver);
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mLocationReceiver);

        notifyService(false);
        mCallback.unbindTrackerService();

        super.onStop();
    }

    @Override
    public void onStart() {
        Log.v(TAG, "onStart: ");
        super.onStart();
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mTimerReseiver, new IntentFilter(TrackerService.TRACKER_SERVICE_TIMER_REQUEST));
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mLocationReceiver, new IntentFilter(TrackerService.TRACKER_SERVICE_REQUEST));

        mMap.onStart();

        mSignal = Antenna.SIGNAL_NO;

        /* Если произошел запуск / перезапуск активити при запущеннном сервисе */
        if (mIsServiceRunning) {
            mCallback.bindTrackerService();

            GPSData data = new GPSData();
            data.load(getContext());

            updateUI(data);

            notifyService(true);
        } else if (!mGeoClient.isStarted()) {
            mGeoClient.getLocationSettings(new LocationSettingsListener());
        }

        // если запущен сервис, кнопка будет иметь обозначение остановки сервиса
        updateImageOnButton(mIsServiceRunning);
    }

    /**
     * Уведодмляет сервис о состояниях UI-части приложения
     *
     * @param isAppRunning true если UI доступен
     */
    private void notifyService(boolean isAppRunning) {
        Intent intent = new Intent(MainFragment.SERVICE_REQUEST);
        intent.putExtra(MainFragment.APP_RUNNING, isAppRunning);
        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
    }

    @Override
    public void onPause() {
        Log.v(TAG, "onPause: ");
        mMap.onPause();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        Log.v(TAG, "onDestroyView: ");
        mMap.onDestroy();
        mGeoClient.disconnect();
        super.onDestroyView();
    }

    private void stopTrackerService() {
        Log.v(TAG, "stopTrackerService: ");
        mCallback.unbindTrackerService();
        mCallback.stopTrackerService();
        mIsServiceRunning = false;
        mGeoClient.start();
        controlRowsVisible(mCompassMenuItem.isVisible() ? MAP : COMPASS);
    }


    private void startTrackerService() {
        if (checkAndRequirePermission(Manifest.permission.ACCESS_FINE_LOCATION, PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION)) {
            Log.v(TAG, "startTrackerService: ");

            mCallback.startTrackerService();
            mCallback.bindTrackerService();

            mIsServiceRunning = true;
            mGeoClient.stop();
            controlRowsVisible(mCompassMenuItem.isVisible() ? MAP : COMPASS);
        }
    }


    /**
     * Определяет является точность определения местоположения приемлимой для начала тренировки
     *
     * @return true если погрешность определения местоположения не более 10м
     */
    private boolean isGoodAccuracy() {
        return mSignal == Antenna.SIGNAL_HIGH || mSignal == Antenna.SIGNAL_MEDIUM_PLUS;
    }

    @Override
    public void onMyLocationChanged(Location location) {
        Log.v(TAG, String.format(Locale.US, "onMyLocationChanged: %f,%f", location.getLatitude(), location.getLongitude()));
        updateCamera(new LatLng(location.getLatitude(), location.getLongitude()));
        Log.v(TAG, "onMyLocationChanged: " + location.getAccuracy());
        updateSignalWidget(location.getAccuracy());
    }

    private void updateSignalWidget(float accuracy) {

        mSignal = Antenna.SIGNAL_NO;

        if (accuracy <= 5 && accuracy > 0.1) {
            mSignal = Antenna.SIGNAL_HIGH;
        } else if (accuracy <= 10) {
            mSignal = Antenna.SIGNAL_MEDIUM_PLUS;
        } else if (accuracy <= 15) {
            mSignal = Antenna.SIGNAL_MEDIUM;
        } else if (accuracy <= 20) {
            mSignal = Antenna.SIGNAL_LOW_PLUS;
        } else if (accuracy <= 40) {
            mSignal = Antenna.SIGNAL_LOW;
        }

        mCallback.updateSignal(mSignal);
    }


    private void updateCamera(LatLng latLng) {
        if (mGoogleMap != null) {
            float zoom = mGoogleMap.getCameraPosition().zoom;
            mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
        }
    }

    public void startGeoClient() {
        mGeoClient.start();
        if (mGoogleMap != null) {
            Location location = mGeoClient.getCurrentLocation();
            if (location != null) {
                updateSignalWidget(location.getAccuracy());
                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 14));
            }
        }
    }

    @Override
    public void onDialogPositiveClick(AppCompatDialogFragment dialog) {
        startStopService(true);
    }

    @Override
    public void onDialogNegativeClick(AppCompatDialogFragment dialog) {

    }

    /**
     * Проверяет качество сигнала и запускает сервис в случае удовлетворительной точности,
     * иначе показыает диалог для подтверждения запуска трекера при низком качестве
     */
    public void checkAccuracyAndRunTracker() {
        if (isGoodAccuracy()) {
            startStopService(true);
        } else {
            // workaround для проблемы #1
            if (!mSavedState) {
                showLowSignalConfirmDialog();
            } else {
                mDelayShowDialog = true;
            }
        }
    }

    private void showLowSignalConfirmDialog() {
        ConfirmDialog dialog = ConfirmDialog.getInstance(R.string.low_gps_signal_title, R.string.low_gps_signal_message, this);
        dialog.show(getFragmentManager(), null);
    }

    public interface OnFragmentInteractionListener {
        void stopTrackerService();

        void startTrackerService();

        void updateSignal(int signal);

        List<LatLng> getLatLngList();

        void unbindTrackerService();

        void bindTrackerService();
    }

    private class ButtonsClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            // если сервис не запущен, нужно проверить включен ли gps, после чего уже произвести запуск сервиса
            if (!mIsServiceRunning) {
                mGeoClient.getLocationSettings(new LocationSettingsListener(true));
            } else {
                startStopService(false);
            }
        }
    }

    /**
     * Класс для проверки настроек GPS и в случае выключенного состояния отправки запроса на включение
     */
    private class LocationSettingsListener implements ResultCallback<LocationSettingsResult> {
        /**
         * true если проверка настроек GPS осуществляется перед запуском трекера (нажата кнопка начать тренировку)
         * false если проверка настроек GPS осуществляется перед запуском локального gps-клиента
         */
        private boolean mIsCheckForService;

        LocationSettingsListener(boolean isCheckForService) {
            mIsCheckForService = isCheckForService;
        }

        LocationSettingsListener() {
            mIsCheckForService = false;
        }

        @Override
        public void onResult(@NonNull LocationSettingsResult result) {
            final Status status = result.getStatus();
            Log.v(TAG, "onResult: " + status.getStatusCode());
            switch (status.getStatusCode()) {
                case LocationSettingsStatusCodes.SUCCESS:
                    if (mIsCheckForService) {
                        checkAccuracyAndRunTracker();
                    } else {
                        startGeoClient();
                    }
                    break;
                case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                    // Location settings are not satisfied, but this can be fixed
                    // by showing the user a dialog.
                    try {
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        status.startResolutionForResult(
                                getActivity(),
                                mIsCheckForService ? REQUETST_CHECK_SETTINGS_FOR_SERVICE : REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException e) {
                        // Ignore the error.
                    }
                    break;
                case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                    // Location settings are not satisfied. However, we have no way
                    // to fix the settings so we won't show the dialog.
                    break;
            }

        }
    }
}
