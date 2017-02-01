package ru.neverdark.csm.fragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

import java.util.Locale;

import ru.neverdark.csm.R;
import ru.neverdark.csm.abs.AbsTabFragment;
import ru.neverdark.csm.abs.OnTabNaviListener;
import ru.neverdark.csm.data.GPSData;
import ru.neverdark.widgets.DataCard;

/**
 * A simple {@link Fragment} subclass.
 */
public class MapTabFragment extends AbsTabFragment {

    private DataCard mDistance;
    private DataCard mAverageSpeed;

    private OnMapReadyCallback mMapReadyCallback;

    public MapTabFragment() {
        // Required empty public constructor
    }

    public static MapTabFragment getInstance(OnTabNaviListener listener, OnMapReadyCallback callback) {
        MapTabFragment fragment = new MapTabFragment();
        fragment.setData(R.layout.fragment_map_tab, listener);
        fragment.mMapReadyCallback = callback;
        return fragment;
    }

    @Override
    public void updateUI(GPSData data) {
        /* может быть вызвана до того, как фрагмент перешел в статус onResumed
         * если так, то запоминаем переданные данные и повторно вызываем эту функцию из onResume */
        if(isResumed()) {
            String distance = String.format(Locale.US, "%.3f", data.distance / 1000);
            String average_speed = String.format(Locale.US, "%.2f", data.average_speed * 3.6);

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
        mDistance.setTitleNote(R.string.km);
        mAverageSpeed.setTitleNote(R.string.kmch);

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map_fragment);
        mapFragment.getMapAsync(mMapReadyCallback);
        return view;
    }

}
