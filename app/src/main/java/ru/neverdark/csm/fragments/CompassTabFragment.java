package ru.neverdark.csm.fragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import ru.neverdark.csm.R;
import ru.neverdark.csm.abs.AbsTabFragment;
import ru.neverdark.csm.abs.OnTabNaviListener;
import ru.neverdark.csm.data.GPSData;
import ru.neverdark.widgets.Compass;
import ru.neverdark.widgets.DataCard;

/**
 * A simple {@link Fragment} subclass.
 */
public class CompassTabFragment extends AbsTabFragment {


    private DataCard mLatitude;
    private DataCard mLongitude;
    private DataCard mAltitude;
    private Compass mCompass;
    private boolean mStartCompass;

    public CompassTabFragment() {
        // Required empty public constructor
    }

    public static CompassTabFragment getInstance(OnTabNaviListener listener) {
        CompassTabFragment fragment = new CompassTabFragment();
        fragment.setData(R.layout.fragment_compass_tab, listener);
        return fragment;
    }

    @Override
    public void updateUI(GPSData data) {
        if (isResumed()) {
            mLatitude.setValue(String.valueOf(data.latitude));
            mLongitude.setValue(String.valueOf(data.longitude));
            mAltitude.setValue(String.valueOf(Math.round(data.altitude)));
        } else {
            repeatUpdateAfterResumed(data);
        }
    }

    @Override
    public void resetUI() {

    }

    public void startCompass() {
        if (!mCompass.isStarted()) {
            mCompass.start();
        }
    }

    public void stopCompass() {
        if (mCompass.isStarted()) {
            mCompass.stop();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mCompass.isStarted()) {
            mCompass.stop();
            mStartCompass = true;
        } else {
            mStartCompass = false;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mStartCompass) {
            startCompass();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        mCompass = (Compass) view.findViewById(R.id.compass);
        bindData(view);
        return view;
    }

    private void bindData(View view) {
        mLatitude = (DataCard) view.findViewById(R.id.latitude);
        mLongitude = (DataCard) view.findViewById(R.id.longitude);
        mAltitude = (DataCard) view.findViewById(R.id.altitude);

        mAltitude.setTitleNote(R.string.m);
    }
}
