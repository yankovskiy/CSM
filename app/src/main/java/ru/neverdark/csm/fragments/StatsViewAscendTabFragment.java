package ru.neverdark.csm.fragments;


import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import lecho.lib.hellocharts.model.PieChartData;
import lecho.lib.hellocharts.model.SliceValue;
import lecho.lib.hellocharts.view.PieChartView;
import ru.neverdark.csm.R;
import ru.neverdark.csm.db.SummaryTable;
import ru.neverdark.csm.utils.Utils;
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

        initDistanceChart(view, green, blue, orange);
        initTimeChart(view, green, blue, orange);
        initGradientData(view);

        return view;
    }

    private void initGradientData(View view) {
        TextView maxAscendGradient = (TextView) view.findViewById(R.id.max_ascend_gradient_value);
        TextView avgAscendGradient = (TextView) view.findViewById(R.id.avg_ascend_gradient_value);
        TextView maxDescendGradient = (TextView) view.findViewById(R.id.max_descend_gradient_value);
        TextView avgDescendGradient = (TextView) view.findViewById(R.id.avg_descend_gradient_value);

        maxAscendGradient.setText(String.format(Locale.US, "%d %%", mSummaryRecord.max_ascend_gradient));;
        avgAscendGradient.setText(String.format(Locale.US, "%d %%", mSummaryRecord.average_ascend_gradient));
        maxDescendGradient.setText(String.format(Locale.US, "%d %%", mSummaryRecord.max_descend_gradient));
        avgDescendGradient.setText(String.format(Locale.US, "%d %%", mSummaryRecord.average_descend_gradient));
    }

    private void initTimeChart(View view, int green, int blue, int orange) {
        float ascendTime = mSummaryRecord.ascend_time;
        float descendTime = mSummaryRecord.descend_time;
        float plainTime = mSummaryRecord.plain_time;
        float totalTime = mSummaryRecord.ascend_time + mSummaryRecord.descend_time + mSummaryRecord.plain_time;

        List<SliceValue> values = new ArrayList<>();
        values.add(new SliceValue(ascendTime, orange));
        values.add(new SliceValue(descendTime, green));
        values.add(new SliceValue(plainTime, blue));

        PieChartData data = new PieChartData(values);

        PieChartView distanceChart = (PieChartView) view.findViewById(R.id.time_chart);
        distanceChart.setChartRotationEnabled(false);
        distanceChart.setValueTouchEnabled(false);
        distanceChart.setPieChartData(data);

        Legend ascendTimeLegend = (Legend) view.findViewById(R.id.ascend_time);
        Legend descendTimeLegend = (Legend) view.findViewById(R.id.descend_time);
        Legend plainTimeLegend = (Legend) view.findViewById(R.id.plain_time);

        String ascendTimePercent = String.format(Locale.US, "%.2f%%", ascendTime / totalTime * 100);
        String descendTimePercent = String.format(Locale.US, "%.2f%%", descendTime / totalTime * 100);
        String plainTimePercent = String.format(Locale.US, "%.2f%%", plainTime / totalTime * 100);

        String fmtAscendTime = Utils.convertMillisToTime(mSummaryRecord.ascend_time);
        String fmtDescendTime = Utils.convertMillisToTime(mSummaryRecord.descend_time);
        String fmtPlainDistance = Utils.convertMillisToTime(mSummaryRecord.plain_time);

        ascendTimeLegend.setValue(fmtAscendTime);
        ascendTimeLegend.setPercents(ascendTimePercent);

        descendTimeLegend.setValue(fmtDescendTime);
        descendTimeLegend.setPercents(descendTimePercent);

        plainTimeLegend.setValue(fmtPlainDistance);
        plainTimeLegend.setPercents(plainTimePercent);
    }

    private void initDistanceChart(View view, int green, int blue, int orange) {
        float upDistance = mSummaryRecord.up_distance;
        float downDistance = mSummaryRecord.down_distance;
        float plainDistance = mSummaryRecord.distance - mSummaryRecord.up_distance - mSummaryRecord.down_distance;
        float distance = mSummaryRecord.distance;

        List<SliceValue> values = new ArrayList<>();
        values.add(new SliceValue(upDistance, orange));
        values.add(new SliceValue(downDistance, green));
        values.add(new SliceValue(plainDistance, blue));

        PieChartData data = new PieChartData(values);

        PieChartView distanceChart = (PieChartView) view.findViewById(R.id.distance_chart);
        distanceChart.setChartRotationEnabled(false);
        distanceChart.setValueTouchEnabled(false);
        distanceChart.setPieChartData(data);

        Legend upLegend = (Legend) view.findViewById(R.id.up_distance);
        Legend downLegend = (Legend) view.findViewById(R.id.down_distance);
        Legend plainLegend = (Legend) view.findViewById(R.id.plain_distance);

        String upPercent = String.format(Locale.US, "%.2f%%", upDistance / distance * 100);
        String downPercent = String.format(Locale.US, "%.2f%%", downDistance / distance * 100);
        String plainPercent = String.format(Locale.US, "%.2f%%", plainDistance / distance * 100);

        String fmtUpDistance = getFormattedDistance((int) upDistance);
        String fmtDownDistance = getFormattedDistance((int) downDistance);
        String fmtPlainDistance = getFormattedDistance((int) plainDistance);

        upLegend.setValue(fmtUpDistance);
        upLegend.setPercents(upPercent);

        downLegend.setValue(fmtDownDistance);
        downLegend.setPercents(downPercent);

        plainLegend.setValue(fmtPlainDistance);
        plainLegend.setPercents(plainPercent);
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
