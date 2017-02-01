package ru.neverdark.csm.fragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Locale;

import ru.neverdark.csm.R;
import ru.neverdark.csm.abs.AbsTabFragment;
import ru.neverdark.csm.abs.OnTabNaviListener;
import ru.neverdark.csm.components.Compass;
import ru.neverdark.csm.data.GPSData;
import ru.neverdark.widgets.DataCard;

/**
 * A simple {@link Fragment} subclass.
 */
public class CompassTabFragment extends AbsTabFragment {


    private DataCard mLatitude;
    private DataCard mLongitude;
    private DataCard mAltitude;
    private Compass mCompass;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        mCompass = new Compass(getContext());
        bindData(view);
        return view;
    }

    private void bindData(View view) {
        mLatitude = (DataCard) view.findViewById(R.id.latitude);
        mLongitude = (DataCard) view.findViewById(R.id.longitude);
        mAltitude = (DataCard) view.findViewById(R.id.altitude);

        mAltitude.setTitleNote(R.string.m);

        TextView deegres = (TextView) view.findViewById(R.id.deegres);
        TextView direction = (TextView) view.findViewById(R.id.direction);
        mCompass.setArrowView(view.findViewById(R.id.compass_ext));
        mCompass.setDeegresTv(deegres);
        mCompass.setDirectionTv(direction);
    }
}
