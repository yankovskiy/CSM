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

import lecho.lib.hellocharts.formatter.SimpleAxisValueFormatter;
import lecho.lib.hellocharts.gesture.ContainerScrollType;
import lecho.lib.hellocharts.gesture.ZoomType;
import lecho.lib.hellocharts.listener.ViewportChangeListener;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.view.LineChartView;
import lecho.lib.hellocharts.view.PreviewLineChartView;
import ru.neverdark.csm.R;
import ru.neverdark.csm.db.GpslogTable;
import ru.neverdark.csm.utils.Constants;
import ru.neverdark.csm.utils.Utils;

/**
 * A simple {@link Fragment} subclass.
 */
public class StatsViewGraphTabFragment extends Fragment {
    private LineChartView mChart;
    private LineChartData mChartData;
    private LineChartData mPreviewChartData;
    private float mSpeedRange;

    public StatsViewGraphTabFragment() {
        // Required empty public constructor
    }

    private static final String TAG = "StatsViewGraphTabFragme";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        Log.v(TAG, "onCreateView: ");
        View view = inflater.inflate(R.layout.fragment_stats_view_graph_tab, container, false);

        initChart(view);
        initPreviewChart(view);

        return view;
    }

    private void initPreviewChart(View view) {
        PreviewLineChartView previewChart = (PreviewLineChartView) view.findViewById(R.id.preview_altitude_chart);
        previewChart.setLineChartData(mPreviewChartData);

        Viewport tempViewport = new Viewport(mChart.getMaximumViewport());
        tempViewport.top = mSpeedRange + mSpeedRange * 0.1f;
        previewChart.setMaximumViewport(tempViewport);
        previewChart.setCurrentViewport(tempViewport);

        float dx = tempViewport.width() / 3;
        tempViewport.inset(dx, 0);
        previewChart.setCurrentViewportWithAnimation(tempViewport);
        previewChart.setZoomType(ZoomType.HORIZONTAL);
        previewChart.setViewportChangeListener(new ViewportListener());
        previewChart.setPreviewColor(ContextCompat.getColor(getContext(), R.color.textColorHints));
        previewChart.setContainerScrollEnabled(true, ContainerScrollType.HORIZONTAL);
    }

    private void initChart(View view) {
        mChart = (LineChartView) view.findViewById(R.id.altitude_chart);
        mChart.setLineChartData(mChartData);
        mChart.setZoomEnabled(false);
        mChart.setScrollEnabled(false);
        Viewport tempViewport = new Viewport(mChart.getMaximumViewport());
        tempViewport.top = mSpeedRange + mSpeedRange * 0.1f;
        mChart.setMaximumViewport(tempViewport);
        mChart.setCurrentViewport(tempViewport);
    }

    public static StatsViewGraphTabFragment getInstance(Context context, List<GpslogTable.TrackRecord> points) {
        StatsViewGraphTabFragment fragment = new StatsViewGraphTabFragment();
        fragment.prepareData(context, points);
        return fragment;
    }

    private static class MaxMin {
        float max_speed;
        double min_altitude;
        double max_altitude;

        MaxMin(List<GpslogTable.TrackRecord> points) {
            for (GpslogTable.TrackRecord record : points) {
                if (max_speed < record.speed) {
                    max_speed = record.speed;
                }

                if (max_altitude < record.altitude) {
                    max_altitude = record.altitude;
                }

                if (min_altitude > record.altitude) {
                    min_altitude = record.altitude;
                }
            }

            Log.v(TAG, "MaxMin: max speed = " + max_speed);
            Log.v(TAG, "MaxMin: max altitude = " + max_altitude);
            Log.v(TAG, "MaxMin: min altitude = " + min_altitude);
        }
    }

    private void prepareData(Context context, List<GpslogTable.TrackRecord> points) {
        List<PointValue> elevationList = new ArrayList<>();
        List<PointValue> speedList = new ArrayList<>();

        MaxMin maxMin = new MaxMin(points);

        mSpeedRange = Utils.convertMeterToKm(maxMin.max_speed);
        double minHeight = maxMin.min_altitude;
        double maxHeight = maxMin.max_altitude;

        double scale = mSpeedRange / maxHeight;
        double sub = (minHeight * scale) / 2;

        double totalDistance = 0;
        float speed = 0;
        int count = 0;
        double altitude = 0;
        for (GpslogTable.TrackRecord record : points) {
            totalDistance += record.distance;
            altitude += record.altitude;
            count++;
            speed = record.speed;
            if (Constants.AVERAGE_ALTITUDE_SEGMENT_COUNT == count) {
                double normalizedHeight = (altitude / count) * scale - sub;
                PointValue elevationValue = new PointValue((float) totalDistance, (float) normalizedHeight);
                PointValue speedValue = new PointValue((float) totalDistance, Utils.convertMeterToKm(speed));
                elevationList.add(elevationValue);
                speedList.add(speedValue);
                altitude = 0;
                count = 0;
            }
        }

        if (count > 0) {
            double normalizedHeight = (altitude / count) * scale - sub;
            PointValue elevationValue = new PointValue((float) totalDistance, (float) normalizedHeight);
            PointValue speedValue = new PointValue((float) totalDistance, Utils.convertMeterToKm(speed));
            speedList.add(speedValue);
            elevationList.add(elevationValue);
        }

        int elevationColor = ContextCompat.getColor(context, R.color.blue_400);
        Line elevationLine = new Line(elevationList);
        elevationLine.setColor(elevationColor);
        elevationLine.setCubic(true);
        elevationLine.setFilled(true);
        elevationLine.setHasPoints(false);
        elevationLine.setStrokeWidth(1);

        int speedColor = ContextCompat.getColor(context, R.color.orange_400);
        Line speedLine = new Line(speedList);
        speedLine.setColor(speedColor);
        speedLine.setCubic(true);
        speedLine.setHasPoints(false);
        speedLine.setStrokeWidth(1);

        List<Line> lines = new ArrayList<>();
        lines.add(elevationLine);
        lines.add(speedLine);

        Axis axisX = new Axis();

        Axis axisYLeft = new Axis();
        axisYLeft.setHasLines(true);
        axisYLeft.setTextColor(elevationColor);
        axisYLeft.setFormatter(new ElevationValueFormatter(scale, sub, 0));

        Axis axisYRight = new Axis();
        axisYRight.setTextColor(speedColor);

        mChartData = new LineChartData(lines);
        mChartData.setAxisXBottom(axisX);
        mChartData.setAxisYLeft(axisYLeft);
        mChartData.setAxisYRight(axisYRight);

        mPreviewChartData = new LineChartData(mChartData);
        mPreviewChartData.getAxisXBottom().setName(String.format("%s [%s]", context.getString(R.string.distance), context.getString(R.string.m)));
        mPreviewChartData.getAxisYLeft().setName(String.format("%s [%s]", context.getString(R.string.altitude), context.getString(R.string.m)));
        mPreviewChartData.getAxisYRight().setName(String.format("%s [%s]", context.getString(R.string.speed), context.getString(R.string.kmch)));
    }

    private class ViewportListener implements ViewportChangeListener {
        @Override
        public void onViewportChanged(Viewport viewport) {
            mChart.setCurrentViewport(viewport);
        }
    }

    private static class ElevationValueFormatter extends SimpleAxisValueFormatter {
        private final double mScale;
        private final double mSub;
        private final int mDecimalDigits;

        ElevationValueFormatter(double scale, double sub, int decimalDigits) {
            mScale = scale;
            mSub = sub;
            mDecimalDigits = decimalDigits;
        }

        @Override
        public int formatValueForAutoGeneratedAxis(char[] formattedValue, float value, int autoDecimalDigits) {
            double scaledValue = (value + mSub) / mScale;
            return super.formatValueForAutoGeneratedAxis(formattedValue, (float) scaledValue, mDecimalDigits);
        }
    }
}
