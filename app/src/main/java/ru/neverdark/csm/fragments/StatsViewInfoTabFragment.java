package ru.neverdark.csm.fragments;


import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import ru.neverdark.csm.R;
import ru.neverdark.csm.data.ActivityTypes;
import ru.neverdark.csm.db.SummaryTable;
import ru.neverdark.csm.utils.Utils;

/**
 * A simple {@link Fragment} subclass.
 */
public class StatsViewInfoTabFragment extends Fragment implements View.OnClickListener {
    private SummaryTable.Record mSummaryRecord;
    private TextView mDistanceTv;
    private TextView mTotalTimeTv;
    private TextView mAverageSpeedTv;
    private TextView mMaxSpeedTv;
    private TextView mMaxAltitudeTv;
    private TextView mFinishTimeTv;
    private TextView mDescriptionTv;
    private TextView mUpAltitudeTv;
    private TextView mDownAltitudeTv;
    private TextView mFinishDateTv;
    private TextView mActivityTypeTv;
    private TextView mPauseDurationTv;
    private TextView mPauseDurationTitle;
    private TextView mTotalTimeTitle;

    public StatsViewInfoTabFragment() {
        // Required empty public constructor
    }

    private static final String TAG = "StatsViewInfoTabFragmen";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        Log.v(TAG, "onCreateView: ");
        View view = inflater.inflate(R.layout.fragment_stats_view_info_tab, container, false);
        bindObjects(view);
        bindData();

        return view;
    }

    private void bindData() {
        String kmch = getString(R.string.kmch);
        String km = getString(R.string.km);
        String m = getString(R.string.m);

        Date date = new Date(mSummaryRecord.finish_date);

        String distanceStr = String.format(Locale.US, "%d %s %d %s", mSummaryRecord.distance / 1000, km, mSummaryRecord.distance % 1000, m);
        String maxSpeedStr = String.format(Locale.US, "%.2f %s", Utils.convertMeterToKm(mSummaryRecord.max_speed), kmch);
        String averageSpeedStr = String.format(Locale.US, "%.2f %s", Utils.convertMeterToKm(mSummaryRecord.average_speed), kmch);
        String maxAltitudeStr = String.format(Locale.US, "%d %s", mSummaryRecord.max_altitude, m);
        String upAltitudeStr = String.format(Locale.US, "%d %s", mSummaryRecord.up_altitude, m);
        String downAltitudeStr = String.format(Locale.US, "%d %s", mSummaryRecord.down_altitude, m);
        String finishDateStr = getDate(date);
        String finishTimeStr = getTime(date);

        mDistanceTv.setText(distanceStr);
        mTotalTimeTv.setText(mSummaryRecord.total_time);
        mAverageSpeedTv.setText(averageSpeedStr);
        mMaxSpeedTv.setText(maxSpeedStr);
        mMaxAltitudeTv.setText(maxAltitudeStr);
        mFinishTimeTv.setText(finishTimeStr);
        mFinishDateTv.setText(finishDateStr);
        mDescriptionTv.setText(mSummaryRecord.description);
        mUpAltitudeTv.setText(upAltitudeStr);
        mDownAltitudeTv.setText(downAltitudeStr);

        if (mSummaryRecord.pause_duration != null) {
            mPauseDurationTv.setVisibility(View.VISIBLE);
            mPauseDurationTitle.setVisibility(View.VISIBLE);
            mPauseDurationTv.setText(mSummaryRecord.pause_duration);
            mTotalTimeTitle.setText(R.string.clean_time);
        } else {
            mTotalTimeTitle.setText(R.string.total_time);
        }

        updateActivityType(mSummaryRecord.activity_type);
    }

    public void updateActivityType(int activityType) {
        String activity = ActivityTypes.getTextName(getContext(), activityType);
        int activityDrawable = ActivityTypes.getActivityIconByType(activityType);

        mActivityTypeTv.setText(activity);
        mActivityTypeTv.setCompoundDrawablesWithIntrinsicBounds(activityDrawable, 0, 0, 0);
    }

    private String getTime(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone(mSummaryRecord.timezone));
        return sdf.format(date);
    }

    private String getDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd.MM.yyyy", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone(mSummaryRecord.timezone));
        return sdf.format(date);
    }

    private void bindObjects(View view) {
        mDistanceTv = (TextView) view.findViewById(R.id.distance_value);
        mTotalTimeTv = (TextView) view.findViewById(R.id.total_time_value);
        mAverageSpeedTv = (TextView) view.findViewById(R.id.average_speed_value);
        mMaxSpeedTv = (TextView) view.findViewById(R.id.max_speed_value);
        mMaxAltitudeTv = (TextView) view.findViewById(R.id.max_altitude_value);
        mFinishTimeTv = (TextView) view.findViewById(R.id.finish_time_value);
        mFinishDateTv = (TextView) view.findViewById(R.id.finish_date_value);
        mDescriptionTv = (TextView) view.findViewById(R.id.description);
        mUpAltitudeTv = (TextView) view.findViewById(R.id.up_altitude_value);
        mDownAltitudeTv = (TextView) view.findViewById(R.id.down_altitude_value);
        mActivityTypeTv = (TextView) view.findViewById(R.id.activity_type);
        mPauseDurationTv = (TextView) view.findViewById(R.id.pause_duration_value);
        mPauseDurationTitle = (TextView) view.findViewById(R.id.pause_duration);
        mTotalTimeTitle = (TextView) view.findViewById(R.id.total_time);

        mDescriptionTv.setOnClickListener(this);
    }

    public static StatsViewInfoTabFragment getInstance(SummaryTable.Record record) {
        StatsViewInfoTabFragment fragment = new StatsViewInfoTabFragment();
        fragment.mSummaryRecord = record;
        return fragment;
    }

    public void updateDesription(String newDescription) {
        mDescriptionTv.setText(newDescription);
    }

    public interface OnDescriptionListener {
        void onDescriptionClick();
    }

    private OnDescriptionListener mCallback;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            // null или нет в зависимости от конструктора
            if (mCallback == null) {
                mCallback = (OnDescriptionListener) context;
            }
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnDescriptionListener");

        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.description:
                mCallback.onDescriptionClick();
                break;
        }
    }
}
