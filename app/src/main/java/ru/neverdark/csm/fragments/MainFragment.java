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
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatDialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.File;
import java.util.List;
import java.util.Locale;

import ru.neverdark.csm.R;
import ru.neverdark.csm.abs.OnTabNaviListener;
import ru.neverdark.csm.activity.TrainingFinishAcitivty;
import ru.neverdark.csm.components.GeoClient;
import ru.neverdark.csm.components.TrackerService;
import ru.neverdark.csm.data.GPSData;
import ru.neverdark.csm.db.Db;
import ru.neverdark.csm.utils.Constants;
import ru.neverdark.csm.utils.Settings;
import ru.neverdark.csm.utils.Utils;
import ru.neverdark.widgets.Antenna;

public class MainFragment extends Fragment implements GeoClient.OnGeoClientListener, ConfirmDialog.NoticeDialogListener, OnTabNaviListener, MapTabFragment.OnMapFragmentListener {
    public static final int REQUEST_CHECK_SETTINGS = 3;
    public static final int TRAINING_RESULT_REQUEST = 1;
    public static final int REQUETST_CHECK_SETTINGS_FOR_SERVICE = 4;
    private static final String TAG = "MainFragment";
    private static final String FRAGMENT_PATH = Constants.PACKAGE_NAME + "." + TAG;
    public static final String APP_RUNNING = FRAGMENT_PATH + ".APP_RUNNING";
    public static final String SERVICE_REQUEST = FRAGMENT_PATH + ".SERVICE_REQUEST";
    public static final String TRAINING_FINISH_DATE = FRAGMENT_PATH + ".FINISH_DATE";
    public static final String TRAINING_START_DATE = FRAGMENT_PATH + ".START_DATE";
    public static final String TRAININD_ID = FRAGMENT_PATH + ".TRAINING_ID";
    public static final String TRAINING_DATA = FRAGMENT_PATH + ".DATA";
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_ACCESS_FINE_LOCATION_FROM_MAP = 2;
    private static final int COMPLETE_MAP_LOAD = 1;
    private static final int COMPLETE_SERVICE_BIND = 2;
    private static final int COMPLETE_ALL = COMPLETE_MAP_LOAD | COMPLETE_SERVICE_BIND;
    private OnFragmentInteractionListener mCallback;
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
    private ViewPager mPager;
    private MapTabFragment mMapTabFragment;
    private CompassTabFragment mCompassTabFragment;
    private InfoTabFragment mInfoTabFragment;
    private int mTrainingDurationRaw;
    private MenuItem mActivityMenuItem;
    private Context mContext;
    private long mStartTrainingDate;

    public MainFragment() {
        // Required empty public constructor
    }

    public static MainFragment newInstance(boolean isServiceRunning) {
        MainFragment fragment = new MainFragment();
        fragment.mIsServiceRunning = isServiceRunning;
        return fragment;
    }

