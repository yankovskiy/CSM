package ru.neverdark.csm.fragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import ru.neverdark.csm.R;
import ru.neverdark.csm.db.GpslogTable;

/**
 * A simple {@link Fragment} subclass.
 */
public class StatsViewGraphTabFragment extends Fragment {


    public StatsViewGraphTabFragment() {
        // Required empty public constructor
    }

    private static final String TAG = "StatsViewGraphTabFragme";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        Log.v(TAG, "onCreateView: ");
        return inflater.inflate(R.layout.fragment_stats_view_graph_tab, container, false);
    }

    public static StatsViewGraphTabFragment getInstance(List<GpslogTable.TrackRecord> points) {
        return new StatsViewGraphTabFragment();
    }
}
