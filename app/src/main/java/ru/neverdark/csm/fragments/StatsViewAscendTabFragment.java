package ru.neverdark.csm.fragments;


import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import lecho.lib.hellocharts.model.PieChartData;
import lecho.lib.hellocharts.model.SliceValue;
import lecho.lib.hellocharts.view.PieChartView;
import ru.neverdark.csm.R;
import ru.neverdark.csm.db.SummaryTable;
import ru.neverdark.widgets.Legend;

/**
 * A simple {@link Fragment} subclass.
 */
public class StatsViewAscendTabFragment extends Fragment {


    private SummaryTable.Record mSummaryRecord;

    public StatsViewAscendTabFragment() {
        // Required empty public constructor
    }

    private static final String TAG = "StatsViewAscendTabFragm";


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        Log.v(TAG, "onCreateView: ");
        View view = inflater.inflate(R.layout.fragment_stats_view_ascend_tab, container, false);
        Context context = getContext();
        int green = ContextCompat.getColor(context, R.color.light_green_400);
        int blue = ContextCompat.getColor(context, R.color.blue_400);
        int orange = ContextCompat.getColor(context, R.color.orange_400);

        float upDistance = mSummaryRecord.up_distance;
        float downDistance = mSummaryRecord.down_distance;
        float plainDistance = mSummaryRecord.distance - mSummaryRecord.up_distance - mSummaryRecord.down_distance;
        float distance = mSummaryRecord.distance;

        List<SliceValue> values = new ArrayList<>();
        values.add(new SliceValue(upDistance, orange));
        values.add(new SliceValue(downDistance, green));
        values.add(new SliceValue(plainDistance, blue));

        PieChartData data = new PieChartData(values);

        PieChartView chart = (PieChartView) view.findViewById(R.id.chart);
        chart.setChartRotationEnabled(false);
        chart.setValueTouchEnabled(false);
        chart.setPieChartData(data);

        Legend upLegend = (Legend) view.findViewById(R.id.up_distance);
        Legend downLegend = (Legend) view.findViewById(R.id.down_distance);
        Legend plainLegend = (Legend) view.findViewById(R.id.plain_distance);

        String upPercent = String.format(Locale.US, "%.2f%%", upDistance / distance * 100);
        String downPercent = String.format(Locale.US, "%.2f%%", downDistance / distance * 100);
        String plainPercent = String.format(Locale.US, "%.2f%%", plainDistance / distance * 100);

        String fmtUpDistance = getFormattedDistance((int) upDistance);
        String fmtDownDistance = getFormattedDistance((int) downDistance);
        String fmtPlainDistance = getFormattedDistance((int) plainDistance);

        Log.v(TAG, "onCreateView: up% = " + upPercent);
        Log.v(TAG, "onCreateView: down% = " + downPercent);
        Log.v(TAG, "onCreateView: plain% = " + plainPercent);

        upLegend.setValue(fmtUpDistance);
        upLegend.setPercents(upPercent);

        downLegend.setValue(fmtDownDistance);
        downLegend.setPercents(downPercent);

        plainLegend.setValue(fmtPlainDistance);
        plainLegend.setPercents(plainPercent);
        return view;
    }

    private String getFormattedDistance(int distance) {
        String km = getString(R.string.km);
        String m = getString(R.string.m);

        return String.format(Locale.US, "%d %s %d %s", distance / 1000, km, distance % 1000, m);
    }

    public static StatsViewAscendTabFragment getInstance(SummaryTable.Record record) {
        StatsViewAscendTabFragment fragment = new StatsViewAscendTabFragment();
        fragment.setData(record);
        return fragment;
    }

    public void setData(SummaryTable.Record record) {
        this.mSummaryRecord = record;
    }
}