    private synchronized void completeJob(int job) {
        if (mCompleteJob != COMPLETE_ALL) {
            mCompleteJob |= job;

            if (mCompleteJob == COMPLETE_ALL) {
                loadTrackPointsFromService();
                mDelayedUpdateMap = false;
            }
        }
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

            resetData();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void resetData() {
        mPolyline.remove();
        mPolyline = null;
        mMapTabFragment.resetUI();
        mInfoTabFragment.resetUI();
        mCompassTabFragment.resetUI();
    }

    private void removeTraining(long recordId) {
        String fileName = Utils.getSnapshotNameById(recordId);
        File file = new File(getContext().getFilesDir(), fileName);
        if (file.exists()) {
            if (!file.delete()) {
                Log.v(TAG, "removeTraining: unable to delete " + file.getAbsolutePath());
            }
        }
        Db.getInstance(getContext()).getSummaryTable().deleteRecord(recordId);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_choose_activity_type:
                showChooseActivityTypeDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showChooseActivityTypeDialog() {
        ChooseActivityTypeDialog dialog = ChooseActivityTypeDialog.getInstance(new ActivityTypeChooseListener());
        dialog.show(getFragmentManager(), null);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate: ");
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate: " + mIsServiceRunning);
        mGeoClient = new GeoClient(getContext(), this);
        mData = new GPSData();

        // если сервис запущен и перезапущен UI нам нужно обновить масштаб на карте
        mDelayedUpdateMap = mIsServiceRunning;

        if (!mIsServiceRunning) {
            getActivity().setTitle(R.string.app_name);
        }
        createReseivers();

        setHasOptionsMenu(true);
    }

    private void createReseivers() {
        mTimerReseiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                mTrainingDurationRaw = intent.getIntExtra(TrackerService.TRACKER_SERVICE_TIMER_DATA, 0);
                int activityTime = intent.getIntExtra(TrackerService.TRACKER_SERVICE_ACTIVITY_TIME, 0);

                if (mMapTabFragment.isResumed()) {
                    mMapTabFragment.updateTime(mTrainingDurationRaw, activityTime);
                }
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
                    mStartTrainingDate = intent.getLongExtra(TrackerService.TRACKER_START_DATE, 0);
                    if (recordId == 0) {
                        Log.v(TAG, "onReceive: incorrect database record id");
                    } else {
                        Log.v(TAG, "onReceive: create new activity with result");
                        List<LatLng> lst = mPolyline != null ? mPolyline.getPoints() : null;

                        // трек более двух точек, длительность более 5 секунд
                        if (lst != null && lst.size() > 2 && mTrainingDurationRaw > 5000) {
                            startTrainingFinishActivity(recordId);
                        } else {
                            removeTraining(recordId);
                            resetData();
                            showLowQualityTrainingDialog();
                        }
                    }
                }
            }
        };
    }

    private void showLowQualityTrainingDialog() {
        InfoDialog dialog = InfoDialog.getInstance(R.string.short_traning_title, R.string.short_training_message);
        dialog.show(getFragmentManager(), null);
    }

    private void startTrainingFinishActivity(long id) {
        Intent intent = new Intent(getContext(), TrainingFinishAcitivty.class);
        intent.putExtra(MainFragment.TRAINING_DATA, mData);
        intent.putExtra(MainFragment.TRAININD_ID, id);
        intent.putExtra(MainFragment.TRAINING_START_DATE, mStartTrainingDate);
        intent.putExtra(MainFragment.TRAINING_FINISH_DATE, System.currentTimeMillis());
        startActivityForResult(intent, MainFragment.TRAINING_RESULT_REQUEST);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.v(TAG, "onCreateView: ");
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        mContext = getContext();

        mStartStopTrainingButton = (FloatingActionButton) view.findViewById(R.id.start_stop_training_button);
        mStartStopTrainingButton.setOnClickListener(new ButtonsClickListener());

        mMapTabFragment = MapTabFragment.getInstance(this, this);
        mCompassTabFragment = CompassTabFragment.getInstance(this);
        mInfoTabFragment = InfoTabFragment.getInstance(this);

        mPager = (ViewPager) view.findViewById(R.id.tab_container);
        mPager.setOffscreenPageLimit(TABS.values().length - 1);
        mPager.setAdapter(new CustomAdapter(getChildFragmentManager()));
        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (position != TABS.MAP.ordinal()) {
                    mStartStopTrainingButton.hide();
                } else {
                    mStartStopTrainingButton.show();
                }

                if (position != TABS.COMPASS.ordinal()) {
                    mCompassTabFragment.stopCompass();
                } else {
                    mCompassTabFragment.startCompass();
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
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
        showHideActivityButton(mIsServiceRunning);
    }

    /**
     * Скрывает или отображает кнопку в ActionBar'e для выбора типа активности
     *
     * @param isHide true если кнопку необходимо скрыть
     */
    private void showHideActivityButton(boolean isHide) {
        mActivityMenuItem.setVisible(!isHide);
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

        mMapTabFragment.updateUI(data);
        mCompassTabFragment.updateUI(data);
        mInfoTabFragment.updateUI(data);

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
    public void onChangeMapType(int mapType) {
        if (mGoogleMap != null) {
            mGoogleMap.setMapType(mapType);
        } else {
            Toast.makeText(getContext(), R.string.map_not_ready, Toast.LENGTH_LONG).show();
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
        if (mGoogleMap != null) {
            Settings.getInstance(getContext()).saveMapType(mGoogleMap.getMapType());
            Settings.getInstance(getContext()).saveMapZoom(mGoogleMap.getCameraPosition().zoom);
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
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        Log.v(TAG, "onDestroyView: ");
        mGeoClient.disconnect();
        super.onDestroyView();
    }

    private void stopTrackerService() {
        Log.v(TAG, "stopTrackerService: ");
        mCallback.unbindTrackerService();
        mCallback.stopTrackerService();
        mIsServiceRunning = false;
        mGeoClient.start();
    }


    private void startTrackerService() {
        if (checkAndRequirePermission(Manifest.permission.ACCESS_FINE_LOCATION, PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION)) {
            Log.v(TAG, "startTrackerService: ");

            mCallback.startTrackerService();
            mCallback.bindTrackerService();

            mIsServiceRunning = true;
            mGeoClient.stop();
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

        GPSData data = new GPSData();
        data.latitude = location.getLatitude();
        data.longitude = location.getLongitude();
        data.altitude = location.getAltitude();
        mCompassTabFragment.updateUI(data);

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

        mMapTabFragment.updateSignal(mSignal);
    }


    private void updateCamera(LatLng latLng) {
        if (mGoogleMap != null) {
            float zoom = mGoogleMap.getCameraPosition().zoom;
            mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Log.v(TAG, "onCreateOptionsMenu: ");
        inflater.inflate(R.menu.main_fragment, menu);
        mActivityMenuItem = menu.findItem(R.id.action_choose_activity_type);
        mActivityMenuItem.setIcon(Settings.getInstance(mContext).loadActivityTypeIcon());
        showHideActivityButton(mIsServiceRunning);
        super.onCreateOptionsMenu(menu, inflater);
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

    @Override
    public void onPrevTab() {
        mPager.setCurrentItem(mPager.getCurrentItem() - 1, true);
    }

    @Override
    public void onNextTab() {
        mPager.setCurrentItem(mPager.getCurrentItem() + 1, true);
    }

    private enum TABS {
        MAP(R.string.map),
        INFO(R.string.info),
        COMPASS(R.string.compass);

        private final int mTitleRes;

        TABS(int titleResIs) {
            mTitleRes = titleResIs;
        }

        public int getTitle() {
            return mTitleRes;
        }
    }

    public interface OnFragmentInteractionListener {
        void stopTrackerService();

        void startTrackerService();

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
                    InfoDialog dialog = InfoDialog.getInstance(R.string.title_gps_not_found, R.string.message_gps_not_found);
                    dialog.show(getFragmentManager(), null);
                    break;
            }

        }
    }

    private class CustomAdapter extends FragmentPagerAdapter {
        CustomAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            TABS tab = TABS.values()[position];

            if (tab == TABS.MAP) {
                return mMapTabFragment;
            } else if (tab == TABS.COMPASS) {
                return mCompassTabFragment;
            } else if (tab == TABS.INFO) {
                return mInfoTabFragment;
            }

            return null;
        }

        @Override
        public int getCount() {
            return TABS.values().length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            TABS tab = TABS.values()[position];
            return getString(tab.getTitle());
        }
    }

    private class ActivityTypeChooseListener implements ChooseActivityTypeDialog.OnActivityTypeChooseListener {
        private void setIcon(int drawable) {
            if (mActivityMenuItem != null) {
                mActivityMenuItem.setIcon(drawable);
                Settings.getInstance(mContext).saveActivityTypeIcon(drawable);
            }
        }

        @Override
        public void onWalingActivity() {
            setIcon(R.drawable.ic_walking);
        }

        @Override
        public void onNordikWalkingActivity() {
            setIcon(R.drawable.ic_nordic_walking);
        }

        @Override
        public void onHikingActivity() {
            setIcon(R.drawable.ic_hiking);
        }

        @Override
        public void onRunActivity() {
            setIcon(R.drawable.ic_run);
        }

        @Override
        public void onRoadBikeActivity() {
            setIcon(R.drawable.ic_road_bike);
        }

        @Override
        public void onMtbActivity() {
            setIcon(R.drawable.ic_mtb);
        }

        @Override
        public void onSkateboardActivity() {
            setIcon(R.drawable.ic_skateboard);
        }
    }
}
