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

import lecho.lib.hellocharts.formatter.SimpleColumnChartValueFormatter;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Column;
import lecho.lib.hellocharts.model.ColumnChartData;
import lecho.lib.hellocharts.model.PieChartData;
import lecho.lib.hellocharts.model.SliceValue;
import lecho.lib.hellocharts.model.SubcolumnValue;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.view.ColumnChartView;
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
        initAvgSpeedChart(view, green, blue, orange);
        initMaxSpeedChart(view, green, blue, orange);

        return view;
    }

    private void initMaxSpeedChart(View view, int descendColor, int plainColor, int ascendColor) {
        float ascendSpeed = mSummaryRecord.ascend_max_speed;
        float descendSpeed = mSummaryRecord.descend_max_speed;
        float plainSpeed = mSummaryRecord.plain_max_speed;
        int chartResId = R.id.max_speed_chart;

        initSpeedChart(view, descendColor, plainColor, ascendColor, ascendSpeed, descendSpeed, plainSpeed, chartResId);
    }

    private void initAvgSpeedChart(View view, int descendColor, int plainColor, int ascendColor) {
        float ascendSpeed = mSummaryRecord.ascend_average_speed;
        float descendSpeed = mSummaryRecord.descend_average_speed;
        float plainSpeed = mSummaryRecord.plain_average_speed;
        int chartResId = R.id.average_speed_chart;

        initSpeedChart(view, descendColor, plainColor, ascendColor, ascendSpeed, descendSpeed, plainSpeed, chartResId);
    }

    private void initSpeedChart(View view, int descendColor, int plainColor, int ascendColor, float ascendSpeed, float descendSpeed, float plainSpeed, int chartResId) {
        List<Column> columns = new ArrayList<>();
        List<SubcolumnValue> values;

        values = new ArrayList<>();
        values.add(new SubcolumnValue(Utils.convertMeterToKm(ascendSpeed), ascendColor));
        columns.add(new Column(values).setHasLabelsOnlyForSelected(true).setFormatter(new SimpleColumnChartValueFormatter(2)));

        values = new ArrayList<>();
        values.add(new SubcolumnValue(Utils.convertMeterToKm(descendSpeed), descendColor));
        columns.add(new Column(values).setHasLabelsOnlyForSelected(true).setFormatter(new SimpleColumnChartValueFormatter(2)));

        values = new ArrayList<>();
        values.add(new SubcolumnValue(Utils.convertMeterToKm(plainSpeed), plainColor));
        columns.add(new Column(values).setHasLabelsOnlyForSelected(true).setFormatter(new SimpleColumnChartValueFormatter(2)));

        List<AxisValue> axisXValues = new ArrayList<>();
        axisXValues.add(new AxisValue(0).setLabel(getString(R.string.uphill)));
        axisXValues.add(new AxisValue(1).setLabel(getString(R.string.downhill)));
        axisXValues.add(new AxisValue(2).setLabel(getString(R.string.plain)));

        Axis axisX = new Axis();
        axisX.setValues(axisXValues);
        axisX.setInside(false);

        Axis axisY = new Axis().setHasLines(true);
        axisY.setName(String.format("%s [%s]", getString(R.string.speed), getString(R.string.kmch)));
//        axisY.setFormatter(new SimpleAxisValueFormatter().setAppendedText(getString(R.string.kmch).toCharArray()));
        axisY.setHasLines(true);
        axisY.setInside(true);
        axisY.setMaxLabelChars(8);

        ColumnChartData data = new ColumnChartData(columns);
        data.setAxisXBottom(axisX);
        data.setAxisYLeft(axisY);

        ColumnChartView chart = (ColumnChartView) view.findViewById(chartResId);
        chart.setColumnChartData(data);

        Viewport v = new Viewport(chart.getMaximumViewport());
        v.top += 1;
        v.left -= 0.3;
        chart.setMaximumViewport(v);
        chart.setCurrentViewport(v);
        chart.setZoomEnabled(false);
        chart.setValueSelectionEnabled(true);
    }

    private void initTimeChart(View view, int descendColor, int plainColor, int ascendColor) {
        float ascendTime = mSummaryRecord.ascend_time;
        float descendTime = mSummaryRecord.descend_time;
        float plainTime = mSummaryRecord.plain_time;
        float totalTime = mSummaryRecord.ascend_time + mSummaryRecord.descend_time + mSummaryRecord.plain_time;

        List<SliceValue> values = new ArrayList<>();
        values.add(new SliceValue(ascendTime, ascendColor));
        values.add(new SliceValue(descendTime, descendColor));
        values.add(new SliceValue(plainTime, plainColor));

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

    private void initDistanceChart(View view, int descendColor, int plainColor, int ascendColor) {
        float upDistance = mSummaryRecord.up_distance;
        float downDistance = mSummaryRecord.down_distance;
        float plainDistance = mSummaryRecord.distance - mSummaryRecord.up_distance - mSummaryRecord.down_distance;
        float distance = mSummaryRecord.distance;

        List<SliceValue> values = new ArrayList<>();
        values.add(new SliceValue(upDistance, ascendColor));
        values.add(new SliceValue(downDistance, descendColor));
        values.add(new SliceValue(plainDistance, plainColor));

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
