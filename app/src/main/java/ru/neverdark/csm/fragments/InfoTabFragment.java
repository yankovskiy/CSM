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

/**
 * A simple {@link Fragment} subclass.
 */
public class InfoTabFragment extends AbsTabFragment {


    public InfoTabFragment() {
        // Required empty public constructor
    }

    @Override
    public void updateUI(GPSData data) {

    }

    @Override
    public void resetUI() {

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
        return view;
    }

}
