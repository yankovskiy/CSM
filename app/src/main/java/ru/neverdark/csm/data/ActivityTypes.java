package ru.neverdark.csm.data;

import android.content.Context;

import ru.neverdark.csm.R;

/**
 * Created by ufo on 23.03.17.
 */

public class ActivityTypes {
    public static final int UNKNOWN = 0;
    public static final int WALKING = 1;
    public static final int NORDIC_WALKING = 1 << 1;
    public static final int HIKING = 1 << 2;
    public static final int RUN = 1 << 3;
    public static final int ROAD_BIKE = 1 << 4;
    public static final int MTB = 1 << 5;

    public static int getActivityTypeByIcon(int drawable) {
        switch (drawable) {
            case R.drawable.ic_walking:
                return WALKING;
            case R.drawable.ic_nordic_walking:
                return NORDIC_WALKING;
            case R.drawable.ic_hiking:
                return HIKING;
            case R.drawable.ic_run:
                return RUN;
            case R.drawable.ic_road_bike:
                return ROAD_BIKE;
            case R.drawable.ic_mtb:
                return MTB;
            default:
                return UNKNOWN;
        }
    }

    public static int getActivityIconByType(int activity_type) {
        switch (activity_type) {
            case WALKING:
                return R.drawable.ic_walking_gray;
            case NORDIC_WALKING:
                return R.drawable.ic_nordic_walking_gray;
            case HIKING:
                return R.drawable.ic_hiking_gray;
            case RUN:
                return R.drawable.ic_run_gray;
            case ROAD_BIKE:
                return R.drawable.ic_road_bike_gray;
            case MTB:
                return R.drawable.ic_mtb_gray;
            default:
                return UNKNOWN;
        }
    }

    public static String getTextName(Context context, int activity_type) {
        switch (activity_type) {
            case WALKING:
                return context.getString(R.string.walking);
            case NORDIC_WALKING:
                return context.getString(R.string.nordic_walking);
            case HIKING:
                return context.getString(R.string.hiking);
            case RUN:
                return context.getString(R.string.run);
            case ROAD_BIKE:
                return context.getString(R.string.road_bike);
            case MTB:
                return context.getString(R.string.mtb);
            default:
                return null;
        }
    }
}
