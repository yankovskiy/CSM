package ru.neverdark.csm.fragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

import java.util.Locale;

import ru.neverdark.csm.R;
import ru.neverdark.csm.abs.AbsTabFragment;
import ru.neverdark.csm.abs.OnTabNaviListener;
import ru.neverdark.csm.data.GPSData;

/**
 * A simple {@link Fragment} subclass.
 */
public class MapTabFragment extends AbsTabFragment {

    private TextView mDistanceTv;
    private TextView mAverageSpeedTv;

    private OnMapReadyCallback mMapReadyCallback;
    private GPSData mCachedData;

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
            String distance = String.format(Locale.US, "%.3f км", data.distance / 1000);
            String average_speed = String.format(Locale.US, "%.2f км/ч", data.average_speed * 3.6);

            mDistanceTv.setText(distance);
            mAverageSpeedTv.setText(average_speed);
        } else {
            repeatUpdateAfterResumed(data);
        }
    }

    @Override
    public void resetUI() {
        mDistanceTv.setText(R.string.zero);
        mAverageSpeedTv.setText(R.string.zero);
    }

    private static final String TAG = "MapTabFragment";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.v(TAG, "onCreateView: ");
        View view = super.onCreateView(inflater, container, savedInstanceState);
        mDistanceTv = (TextView) view.findViewById(R.id.distance_value);
        mAverageSpeedTv = (TextView) view.findViewById(R.id.average_speed_value);

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map_fragment);
        mapFragment.getMapAsync(mMapReadyCallback);
        return view;
    }

}
