package ru.neverdark.csm.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.android.gms.maps.GoogleMap;

import ru.neverdark.csm.R;

public final class Settings {
    private static final String FILE_NAME = "prefs";
    private static final String MAP_TYPE = "map-type";
    private static final String MAP_ZOOM = "map-zoom";
    private static final String APP_IS_MODERN_COORDINATE_SYSTEM = "cb_format_coordinates";
    private static final String ACTIVITY_TYPE_ICON = "activity-type-icon";
    private static final String APP_IS_AUTOPAUSE = "cb_autopause";

    private static Settings mInstance;
    private final SharedPreferences mPrefs;
    private final SharedPreferences.Editor mEditor;
    private final SharedPreferences mAppSettings;

    public static Settings getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new Settings(context);
        }

        return mInstance;
    }

    private Settings(Context context) {
        mPrefs = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        mEditor = mPrefs.edit();
        mAppSettings = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void saveMapType(int mapType) {
        mEditor.putInt(MAP_TYPE, mapType);
        mEditor.apply();
    }

    public int loadMapType() {
        return mPrefs.getInt(MAP_TYPE, GoogleMap.MAP_TYPE_NORMAL);
    }

    public void saveMapZoom(float zoom) {
        mEditor.putFloat(MAP_ZOOM, zoom);
        mEditor.apply();
    }

    public float loadMapZoom() {
        return mPrefs.getFloat(MAP_ZOOM, 15);
    }

    public void saveActivityTypeIcon(int drawable) {
        mEditor.putInt(ACTIVITY_TYPE_ICON, drawable);
        mEditor.apply();
    }

    public int loadActivityTypeIcon() {
        return mPrefs.getInt(ACTIVITY_TYPE_ICON, R.drawable.ic_road_bike);
    }

    public boolean isModernCoordinateSystem() {
        return mAppSettings.getBoolean(APP_IS_MODERN_COORDINATE_SYSTEM, false);
    }

    public boolean isAutopauseEnabled() {
        return mAppSettings.getBoolean(APP_IS_AUTOPAUSE, false);
    }
}
