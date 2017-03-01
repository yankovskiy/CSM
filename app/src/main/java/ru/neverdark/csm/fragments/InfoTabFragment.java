package ru.neverdark.csm.fragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Locale;

import ru.neverdark.csm.R;
import ru.neverdark.csm.abs.AbsTabFragment;
import ru.neverdark.csm.abs.OnTabNaviListener;
import ru.neverdark.csm.data.GPSData;
import ru.neverdark.csm.utils.Utils;
import ru.neverdark.widgets.DataCard;

/**
 * A simple {@link Fragment} subclass.
 */
public class InfoTabFragment extends AbsTabFragment {


    private DataCard mDistance;
    private DataCard mSpeed;
    private DataCard mAverageSpeed;
    private DataCard mMaxSpeed;
    private DataCard mAltitude;
    private DataCard mMaxAltitude;
    private DataCard mUpAltitude;
    private DataCard mDownAltitude;
    private DataCard mUpDistance;
    private DataCard mDownDistance;
    private DataCard mLatitude;
    private DataCard mLongitude;

    public InfoTabFragment() {
        // Required empty public constructor
    }

    @Override
    public void updateUI(GPSData data) {
        if (isResumed()) {
            mDistance.setValue(String.format(Locale.US, "%.3f", data.distance / 1000));
            mSpeed.setValue(String.format(Locale.US, "%.1f", Utils.convertMeterToKm(data.speed)));
            mAverageSpeed.setValue(String.format(Locale.US, "%.1f", Utils.convertMeterToKm(data.average_speed)));
            mMaxSpeed.setValue(String.format(Locale.US, "%.1f", Utils.convertMeterToKm(data.max_speed)));
            mAltitude.setValue(String.valueOf(Math.round(data.altitude)));
            mMaxAltitude.setValue(String.valueOf(Math.round(data.max_altitude)));
            mUpAltitude.setValue(String.valueOf(Math.round(data.up_altitude)));
            mDownAltitude.setValue(String.valueOf(Math.round(data.down_altitude)));
            mUpDistance.setValue(String.format(Locale.US, "%.3f", data.up_distance / 1000));
            mDownDistance.setValue(String.format(Locale.US, "%.3f", data.down_distance / 1000));
            mLatitude.setValue(String.valueOf(data.latitude));
            mLongitude.setValue(String.valueOf(data.longitude));
        } else {
            repeatUpdateAfterResumed(data);
        }
    }

    @Override
    public void resetUI() {
        mDistance.setValue(R.string.zero);
        mSpeed.setValue(R.string.zero);
        mAverageSpeed.setValue(R.string.zero);
        mMaxSpeed.setValue(R.string.zero);
        mAltitude.setValue(R.string.zero);
        mMaxAltitude.setValue(R.string.zero);
        mUpAltitude.setValue(R.string.zero);
        mDownAltitude.setValue(R.string.zero);
        mUpDistance.setValue(R.string.zero);
        mDownDistance.setValue(R.string.zero);
        mLatitude.setValue(R.string.zero);
        mLongitude.setValue(R.string.zero);
    }

    public static InfoTabFragment getInstance(OnTabNaviListener listener) {
        InfoTabFragment fragment = new InfoTabFragment();
        fragment.setData(R.layout.fragment_info_tab, listener);
        return fragment;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        bindData(view);
        return view;
    }

    private void bindData(View view) {
        int km = R.string.km;
        int kmch = R.string.kmch;
        int m = R.string.m;

        mDistance = (DataCard) view.findViewById(R.id.distance);
        mDistance.setTitleNote(km);

        mSpeed = (DataCard) view.findViewById(R.id.speed);
        mSpeed.setTitleNote(kmch);

        mAverageSpeed = (DataCard) view.findViewById(R.id.average_speed);
        mAverageSpeed.setTitleNote(kmch);

        mMaxSpeed = (DataCard) view.findViewById(R.id.max_speed);
        mMaxSpeed.setTitleNote(kmch);

        mAltitude = (DataCard) view.findViewById(R.id.altitude);
        mAltitude.setTitleNote(m);

        mMaxAltitude = (DataCard) view.findViewById(R.id.max_altitude);
        mMaxAltitude.setTitleNote(m);

        mUpAltitude = (DataCard) view.findViewById(R.id.up_altitude);
        mUpAltitude.setTitleNote(m);

        mDownAltitude = (DataCard) view.findViewById(R.id.down_altitude);
        mDownAltitude.setTitleNote(m);

        mUpDistance = (DataCard) view.findViewById(R.id.up_distance);
        mUpDistance.setTitleNote(km);

        mDownDistance = (DataCard) view.findViewById(R.id.down_distance);
        mDownDistance.setTitleNote(km);

        mLatitude = (DataCard) view.findViewById(R.id.latitude);
        mLongitude = (DataCard) view.findViewById(R.id.longitude);
    }

}
