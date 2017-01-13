package ru.neverdark.csm.fragments;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
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

import java.util.Locale;

import ru.neverdark.csm.components.Compass;
import ru.neverdark.csm.components.TrackerService;
import ru.neverdark.csm.data.GPSData;
import ru.neverdark.csm.components.GeoClient;
import ru.neverdark.csm.R;

public class MainFragment extends Fragment implements OnMapReadyCallback, GeoClient.OnGeoClientListener {
    public static final int COMPASS = 0;
    public static final int MAP = 1;
    private static final String TAG = "MainFragment";
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_ACCESS_FINE_LOCATION_FROM_MAP = 2;
    public static final int REQUEST_CHECK_SETTINGS = 3;
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
    public static final int REQUETST_CHECK_SETTINGS_FOR_SERVICE = 4;
    private BroadcastReceiver mReseiver;

    public MainFragment() {
        // Required empty public constructor
    }

    public static MainFragment newInstance(boolean isServiceRunning) {
        MainFragment fragment = new MainFragment();
        fragment.mIsServiceRunning = isServiceRunning;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate: ");
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate: " + mIsServiceRunning);
        mCompass = new Compass(getContext());
        mGeoClient = new GeoClient(getContext(), this);
        setHasOptionsMenu(true);
        mReseiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                int time = intent.getIntExtra(TrackerService.TRACKER_SERVICE_TIMER_DATA, 0);
                int seconds = time % 60 ;
                int minutes = (time / 60) % 60;
                int hours   = time / (60 * 60);
                String timeStr = String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds);
                getActivity().setTitle(timeStr);
                Log.v(TAG, "onReceive: " + timeStr);
            }
        };
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

    public void setButtonsEnabledState() {
        Log.v(TAG, "setButtonsEnabledState: " + mIsServiceRunning);
        if (mIsServiceRunning) {
            mStartStopTrainingButton.setImageResource(R.drawable.fab_stop_training);
        } else {
            mStartStopTrainingButton.setImageResource(R.drawable.fab_start_training);
        }
    }

    public void handleStartStopButton() {
        Log.v(TAG, "handleStartStopButton: " + mIsServiceRunning);
        if (!mIsServiceRunning) {
            startTrackerService();
        } else {
            stopTrackerService();
        }
        setButtonsEnabledState();
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
        String altitude = String.format(Locale.US, "%d", data.altitude);
        String speed = String.format(Locale.US, "%.2f", data.speed * 3.6);
        String distance = String.format(Locale.US, "%d km %d m", data.distance / 1000, data.distance % 1000);
        String average_speed = String.format(Locale.US, "%.2f", data.average_speed * 3.6);

        Log.v(TAG, "updateUI: latitude = " + latitude);
        Log.v(TAG, "updateUI: longitude = " + longitude);

        mLatitudeTv.setText(latitude);
        mLongitudeTv.setText(longitude);
        mAltitudeTv.setText(altitude);
        mSpeedTv.setText(speed);
        mDistanceTv.setText(distance);
        mAverageSpeedTv.setText(average_speed);

        updateCamera(new LatLng(data.latitude, data.longitude));
    }

    /**
     * Проверяет наличие прав и в случае необходимости запрашивает их
     *
     * @param permission  права доступа для проверки и запроса разрешения
     * @param requestCode код запроса прав доступа для обработки onRequestPermissionsResult
     * @return true если права доступа есть
     */
    public boolean checkAndRequirePermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(getContext(), permission) == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            requestPermissions(new String[]{permission}, requestCode);
            return false;
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.v(TAG, "onMapReady: ");
        mGoogleMap = googleMap;
        enableMyLocationButton();
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
                    handleStartStopButton();
                }
                break;
            case PERMISSION_REQUEST_ACCESS_FINE_LOCATION_FROM_MAP:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    enableMyLocationButton();
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
    }

    @Override
    public void onStop() {
        Log.v(TAG, "onStop: ");
        mMap.onStop();

        if (mMapMenuItem.isVisible()) {
            mCompass.stop();
        }

        mGeoClient.stop();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mReseiver);
        super.onStop();
    }

    @Override
    public void onStart() {
        Log.v(TAG, "onStart: ");
        super.onStart();
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mReseiver, new IntentFilter(TrackerService.TRACKER_SERVICE_TIMER_REQUEST));
        mMap.onStart();
        setButtonsEnabledState();
        if (!mIsServiceRunning) {
            mGeoClient.getLocationSettings(new LocationSettingsListener());
        }
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
        mCallback.stopTrackerService();
        mIsServiceRunning = false;
        mGeoClient.start();
        controlRowsVisible(mCompassMenuItem.isVisible()? MAP: COMPASS);
    }

    public void startTrackerService() {
        if (checkAndRequirePermission(Manifest.permission.ACCESS_FINE_LOCATION, PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION)) {
            Log.v(TAG, "startTrackerService: ");
            mCallback.startTrackerService();
            mIsServiceRunning = true;
            mGeoClient.stop();
            controlRowsVisible(mCompassMenuItem.isVisible()? MAP: COMPASS);
        }
    }

    @Override
    public void onMyLocationChanged(Location location) {
        Log.v(TAG, String.format(Locale.US, "onMyLocationChanged: %f,%f", location.getLatitude(), location.getLongitude()));
        updateCamera(new LatLng(location.getLatitude(), location.getLongitude()));
    }

    private void updateCamera(LatLng latLng) {
        if (mGoogleMap != null) {
            float zoom = mGoogleMap.getCameraPosition().zoom;
            mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
        }
    }

    public interface OnFragmentInteractionListener {
        void stopTrackerService();

        void startTrackerService();
    }

    private class ButtonsClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            mGeoClient.getLocationSettings(new LocationSettingsListener(true));
        }
    }

    public void startGeoClient() {
        mGeoClient.start();
        if (mGoogleMap != null) {
            Location location = mGeoClient.getCurrentLocation();
            if (location != null) {
                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 14));
            }
        }
    }

    private class LocationSettingsListener implements ResultCallback<LocationSettingsResult> {
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
            //final LocationSettingsStates= result.getLocationSettingsStates();
            Log.v(TAG, "onResult: " + status.getStatusCode());
            switch (status.getStatusCode()) {
                case LocationSettingsStatusCodes.SUCCESS:
                    // All location settings are satisfied. The client can
                    // initialize location requests here.
                    if (mIsCheckForService) {
                        handleStartStopButton();
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
                                mIsCheckForService? REQUETST_CHECK_SETTINGS_FOR_SERVICE : REQUEST_CHECK_SETTINGS);
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
