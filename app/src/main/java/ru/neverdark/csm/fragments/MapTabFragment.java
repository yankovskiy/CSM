package ru.neverdark.csm.fragments;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

import java.util.Locale;

import ru.neverdark.csm.R;
import ru.neverdark.csm.abs.AbsTabFragment;
import ru.neverdark.csm.abs.OnTabNaviListener;
import ru.neverdark.csm.data.GPSData;
import ru.neverdark.csm.utils.Utils;
import ru.neverdark.widgets.Antenna;
import ru.neverdark.widgets.DataCard;

/**
 * A simple {@link Fragment} subclass.
 */
public class MapTabFragment extends AbsTabFragment implements OnMapReadyCallback {

    private DataCard mDistance;
    private DataCard mAverageSpeed;
    private Antenna mAntenna;
    private int mSignal = -1;

    public interface OnMapFragmentListener {
        void onChangeMapType(int mapType);
        void onMapReady(GoogleMap googleMap);
    }

    private OnMapFragmentListener mCallback;

    public MapTabFragment() {
        // Required empty public constructor
    }

    public void updateSignal(int signal) {
        if (isResumed()) {
            mAntenna.setSignal(signal);
        } else {
            repeatUpdateSignalAfterResumed(signal);
        }
    }

    private void repeatUpdateSignalAfterResumed(int signal) {
        mSignal = signal;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mSignal != -1) {
            mAntenna.setSignal(mSignal);
            mSignal = -1;
        }
    }

    public static MapTabFragment getInstance(OnTabNaviListener listener, OnMapFragmentListener callback) {
        MapTabFragment fragment = new MapTabFragment();
        fragment.setData(R.layout.fragment_map_tab, listener);
        fragment.mCallback = callback;
        return fragment;
    }

    @Override
    public void updateUI(GPSData data) {
        /* может быть вызвана до того, как фрагмент перешел в статус onResumed
         * если так, то запоминаем переданные данные и повторно вызываем эту функцию из onResume */
        if(isResumed()) {
            String distance = String.format(Locale.US, "%.3f", data.distance / 1000);
            String average_speed = String.format(Locale.US, "%.2f", Utils.convertMeterToKm(data.average_speed));

            mDistance.setValue(distance);
            mAverageSpeed.setValue(average_speed);
        } else {
            repeatUpdateAfterResumed(data);
        }
    }

    @Override
    public void resetUI() {
        mDistance.setValue(R.string.zero);
        mAverageSpeed.setValue(R.string.zero);
    }

    private static final String TAG = "MapTabFragment";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.v(TAG, "onCreateView: ");
        View view = super.onCreateView(inflater, container, savedInstanceState);

        mDistance = (DataCard) view.findViewById(R.id.distance);
        mAverageSpeed = (DataCard) view.findViewById(R.id.average_speed);
        mAntenna = (Antenna) view.findViewById(R.id.antenna);

        mDistance.setTitleNote(R.string.km);
        mAverageSpeed.setTitleNote(R.string.kmch);

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map_fragment);
        mapFragment.getMapAsync(this);
        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Log.v(TAG, "onCreateOptionsMenu: ");
        inflater.inflate(R.menu.main, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_map_type_normal:
                mCallback.onChangeMapType(GoogleMap.MAP_TYPE_NORMAL);
                break;
            case R.id.action_map_type_hybrid:
                mCallback.onChangeMapType(GoogleMap.MAP_TYPE_HYBRID);
                break;
            case R.id.action_map_type_satellite:
                mCallback.onChangeMapType(GoogleMap.MAP_TYPE_SATELLITE);
                break;
            case R.id.action_map_type_terrain:
                mCallback.onChangeMapType(GoogleMap.MAP_TYPE_TERRAIN);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mCallback.onMapReady(googleMap);
    }
}
